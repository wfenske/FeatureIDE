/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.featurehouse.signature.custom;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.signature.ProjectSignatures;
import de.ovgu.featureide.core.signature.base.AbstractClassSignature;
import de.ovgu.featureide.core.signature.base.AbstractSignature;
import de.ovgu.featureide.core.signature.base.FOPFeatureData;
import de.ovgu.featureide.core.signature.base.FeatureDataConstructor;
import de.ovgu.featureide.core.signature.base.PreprocessorFeatureData;

/**
 * Collects all signatures in a FeatureHouse project.
 */
public abstract class FeatureHouseSignatureBuilder {

	private static String readFile(IFile file) throws CoreException {
		final int bufferSize = 1024;
		final char[] buffer = new char[bufferSize];
		final StringBuilder contents = new StringBuilder();

		try (final InputStreamReader in = new InputStreamReader(file.getContents())) {
			while (true) {
				final int readChars = in.read(buffer, 0, buffer.length);
				if (readChars < 0) {
					break;
				}
				contents.append(buffer, 0, readChars);
			}
		} catch (final IOException e) {
			CorePlugin.getDefault().logError(e);
		}
		return contents.toString();
	}

	@SuppressWarnings("unused")
	private static Collection<AbstractSignature> parse(ProjectSignatures projectSignatures, ASTNode root) {
		final HashMap<AbstractSignature, AbstractSignature> map = new HashMap<>();

		final CompilationUnit cu = (CompilationUnit) root;
		final PackageDeclaration pckgDecl = cu.getPackage();
		final String packageName = (pckgDecl == null) ? null : pckgDecl.getName().getFullyQualifiedName();
		final List<?> l = cu.getCommentList();
		final List<Javadoc> cl = new LinkedList<>();
		for (final Object object : l) {
			if (object instanceof Javadoc) {
				final Javadoc comment = (Javadoc) object;
				cl.add(comment);
			}
		}

		final ListIterator<Javadoc> it = cl.listIterator();
		final FeatureDataConstructor featureDataConstructor = new FeatureDataConstructor(projectSignatures, FeatureDataConstructor.TYPE_PP);

		root.accept(new ASTVisitor() {

			private BodyDeclaration curDeclaration = null;
			private PreprocessorFeatureData curfeatureData = null;
			private String lastComment = null;
			private MethodDeclaration lastCommentedMethod = null;

			@Override
			public boolean visit(Javadoc node) {
				if (curDeclaration != null) {
					final StringBuilder sb = new StringBuilder();
					while (it.hasNext()) {
						final Javadoc comment = it.next();
						if (comment.getStartPosition() <= curDeclaration.getStartPosition()) {
							sb.append(comment);
							sb.append("\n");
						} else {
							it.previous();
							break;
						}
					}
					lastComment = sb.toString();

					curfeatureData.setComment(lastComment);
					lastCommentedMethod = (curDeclaration instanceof MethodDeclaration) ? (MethodDeclaration) curDeclaration : null;
				}
				return false;
			}

			private void attachFeatureData(AbstractSignature curSignature, BodyDeclaration curDeclaration) {
				this.curDeclaration = curDeclaration;
				final Javadoc javadoc = curDeclaration.getJavadoc();
				final int startPosition = (javadoc == null) ? curDeclaration.getStartPosition() : curDeclaration.getStartPosition() + javadoc.getLength();
				curfeatureData = (PreprocessorFeatureData) featureDataConstructor.create(null, unit.getLineNumber(startPosition),
						unit.getLineNumber(curDeclaration.getStartPosition() + curDeclaration.getLength()));
				curSignature.setFeatureData(curfeatureData);
				map.put(curSignature, curSignature);
			}

			@Override
			public boolean visit(CompilationUnit unit) {
				this.unit = unit;
				return true;
			}

			CompilationUnit unit = null;

			@Override
			public boolean visit(MethodDeclaration node) {
				System.out.println(" This is the method call at the moment: " + node.toString());
				if (node != null) {

					final int pos = unit.getLineNumber(node.getBody().getStartPosition());
					final int end = unit.getLineNumber(node.getBody().getStartPosition() + node.getBody().getLength());

					FeatureHouseMethodSignature methodSignature = null;
					try {
						methodSignature = new FeatureHouseMethodSignature(getParent(node.getParent()), node.getName().toString(), node.getModifiers(),
								node.getReturnType2().toString(), node.parameters(), node.isConstructor(), pos, end);
					} catch (final JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//
					attachFeatureData(methodSignature, node);
				}

				if ((node.getJavadoc() == null) && (lastCommentedMethod != null) && lastCommentedMethod.getName().equals(node.getName())) {
					curfeatureData.setComment(lastComment);
				} else {
					lastCommentedMethod = null;
				}
				return true;

			}

			private AbstractClassSignature getParent(ASTNode astnode) throws JavaModelException {
				final AbstractClassSignature sig;
				if (astnode instanceof IType) {
					final IType node = (IType) astnode;
					sig = new FeatureHouseClassSignature(null, node.getElementName(), node.getFlags(), node.isInterface() ? "interface" : "class", packageName,
							node, null);
				} else {
					return null;
				}
				final AbstractClassSignature uniqueSig = (AbstractClassSignature) map.get(sig);
				if (uniqueSig == null) {
					visit((Javadoc) astnode);
				}
				return uniqueSig;
			}

			@Override
			public boolean visit(FieldDeclaration node) {
				for (final Iterator<?> it = node.fragments().iterator(); it.hasNext();) {
					final VariableDeclarationFragment fragment = (VariableDeclarationFragment) it.next();

					FeatureHouseFieldSignature fieldSignature = null;
					try {
						fieldSignature = new FeatureHouseFieldSignature(getParent(node.getParent()), fragment.getName().toString(), node.getModifiers(),
								(IType) node.getType());
					} catch (final JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					attachFeatureData(fieldSignature, node);
				}

				return true;
			}

//			@Override
//			public boolean visit(TypeDeclaration node) {
//				final FeatureHouseClassSignature classSignature = new FeatureHouseClassSignature(getParent(node.getParent()), node.getName().getIdentifier(),
//						node.getModifiers(), node.isInterface() ? "interface" : "class", packageName, node, null);
//
//				attachFeatureData(classSignature, node);
//
//				return super.visit(node);
////			}
//
		});
		return map.keySet();
	}

//	public static ProjectSignatures build(IFeatureProject featureProject) {
//		final ProjectSignatures projectSignatures = new ProjectSignatures(featureProject.getFeatureModel());
//		final ArrayList<AbstractSignature> signatureList = new ArrayList<>();
//
//		@SuppressWarnings("deprecation")
//		final ASTParser parser = ASTParser.newParser(AST.JLS4);
//
//		final IFolder sourceFolder = featureProject.getSourceFolder();
//		try {
//			sourceFolder.accept(new IResourceVisitor() {
//
//				@Override
//				public boolean visit(IResource resource) throws CoreException {
//					if (resource instanceof IFolder) {
//						return true;
//					} else if (resource instanceof IFile) {
//						final char[] content = readFile((IFile) resource).toCharArray();
//						if (content.length > 0) {
//							parser.setSource(content);
//							signatureList.addAll(parse(projectSignatures, parser.createAST(null)));
//						}
//					}
//
//					return false;
//				}
//			});
//		} catch (final CoreException e) {
//			CorePlugin.getDefault().logError(e);
//		}
//
//		projectSignatures.setSignatureArray(signatureList.toArray(new AbstractSignature[0]));
//		return projectSignatures;
//	}

	/**
	 * The incomplete implementation below is an attempt to create a replication of rename refactoring via Fuji/ExtendJ with default eclipse plugins API. The
	 * default/ previously used approach is the code snippet comment above.
	 **/

	private final static HashMap<AbstractSignature, SignatureReference> signatureSet = new HashMap<AbstractSignature, SignatureReference>();
	private final static HashMap<String, AbstractSignature> signatureTable = new HashMap<String, AbstractSignature>();

	private static FeatureDataConstructor featureDataConstructor = null;

	public static ProjectSignatures build(IFeatureProject featureProject) throws JavaModelException {
		final ProjectSignatures projectSignatures = new ProjectSignatures(featureProject.getFeatureModel());
		final ArrayList<AbstractSignature> signatureList = new ArrayList<>();

		featureDataConstructor = new FeatureDataConstructor(projectSignatures, FeatureDataConstructor.TYPE_FOP);

		final LinkedList<IType> stack = new LinkedList<IType>();
		final LinkedList<AbstractClassSignature> roleStack = new LinkedList<AbstractClassSignature>();

		final IProject project = featureProject.getProject();

		// get features from the project

		final IJavaProject javaProject = JavaCore.create(project);

		// get package fragments from project

		final IPackageFragment[] packages = javaProject.getPackageFragments();
		for (final IPackageFragment myPackage : packages) {

			// get compilation units
			for (final ICompilationUnit unit : myPackage.getCompilationUnits()) {

				final String featurename = featureProject.getFeatureName(unit.getResource());

//				final TypeDeclaration[] typeDeclList = (TypeDeclaration[]) unit.getTypes();

				final IType[] types = unit.getTypes();

				final String pckg = "package"; // unit.getPackageDeclaration(null).toString();

				final IImportDeclaration[] imports = unit.getImports();
				final List<IImportDeclaration> importList = new ArrayList<>();
				for (final IImportDeclaration importe : imports) {
					importList.add(importe);
				}

				for (int i = 0; i < types.length; i++) {

					final IType rootTypeDecl = types[i];
					stack.push(rootTypeDecl);

					do {

						final IType typeDecl = stack.pop();

						String name = typeDecl.getFullyQualifiedName();

						final String modifierString = "";
//							String modifierString = classModifierSB.toString();

						String typeString = null;
						if (typeDecl instanceof ClassDeclaration) {
							typeString = "class";
						} else if (typeDecl instanceof InterfaceDeclaration) {
							typeString = "interface";
						}
						AbstractClassSignature parent = null;
						if (!roleStack.isEmpty()) {
							parent = roleStack.pop();
						}

						final ISourceRange sourceRange = typeDecl.getSourceRange();
//						featurename = getFeatureName((ASTNode) typeDecl); asumption that all the compilation units here will have the same feature name

						final FeatureHouseClassSignature curClassSig = (FeatureHouseClassSignature) addFeatureID(
								new FeatureHouseClassSignature(parent, name, typeDecl.getFlags(), typeString, pckg, typeDecl, importList),
								projectSignatures.getFeatureID(featurename), sourceRange.getOffset(), sourceRange.getOffset() + sourceRange.getLength());

						for (final IImportDeclaration importDecl : importList) {
							curClassSig.addImport(importDecl.toString());
						}

						final IMethod[] methods = typeDecl.getMethods();

						for (final IMethod method : methods) {

							name = method.getElementName();
							final String type = method.getReturnType();
							final ITypeParameter[] parameters = method.getTypeParameters();

							final List<SingleVariableDeclaration> parameterList = new ArrayList<>();

							for (final ITypeParameter parameter : parameters) {
								parameterList.add((SingleVariableDeclaration) parameter);
							}
							final String[] exceptions = method.getExceptionTypes();

//							featurename = getFeatureName((ASTNode) method);

							final ISourceRange methodSourceRange = method.getSourceRange();

							addFeatureID(new FeatureHouseMethodSignature(curClassSig, name, method.getFlags(), type, parameterList, false),
									projectSignatures.getFeatureID(featurename), methodSourceRange.getOffset(),
									methodSourceRange.getLength() - sourceRange.getOffset());

						}

						final IField[] fields = typeDecl.getFields();
						for (final IField field : fields) {

							name = field.getElementName();
							final IType type = field.getDeclaringType();
							final ISourceRange fieldSourceRange = field.getSourceRange();

//							featurename = getFeatureName((ASTNode) field);
							addFeatureID(new FeatureHouseFieldSignature(curClassSig, name, field.getFlags(), type), projectSignatures.getFeatureID(featurename),
									fieldSourceRange.getOffset(), fieldSourceRange.getOffset() + fieldSourceRange.getLength());

						}

					} while (!stack.isEmpty());

				}

			}
		}

		final AbstractSignature[] sigArray = new AbstractSignature[signatureSet.size()];
		int i = -1;

		for (final SignatureReference sigRef : signatureSet.values()) {
			final AbstractSignature sig = sigRef.getSig();
			sig.setFeatureData(sigRef.getFeatureData());
			sigArray[++i] = sig;
		}

		projectSignatures.setSignatureArray(sigArray);

		return projectSignatures;
	}

	private static java.util.List<String> featureModulePathnames = null;

	private static String getFeatureName(ASTNode astNode) {
		final int featureID = astNode.getParent().hashCode(); // not sure if this should work as feature ID but check how its actually calculated

		final String featurename = featureModulePathnames.get(featureID);
		return featurename.substring(featurename.lastIndexOf(File.separator) + 1);
	}

	private static AbstractSignature addFeatureID(AbstractSignature sig, int featureID, int startLine, int endLine) {
		SignatureReference sigRef = signatureSet.get(sig);
		if (sigRef == null) {
			sigRef = new SignatureReference(sig);
			signatureSet.put(sig, sigRef);
			signatureTable.put(sig.getFullName(), sig);
		}
		sigRef.addID((FOPFeatureData) featureDataConstructor.create(featureID, startLine, endLine));
		return sigRef.getSig();
	}

	private static final class SignatureReference {

		private final HashMap<Integer, FOPFeatureData> ids = new HashMap<>();
		private final AbstractSignature sig;

		public SignatureReference(AbstractSignature sig) {
			this.sig = sig;
		}

		public final FOPFeatureData[] getFeatureData() {
			final FOPFeatureData[] ret = new FOPFeatureData[ids.size()];
			int i = -1;
			for (final FOPFeatureData id : ids.values()) {
				ret[++i] = id;
			}
			return ret;
		}

		public final void addID(FOPFeatureData featureData) {
			if (!ids.containsKey(featureData.getID())) {
				ids.put(featureData.getID(), featureData);
			}
		}

		public final AbstractSignature getSig() {
			return sig;
		}

//		public void setAbsoluteFilePath(int featureID, String absoluteFilePath) {
//			if (ids.containsKey(featureID)) {
//				ids.get(featureID).setAbsoluteFilePath(absoluteFilePath);
//			}
//		}

	}
}

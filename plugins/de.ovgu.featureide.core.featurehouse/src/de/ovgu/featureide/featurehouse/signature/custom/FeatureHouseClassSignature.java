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

import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;

import de.ovgu.featureide.core.signature.base.AbstractClassSignature;

/**
 * Holds the java signature of a class.
 */
public class FeatureHouseClassSignature extends AbstractClassSignature {

	protected final List<IImportDeclaration> importList;
	protected final LinkedList<IType> superTypes;
	protected final LinkedList<IType> implementTypes;

	public FeatureHouseClassSignature(AbstractClassSignature parent, String name, int modifiers, String type, String pckg, IType typeDecl,
			List<IImportDeclaration> importList) {
		super(parent, name, Modifier.toString(modifiers), type, pckg);

		this.importList = importList;

		superTypes = new LinkedList<IType>();
		implementTypes = new LinkedList<IType>();

		if (typeDecl instanceof ClassDeclaration) {
			final ClassDeclaration classDecl = (ClassDeclaration) typeDecl;
			superTypes.add((IType) classDecl.getSuperclass());
			addExtend(classDecl.getQualifiedName());
			if (!classDecl.getQualifiedName().equals("Object")) {
				addExtend(classDecl.getQualifiedName());
			}
			@SuppressWarnings("unchecked")
			final Iterator<IType> implementInterfaceIt = (Iterator<IType>) classDecl.getSuperinterfaces();
			while (implementInterfaceIt.hasNext()) {
				final IType implementType = implementInterfaceIt.next();
				implementTypes.add(implementType);
				try {
					addImplement(implementType.getSuperclassName());
				} catch (final JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (typeDecl instanceof InterfaceDeclaration) {
			@SuppressWarnings("unchecked")
			final Iterator<IType> superInterfaceIt = (Iterator<IType>) ((InterfaceDeclaration) typeDecl).getSuperinterfaces();
			while (superInterfaceIt.hasNext()) {
				final IType superInterface = superInterfaceIt.next();
				superTypes.add(superInterface);
				if (!superInterface.getFullyQualifiedName().equals("Object")) {
					try {
						addExtend(superInterface.getSuperclassName());
					} catch (final JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

//		sb.append(super.toString());
//		sb.append(LINE_SEPARATOR);

		if (mergedjavaDocComment != null) {
			sb.append(mergedjavaDocComment);
		}

		if (modifiers.length > 0) {
			for (final String modifier : modifiers) {
				sb.append(modifier);
				sb.append(' ');
			}
		}
		sb.append(type);
		sb.append(' ');
		sb.append(name);

		return sb.toString();
	}

	@Override
	protected void computeHashCode() {
		super.computeHashCode();
//		hashCode *= hashCodePrime;
//		for (TypeDecl thisSuperType : superTypes) {
//			hashCode += thisSuperType.hashCode();
//		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}

		final FeatureHouseClassSignature otherSig = (FeatureHouseClassSignature) obj;

		if (!super.sigEquals(otherSig)) {
			return false;
		}

		return true;
	}
}

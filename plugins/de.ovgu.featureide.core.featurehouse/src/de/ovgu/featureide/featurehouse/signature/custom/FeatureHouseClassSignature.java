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

import de.ovgu.featureide.core.signature.base.AbstractClassSignature;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;


/**
 * Holds the java signature of a class.
 */
public class FeatureHouseClassSignature extends AbstractClassSignature {
	
	protected final List<ImportDeclaration> importList;
	protected final LinkedList<TypeDeclaration> superTypes;
	protected final LinkedList<TypeDeclaration> implementTypes;

	@SuppressWarnings("deprecation")
	public FeatureHouseClassSignature(AbstractClassSignature parent, String name, int modifiers, String type, String pckg, TypeDeclaration typeDecl, List<ImportDeclaration> importList) {
		super(parent, name, Modifier.toString(modifiers), type, pckg);
		
		this.importList = importList;
		
		superTypes = new LinkedList<TypeDeclaration>();
		implementTypes = new LinkedList<TypeDeclaration>();
		
		if (typeDecl instanceof ClassDeclaration) {
			final ClassDeclaration classDecl = (ClassDeclaration) typeDecl;
			superTypes.add((TypeDeclaration) classDecl.getSuperclass());
			addExtend(classDecl.getQualifiedName());
			if (!classDecl.getQualifiedName().equals("Object")) {
				addExtend(classDecl.getQualifiedName());
			}
			@SuppressWarnings("unchecked")
			final Iterator<TypeDeclaration> implementInterfaceIt = (Iterator<TypeDeclaration>) classDecl.getSuperinterfaces();
			while (implementInterfaceIt.hasNext()) {
				final TypeDeclaration implementType = implementInterfaceIt.next();
				implementTypes.add(implementType);
				addImplement(implementType.getSuperclass().getFullyQualifiedName());
			}
		} else if (typeDecl instanceof InterfaceDeclaration) {
			@SuppressWarnings("unchecked")
			final Iterator<TypeDeclaration> superInterfaceIt = (Iterator<TypeDeclaration>) ((InterfaceDeclaration) typeDecl).getSuperinterfaces();
			while (superInterfaceIt.hasNext()) {
				final TypeDeclaration superInterface = superInterfaceIt.next();
				superTypes.add(superInterface);
				if (!superInterface.getName().equals("Object")) {
					addExtend(superInterface.getSuperclass().getFullyQualifiedName());
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

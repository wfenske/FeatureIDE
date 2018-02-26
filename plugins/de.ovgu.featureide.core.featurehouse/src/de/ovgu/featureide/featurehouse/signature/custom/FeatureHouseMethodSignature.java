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

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
<<<<<<< HEAD
=======
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
>>>>>>> parent of 2434e2b54... remodifying the signature builders to use eclipse IType

import com.sun.mirror.declaration.ParameterDeclaration;
import de.ovgu.featureide.core.signature.base.AbstractClassSignature;
import de.ovgu.featureide.core.signature.base.AbstractMethodSignature;

/**
 * Holds the java signature of a method.
 *
 */
public class FeatureHouseMethodSignature extends AbstractMethodSignature {

	protected List<SingleVariableDeclaration> p = new LinkedList<>();
<<<<<<< HEAD

	protected String returnType;
	protected List<SingleVariableDeclaration> parameterList;
//	protected List<Access> exceptionList;

	public FeatureHouseMethodSignature(AbstractClassSignature parent, String name, int modifiers, String returnType, List<SingleVariableDeclaration> parameters,
			boolean isConstructor) {
=======
	
	protected TypeDeclaration returnType;
	protected List<ParameterDeclaration> parameterList;
//	protected List<Access> exceptionList;

	public FeatureHouseMethodSignature(AbstractClassSignature parent, String name, int modifiers, TypeDeclaration returnType, List<ParameterDeclaration> parameters, boolean isConstructor) {
>>>>>>> parent of 2434e2b54... remodifying the signature builders to use eclipse IType
		super(parent, name, Modifier.toString(modifiers), returnType.toString(), new LinkedList<String>(), isConstructor);
		
		this.returnType = returnType;
		this.parameterList = parameters;
		for (final Object parameter : parameters) {
			final SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration) parameter;
			p.add(parameterDeclaration);
			parameterTypes.add(parameterDeclaration.getType().toString());
		}
	}

<<<<<<< HEAD
	@SuppressWarnings("unchecked")
	public FeatureHouseMethodSignature(AbstractClassSignature parent, String name, int modifiers, String returnType, List<?> parameters, boolean isConstructor,
=======
	public FeatureHouseMethodSignature(AbstractClassSignature parent, String name, int modifiers, Type returnType, List<?> parameters, boolean isConstructor,
>>>>>>> parent of 2434e2b54... remodifying the signature builders to use eclipse IType
			int startLine, int endLine) {
		super(parent, name, Modifier.toString(modifiers), returnType.toString(), new LinkedList<String>(), isConstructor, startLine, endLine);
		for (final Object parameter : parameters) {
			final SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration) parameter;
			p.add(parameterDeclaration);
			parameterTypes.add(parameterDeclaration.getType().toString());
		}
	}

	@Override
	public String toString() {
		final StringBuilder methodString = new StringBuilder();

		if (mergedjavaDocComment != null) {
			methodString.append(mergedjavaDocComment);
		}

		if (modifiers.length > 0) {
			for (final String modifier : modifiers) {
				methodString.append(modifier);
				methodString.append(' ');
			}
		}

		if (!isConstructor) {
			methodString.append(type);
			methodString.append(' ');
		}

		methodString.append(name);
		methodString.append('(');
		boolean notfirst = false;
		for (final ParameterDeclaration parameter : parameterList) {
			if (notfirst) {
				methodString.append(", ");
			} else {
				notfirst = true;
			}
			methodString.append(parameter.getType().toString());
			methodString.append(' ');
			methodString.append(parameter.getSimpleName());
		}
		methodString.append(')');

		return methodString.toString();
	}

	@Override
	protected void computeHashCode() {
		super.computeHashCode();

		hashCode = (hashCodePrime * hashCode) + type.hashCode();

		hashCode = (hashCodePrime * hashCode) + (isConstructor ? 1231 : 1237);
		for (final ParameterDeclaration parameter : parameterList) {
			hashCode = (hashCodePrime * hashCode) + parameter.getType().toString().hashCode();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}

		final FeatureHouseMethodSignature otherSig = (FeatureHouseMethodSignature) obj;

		if (!super.sigEquals(otherSig)) {
			return false;
		}
		if (isConstructor != otherSig.isConstructor) {
			return false;
		}

		if (p.size() != otherSig.p.size()) {
			return false;
		}

		final Iterator<ParameterDeclaration> thisIt = parameterList.iterator();
		final Iterator<ParameterDeclaration> otherIt = otherSig.parameterList.iterator();
		while (thisIt.hasNext()) {
			final ParameterDeclaration tNext = thisIt.next();
			final ParameterDeclaration oNext = otherIt.next();
			if (!tNext.getType().equals(oNext.getType())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String getReturnType() {
		return type;
	}
}

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
import org.eclipse.jdt.core.dom.Type;

import de.ovgu.featureide.core.signature.base.AbstractClassSignature;
import de.ovgu.featureide.core.signature.base.AbstractMethodSignature;

/**
 * Holds the java signature of a method.
 *
 */
public class FeatureHouseMethodSignature extends AbstractMethodSignature {

	protected List<SingleVariableDeclaration> p = new LinkedList<>();

	protected Type returnType;
	protected List<SingleVariableDeclaration> parameterList;
//	protected List<Access> exceptionList;

	public FeatureHouseMethodSignature(AbstractClassSignature parent, String name, int modifiers, Type returnType, List<SingleVariableDeclaration> parameters,
			boolean isConstructor) {
		super(parent, name, Modifier.toString(modifiers), returnType.toString(), new LinkedList<String>(), isConstructor);

		this.returnType = returnType;
		parameterList = parameters;
		for (final Object parameter : parameters) {
			final SingleVariableDeclaration parameterDeclaration = (SingleVariableDeclaration) parameter;
			p.add(parameterDeclaration);
			parameterTypes.add(parameterDeclaration.getType().toString());
		}
	}

	@SuppressWarnings("unchecked")
	public FeatureHouseMethodSignature(AbstractClassSignature parent, String name, int modifiers, Type returnType, List<?> parameters, boolean isConstructor,
			int startLine, int endLine) {
		super(parent, name, Modifier.toString(modifiers), null, new LinkedList<String>(), isConstructor, startLine, endLine);

		this.returnType = returnType;
		parameterList = (List<SingleVariableDeclaration>) parameters;

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
		for (final SingleVariableDeclaration parameter : parameterList) {
			if (notfirst) {
				methodString.append(", ");
			} else {
				notfirst = true;
			}
			methodString.append(parameter.getType().toString());
			methodString.append(' ');
//			methodString.append(parameter.getSimpleName());
			methodString.append(parameter.toString());
		}
		methodString.append(')');

		return methodString.toString();
	}

	@Override
	protected void computeHashCode() {
		super.computeHashCode();

		hashCode = (hashCodePrime * hashCode) + type.hashCode();

		hashCode = (hashCodePrime * hashCode) + (isConstructor ? 1231 : 1237);
		for (final SingleVariableDeclaration parameter : parameterList) {
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

		final Iterator<SingleVariableDeclaration> thisIt = parameterList.iterator();
		final Iterator<SingleVariableDeclaration> otherIt = otherSig.parameterList.iterator();
		while (thisIt.hasNext()) {
			final SingleVariableDeclaration tNext = thisIt.next();
			final SingleVariableDeclaration oNext = otherIt.next();
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

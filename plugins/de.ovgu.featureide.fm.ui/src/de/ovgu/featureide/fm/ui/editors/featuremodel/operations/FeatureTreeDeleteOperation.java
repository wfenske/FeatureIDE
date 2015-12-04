/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.ui.editors.featuremodel.operations;

import static de.ovgu.featureide.fm.core.localization.StringTable.DELETE_ERROR;
import static de.ovgu.featureide.fm.core.localization.StringTable.DELETE_INCLUDING_SUBFEATURES;
import static de.ovgu.featureide.fm.core.localization.StringTable.UNABLE_TO_DELETE_THIS_FEATURES_UNTIL_ALL_RELEVANT_CONSTRAINTS_ARE_REMOVED_;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.event.FeatureModelEvent;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;

/**
 * Allows to delete a feature including its sub features
 * 
 * @author Jan Wedding
 * @author Melanie Pflaume
 * @author Marcus Pinnecke
 */
public class FeatureTreeDeleteOperation extends AbstractFeatureModelOperation implements GUIDefaults {

	private IFeature feature;
	private LinkedList<IFeature> featureList;
	private LinkedList<IFeature> containedFeatureList;
	private Deque<AbstractFeatureModelOperation> operations = new LinkedList<AbstractFeatureModelOperation>();

	public FeatureTreeDeleteOperation(IFeatureModel featureModel, IFeature parent) {
		super(featureModel, DELETE_INCLUDING_SUBFEATURES);
		this.feature = parent;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		featureList = new LinkedList<IFeature>();
		containedFeatureList = new LinkedList<IFeature>();
		LinkedList<IFeature> list = new LinkedList<IFeature>();
		list.add(feature);
		getFeaturesToDelete(list);

		if (containedFeatureList.isEmpty()) {
			for (IFeature feat : featureList) {
				AbstractFeatureModelOperation op = new DeleteFeatureOperation(featureModel, feat);
				executeOperation(op);
				operations.add(op);
			}
		} else {
			final String containedFeatures = containedFeatureList.toString();
			MessageDialog dialog = new MessageDialog(new Shell(), DELETE_ERROR, FEATURE_SYMBOL, "The following features are contained in constraints:" + '\n'
					+ containedFeatures.substring(1, containedFeatures.length() - 1) + '\n' + '\n'
					+ UNABLE_TO_DELETE_THIS_FEATURES_UNTIL_ALL_RELEVANT_CONSTRAINTS_ARE_REMOVED_, MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);

			dialog.open();
		}
		return Status.OK_STATUS;
	}

	private void executeOperation(AbstractFeatureModelOperation op) {
		try {
			PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, null, null);
		} catch (ExecutionException e) {
			FMUIPlugin.getDefault().logError(e);

		}
	}

	@Override
	protected FeatureModelEvent internalRedo() {
		for (Iterator<AbstractFeatureModelOperation> it = operations.iterator(); it.hasNext();) {
			AbstractFeatureModelOperation operation = it.next();
			if (operation.canRedo()) {
				operation.redo();
			}
		}
		return null;
	}

	@Override
	protected FeatureModelEvent internalUndo() {
		for (Iterator<AbstractFeatureModelOperation> it = operations.descendingIterator(); it.hasNext();) {
			AbstractFeatureModelOperation operation = it.next();
			if (operation.canUndo()) {
				operation.undo();
			}
		}
		return null;
	}

	/**
	 * traverses through the whole subtree and collects the features that should
	 * be deleted
	 * 
	 * @param linkedList
	 */
	private void getFeaturesToDelete(List<IFeature> linkedList) {
		for (IFeature feat : linkedList) {
			if (!feat.getStructure().getRelevantConstraints().isEmpty()) {
				containedFeatureList.add(feat);
			}
			if (feat.getStructure().hasChildren()) {
				getFeaturesToDelete(FeatureUtils.convertToFeatureList(feat.getStructure().getChildren()));
			}
			featureList.add(feat);
		}
	}
}
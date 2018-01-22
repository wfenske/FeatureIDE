package de.ovgu.featureide.featurehouse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.signature.base.AbstractSignature;
import de.ovgu.featureide.featurehouse.refactoring.FujiSelector;
import de.ovgu.featureide.featurehouse.refactoring.pullUp.PullUpRefactoring;
import de.ovgu.featureide.featurehouse.refactoring.pullUp.PullUpRefactoringWizard;
//import de.ovgu.featureide.featurehouse.signature.fuji.FujiClassSignature;
import de.ovgu.featureide.featurehouse.signature.custom.FeatureHouseClassSignature;

public class PullUpHandler extends RefactoringHandler {

	@Override
	protected void singleAction(Object element, String file) {
		try {
			final IFeatureProject featureProject = getFeatureProject();
			if (featureProject == null) {
				return;
			}

			final PullUpRefactoring pullUp = new PullUpRefactoring((FeatureHouseClassSignature) element, featureProject, file);
			final PullUpRefactoringWizard refactoringWizard = new PullUpRefactoringWizard(pullUp);
			final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(refactoringWizard);
			op.run(getShell(), "PullUp-Refactoring");

		} catch (final InterruptedException e) {}
	}

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final ITextEditor editor = (ITextEditor) page.getActiveEditor();

		final IJavaElement elem = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		if (elem instanceof ICompilationUnit) {
			final String file = ((ICompilationUnit) elem).getResource().getRawLocation().toOSString();

			final IFeatureProject featureProject = getFeatureProject();
			createSignatures(featureProject);

			FujiSelector selector = null;
			try {
				selector = new FujiSelector(featureProject, file);
			} catch (final JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			final AbstractSignature signature = selector.getSelectedClassSignature();
			if (signature != null) {
				singleAction(signature, file);
			}
		}

		return null;
	}

}

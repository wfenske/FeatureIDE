package de.ovgu.featureide.featurehouse.ui.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.signature.base.AbstractSignature;
import de.ovgu.featureide.featurehouse.refactoring.FujiSelector;
import de.ovgu.featureide.featurehouse.refactoring.RenameFieldRefactoring;
import de.ovgu.featureide.featurehouse.refactoring.RenameLocalVariableRefactoring;
import de.ovgu.featureide.featurehouse.refactoring.RenameMethodRefactoring;
import de.ovgu.featureide.featurehouse.refactoring.RenameRefactoring;
import de.ovgu.featureide.featurehouse.refactoring.RenameRefactoringWizard;
import de.ovgu.featureide.featurehouse.refactoring.RenameTypeRefactoring;
import de.ovgu.featureide.featurehouse.signature.custom.FeatureHouseClassSignature;
import de.ovgu.featureide.featurehouse.signature.custom.FeatureHouseFieldSignature;
import de.ovgu.featureide.featurehouse.signature.custom.FeatureHouseLocalVariableSignature;
import de.ovgu.featureide.featurehouse.signature.custom.FeatureHouseMethodSignature;

public class RenameHandler extends RefactoringHandler {

	@Override
	protected void singleAction(Object element, String file) {
		try {
			final IFeatureProject featureProject = getFeatureProject();
			if (featureProject == null) {
				return;
			}

			RenameRefactoring refactoring;
			if (element instanceof FeatureHouseMethodSignature) {
				final FeatureHouseMethodSignature method = (FeatureHouseMethodSignature) element;

				if (method.isConstructor()) {
					refactoring = new RenameTypeRefactoring((FeatureHouseClassSignature) method.getParent(), featureProject, file);
				} else {
					refactoring = new RenameMethodRefactoring(method, featureProject, file);
				}
			} else if (element instanceof FeatureHouseClassSignature) {
				refactoring = new RenameTypeRefactoring((FeatureHouseClassSignature) element, featureProject, file);
			} else if (element instanceof FeatureHouseFieldSignature) {
				refactoring = new RenameFieldRefactoring((FeatureHouseFieldSignature) element, featureProject, file);
			} else if (element instanceof FeatureHouseLocalVariableSignature) {
				refactoring = new RenameLocalVariableRefactoring((FeatureHouseLocalVariableSignature) element, featureProject, file);
			} else {
				return;
			}

			final RenameRefactoringWizard refactoringWizard = new RenameRefactoringWizard(refactoring);
			final RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(refactoringWizard);
			op.run(getShell(), "Rename-Refactoring");
		} catch (final InterruptedException e) {}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final ITextEditor editor = (ITextEditor) page.getActiveEditor();

		final IJavaElement elem = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		if (elem instanceof ICompilationUnit) {
			final ITextSelection sel = (ITextSelection) editor.getSelectionProvider().getSelection();

			final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

			int lineOffset = 0;
			try {
				lineOffset = document.getLineOffset(sel.getStartLine());
			} catch (final BadLocationException e1) {
				e1.printStackTrace();
			}
			final int column = sel.getOffset() - lineOffset;

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
			final AbstractSignature signature = selector.getSelectedSignature(sel.getStartLine() + 1, column);
			if (signature != null) {
				singleAction(signature, file);
			}
		}

		return null;
	}
}

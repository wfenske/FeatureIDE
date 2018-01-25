package de.ovgu.featureide.cloneanalysis.plugin;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.pmd.cpd.Match;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.internal.editors.text.UISynchronizationContext;
import org.eclipse.ui.texteditor.IDocumentProvider;
//Ad ch st
import org.eclipse.ui.IWorkbenchPart;
import de.ovgu.featureide.cloneanalysis.impl.CPDCloneAnalysis;
import de.ovgu.featureide.cloneanalysis.impl.CloneOccurence;
import de.ovgu.featureide.cloneanalysis.markers.CloneAnalysisMarkers;
import de.ovgu.featureide.cloneanalysis.results.CPDResultConverter;
import de.ovgu.featureide.cloneanalysis.results.CloneAnalysisGraphResults;
import de.ovgu.featureide.cloneanalysis.results.CloneAnalysisResults;
import de.ovgu.featureide.cloneanalysis.results.FeatureRootLocation;
import de.ovgu.featureide.cloneanalysis.results.IClonePercentageData;
import de.ovgu.featureide.cloneanalysis.results.VariantAwareClone;
import de.ovgu.featureide.cloneanalysis.utils.CloneAnalysisUtils;
import de.ovgu.featureide.cloneanalysis.views.CloneAnalysisView;
import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;

/**
 * Command that is used by UI-elements and starts a CC analysis on the selected
 * sourcefolder.
 * 
 * @author Konstantin Tonscheidt
 * 
 */
public class CloneAnalysisCommandHandler extends AbstractHandler implements ICoreRunnable {

	private static final boolean UPDATE_MARKERS = true;
	private static final boolean UPDATE_GRAPHS = false;

	public CloneAnalysisCommandHandler() {

	}

	private String name = "CloneProgress";
	private IProgressMonitor monitor;
	private static ExecutionEvent currentEvent;
	static CloneAnalysisView cloneAnalysis;
	static IStructuredSelection currentSelection;
	static Display globalDisplay;

	public void progressMethod() {
		Display localDisplay = Display.getCurrent() != null ? Display.getCurrent() : Display.getDefault();
		globalDisplay = localDisplay;
		cloneAnalysis.scheduleJob(name, CloneAnalysisCommandHandler.class);
		cloneAnalysis.showProgress();
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		System.out.println("CloneAnalysis Object" + cloneAnalysis);
		System.out.println("Current Selection" + currentSelection);
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Code Clone Analysis", 100);
		System.out.println("Name of the thread@ run is: " + Thread.currentThread().getName());
		// start a new thread for execute and display
		executeAndDisplayProgress(subMonitor.split(100));
	}

	private void executeAndDisplayProgress(IProgressMonitor monitor) {
		
		try {
			long time = System.currentTimeMillis();
			CPDCloneAnalysis analysis = new CPDCloneAnalysis();

			System.out.println("Name of the thread@ execute is: " + Thread.currentThread().getName());

			final Iterator<Match> cpdResults = analysis.analyze(currentSelection);
			CloneAnalysisResults<VariantAwareClone> formattedResults = CPDResultConverter.convertMatchesToReadableResults(cpdResults);
			final IClonePercentageData percentageData = formattedResults.getPercentageData();

			System.out.println(formattedResults.getRelevantFeatures().size() + " relevant features ");
			System.out.println(formattedResults.getRelevantFeatures().toString());
			for (FeatureRootLocation feature : formattedResults.getRelevantFeatures()) {
				System.out.println(
						feature.getLocation().lastSegment() + " total: " + percentageData.getTotalLineCount(feature)
								+ " cloned: " + percentageData.getTotalCloneLength(feature) + " lines: "
								+ percentageData.getClonedLineCount(feature));
			}

			if (UPDATE_GRAPHS)
				CloneAnalysisGraphResults.createGraphsForResults(formattedResults,
						"/Users/steffen/Desktop/Analyse/charts/", formattedResults.getRelevantFeatures());

			if (UPDATE_MARKERS) {
				SubMonitor subMonitorProgress = SubMonitor.convert(monitor, 100);
				globalDisplay.syncExec(() -> {
				 deleteAllMarkersForCodeClones();
				});

				final Map<IFile, IDocument> documents = new HashMap<IFile, IDocument>();

				final IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
				for (VariantAwareClone clone : formattedResults.getClones()) {
					StringBuilder  message=new StringBuilder();
					StringBuilder formattedMessage = new StringBuilder();
					int count = 0;
					for (CloneOccurence occ : clone.getOccurences()) {

						String file = CloneAnalysisUtils.getFileFromPath(occ.getFile()).getLocation().toString();
//						message += occ.getFile() + ";";
						message.append(occ.getFile()).append(";");
//						formattedMessage += "[ " + occ.getFile().segment(6) + " ]   " + occ.getFile().lastSegment()
//								+ ";    ";
						formattedMessage.append("[ ").append( occ.getFile().segment(6)).append(" ]   ").append(occ.getFile().lastSegment()).append(";    ");
					}

					final List<IPath> distinctFiles = clone.getDistinctFiles();
					final Map<String, List<IPath>> allPathsForFileName = new HashMap<String, List<IPath>>();
					int index = 0;
					for (IPath iPath : distinctFiles) {

						String fileName = iPath.removeFirstSegments(iPath.segmentCount() - 1).toString();

						List<IPath> allFiles;
						if (allPathsForFileName.containsKey(fileName))
							allFiles = allPathsForFileName.get(fileName);
						else {

							allFiles = new ArrayList<IPath>();

							allPathsForFileName.put(fileName, allFiles);

						}

						allFiles.add(iPath);

					}
					int noOfFiles = 0;
					for (CloneOccurence occ : clone.getOccurences()) {
						final IFile file = CloneAnalysisUtils.getFileFromPath(occ.getFile());

						if (file == null || file.getLocation() == null)
							System.out.println("trying to create marker in null file " + (occ.getFile() != null
									? occ.getFile().makeRelativeTo(CloneAnalysisUtils.getWorkspaceRoot().getLocation())
											.toString()
									: ""));
						else {
							noOfFiles++;

							if (noOfFiles % 50 == 0) {
								subMonitorProgress.worked((noOfFiles/50)*9);
								System.out.println("creating marker in file " + file.getLocation().toString());
							}
						}

						final IDocument document = getDocumentForFile(file, documents);
						final int[] markerPositions = getMarkerPositions(document, occ);
						final Map<String, String> errorMap = new HashMap<String, String>();
						errorMap.put("first", "first value");
						errorMap.put("second", "second value");
						errorMap.put("third", "third value");
						errorMap.put("fourth", "fourth value");

						try{
						 globalDisplay.syncExec(() -> {
						 CloneAnalysisMarkers.addProblemMarker(file, message.toString(),formattedMessage.toString(), occ.getStartIndex(), markerPositions[0], markerPositions[1]);
						 });
						}catch( SWTException a){
							a.printStackTrace();
						}catch( org.eclipse.core.runtime.AssertionFailedException a){
							a.printStackTrace();
						}
						
						 if (subMonitorProgress.isCanceled()) 
						        throw new OperationCanceledException();
					}
//					subMonitorProgress.worked(100);
				}
				subMonitorProgress.done();
				
				if (subMonitorProgress.isCanceled()) 
			        throw new OperationCanceledException();
				
			}
			 globalDisplay.syncExec(() -> {
			cloneAnalysis.showResults(formattedResults);
			 });
			
			time = System.currentTimeMillis() - time;
			double timeD = ((double) time) / 1000.0;
			System.out.println("Overall clone analysis execution time: " + timeD + "s");
			
		}catch (NullPointerException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		this.currentEvent = event;
		if (HandlerUtil.getCurrentSelection(currentEvent) instanceof IStructuredSelection)
			currentSelection = (IStructuredSelection) HandlerUtil.getCurrentSelection(currentEvent);
		CloneAnalysisView cloneAnalysisView = (CloneAnalysisView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().findView(CloneAnalysisView.ID);
		this.cloneAnalysis = cloneAnalysisView;
		progressMethod();
		return null;

	}

	private int[] getMarkerPositions(final IDocument document, final CloneOccurence occ) {

		int[] result = new int[2];
		try {
			int startLine = occ.getStartIndex() - 1;
			int endLine = startLine + occ.getClone().getLineCount() - 1;
			result[0] = document.getLineOffset(startLine);
			result[1] = document.getLineOffset(endLine) + document.getLineLength(endLine) - 1;

		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		return result;
	}

	private IDocument getDocumentForFile(final IFile file, final Map<IFile, IDocument> documents) {
		if (documents.containsKey(file))
			return documents.get(file);

		IDocumentProvider provider = new TextFileDocumentProvider();
		try {
			provider.connect(file);
			final IDocument document = provider.getDocument(file);
			documents.put(file, document);
			return document;
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void deleteAllMarkersForCodeClones() {

		try {
			IFeatureProject featureProject = getFeatureProject();
			if (featureProject == null)
				return;
			featureProject.getProject().deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);

		} catch (CoreException e) {
		}
	}

	protected IFeatureProject getFeatureProject() {
		IEditorInput fileEditorInput = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor().getEditorInput();
		final IFile file = ResourceUtil.getFile(fileEditorInput);
		if (file == null)
			return null;

		return CorePlugin.getFeatureProject(file);
	}

}

/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.figures;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.prop4j.NodeWriter;

import de.ovgu.featureide.fm.core.Constraint;
import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.FeatureStatus;
import de.ovgu.featureide.fm.ui.editors.FeatureDiagramExtension;
import de.ovgu.featureide.fm.ui.editors.FeatureUIHelper;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.fm.ui.editors.featuremodel.figures.anchors.SourceAnchor;
import de.ovgu.featureide.fm.ui.editors.featuremodel.figures.anchors.TargetAnchor;


/**
 * A figure that represents a feature with its name.
 * 
 * @author Thomas Thuem
 */
public class FeatureFigure extends Figure implements GUIDefaults {
	
	private final Label label = new Label();
	
	private final FreeformLayout layout = new FreeformLayout();

	private final ConnectionAnchor sourceAnchor;
	
	private final ConnectionAnchor targetAnchor;

	private Feature feature;

	private FeatureModel featureModel;
	
	private String CONCRETE = " Concrete";
	private String ABSTRACT = " Abstract";
	private String HIDDEN = " hidden";
	private String DEAD = "is dead ";
	private String FEATURE = " feature ";
	private String FALSE_OPTIONAL = "is false optional ";
	private String VOID = " Feature Model is void ";
	private String ROOT = " Root ";

	public FeatureFigure(Feature feature, FeatureModel featureModel) {
		super();
		this.feature = feature;
		this.featureModel = featureModel;
		
		sourceAnchor = new SourceAnchor(this, feature);
		targetAnchor = new TargetAnchor(this, feature);
		
		setLayoutManager(layout);

		label.setForegroundColor(FEATURE_FOREGROUND);
		label.setFont(DEFAULT_FONT);

		label.setLocation(new Point(FEATURE_INSETS.left, FEATURE_INSETS.top));
		
		setName(feature.getName());

		setProperties();
		
		
		FeatureUIHelper.setSize(feature,getSize());
		
		add(label);
		setOpaque(true);

		if (FeatureUIHelper.getLocation(feature) != null)
			setLocation(FeatureUIHelper.getLocation(feature));
		
		if(isHidden(feature)){
			setSize(new Dimension(0,0));
		}
	}
	
	boolean isHidden(Feature feature){
		if(featureModel.showHiddenFeatures()){
			return false;
		} else {
			if(feature.isHidden()){
				return feature.isHidden();
			}
			if(!feature.isRoot()){
				return isHidden(feature.getParent());
			} else {
				return feature.isHidden();
			}
		}

	}

	public void setProperties() {
	
		String toolTip = "";
		boolean modelIsValid = featureModel.valid();
		
		label.setForegroundColor(FEATURE_FOREGROUND);
		setBackgroundColor(CONCRETE_BACKGROUND);
		setBorder(feature.isConstraintSelected() ? CONCRETE_SELECTED_BORDER : CONCRETE_BORDER);
		
		if (feature.isConcrete()) toolTip += CONCRETE;
		
		if (feature.isAbstract()){
			setBackgroundColor(ABSTRACT_BACKGROUND);
			setBorder(feature.isConstraintSelected() ? ABSTRACT_SELECTED_BORDER : ABSTRACT_BORDER);
			if (feature.isRoot()){
				toolTip += ROOT;
			} else {
				toolTip += ABSTRACT;
			}
		}
		
		if (feature.hasHiddenParent()){
			setBorder(feature.isConstraintSelected() ? HIDDEN_SELECTED_BORDER : HIDDEN_BORDER);
			label.setForegroundColor(HIDDEN_FOREGROUND);
			if (feature.isHidden()) toolTip += HIDDEN;
		}
		
		if (!feature.isRoot()) toolTip += FEATURE;
		
		if ((feature.getFeatureStatus() == FeatureStatus.DEAD) && modelIsValid){
			setBackgroundColor(DEAD_BACKGROUND);
			setBorder(feature.isConstraintSelected() ? DEAD_SELECTED_BORDER : DEAD_BORDER);
			toolTip += DEAD;			
		}
		
		if (feature.getFeatureStatus() == FeatureStatus.FALSE_OPTIONAL){
			setBackgroundColor(DEAD_BACKGROUND);
			setBorder(feature.isConstraintSelected() ? DEAD_SELECTED_BORDER : DEAD_BORDER);
			toolTip += FALSE_OPTIONAL;
		}
		
		if (feature.isRoot() && !modelIsValid){
			setBackgroundColor(DEAD_BACKGROUND);
			setBorder(feature.isConstraintSelected() ? DEAD_SELECTED_BORDER : DEAD_BORDER);
			toolTip = VOID;
		}
		
		toolTip += getRelevantConstraints();
		Figure toolTipContent = new Figure();
		toolTipContent.setLayoutManager(new GridLayout());
		toolTipContent.add(new Label(toolTip));
		
		// call of the FeatureDiagramExtensions
		for (FeatureDiagramExtension extension : FeatureDiagramExtension.getExtensions()) {
			toolTipContent = extension.extendFeatureFigureToolTip(toolTipContent, this);
		}
		
		setToolTip(toolTipContent);
	}
	
	private String getRelevantConstraints() {
		String relevant = "";
		for (Constraint constraint : featureModel.getConstraints()) {
			String node = constraint.getNode().toString(NodeWriter.logicalSymbols);
			if (node.contains(feature.getName()))
				relevant += "\n " + node + " ";
		}
		if (!relevant.isEmpty())
			return "\n" + relevant;
		return "";
	}

	public ConnectionAnchor getSourceAnchor() {
		return sourceAnchor;
	}

	public ConnectionAnchor getTargetAnchor() {
		return targetAnchor;
	}

	public void setName(String newName) {
		label.setText(newName);
		Dimension labelSize = label.getPreferredSize();
		
		if (labelSize.equals(label.getSize()))
			return;
		label.setSize(labelSize);

		Rectangle bounds = getBounds();
		int w = FEATURE_INSETS.getWidth();
		int h = FEATURE_INSETS.getHeight();
		
		bounds.setSize(labelSize.expand(w, h));
			

		Dimension oldSize = getSize();
		if (!oldSize.equals(0, 0)) {
			int dx = (oldSize.width - bounds.width) / 2;
			bounds.x += dx;
		}

		setBounds(bounds);
	}

	public Rectangle getLabelBounds() {
		return label.getBounds();
	}
	
}

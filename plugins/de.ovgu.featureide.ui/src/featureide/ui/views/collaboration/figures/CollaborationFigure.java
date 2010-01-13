/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2009  FeatureIDE Team, University of Magdeburg
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
package featureide.ui.views.collaboration.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import featureide.ui.views.collaboration.GUIDefaults;
import featureide.ui.views.collaboration.model.Collaboration;

/**
 * An instance of this class represents the graphical representation of the 
 * Collaboration.
 * 
 * @author Constanze Adler
 */
public class CollaborationFigure extends Figure implements GUIDefaults{

	private final Label label = new Label();
	
	private final FreeformLayout layout = new FreeformLayout();
	/**
	 * 
	 */
	public CollaborationFigure(Collaboration coll) {
		
		super();
		
		this.setLayoutManager(layout);
		
		setBackgroundColor(COLL_BACKGROUND_EVEN);
		setBorder(ROLE_BORDER);
		label.setForegroundColor(FOREGROUND);
		label.setFont(DEFAULT_FONT);
		label.setLocation(new Point(COLLABORATION_INSETS.left, COLLABORATION_INSETS.top));
		
		this.setName(coll.getName());
		
		this.add(label);
		this.setOpaque(true);
		
		
	}
	
	private void setName(String name){
		label.setText(name);
		Dimension labelSize = label.getPreferredSize();
		
		if (labelSize.equals(label.getSize()))
			return;
		label.setSize(labelSize);

		Rectangle bounds = getBounds();
		int w = COLLABORATION_INSETS.getWidth();
		int h = COLLABORATION_INSETS.getHeight();
		bounds.setSize(labelSize.expand(w, h));

		Dimension oldSize = getSize();
		if (!oldSize.equals(0, 0)) {
			int dx = (oldSize.width - bounds.width) / 2;
			bounds.x += dx;
		}

		setBounds(bounds);
	}
}

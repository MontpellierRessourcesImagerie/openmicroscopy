/*
 * org.openmicroscopy.shoola.agents.viewer3D.canvas.DrawingCanvas
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.agents.viewer.viewer3D.canvas;

//Java imports
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.JComponent;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.viewer.viewer3D.Viewer3D;
import org.openmicroscopy.shoola.agents.viewer.viewer3D.Viewer3DManager;

/** 
 * A simple visual component to draw one shape at a time.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2 
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class DrawingCanvas
	extends JComponent
{
	
	/** The model and controller component of this widget. */    
	private DrawingCanvasMng 	manager;  
    
	/** current Selected point. */
	private Point				currentPointXY, currentPointXZ, currentPointZY;
    
	/** Width and height of the XY-image.  */
	private int 				width, height;
	
	/** width (resp. height) of the ZYimage (resp. XZimage) */
	private int					zWidth;
    
    /** coordinates of the top-left corner of the XYimage.*/
    private int					x, y;
    
	public DrawingCanvas(Viewer3DManager control)
	{
		manager = new DrawingCanvasMng(this, control);
		currentPointXY = null;
		setOpaque(false);
	}
	
	public DrawingCanvasMng getManager() { return manager; }
	
	public void setDrawingDimension(int width, int height, int zWidth, int x, 
									int y)
	{
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		this.zWidth = zWidth;
	}
	
	Point getCurrentPointXY() { return currentPointXY; }
	
	/** Erases current shape, if any. */
	void erase()
	{
		currentPointXY = null;
		repaint();
	}
    
	/** 
	 * Draws the 2 perpendicular lines on the canvas. 
	 * 
	 * @param p   The intersection of the lines.
	 */
	void drawXY(Point p)
	{
		if (p != null) {
			currentPointXY = p;
			repaint();
		}
	}
    
	/** 
	 * Draws the 2 perpendicular lines on the canvas. 
	 * 
	 * @param p   The intersection of the lines.
	 */
	void drawXZ(Point p)
	{
		if (p != null) {
			currentPointXZ = p;
			if (currentPointXY != null) currentPointXY.x = p.x;
			if (currentPointZY != null) currentPointZY.x = p.y;
			repaint();
		}
	}
	
	/** 
	 * Draws the 2 perpendicular lines on the canvas. 
	 * 
	 * @param p   The intersection of the lines.
	 */
	void drawZY(Point p)
	{
		if (p != null) {
			currentPointZY = p;
			if (currentPointXY != null) currentPointXY.y = p.y;
			if (currentPointZY != null) currentPointZY.x = p.y;
			repaint();
		}
	}
	
	/** Overrides the paintComponent. */
	public void paintComponent(Graphics g)
	{
		Graphics2D g2D = (Graphics2D) g;
		if (currentPointXY != null) paintXY(g2D);
	}

	private void paintXY(Graphics2D g2D)
	{
		g2D.setColor(Viewer3D.YlineColor);
		//line on the XYimage.
		g2D.drawLine(currentPointXY.x, y, currentPointXY.x, y+height);
		//line on the XZimage
		g2D.drawLine(currentPointXY.x, y-zWidth-Viewer3D.SPACE, 
					currentPointXY.x, y-Viewer3D.SPACE);		 
		g2D.setColor(Viewer3D.XlineColor);
		//line on the ZYimage;
		g2D.drawLine(x-zWidth-Viewer3D.SPACE, currentPointXY.y, 
					x-Viewer3D.SPACE, currentPointXY.y);
		//line on the XYimage.
		g2D.drawLine(x, currentPointXY.y, x+width, currentPointXY.y);
	}
	
}

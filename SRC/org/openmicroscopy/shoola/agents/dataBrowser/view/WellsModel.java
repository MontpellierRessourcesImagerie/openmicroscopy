/*
 * org.openmicroscopy.shoola.agents.dataBrowser.view.WellsModel 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.dataBrowser.view;


//Java imports
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserAgent;
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserLoader;
import org.openmicroscopy.shoola.agents.dataBrowser.DataBrowserTranslator;
import org.openmicroscopy.shoola.agents.dataBrowser.PlateSaver;
import org.openmicroscopy.shoola.agents.dataBrowser.ThumbnailLoader;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.BrowserFactory;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.CellDisplay;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageDisplay;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageNode;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.ImageSet;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.WellImageSet;
import org.openmicroscopy.shoola.agents.dataBrowser.browser.WellSampleNode;
import org.openmicroscopy.shoola.agents.dataBrowser.layout.LayoutFactory;
import org.openmicroscopy.shoola.agents.util.EditorUtil;
import org.openmicroscopy.shoola.util.image.geom.Factory;
import org.openmicroscopy.shoola.util.ui.colourpicker.ColourObject;

import pojos.DataObject;
import pojos.ImageData;
import pojos.PlateData;
import pojos.WellData;
import pojos.WellSampleData;

/** 
 * A concrete model for a plate.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta3
 */
class WellsModel
	extends DataBrowserModel
{
	
	/** The number of rows. */
	private int			  		rows;
	
	/** The number of columns. */
	private int           		columns;
	
	/** The dimension of a well. */
	private Dimension 			wellDimension;
	
	/** The collection of nodes hosting the wells. */
	private List				wellNodes;
	
	/** The collection of nodes used to display cells e.g. A-1. */
	private Set<CellDisplay> 	cells;
	
	/** The number of fields per well. */
	private int					fieldsNumber;
	
	/** The selected field. */
	private int					selectedField;
	
	/** 
	 * Sorts the passed nodes by row.
	 * 
	 * @param nodes The nodes to sort.
	 * @return See above.
	 */
	private List sortByRow(Set nodes)
	{
		List l = new ArrayList();
		if (nodes == null) return l;
		Iterator i = nodes.iterator();
		while (i.hasNext()) {
			l.add(i.next());
		}
		Comparator c = new Comparator() {
            public int compare(Object o1, Object o2)
            {
                WellData w1 = (WellData) 
                		((WellImageSet) o1).getHierarchyObject(),
                         w2 = (WellData) 
                         ((WellImageSet) o2).getHierarchyObject();
                int n1 = w1.getRow();
                int n2 = w2.getRow();
                int v = 0;
                if (n1 < n2) v = -1;
                else if (n1 > n2) v = 1;
                else if (n1 == n2) {
                	int c1 = w1.getColumn();
                	int c2 = w2.getColumn();
                	 if (c1 < c2) v = -1;
                     else if (c1 > c2) v = 1;
                }
                return v;
            }
        };
        Collections.sort(l, c);
		return l;
	}
	
	/**
	 * Creates the color related to the passed Well.
	 * 
	 * @param data The well to handle.
	 * @return See above.
	 */
	private Color createColor(WellData data)
	{
		int red = data.getRed();
		int green = data.getGreen();
		int blue = data.getBlue();
		int alpha = data.getAlpha();
		if (red < 0 || green < 0 || blue < 0 || alpha < 0) return null;
		if (red > 255 || green > 255 || blue > 255 || alpha > 255) return null;
		return new Color(red, green, blue, alpha);
	}
	
	/**
	 * Returns <code>true</code> if the passed colors are the same, 
	 * <code>false</code> otherwise.
	 * 
	 * @param c1 The color to handle.
	 * @param c2 The color to handle.
	 * @return See above.
	 */
	private boolean isSameColor(Color c1, Color c2)
	{
		if (c1 == null && c2 == null) return true;
		if (c1 == null && c2 != null) return false;
		if (c1 != null && c2 == null) return false;
		return (c1.getRed() == c2.getRed() && c1.getBlue() == c2.getBlue() &&
				c1.getGreen() == c2.getGreen() && 
				c1.getAlpha() == c2.getAlpha());
	}
	
	/**
	 * Returns <code>true</code> if the passed description are the same, 
	 * <code>false</code> otherwise.
	 * 
	 * @param d1 The color to handle.
	 * @param d2 The color to handle.
	 * @return See above.
	 */
	private boolean isSameDescription(String d1, String d2)
	{
		if (d1 == null && d2 == null) return true;
		if (d1 == null && d2 != null) return false;
		if (d1 != null && d2 == null) return false;
		return d1.trim().equals(d2.trim());
	}
	
	/**
	 * Handles the selection of a cell
	 * 
	 * @param cell The selected cell.
	 * @param well The well to handle.
	 * @param results The collection of objects to update.
	 */
	private void handleCellSelection(CellDisplay cell, WellImageSet well,
			List<DataObject> results)
	{
		String description = cell.getDescription();
		Color c = cell.getHighlight();
		WellData data = (WellData) well.getHierarchyObject();
		data.setWellType(description);
		well.setDescription(description);
		results.add(data);
		if (c == null || !cell.isSpecified()) {
			data.setRed(null);
		} else {
			data.setRed(c.getRed());
			data.setGreen(c.getGreen());
			data.setBlue(c.getBlue());
			data.setAlpha(c.getAlpha());
		}
		well.setHighlight(c);
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param parent	The parent of the wells.
	 * @param wells 	The collection to wells the model is for.
	 */
	WellsModel(Object parent, Set<WellData> wells)
	{
		super();
		if (wells  == null) 
			throw new IllegalArgumentException("No wells.");
		wellDimension = null;
		this.parent = parent;
		wellNodes = sortByRow(DataBrowserTranslator.transformHierarchy(wells, 
				DataBrowserAgent.getUserDetails().getId(), 0));
		PlateData plate = (PlateData) parent;
		int columSequenceIndex = plate.getColumnSequenceIndex();
		int rowSequenceIndex = plate.getRowSequenceIndex();
		selectedField = plate.getDefaultSample();
		if (selectedField < 0) selectedField = 0;
		Set<ImageDisplay> samples = new HashSet<ImageDisplay>();
		cells = new HashSet<CellDisplay>();
        rows = -1;
        columns = -1;
        int row, column;
		Iterator j = wellNodes.iterator();
		WellImageSet node;
		ImageNode selected;
		int f;
		String columSequence;
		String rowSequence;
		Map<Integer, ColourObject> cMap = new HashMap<Integer, ColourObject>();
		Map<Integer, ColourObject> rMap = new HashMap<Integer, ColourObject>();
		WellData data;
		String type;
		ColourObject co;
		Color color;
		while (j.hasNext()) {
			node = (WellImageSet) j.next();
			row = node.getRow();
			column = node.getColumn();
			data = (WellData) node.getHierarchyObject();
			type = data.getWellType();
			if (cMap.containsKey(column)) {
				co = cMap.get(column);
				color = createColor(data);
				if (!isSameColor(co.getColor(), color) ||
						!isSameDescription(co.getDescription(), type)) {
					co.setColor(null);
					co.setDescription(null);
					cMap.put(column, co);
				}
			} else {
				cMap.put(column, new ColourObject(createColor(data), type));
			}
			
			if (rMap.containsKey(row)) {
				co = rMap.get(row);
				color = createColor(data);
				if (!isSameColor(co.getColor(), color) ||
						!isSameDescription(co.getDescription(), type)) {
					co.setColor(null);
					co.setDescription(null);
					rMap.put(row, co);
				}
			} else {
				rMap.put(row, new ColourObject(createColor(data), type));
			}
			if (row > rows) rows = row;
			if (column > columns) columns = column;
			columSequence = "";
			if (columSequenceIndex == PlateData.ASCENDING_LETTER)
				columSequence = EditorUtil.LETTERS.get(column+1);
			else if (columSequenceIndex == PlateData.ASCENDING_NUMBER)
				columSequence = ""+(column+1);
			rowSequence = "";
			if (rowSequenceIndex == PlateData.ASCENDING_LETTER)
				rowSequence = EditorUtil.LETTERS.get(row+1);
			else if (rowSequenceIndex == PlateData.ASCENDING_NUMBER)
				rowSequence = ""+(row+1);
			node.setCellDisplay(columSequence, rowSequence);
			f = node.getNumberOfSamples();
			if (fieldsNumber < f) fieldsNumber = f;
			node.setSelectedWellSample(selectedField);
			selected = node.getSelectedWellSample();
			samples.add(selected);
			if (((DataObject) selected.getHierarchyObject()).getId() >= 0 &&
					wellDimension == null) {
				wellDimension = selected.getThumbnail().getOriginalSize();
			}
		}
		
		columns++;
		rows++;
		
		CellDisplay cell;
		for (int k = 1; k <= columns; k++) {
			columSequence = "";
			if (columSequenceIndex == PlateData.ASCENDING_LETTER)
				columSequence = EditorUtil.LETTERS.get(k+1);
			else if (columSequenceIndex == PlateData.ASCENDING_NUMBER)
				columSequence = ""+k;
			cell = new CellDisplay(k-1, columSequence);
			co = cMap.get(k-1);
			if (co != null) {
				cell.setHighlight(co.getColor());
				cell.setDescription(co.getDescription());
			}
			samples.add(cell);
			cells.add(cell);
		}
		for (int k = 1; k <= rows; k++) {
			rowSequence = "";
			if (rowSequenceIndex == PlateData.ASCENDING_LETTER)
				rowSequence = EditorUtil.LETTERS.get(k);
			else if (rowSequenceIndex == PlateData.ASCENDING_NUMBER)
				rowSequence = ""+k;
			
			cell = new CellDisplay(k-1, rowSequence, CellDisplay.TYPE_VERTICAL);
			co = rMap.get(k-1);
			if (co != null) {
				cell.setHighlight(co.getColor());
				cell.setDescription(co.getDescription());
			}
			samples.add(cell);
			cells.add(cell);
		}
		String title = null;
		if (parent instanceof PlateData) {
			title = ((PlateData) parent).getName();
		}
        browser = BrowserFactory.createBrowser(samples, title);
		layoutBrowser(LayoutFactory.PLATE_LAYOUT);
	}
	
	/**
	 * Returns the number of fields per well
	 * 
	 * @return See above.
	 */
	int getFieldsNumber() { return fieldsNumber; }
	
	/**
	 * Returns the selected field, the default value is <code>0</code>.
	 * 
	 * @return See above.
	 */
	int getSelectedField() { return selectedField; }
	
	/**
	 * Views the selected field. 
	 * 
	 * @param index 	The index of the field to view.
	 */
	void viewField(int index)
	{
		if (index < 0 || index >= fieldsNumber) return;
		selectedField = index;
		Set<ImageDisplay> samples = new HashSet<ImageDisplay>();
		List l = getNodes();
		Iterator i = l.iterator();
		WellImageSet well;
		int row = -1;
		int col = -1;
		Collection c = browser.getSelectedDisplays(); 
		Map<Integer, Integer> location = new HashMap<Integer, Integer>();
		WellSampleNode selected;
		if (c != null && c.size() > 0) {
			Iterator j = c.iterator();
			Object object;
			while (j.hasNext()) {
				object = j.next();
				if (object instanceof WellSampleNode) {
					selected = (WellSampleNode) object;
					location.put(selected.getRow(), selected.getColumn());
				}
			}
		}
		List<ImageDisplay> nodes = new ArrayList<ImageDisplay>();
		while (i.hasNext()) {
			well = (WellImageSet) i.next();
			well.setSelectedWellSample(index);
			selected = (WellSampleNode) well.getSelectedWellSample();
			row = selected.getRow();
			if (location.containsKey(row)) {
				col = location.get(row);
				if (selected.getColumn() == col) nodes.add(selected);
			}
			samples.add(selected);
		}
		samples.addAll(cells);
		browser.refresh(samples, nodes);
		layoutBrowser(LayoutFactory.PLATE_LAYOUT);
		//quietly save the field.
		PlateData plate = (PlateData) parent;
		plate.setDefaultSample(selectedField);
		List<DataObject> list = new ArrayList<DataObject>();
		list.add(plate);
		DataBrowserLoader loader = new PlateSaver(component, list);
		loader.load();
	}
	
	/**
	 * Sets the values for the row or the column.
	 * Returns the collection of wells to update.
	 * 
	 * @param cell The selected cell.
	 */
	void setSelectedCell(CellDisplay cell)
	{
		if (cell == null) return;
		List<DataObject> results = new ArrayList<DataObject >();
		List l = getNodes();
		Iterator i = l.iterator();
		WellImageSet well;
		int index = cell.getIndex();
		if (cell.getType() == CellDisplay.TYPE_HORIZONTAL) {
			while (i.hasNext()) {
				well = (WellImageSet) i.next();
				if (well.getColumn() == index) {
					handleCellSelection(cell, well, results);
				}
			}
		} else {
			while (i.hasNext()) {
				well = (WellImageSet) i.next();
				if (well.getRow() == index) {
					handleCellSelection(cell, well, results);
				}
			}
		}
		if (results.size() > 0) {
			DataBrowserLoader loader = new PlateSaver(component, results);
			loader.load();
		}
	}
	
	/**
	 * Creates a concrete loader.
	 * @see DataBrowserModel#createDataLoader(boolean, Collection)
	 */
	protected DataBrowserLoader createDataLoader(boolean refresh, 
			Collection ids)
	{
		List l = getNodes();
		Iterator i = l.iterator();
		ImageSet node;
		List<ImageData> images = new ArrayList<ImageData>();
		ImageNode selected;
		WellSampleData data;
		while (i.hasNext()) {
			node = (ImageSet) i.next();
			if (node instanceof WellImageSet) {
				selected = ((WellImageSet) node).getSelectedWellSample();
				data = (WellSampleData) selected.getHierarchyObject();
				if (data.getId() < 0)
					selected.getThumbnail().setFullScaleThumb(
							Factory.createDefaultImageThumbnail(
									wellDimension.width, wellDimension.height));
				else 
					images.add(data.getImage());
			}
		}

		if (images.size() == 0) return null;
		return new ThumbnailLoader(component, images);
	}
	
	/**
	 * Returns the type of this model.
	 * @see DataBrowserModel#getType()
	 */
	protected int getType() { return DataBrowserModel.WELLS; }
	
	/**
	 * No-operation implementation in our case.
	 * @see DataBrowserModel#getNodes()
	 */
	protected List<ImageDisplay> getNodes() { return wellNodes; }
	
}

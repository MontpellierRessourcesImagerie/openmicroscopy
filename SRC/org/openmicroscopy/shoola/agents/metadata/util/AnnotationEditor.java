/*
 * org.openmicroscopy.shoola.agents.metadata.util.AnnotationEditor 
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
package org.openmicroscopy.shoola.agents.metadata.util;


//Java imports
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.util.ui.UIUtilities;


/** 
 * Edits the passed annotation.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since OME3.0
 */
public class AnnotationEditor 
	extends JDialog
	implements ActionListener, DocumentListener
{

	/** Bound property indicating the tag has been edited. */
	public static final String	EDIT_PROPERTY = "edit";
	
	/** Action command id to cancel the edition. */
	private static final int	CANCEL = 0;
	
	/** Action command id to save the edition. */
	private static final int	SAVE = 1;
	
	/** Button to cancel the edition. */
	private JButton			cancel;
	
	/** Button to save the data. */
	private JButton			save;
	
	/** The area where to enter the text. */
	private JTextArea		area;
	
	/** The text entered in the {@link #area} when initialized. */
	private String			originalText;
	
	/** Initializes the components. */
	private void initComponents()
	{
		cancel = new JButton("Cancel");
		cancel.setToolTipText("Cancel edition.");
		cancel.addActionListener(this);
		cancel.setActionCommand(""+CANCEL);
		save = new JButton("OK");
		save.setToolTipText("Save edition.");
		save.addActionListener(this);
		save.setActionCommand(""+SAVE);
		save.setEnabled(false);
		//area = new MultilineLabel(originalText);
		area = new JTextArea(originalText);
		area.setBorder(
					BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		area.getDocument().addDocumentListener(this);
		getRootPane().setDefaultButton(save);
	}
	
	/** 
	 * Enables or not the {@link #save} button depending on the 
	 * text entered.
	 */
	private void handleTextModification()
	{
		String text = area.getText();
		text = text.trim();
		save.setEnabled(!originalText.equals(text));
	}
	
	/** Closes the window and disposes. */
	private void cancel()
	{
		setVisible(false);
		dispose();
	}
	
	/** Saves the edition. */
	private void save()
	{
		String text = area.getText();
		firePropertyChange(EDIT_PROPERTY, originalText, text);
		cancel();
	}
	
	/**
	 * Builds and lays out the tool bar.
	 * 
	 * @return See above.
	 */
	private JPanel buildBar()
	{
		JPanel bar = new JPanel();
		bar.setBorder(null);
		bar.add(cancel);
		bar.add(Box.createHorizontalStrut(5));
		bar.add(save);
		bar.add(Box.createHorizontalStrut(5));
		return UIUtilities.buildComponentPanelRight(bar);
	}
	
	/** Builds and lays out the UI. */
	private void buildGUI()
	{
		Container c = getContentPane();
        c.setLayout(new BorderLayout(0, 0));
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        p.add(area, BorderLayout.CENTER);
        p.add(buildBar(), BorderLayout.SOUTH);
        add(p, BorderLayout.CENTER);
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param owner 		The owner of the dialog.
	 * @param originalText	The text to edit. Mustn't be <code>null</code>.
	 */
	public AnnotationEditor(JFrame owner, String originalText)
	{
		super(owner);
		if (originalText == null)
			throw new IllegalArgumentException("No annotation to edit.");
		this.originalText = originalText;
		setTitle("Edit Annotation");
		setModal(true);
		initComponents();
		buildGUI();
		setSize(250, 150);
	}

	/**
	 * Saves or cancels.
	 * @see ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		int index = Integer.parseInt(e.getActionCommand());
		switch (index) {
			case CANCEL:
				cancel();
				break;
			case SAVE:
				save();
		}
	}
	
	/**
	 * Enables or not the {@link #save} control.
	 * @see DocumentListener#insertUpdate(DocumentEvent)
	 */
	public void insertUpdate(DocumentEvent e) { handleTextModification(); }

	/**
	 * Enables or not the {@link #save} control.
	 * @see DocumentListener#removeUpdate(DocumentEvent)
	 */
	public void removeUpdate(DocumentEvent e) { handleTextModification(); }
	
	/**
	 * Required by the {@link DocumentListener} I/F but no-op implementation 
	 * in our case.
	 * @see DocumentListener#changedUpdate(DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent e) {}
	
}

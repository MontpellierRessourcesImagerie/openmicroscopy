/*
 * org.openmicroscopy.shoola.agents.util.ui.MovieExportParameters 
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2009 University of Dundee. All rights reserved.
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
package org.openmicroscopy.shoola.agents.util.ui;

//Java imports

//Third-party libraries

//Application-internal dependencies

/** 
 * Object hosting the parameters used to create a movie.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author Donald MacDonald &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:donald@lifesci.dundee.ac.uk">donald@lifesci.dundee.ac.uk</a>
 * @version 3.0
 * <small>
 * (<b>Internal version:</b> $Revision: $Date: $)
 * </small>
 * @since 3.0-Beta4
 */
public class MovieExportParameters
{

	/** Movie across z-section. */
	public static final int		Z_MOVIE = 0;
	
	/** Movie across time. */
	public static final int		T_MOVIE = 1;
	
	/** Movie across time. */
	public static final int		ZT_MOVIE = 2;
	
	/** Identify the <code>mpeg</code> format. */
	public static final int		MPEG = 0;
	
	/** The default number of frames per second. */
	static final int 			DEFAULT_FPS  = 25;

	/** The extension corresponding to the {@link #MPEG} movie. */
	private static final String MPEG_EXTENSION = ".avi";
		
	/** The name of the image. */
	private String 	name;
	
	/** The number of frame per second. */
	private int 	fps;
	
	/** The selected format. */
	private int 	format;
	
	/** The scale bar if displayed. */
	private int 	scaleBar;
	
	/** The lower bound of the time interval. */
	private int		startT;
	
	/** The upper bound of the time interval. */
	private int		endT;
	
	/** The lower bound of the z-section interval. */
	private int		startZ;
	
	/** The upper bound of the z-section interval. */
	private int		endZ;
	
	/** Movie either across time of z-section. */
	private int		type;
	
	/** 
	 * Controls if the passed type is supported.
	 * 
	 * @param type The value to check.
	 */
	private void checkType(int type)
	{
		switch (type) {
			case Z_MOVIE:
			case T_MOVIE:
			case ZT_MOVIE:
				break;
			default:
				throw new IllegalArgumentException("Type not supported.");
		}
	}
	
	/** 
	 * Controls if the passed format is supported.
	 * Returns the name with the extension corresponding to the selected format.
	 * 
	 * @param value The value to check.
	 * @param name  The name of the movie.
	 * @return See above.
	 */
	private String checkFormat(int value, String name)
	{
		switch (value) {
			case MPEG:
				if (!name.endsWith(MPEG_EXTENSION))
					name += MPEG_EXTENSION;
				return name;
			default:
				throw new IllegalArgumentException("Format not supported.");
		}
	}
	
	/** Initializes the time and z-section intervals. */
	private void initialize()
	{
		startT = -1;
		startZ = -1;
		endT = -1;
		endZ = -1;
	}
	
	/**
	 * Creates a new instance.
	 * 
	 * @param name		The name of the file.
	 * @param fps		The number of frames per second.
	 * @param format 	The selected format.
	 * @param scaleBar	The scale bar. Set to <code>0</code> 
	 * 					if the scale bar is not visible.
	 * @param type		Movie either across time or z-section.
	 */
	MovieExportParameters(String name, int fps, int format, int scaleBar, 
			int type)
	{
		if (name == null || name.trim().length() == 0)
			throw new IllegalArgumentException("No name specified.");
		checkType(type);
		this.name = checkFormat(format, name);
		this.type = type;
		if (fps < 0) fps = DEFAULT_FPS;
		this.format = format;
		if (scaleBar < 0) scaleBar = 0;
		this.scaleBar = scaleBar;
		initialize();
	}
	
	/**
	 * Sets the time interval.
	 * 
	 * @param startT The lower bound of the time interval.
	 * @param endT	The upper bound of the time interval.
	 */
	void setTimeInterval(int startT, int endT)
	{
		this.startT = startT;
		this.endT = endT;
	}
	
	/**
	 * Sets the z-section interval.
	 * 
	 * @param startZ The lower bound of the time interval.
	 * @param endZ	The upper bound of the time interval.
	 */
	void setZsectionInterval(int startZ, int endZ)
	{
		this.startZ = startZ;
		this.endZ = endZ;
	}
	
	/**
	 * Returns the name of the file.
	 * 
	 * @return See above.
	 */
	public String getName() { return name; }
	
	/**
	 * Returns the number of frame per second.
	 * 
	 * @return See above.
	 */
	public int getFps() { return fps; }
	
	/**
	 * Returns the format. One of the constants defined by this class.
	 * 
	 * @return See above.
	 */
	public int getFormat() { return format; }
	
	/**
	 * Returns the scale Bar.
	 * 
	 * @return See above.
	 */
	public int getScaleBar() { return scaleBar; }
	
	/**
	 * Returns <code>true</code> if the scale bar is visible, <code>false</code>
	 * otherwise.
	 * 
	 * @return See above.
	 */
	public boolean isScaleBarVisible() { return scaleBar != 0; }

	/**
	 * Returns the lower bound of the time interval.
	 * 
	 * @return See above.
	 */
	public int getStartT() { return startT; }
	
	/**
	 * Returns the upper bound of the time interval.
	 * 
	 * @return See above.
	 */
	public int getEndT() { return endT; }
	
	/**
	 * Returns the lower bound of the z-section interval.
	 * 
	 * @return See above.
	 */
	public int getStartZ() { return startZ; }
	
	/**
	 * Returns the upper bound of the z-section interval.
	 * 
	 * @return See above.
	 */
	public int getEndZ() { return endZ; }
	
	/**
	 * Returns the type of movie to create.
	 * One of the following constants: {@link #Z_MOVIE}, {@link #T_MOVIE}
	 * and {@link #ZT_MOVIE}.
	 * 
	 * @return See above.
	 */
	public int getType() { return type; }
	
	
}

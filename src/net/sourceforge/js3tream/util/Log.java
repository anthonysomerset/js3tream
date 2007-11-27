/*******************************************************
 * 
 *  @author spowell
 *  Log.java
 *  Dec 20, 2006
 *  $Id: Log.java,v 1.1 2007/01/05 20:40:07 shaneapowell Exp $
 Copyright (C) 2006  Shane Powell

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 ******************************************************/
package net.sourceforge.js3tream.util;

/*******************************************************
 *
 ******************************************************/
public class Log
{
	
	private static final Integer semaphore_ = new Integer(1); 
	
	private static boolean info_ = true;
	private static boolean warn_ = true;
	private static boolean error_ = true;
	private static boolean debug_ = true;

	/******************************************************
	 * 
	 ******************************************************/
	private Log()
	{
	}
	
	/********************************************************
	 * @return
	 *******************************************************/
	public static boolean isInfo()
	{
		return Log.info_;
	}
	
	/********************************************************
	 * @return
	 *******************************************************/
	public static boolean isWarn()
	{
		return Log.warn_;
	}
	
	/********************************************************
	 * @return
	 *******************************************************/
	public static boolean isError()
	{
		return Log.error_;
	}
	
	/********************************************************
	 * @return
	 *******************************************************/
	public static boolean isDebug()
	{
		return Log.debug_;
	}
	
	/********************************************************
	 * @param info
	 * @param warn
	 * @param error
	 * @param debug
	 *******************************************************/
	public static void init(boolean info, boolean warn, boolean error, boolean debug)
	{
		Log.info_ = info;
		Log.warn_ = warn;
		Log.error_ = error;
		Log.debug_ = debug;
	}
	
	
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void info(String message)
	{
		if (Log.info_)
		{
			printMessage(message);
		}
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void warn(String message)
	{
		if (Log.warn_)
		{
			printMessage(message);
		}
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void error(String message)
	{
		if (Log.error_)
		{
			printMessage(message);
		}
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void error(String message, Exception e)
	{
		if (Log.error_)
		{
			printMessage(message);
			e.printStackTrace();
		}
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void debug(String message)
	{
		if (Log.debug_)
		{
			printMessage(message);
		}
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void debug(String message, Exception e)
	{
		if (Log.debug_)
		{
			printMessage(message);
			e.printStackTrace();
		}
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	public static void exception(String message, Exception e)
	{
		printMessage(message);
	}
	
	/*******************************************************
	 * @param message
	 *******************************************************/
	private static void printMessage(String message)
	{
		synchronized(Log.semaphore_)
		{
			System.err.print(message);
		}
	}
	
}

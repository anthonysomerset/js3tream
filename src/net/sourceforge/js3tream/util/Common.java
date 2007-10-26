/*******************************************************
 * 
 *  @author spowell
 *  Common.java
 *  Dec 19, 2006
 *  $Id: Common.java,v 1.1 2007/01/05 20:40:07 shaneapowell Exp $
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

import java.security.MessageDigest;


/*******************************************************
 * A suite of common utility methods.
 ******************************************************/
public class Common
{
	
	
	
	/*******************************************************
	 * @param data
	 * @return
	 *******************************************************/
	public static String toHex(byte[] data)
	{
		StringBuffer sb = new StringBuffer(data.length * 2);
		for (int i = 0; i < data.length; i++)
		{
			String hex = Integer.toHexString(data[i]);
			
			if (hex.length() == 1)
			{
				// Append leading zero.
				sb.append("0");
			}
			else if (hex.length() == 8)
			{
				// Remove ff prefix from negative numbers.
				hex = hex.substring(6);
			}
			
			sb.append(hex);
		}
		
		return sb.toString().toLowerCase();
	}

    

	/*******************************************************
	 * @param bytes
	 * @param length
	 * @return
	 *******************************************************/
	public static String computeMD5Hash(byte[] bytes, int length) throws Exception
	{
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		messageDigest.update(bytes, 0, length);
		return Common.toHex(messageDigest.digest());
	}
	

}

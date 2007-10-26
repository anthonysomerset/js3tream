/*******************************************************
 * 
 *  @author spowell
 *  Access.java
 *  Dec 18, 2006
 *  $Id: Access.java,v 1.1 2007/01/05 20:40:07 shaneapowell Exp $
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/*******************************************************
 *
 ******************************************************/
public class Access
{

	
	
	public static final String HMAC_SHA1 = "HmacSHA1";
	public static final String S3SIG_PREFIX = "AmazonS3";

	
	private static String accessKey_ = null;
	private static String accessSecret_ = null;
	private Date date_ = new Date();
	
	/** 2005-01-31T23:59:59.183Z */
	private DateFormat calFormat_ = null;
	
	
	private Mac mac_ = null;
	
	
	
	/*******************************************************
	 * Initialize the Access class with our ID and Key
	 * 
	 * @param key
	 * @param secret
	 *******************************************************/
	public static void init(String key, String secret)
	{
		Access.accessKey_ = key;
		Access.accessSecret_ = secret;
	}
	
	
	/*******************************************************
	 * @throws Exception
	 ******************************************************/
	public Access() throws Exception
	{
		this.calFormat_ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		this.calFormat_.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.mac_ = Mac.getInstance(HMAC_SHA1);
		this.mac_.init(new SecretKeySpec(Access.accessSecret_.getBytes(), HMAC_SHA1));
		
	}
	
	/******************************************************
	 * @return
	 *******************************************************/
	public final String getAccessKey()
	{
		return Access.accessKey_;
	}
	
	
	/********************************************************
	 * @return
	 *******************************************************/
	public Calendar getAccessCalendar()
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(this.date_);
		return cal;
	}
	
	
	/********************************************************
	 * @param operation
	 * @param cal
	 * @return
	 *******************************************************/
	public String generateSignature(String operation)
	{
			
		String sig = S3SIG_PREFIX + operation + calFormat_.format(this.date_);
		byte[] b64 = Base64.encodeBase64(this.mac_.doFinal(sig.getBytes())); 
        	return new String(b64);
        
	}
	
}


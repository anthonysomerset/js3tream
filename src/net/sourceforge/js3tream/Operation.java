/*******************************************************
 * 
 *  @author spowell
 *  Operation.java
 *  Dec 19, 2006
 *  $Id: Operation.java,v 1.2 2007/01/05 20:39:55 shaneapowell Exp $
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
package net.sourceforge.js3tream;


import java.net.URL;

import com.amazonaws.s3.doc._2006_03_01.AmazonS3_PortType;
import com.amazonaws.s3.doc._2006_03_01.AmazonS3_ServiceLocator;

/*******************************************************
 * This interface represents all operations.  An operation
 * class is created and then started. 
 ******************************************************/
public abstract class Operation
{

	/** The default bucket/prefix separator, and default prefix/object separator */
	public static final String BUCKET_PREFIX_SEPARATOR = ":";
	
	/** The bucket object prefix separator */
	public static final String OBJECT_PREFIX_SEPARATOR = ":";
	
	/** The default mime type  */
	public static String MIMETYPE_OCTET_STREAM = "application/octet-stream";
	
	/** The date format string used to convert to and from time values */
	public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
	
	/** The default size of a single bucket sub-entry in bytes. */
	public static final int DEFAULT_BUCKET_OBJECT_BYTES = 5000000; 
	
	/** The maximum number of S3 activity attempts before we consider the operation a failure */
	public static final int MAX_S3_READWRITE_ATTEMPTS = 2;
	
	/** After a failure, go to sleep for this time until the next try. */
	public static final int FAIL_SLEEP_TIME = 250;
	
	/** After a set of 5 failures, the process will wait this many minutes befor trying again */
	public static final int NEVER_DIE_SLEEP_TIME = 10;

	/** Each bucket can have a tag field associated with it */
	public static final String META_DATA_TAG = "tag";
	
	/** When a list of keys is requested, this is the size of the list we get by default.  
	 * S3 supports paging for very large key lists. */
	public static final int MAX_KEYS_PER_LIST = 250;

	/* Pretend mode */
	private boolean pretend_ = false;
	
	private String bucket_ = null;
	
	private String prefix_ = null;
	
	/** The amazon port */
	private AmazonS3_PortType s3Port_ = null;
	
	/*******************************************************
	 * @param pretend
	 *******************************************************/
	public Operation(boolean pretend, String bucket)
	{
		this.pretend_ = pretend;
		this.bucket_ = bucket;
		
		/* Check for a prefix */
		if ((this.bucket_ != null) && (this.bucket_.indexOf(BUCKET_PREFIX_SEPARATOR) != -1))
		{
			// TODO 
			/* Make sure that the user has not over used the ":" separator character */
			String[] parts = this.bucket_.split(BUCKET_PREFIX_SEPARATOR, 2);
			this.bucket_ = parts[0];
			this.prefix_ = parts[1];
		}
	}
	
	
	/******************************************************
	 * @param pretend
	 * @param bucket
	 * @param prefix
	 ******************************************************/
	public Operation(boolean pretend, String bucket, String prefix)
	{
		this.pretend_ = pretend;
		this.bucket_ = bucket;
		this.prefix_ = prefix;
	}
	
	/******************************************************
	 * @return
	 *******************************************************/
	public boolean isPretendMode()
	{
		return this.pretend_;
	}
	
	
	/*******************************************************
	 * @return
	 *******************************************************/
	public String getBucketName()
	{
		return this.bucket_;
	}

	
	/*******************************************************
	 * Return the optional prefix. This will return a 
	 * null if no prefix was specified.
	 * @return
	 *******************************************************/
	public String getPrefixName()
	{
		return this.prefix_;
	}
	
	/*******************************************************
	 * Generate an object key for the given index. This will
	 * result in a stirng formatted as [prefix]SEPARATOR[index].
	 * eg.  A1:00000000000000023
	 * @param index
	 * @return
	 *******************************************************/
	public String generateObjectKey(long index)
	{
		String key = String.format("%020d", index);
		
		if (getPrefixName() != null)
		{
			key = getPrefixName() + OBJECT_PREFIX_SEPARATOR + key;
		}
		
		return key;
	}
	
	/*******************************************************
	 * start the requested operation
	 * @throws Exception
	 *******************************************************/
	public abstract void start() throws Exception;

	
	
	
	/********************************************************
	 * This method will return the currently cached port that
	 * was created either by this method, or by createS3Port(URL).
	 * IF the port was created,this method will ALWAYS return 
	 * the currently created on.  If you wish to force a new 
	 * port to be created for things like a 307 redirect, call
	 * the createS3Port(URL)
	 * @return
	 *******************************************************/
	public AmazonS3_PortType getS3Port() throws Exception
	{
		if (this.s3Port_ == null)
		{
			this.s3Port_ = new AmazonS3_ServiceLocator().getAmazonS3();
		}
		
		return this.s3Port_;
	}
	
	
	/********************************************************
	 * Unlike the above getS3port() this method will ALWAYS
	 * cause the creation of a new port object.  If you call this
	 * method once, you can call getS3Port() in the future
	 * to obtain the already created port.
	 * @return
	 *******************************************************/
	public AmazonS3_PortType createS3Port(String endpoint) throws Exception
	{
		AmazonS3_ServiceLocator loc = new AmazonS3_ServiceLocator();
		loc.setAmazonS3EndpointAddress(endpoint);
		this.s3Port_ = loc.getAmazonS3();
		return this.s3Port_;
	}
	
	
}
	
	

/*******************************************************
 * 
 *  @author spowell
 *  Restore.java
 *  Dec 19, 2006
 *  $Id: ListOperation.java,v 1.2 2007/01/05 20:39:55 shaneapowell Exp $
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


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.js3tream.util.Access;
import net.sourceforge.js3tream.util.Log;

import com.amazonaws.s3.doc._2006_03_01.AmazonS3_ServiceLocator;
import com.amazonaws.s3.doc._2006_03_01.GetObjectResult;
import com.amazonaws.s3.doc._2006_03_01.ListAllMyBucketsEntry;
import com.amazonaws.s3.doc._2006_03_01.ListAllMyBucketsResult;
import com.amazonaws.s3.doc._2006_03_01.ListBucketResult;
import com.amazonaws.s3.doc._2006_03_01.ListEntry;

/*******************************************************
 * Create a new restore operation
 ******************************************************/
public class ListOperation extends Operation
{
	
//	protected long itemCount_ = 0;
//	protected double totalSize_ = 0;
//	
	private SimpleDateFormat df_ = new SimpleDateFormat(DATE_FORMAT);
	
	
	private Map<String, BucketArchiveDesc> archives_ = new TreeMap<String,BucketArchiveDesc>();
	
	/*******************************************************
	 * @param bucket
	 * @return
	 * @throws Exception
	 *******************************************************/
	public ListOperation(String bucket) throws Exception
	{
		super(false, bucket);
	}
	
	/******************************************************
	 * @param archiver
	 ******************************************************/
	public ListOperation(String bucket, String prefix) throws Exception
	{
		super(false, bucket, prefix);
	}
	
	
	/*******************************************************
	 * Override
	 * @see org.sourceforge.js3tar.io.Operation#start()
	 *******************************************************/
	@Override
	public void start() throws Exception
	{
		
//		this.totalSize_ = 0;
//		this.totalSize_ = 0;
		
		
		/* If no bucket is specified, then list the buckets */
		if (getBucketName() == null)
		{
		
			Log.info("Bucket List\n");
			
			/* Get the list of files from S3 */
			ListAllMyBucketsResult listResult = null;
	
			/* Get a page of results */
			listResult = getBucketList();
			
			long bucketCount = 0;
			
			/* Process this list of keys */
			for (ListAllMyBucketsEntry listEntry : listResult.getBuckets())
			{
				String bucket = listEntry.getName();
				Log.info(this.df_.format(listEntry.getCreationDate().getTime()) + " - " + bucket + "\n");				
				bucketCount++;
			}
			
			
			Log.info(String.format("total: %d\n", bucketCount));
			
		}
		else
		{
			
			/* If a bucket is specified, then list the files in the bucket */
			String marker = null;
			
			/* Get the list of files from S3 */
			ListBucketResult listResult = null;
			
			/* This do while loop deals with the paging of s3 lists */
			do
			{
				/* Get a page of results */
				listResult = getBucketObjectList(marker, getBucketName(), getPrefixName(), MAX_KEYS_PER_LIST);
				
				if (listResult.getContents() != null)
				{
					/* Process this list of keys */
					for (ListEntry listEntry : listResult.getContents())
					{
						marker = listEntry.getKey();
						processListEntry(listEntry);
						
					}
					
				}
						
			}
			while(listResult.isIsTruncated());
			
			
			/* Now that each object has been processed, dump the results to the screen */
			Iterator keyIterator = this.archives_.keySet().iterator();
			while (keyIterator.hasNext())
			{
				
				BucketArchiveDesc arch = this.archives_.get(keyIterator.next());
				
				String bucketPrefixName = getBucketName();
				String size = size = String.format("%.2fk", arch.kBytes);
				
				if (arch.kBytes > 10000)
				{
					size = String.format("%.2fM", arch.kBytes / 1000);
				}

				if ((arch.prefix != null) && (arch.prefix.length() > 0))
				{
					bucketPrefixName += OBJECT_PREFIX_SEPARATOR + arch.prefix;
				}
				
				/* Dump the archive into to the screen */
				Log.info(String.format("%s - %s - %s in %d data blocks\n", 
													this.df_.format(arch.lastModifiedDate), 
													bucketPrefixName, 
													size,
													arch.objectCount));
				
				/* Append the tag, if it exists */
				if (arch.tag != null)
				{
					Log.info("\tTAG: " + arch.tag + "\n");
				}
				
			}
			
			
			Log.info(String.format("total: %d\n", this.archives_.size()));
			
		}
							
		
	}
	
	
	
	/*******************************************************
	 * Process the list entry, calculating and storing the 
	 * data desired into a Map.
	 * @param key
	 *******************************************************/
	private void processListEntry(ListEntry listEntry) throws Exception
	{
		
		Log.debug(this.df_.format(listEntry.getLastModified().getTime()) + " - " + listEntry.getKey() + " - " + (listEntry.getSize() / 1000) + "K\n");
		
		BucketArchiveDesc arch = null;
		
		String key = listEntry.getKey();
		String[] keyParts = key.split(OBJECT_PREFIX_SEPARATOR);
		String tag = null;
		String prefix = "";

		/* If the key contains a prefix, then we'll extract it here */
		if (keyParts.length > 1)
		{
			prefix = keyParts[0];
		}
		
		
		Date timestamp = listEntry.getLastModified().getTime();
		double kBytes = listEntry.getSize() / 1000.0;
		
		/* Look for an existing entry in our map.  If it's there, then add to it. */
		if (this.archives_.containsKey(prefix))
		{
			arch = this.archives_.get(prefix);
			arch.objectCount++;
			arch.kBytes += kBytes;
			if (arch.lastModifiedDate.compareTo(timestamp) < 0)
			{
				arch.lastModifiedDate = timestamp;
			}
		}
		else
		{
			/* Otherwise, create a new entry  */
			arch = new BucketArchiveDesc();
			arch.prefix = prefix;
			arch.tag = tag;
			arch.objectCount = 1;
			arch.kBytes = kBytes;
			arch.lastModifiedDate = timestamp;
			
			/* Given this is the first object found with this prefix, we'll use this
			 * key to get it's metadata for the TAG value */
			Access access = new Access();
			GetObjectResult result = new AmazonS3_ServiceLocator().getAmazonS3().getObject(getBucketName(),
																							key,
																							true,
																							false,
																							false,
																							access.getAccessKey(),
																							access.getAccessCalendar(),
																							access.generateSignature("GetObject"),
																							null);
			
			/* get the meta data TAG value */
			for (int index = 0; index < result.getMetadata().length; index++)
			{
				if (META_DATA_TAG.equals(result.getMetadata()[index].getName()))
				{
					arch.tag = result.getMetadata()[index].getValue();
					break;
				}
			}
			
			/* Add this archive object to our map */
			this.archives_.put(prefix, arch);
		}
		
	}
	
	
	/*******************************************************
	 * Get a list of keys.  the marker parameter is used
	 * in the s3 call to support paging.
	 * 
	 * @param marker
	 * @return
	 *******************************************************/
	public ListAllMyBucketsResult getBucketList() throws Exception
	{
		Access access = new Access();
		
		/* Get the list of files from S3 */
		ListAllMyBucketsResult listResult = null;
		
		/* Get a page of results */
		listResult = getS3Port().listAllMyBuckets( 
											access.getAccessKey(), 
											access.getAccessCalendar(), 
											access.generateSignature("ListAllMyBuckets"));
		
		return listResult;
	}

	
	/*******************************************************
	 * Get a list of keys.  the marker parameter is used
	 * in the s3 call to support paging.
	 * 
	 * @param marker
	 * @return
	 *******************************************************/
	public ListBucketResult getBucketObjectList(String marker, String bucket, String prefix, int size) throws Exception
	{
		Access access = new Access();
		
		/* Get the list of files from S3 */
		ListBucketResult listResult = null;
		
		if (prefix != null)
		{
			prefix += OBJECT_PREFIX_SEPARATOR;
		}
		
		/* Get a page of results */
		listResult = getS3Port().listBucket(bucket,
											prefix, 
											marker, 
											size, 
											null, 
											access.getAccessKey(), 
											access.getAccessCalendar(), 
											access.generateSignature("ListBucket"), 
											null);
		
		return listResult;
	}
	
	
	
	/******************************************************
	 * This is just a simple struct/class to keep track
	 * of a single bucket archive description.
	 ******************************************************/
	private static class BucketArchiveDesc
	{
		public String prefix = null;
		public String tag = null;
		public long objectCount = 0;
		public double kBytes = 0;
		public Date lastModifiedDate = null;
	}
	
}

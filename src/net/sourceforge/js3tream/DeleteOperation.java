/*******************************************************
 * 
 *  @author spowell
 *  Restore.java
 *  Dec 19, 2006
 *  $Id: DeleteOperation.java,v 1.2 2007/01/05 20:39:54 shaneapowell Exp $
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

import net.sourceforge.js3tream.util.Access;
import net.sourceforge.js3tream.util.Log;



import com.amazonaws.s3.doc._2006_03_01.ListBucketResult;
import com.amazonaws.s3.doc._2006_03_01.ListEntry;

/*******************************************************
 * Create a new restore operation
 ******************************************************/
public class DeleteOperation extends Operation
{
	
	/******************************************************
	 * @param archiver
	 ******************************************************/
	public DeleteOperation(String bucket) throws Exception
	{
		super(false, bucket);
	}
	
	
	/*******************************************************
	 * Override
	 * @see org.sourceforge.js3tar.io.Operation#start()
	 *******************************************************/
	@Override
	public void start() throws Exception
	{
		
		long count = 0;

		if (getPrefixName() == null)
		{
			Log.info("Deleting ENTIRE S3 Bucket [" + getBucketName() + "] in 10 seconds.  CTRL-C to cancel\n");
		}
		else
		{
			Log.info("Deleting S3 Bucket:Prefix [" + getBucketName() + BUCKET_PREFIX_SEPARATOR + getPrefixName() + "] in 10 seconds.  CTRL-C to cancel\n");
		}
		
		for (int index = 10; index >= 0; index--)
		{
			
			Log.info(index + " ");
			Thread.sleep(1000);
			if (index == 0)
			{
				Log.info("\n");
			}
		}
		

		Log.info("Deleting...\n");
		
		String marker = null;
		
		/* Get the list of files from S3 */
		ListBucketResult listResult = null;

		
		/* This do while loop deals with the paging of s3 lists */
		do
		{
			/* Get a page of results */
			listResult = getBucketKeyList(marker);
			
			if (listResult.getContents() != null)
			{
			
				/* Get the marker in case there are more pages of keys */
				marker = listResult.getMarker();
						
				
				/* Process this list of keys */
				for (ListEntry listEntry : listResult.getContents())
				{
					deleteS3Object(listEntry.getKey());
					count++;
				}
				
				Log.info(count + " blocks deleted from archive so far\n");
					
			}
			
		}
		while(listResult.isIsTruncated());


		/* Delete the bucket, only if the user did not specify a prefix */
		if (getPrefixName() == null)
		{
			deleteS3Bucket();
			Log.info(count + " total data blocks deleted from archive before removing archive itself\n");
		}
		
	}
	
	/*******************************************************
	 * Get a list of keys.  the marker parameter is used
	 * in the s3 call to support paging.
	 * 
	 * @param marker
	 * @return
	 *******************************************************/
	public ListBucketResult getBucketKeyList(String marker) throws Exception
	{
		Access access = new Access();
		
		/* Get the list of files from S3 */
		ListBucketResult listResult = null;
		
		String prefix = null;
		
		if (getPrefixName() != null)
		{
			prefix = getPrefixName() + OBJECT_PREFIX_SEPARATOR;
		}
		
		/* Get a page of results */
		listResult = getS3Port().listBucket(getBucketName(),
											prefix, 
											marker, 
											MAX_KEYS_PER_LIST, 
											null, 
											access.getAccessKey(), 
											access.getAccessCalendar(), 
											access.generateSignature("ListBucket"), 
											null);
		
		return listResult;
	}
	
	/*******************************************************
	 * @throws Exception
	 *******************************************************/
	public void deleteS3Bucket() throws Exception
	{
		if (isPretendMode())
		{
			return;
		}
		
		Access access = new Access();
		getS3Port().deleteBucket(getBucketName(), access.getAccessKey(), access.getAccessCalendar(), access.generateSignature("DeleteBucket"), null);
	}
	

	/*******************************************************
	 * delete the given archive file from the bucket.
	 * @param key
	 *******************************************************/
	public void deleteS3Object(String key) throws Exception
	{
		if (isPretendMode())
		{
			return;
		}
		
		Access access = new Access();
		getS3Port().deleteObject(getBucketName(), key, access.getAccessKey(), access.getAccessCalendar(), access.generateSignature("DeleteObject"), null);
	}
	
}

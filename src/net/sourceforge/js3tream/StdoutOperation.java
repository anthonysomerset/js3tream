/*******************************************************
 * 
 *  @author spowell
 *  StdinOperation.java
 *  Dec 21, 2006
 *  $Id: StdoutOperation.java,v 1.3 2007/10/26 13:23:29 shaneapowell Exp $
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
 ******************************************************/
package net.sourceforge.js3tream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;


import org.apache.axis.attachments.AttachmentPart;

import net.sourceforge.js3tream.StdinOperation.ExposedByteArrayOutputStream;
import net.sourceforge.js3tream.util.Access;
import net.sourceforge.js3tream.util.Common;
import net.sourceforge.js3tream.util.Log;


import com.amazonaws.s3.doc._2006_03_01.AmazonS3SoapBindingStub;
import com.amazonaws.s3.doc._2006_03_01.AmazonS3_ServiceLocator;
import com.amazonaws.s3.doc._2006_03_01.GetObjectResult;
import com.amazonaws.s3.doc._2006_03_01.ListBucketResult;
import com.amazonaws.s3.doc._2006_03_01.ListEntry;

/*******************************************************
 * Read the S3 bucket data, and write it's data to the 
 * STDOUT stream.
 ******************************************************/
public class StdoutOperation extends Operation
{
	
	private OutputStream os_ = null;
	
	private File tempFile_ = null;
	
	private boolean useFile_ = false;
	
	private ExposedByteArrayOutputStream memoryBuffer_ = null;
	
	private boolean neverDie_ = false;
	
	private double kBytesProcessed_ = 0.0;
	
	/******************************************************
	 * @param pretend IN - pretend to send to S3 or not.
	 * @param bucket IN - the bucket to write the data to.
	 * @param os IN - the output stream to write the data to.
	 ******************************************************/
	StdoutOperation(boolean pretend, boolean neverDie, boolean useFile, String bucket, OutputStream os) throws Exception
	{
		super(pretend, bucket);
		
		this.os_ = os;
		this.useFile_ = useFile;
		this.neverDie_ = neverDie;
		
		
		/* Setup the temp buffer */
		if (!this.useFile_)
		{
			this.memoryBuffer_ = new ExposedByteArrayOutputStream(DEFAULT_BUCKET_OBJECT_BYTES);
		}
		else
		{
			this.tempFile_ = File.createTempFile("s3stream", "io");
			this.tempFile_.deleteOnExit();
		}

	}

	/*******************************************************
	 * Override
	 * @see net.sourceforge.js3tream.Operation#start()
	 *******************************************************/
	@Override
	public void start() throws Exception
	{
		Log.info("Sending S3 data to STDOUT\n");
		Log.info("Bucket [" + getBucketName() + "]\n");
		if (getPrefixName() != null)
		{
			Log.info("Prefix [" + getPrefixName() + "]\n");
		}
		
		ListOperation listOp = new ListOperation(getBucketName(), getPrefixName());
		
		String marker = null;
		
		/* Get the list of files from S3 */
		ListBucketResult listResult = null;
		
		/* This do while loop deals with the paging of s3 lists */
		do
		{
			/* Get a page of results */
			listResult = listOp.getBucketObjectList(marker, getBucketName(), getPrefixName(), MAX_KEYS_PER_LIST);
			
			if (listResult.getContents() != null)
			{
				/* Process this list of keys */
				for (ListEntry listEntry : listResult.getContents())
				{
					marker = listEntry.getKey();
					restoreS3Object(listEntry.getKey());
					
				}
				
			}
					
		}
		while(listResult.isIsTruncated());
		
		
		if (this.kBytesProcessed_ > 10000)
		{
			Log.info(String.format("%.2fM transfered\n", (this.kBytesProcessed_ / 1000.0)));
		}
		else
		{
			Log.info(String.format("%.2fK transfered\n", this.kBytesProcessed_));
		}
		
	}
	
	

	/*******************************************************
	 * Returns the buffer output stream used to store
	 * the bytes before sending to S3.  Each call to this
	 * method will result in a fresh buffer returned.  That
	 * is, the byte array will be reset, or the temp file
	 * will be cleared.
	 * 
	 * @return
	 *******************************************************/
	private OutputStream createBufferOutputStream() throws Exception
	{
		
		if (!this.useFile_)
		{
			this.memoryBuffer_.reset();
			return this.memoryBuffer_;
		}
		else
		{
			return new FileOutputStream(this.tempFile_, false);
		}
		
	}
	
	
	/*******************************************************
	 * Get the buffer in the form of an input stream.
	 * Each call to this method results in the return of a
	 * new input stream object, so once called, hang onto your
	 * reference. 
	 * @return
	 *******************************************************/
	private InputStream createBufferInputStream() throws Exception
	{
		if (!this.useFile_)
		{
			return new ByteArrayInputStream(this.memoryBuffer_.getBytes(), 0, this.memoryBuffer_.getCount());
		}
		else
		{
			return new FileInputStream(this.tempFile_);
		}
		
	}
	
	
	/********************************************************
	 * @param key
	 *******************************************************/
	public void restoreS3Object(String key) throws Exception
	{
		
		Log.info("Fetching S3 object [" + key + "] ");
		
		if (isPretendMode())
		{
			Log.info("\n");
			return;
		}
		
		Access access = new Access();
		
		/* Get the metadata for the given file */
		GetObjectResult result = null;
		
		
		/* Try a few times to get the object */
		/* Put the file, We'll try a few times */
		int attemptCount = 0;
		while (attemptCount < MAX_S3_READWRITE_ATTEMPTS)
		{
			attemptCount++;
			
			try
			{
				
				long startTime = System.currentTimeMillis();
				
				
				AmazonS3_ServiceLocator locator = new AmazonS3_ServiceLocator();
				AmazonS3SoapBindingStub binding = new AmazonS3SoapBindingStub(new URL(locator.getAmazonS3Address()), locator);
				
				result = binding.getObject(getBucketName(),
											key,
											false,
											true,
											false,
											access.getAccessKey(),
											access.getAccessCalendar(),
											access.generateSignature("GetObject"),
											null);
				
				
				long endTime = System.currentTimeMillis();
				
								
				/* Get the attachments. Note, the getAttachments() method will ONLY return the object[] on the first call.  Subsiquent calls will return null */
				Object[] attachments = binding.getAttachments();
				if (attachments.length != 1)
				{
					throw new Exception("The S3 Object returned [" + attachments.length + "] when we expected exactly 1");
				}
				
				/* Setup the MD5 digest */
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");
				
				/* Get the attachment, and pipe it's data to the buffer */
				OutputStream os = createBufferOutputStream();
				AttachmentPart part = (AttachmentPart) attachments[0];
				InputStream attachmentStream = part.getDataHandler().getInputStream();
				long byteCount = 0;
				int b = 0;
				while ((b = attachmentStream.read()) != -1)
				{
					byteCount++;
					messageDigest.update((byte)b);
					os.write(b);
				}
				
				os.flush();
				os.close();
				
				this.kBytesProcessed_ += ((double)byteCount / 1000.0);
				
				Log.info(String.format("%6.02f Kb/s\n", (((double)((double)byteCount * (double)Byte.SIZE)) / 1000D) / ((endTime - startTime) / 1000)));
				Log.debug(byteCount + " bytes written to buffer\n");

				
				/* Calculate the MD5 value */
				String md5 =  Common.toHex(messageDigest.digest());
				
				/* compare md5 hashes */
				if (md5.equals(result.getETag().replaceAll("\"", "")) == false)
				{
					throw new Exception("After getting the S3 object [" + key + "], we compared the md5 hash codes. They did not match\n" + "original: [" + md5 + "]\nS3: [" + result.getETag() + "]");
				}
				
				/* Now, stream the file to stdout */
				byteCount = 0;
				InputStream is = createBufferInputStream();
				while ((b = is.read()) != -1)
				{
					byteCount++;
					this.os_.write(b);
				}
				is.close();
				is = null;
				
				Log.debug(byteCount + " bytes sent to STDOUT\n");
				return;
				
			}
			catch(Exception e)
			{
				Log.warn("Attempt [" + attemptCount + " of " + MAX_S3_READWRITE_ATTEMPTS + "] failed on: [" + key + "]\n");
				Log.debug("S3 Get Failure\n", e);
				
				/* If we've exceeded our  max try count, then fail the whole process */
				if (attemptCount >= MAX_S3_READWRITE_ATTEMPTS)
				{
					
					String warningMessage = attemptCount + " attempts were made to get object [" +  key + "] to S3.\nNone of which were successfull.";
					
					if (this.neverDie_)
					{
						attemptCount = 0;
						Log.warn(warningMessage + "\n we'll try again in " + NEVER_DIE_SLEEP_TIME + " minutes");
						Thread.sleep(NEVER_DIE_SLEEP_TIME * 60 * 1000);
					}
					else
					{
						throw new Exception(warningMessage + "\nHere is the last error", e);
					}
					
				}
				
			} /* end catch */
			
		} /* end while attempt loop */
		
	}

}

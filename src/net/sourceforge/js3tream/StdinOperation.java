/*******************************************************
 * 
 *  @author spowell
 *  StdinOperation.java
 *  Dec 21, 2006
 *  $Id: StdinOperation.java,v 1.3 2007/10/26 13:23:29 shaneapowell Exp $
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;

import javax.activation.DataHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.axis.attachments.SourceDataSource;

import net.sourceforge.js3tream.util.Access;
import net.sourceforge.js3tream.util.Common;
import net.sourceforge.js3tream.util.Log;

import com.amazonaws.s3.doc._2006_03_01.AmazonS3SoapBindingStub;
import com.amazonaws.s3.doc._2006_03_01.AmazonS3_ServiceLocator;
import com.amazonaws.s3.doc._2006_03_01.ListAllMyBucketsEntry;
import com.amazonaws.s3.doc._2006_03_01.ListAllMyBucketsResult;
import com.amazonaws.s3.doc._2006_03_01.ListBucketResult;
import com.amazonaws.s3.doc._2006_03_01.MetadataEntry;
import com.amazonaws.s3.doc._2006_03_01.PutObjectResult;
import com.amazonaws.s3.doc._2006_03_01.StorageClass;

/*******************************************************************************
 * Read the java stdin stream, and write it's data to the S3 bucket. If the data
 * will not fit into a single bucket object, then it will be split into bucket
 * objects of size MAX_BUCKET_ENTRY_BYTES each.
 ******************************************************************************/
public class StdinOperation extends Operation
{

	private InputStream	is_				= null;

	private int			bufferSize_		= DEFAULT_BUCKET_OBJECT_BYTES;

	private String		tag_ = null;
	
	private long		objectCount_	= 0;

	private File tempFile_ = null;
	
	private boolean useFile_ = false;
	
	private ExposedByteArrayOutputStream memoryBuffer_ = null;
	
	private boolean neverDie_ = false;
	
	private double kBytesProcessed_ = 0.0;
	
	/***************************************************************************
	 * @param pretend
	 *            IN - pretend to send to S3 or not.
	 * @param bucket
	 *            IN - the bucket to write the data to.
	 * @param is
	 *            IN - the input stream to read the data from.
	 * @param bufferSize
	 *            IN - the size of the read buffer. if null, the default
	 *            MAX_BUCKET_OBJECT_BYTES is used.
	 **************************************************************************/
	StdinOperation(boolean pretend, boolean neverDie, boolean useFile, String bucket, InputStream is, Integer bufferSize, String tag) throws Exception
	{
		super(pretend, bucket);

		this.tag_ = tag;
		this.useFile_ = useFile;
		this.neverDie_ = neverDie;
		
		this.is_ = is;
		if (bufferSize != null)
		{
			this.bufferSize_ = bufferSize;
		}
		
		/* Setup the temp buffer */
		if (!this.useFile_)
		{
			this.memoryBuffer_ = new ExposedByteArrayOutputStream(this.bufferSize_);
		}
		else
		{
			this.tempFile_ = File.createTempFile("s3stream", "io");
			this.tempFile_.deleteOnExit();
		}
		
	}

	/***************************************************************************
	 * Override
	 * 
	 * @see net.sourceforge.js3tream.Operation#start()
	 **************************************************************************/
	@Override
	public void start() throws Exception
	{
		Log.info("Sending STDIN data to S3\n");
		Log.info("Bucket [" + getBucketName() + "]\n");
		
		if (getPrefixName() != null)
		{
			Log.info("Prefix [" + getPrefixName() + "]\n");
		}

		if (isPretendMode() == false)
		{
			/* Create the bucket if it doesn't yet exist */
			boolean bucketFound = false;

			/* First, create the bucket if it doesn't yet exist */
			ListOperation listOperation = new ListOperation(null, null);
			ListAllMyBucketsResult list = listOperation.getBucketList();

			for (ListAllMyBucketsEntry bucket : list.getBuckets())
			{
				if (getBucketName().equals(bucket.getName()))
				{
					bucketFound = true;
					break;
				}
			}

			/* If it wasn't listed, then create it first */
			if (bucketFound == false)
			{
				createBucket();
			}
			else
			{
				/* If the bucket was found, then check the existance of the prefix */
				if (getPrefixName() == null)
				{
					Log.error("Bucket: " + getBucketName() + " already exists.\nWe cannot send data to an existing bucket, please specify a new bucket name or use a preifx\n");
					return;
				}
				else
				{
					/* Look for our prefix.  If it exists, then throw an error */
					ListBucketResult bucketList = listOperation.getBucketObjectList(null, getBucketName(), getPrefixName(), 1);
					
					if ((bucketList.getContents() != null) && (bucketList.getContents().length > 0))
					{
						Log.error("Bucket: " + getBucketName() + " Prefix: " + getPrefixName() + " already exists.\nWe cannot send data to an existing bucket:prefix, please specify a new bucket:prefix name\n");
						return;
					}
					
				}
				
			}

		}

		/* Setup the MD5 digest */
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");

		/* read into our temp file buffer, one byte at a time */
		long byteCount = 0;
		int b = 0;
		OutputStream bufferOutputStream = null;
		while ((b = this.is_.read()) != -1)
		{

			/* Create a new buffer */
			if (bufferOutputStream == null)
			{
				bufferOutputStream = createBufferOutputStream();
				byteCount = 0;
				
				if (this.bufferSize_ > 1000000)
				{
					Log.info(String.format("Buffering stream to %6.02fM " + (this.useFile_?"file":"memory")  + " buffer\n", (this.bufferSize_ / 1000000D)));
				}
				else
				{
					Log.info(String.format("Buffering stream to %6.02fK " + (this.useFile_?"file":"memory") + " buffer\n", (this.bufferSize_ / 1000D)));
				}
				
			}

			/* Add the byte to the buffer */
			bufferOutputStream.write(b);
			byteCount++;
			
			/* Calculate the MD5 on the way through */
			messageDigest.update((byte)b);
			
			
			/* If this was the end of the buffer, then send the buffer to S3,
			 * and reset the buffer */
			if (byteCount >= this.bufferSize_)
			{
				/* Flush and close the file stream */
				bufferOutputStream.flush();
				bufferOutputStream.close();

				/* Send the bytes off */
				sendS3Object(createBufferInputStream(), byteCount, Common.toHex(messageDigest.digest()));

				/* reset the buffer */
				bufferOutputStream = null;
				byteCount = 0;

			}

		} /* end while */

		/* Since we're now outside the loop, there must be no more data. But..
		 * the current buffer might have some data left to be pushed. So, we'll do
		 * one last put */
		bufferOutputStream.flush();
		bufferOutputStream.close();
		sendS3Object(createBufferInputStream(), byteCount, Common.toHex(messageDigest.digest()));
		bufferOutputStream = null;

		
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
	

	/***************************************************************************
	 * create the bucket.
	 **************************************************************************/
	public void createBucket() throws Exception
	{
		Log.info("Creating New Bucket: " + getBucketName() + "\n");
		Access access = new Access();
		getS3Port().createBucket(getBucketName(), null, access.getAccessKey(), access.getAccessCalendar(), access.generateSignature("CreateBucket"));
	}

	/***************************************************************************
	 * Send the data to S3.
	 * 
	 * @param data
	 * @param key
	 **************************************************************************/
	public void sendS3Object(InputStream is, long length, String md5) throws Exception
	{
		
		this.kBytesProcessed_ += length / 1000;

		/* Calculate the key */
		String key = generateObjectKey(this.objectCount_++);
		
		if (length > 1000000)
		{
			Log.info(String.format("Sending %6.02f M to S3 in the form of object [%s] ", (length / 1000000D), key));
		}
		else
		{
			Log.info(String.format("Sending %6.02f K to S3 in the form of object [%s] ", (length / 1000D), key));
		}
		
		if (isPretendMode())
		{
			Log.info("\n");
			return;
		}
		

		MetadataEntry[] metaData = null;
		StorageClass storageClass = StorageClass.STANDARD;
		Access access = new Access();

		/* Setup the optional meta data */
		if (this.tag_ != null)
		{
			metaData = new MetadataEntry[1];
			metaData[0] = new MetadataEntry(META_DATA_TAG, this.tag_);
		}
		
		
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
				DataHandler dataHandler = new DataHandler(new SourceDataSource(null, MIMETYPE_OCTET_STREAM, new StreamSource(is)));
	            binding.addAttachment(dataHandler);
				
				PutObjectResult result = binding.putObject(getBucketName(), 
															key, 
															metaData , 
															length, 
															null, 
															storageClass, 
															access.getAccessKey(), 
															access.getAccessCalendar(), 
															access.generateSignature("PutObject"), 
															null);
				
								
				long endTime = System.currentTimeMillis();
				
				Log.info(String.format("%6.02f Kb/s\n", (((double)((double)length * (double)Byte.SIZE)) / 1000D) / ((endTime - startTime) / 1000)));
	
				/* compare md5 hashes */
				if (md5.equals(result.getETag().replaceAll("\"", "")) == false)
				{
					throw new Exception("After putting the S3 object [" + key + "], we compared the md5 hash codes. They did not match\n" + "original: [" + md5 + "]\nS3: [" + result.getETag() + "]");
				}
				
				/* If we got here, then all is good */
				return;
				
			}
			catch(Exception e)
			{
				Log.warn("Attempt [" + attemptCount + " of " + MAX_S3_READWRITE_ATTEMPTS + "] failed on: [" + key + "]\n");
				Log.debug("S3 Put Failure\n", e);
				
				/* If we've exceeded our  max try count, then fail the whole process */
				if (attemptCount >= MAX_S3_READWRITE_ATTEMPTS)
				{
					String warningMessage = attemptCount + " attempts were made to send object [" +  key + "] to S3.\nNone of which were successfull.";
					
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
				
			}
			
		}
		
	}
	
	
	/******************************************************
	 * This class extends teh bytearray output stream just
	 * so we can expose the buf object.  This buf object
	 * is used when we create the ByteArrayInputStream
	 * to read from the buffer.
	 *****************************************************/
	protected static class ExposedByteArrayOutputStream extends ByteArrayOutputStream
	{
		/******************************************************
		 * @param size
		 ******************************************************/
		public ExposedByteArrayOutputStream(int size)
		{
			super(size);
		}
		
		/********************************************************
		 * @return
		 *******************************************************/
		public byte[] getBytes()
		{
			return buf;
		}
		
		/********************************************************
		 * @return
		 *******************************************************/
		public int getCount()
		{
			return count;
		}
	}
	
}

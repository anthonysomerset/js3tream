/*******************************************************
 * 
 *  @author spowell
 *  Service.java
 *  Dec 19, 2006
 *  $Id: Command.java,v 1.4 2007/10/26 13:37:36 shaneapowell Exp $
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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import net.sourceforge.js3tream.util.Access;
import net.sourceforge.js3tream.util.Log;


import jargs.gnu.CmdLineParser;

/*******************************************************
 * This is the primary startup class for the js3tar 
 * program.  If you plan to perform any sort of js3tar
 * operation, then this is the class that will kick it off.
 * 
 ******************************************************/
public class Command
{
	
	private static final int MAJOR_VERSION = 0;
	private static final int MINOR_VERSION = 6;
	private static final String DATE_VERSION = "December 17, 2007";
	private static final String LICENSE = "Protected under the LGPL";
	private static final String COPYRIGHT = "Copyright (c) Shane Powell 2007";
	private static final String WEBSITE = "http://js3tream.sourceforge.net";
	
	private static final String VERSION = "JS3tream v" + MAJOR_VERSION + "." + MINOR_VERSION + " - " + DATE_VERSION + "\n" + LICENSE + "\n" + COPYRIGHT + "\n" + WEBSITE;
	
	
	private static final String USAGE =
		VERSION + "\n" +
		"\nUsage:\n" +
			"[-K,--keyfile] <file> - A key/secret properties file. This is the preferred method.\n" +
			"\tThe property file must have 2 entries each on it's own line\n" +
			"\t\tkey=<your key id>\n" +
			"\t\tsecret=<your s3 secret>\n" + 
			"[-k,--key] <key> - The Amazon S3 Key ID.  Not needed with -K option.\n" +
			"[-s,--secret] <secret> - The Amazon S3 Secret Key.  It is not a good idea to use \n" +
			"\tthis option Because someone can do a 'ps' on your system, and see the full command\n" +
			"\tline, including your Secret and key.  Use the -K method instead.\n" +
			"[-b,--bucket] <bucket><:prefix> - The Amazon S3 bucket.  This is what we refer to as\n" +
			"\tan S3 Archive.  You can also specify an optional object prefix with the \":\"\n" +
			"\tcharacter. This allows a single bucket to hold multiple streams of data.  For\n" +
			"\texample \"bucket-1234:A\" puts all the data into bucket \"bucket-1234\" and prefixes\n" +
			"\teach data block with \"A\".  You can then put another stream into \"bucket-1234:B\"\n" +
			"\tand they will not impact each other.\n" +
			"[-i,--in] Read the STDIN stream, and send it to S3.\n" +
			"[-o,--out] - Read the data from S3, and pipe it to STDOUT\n" +
			"[-l,--list] - List the archives on S3. This is basically the list of S3 buckets.  If\n" +
			"\tyou provide a bucket name, it it will list the contents of the bucket\n" +
			"[-d,--delete] - Delete the S3 archive.  If you specified a prefix, then only the data\n" +
			"\tassociated with that bucket:prefix combo is deleted.  If you did not specify a prefix\n" +
			"\tthen ALL bucket data, and the bucket itself will be deleted\n" +
			"[-t,--tag] - Add a description tag to the s3 stream.  This will be displayed with\n" +
			"\tthe -l option.\n" +
			"[-p,--pretend] - Pretend to perform the requested action and output what would have\n" +
			"\thappened to stderr.  Implies -v option.\n" +
			"[-f,--file] - Buffer all stream data into temp file rather than memory.  This is\n" +
			"\taffected by the -z option.  Use this when you want to send very large stream parts.\n" +
			"[-z,--size] - Specify the size in bytes that the stream will be broken into inside the\n" +
			"\tbucket. Default is 5000000 This can impact memory requirements on your system, as a\n" +
			"\tbuffer if this size is created in memroy or on file based on the -f option.  This is\n" +
			"\tonly used when sending data to S3 with the -i option.\n" +
			"[-n,--neverdie] - Specify this option to tell JS3tream to NEVER giveup try to send data\n" +
			"\tto S3.  The default is to make 5 attempts, then quit.  But, if this happens, then we must\n" +
			"\trestart the entire operation.  However, specify the -n option, and JS3tream will trying to\n" +
			"\tcomplete the operation, unless broken by the user.  After 5 failed attempts in a row,\n" +
			"\t JS3tream will wait 30 minutes, and start another 5 retries.  This will repeat forever.\n" +
			"[-v,--verbose] - Output Verbose activity messages to stderr.  This has no impact on your\n" +
			"\t stdin or sdtout streams\n" +
			"[-V, --version] - Output the JS3tream version.\n" +
			"[--debug] - Turn on Debugging output.  This will result in more detailed status messages.\n" +
			"[-h,--help] - Print this usage.\n\n";

	/********************************************************
	 * 
	 *******************************************************/
	private static void printUsage(String extraMessage)
	{
		if (extraMessage != null)
		{
			Log.error(extraMessage + "\n\n");
		}
		
		Log.error(USAGE);
	}
	
	/*******************************************************
	 * @param args
	 *******************************************************/
	public static void main(String args[])
	{
		try
		{
			/* Setup the command line parser */
			CmdLineParser parser = new CmdLineParser();
			
			/* Add our required options */
			CmdLineParser.Option keyfileOpt = parser.addStringOption('K', "keyfile");
			CmdLineParser.Option keyOpt = parser.addStringOption('k', "key");
			CmdLineParser.Option secretOpt = parser.addStringOption('s', "secret");
			CmdLineParser.Option bucketOpt = parser.addStringOption('b', "bucket");
			CmdLineParser.Option stdinOpt = parser.addBooleanOption('i', "stdin");
			CmdLineParser.Option stdoutOpt = parser.addBooleanOption('o', "stdout");
			CmdLineParser.Option listOpt = parser.addBooleanOption('l', "list");
			CmdLineParser.Option tagOpt = parser.addStringOption('t', "tag");
			CmdLineParser.Option deleteOpt = parser.addBooleanOption('d', "delete");
			CmdLineParser.Option pretendOpt = parser.addBooleanOption('p', "pretend");
			CmdLineParser.Option verboseOpt = parser.addBooleanOption('v', "verbose");
			CmdLineParser.Option versionOpt = parser.addBooleanOption('V', "version");
			CmdLineParser.Option fileOpt = parser.addBooleanOption('f', "file");
			CmdLineParser.Option sizeOpt = parser.addIntegerOption('z', "size");
			CmdLineParser.Option neverDieOpt = parser.addBooleanOption('n', "neverdie");
			CmdLineParser.Option debugOpt = parser.addBooleanOption("debug");
			CmdLineParser.Option helpOpt = parser.addBooleanOption('h', "help");
			
			
			/* Parse the command line */
			try
			{
				parser.parse(args);
			}
			catch(CmdLineParser.OptionException oe)
			{
				printUsage(oe.getMessage());
				return;
			}
			
			/* get the command line values */
			String keyfile = (String)parser.getOptionValue(keyfileOpt);
			String key = (String)parser.getOptionValue(keyOpt);
			String secret = (String)parser.getOptionValue(secretOpt);
			String bucket = (String)parser.getOptionValue(bucketOpt);
			String tag = (String)parser.getOptionValue(tagOpt);
			Boolean stdin = (Boolean)parser.getOptionValue(stdinOpt, Boolean.FALSE);
			Boolean stdout = (Boolean)parser.getOptionValue(stdoutOpt, Boolean.FALSE);
			Boolean list = (Boolean)parser.getOptionValue(listOpt, Boolean.FALSE);
			Boolean delete = (Boolean)parser.getOptionValue(deleteOpt, Boolean.FALSE);
			Boolean pretend = (Boolean)parser.getOptionValue(pretendOpt, Boolean.FALSE);
			Boolean verbose = (Boolean)parser.getOptionValue(verboseOpt, new Boolean(pretend));
			Boolean version = (Boolean)parser.getOptionValue(versionOpt, Boolean.FALSE);
			Boolean neverDie = (Boolean)parser.getOptionValue(neverDieOpt, Boolean.FALSE);
			Boolean useFile = (Boolean)parser.getOptionValue(fileOpt, Boolean.FALSE);
			Integer size = (Integer)parser.getOptionValue(sizeOpt);
			Boolean debug = (Boolean)parser.getOptionValue(debugOpt, Boolean.FALSE);
			Boolean help = (Boolean)parser.getOptionValue(helpOpt, Boolean.FALSE);
			
			
			
			/* Analyze the command line options, and execute accordingly */
			if (help)
			{
				printUsage(null);
				return;
			}
			
			
			if (version)
			{
				Log.info(VERSION + "\n");
				return;
			}
			
			
			/* Key file */
			if (keyfile != null)
			{
				File propsFile = new File(keyfile);
				if (propsFile.exists() == false)
				{
					throw new Exception("Key Property File [" + keyfile + "] does not exist");
				}
				
				Properties keyProps = new Properties();
				keyProps.load(new FileInputStream(new File(keyfile)));
				
				key = keyProps.getProperty("key");
				secret = keyProps.getProperty("secret");
			}
			
			/* the s3 key id */
			if (key == null)
			{
				printUsage("You must provide an S3 key ID");
				return;
			}
			
			/* The s3 secret */
			if (secret == null)
			{
				printUsage("You must provide an S3 Secret Key");
			}
			
			
			/* Only one action at a time */
			if	(
				((stdin) 		&& (false  || stdout || list  || delete)) ||
				((stdout) 		&& (stdin  || false  || list  || delete)) ||
				((list) 		&& (stdin  || stdout || false || delete)) ||
				((delete) 		&& (stdin  || stdout || list  || false))
				)
			{
				printUsage("You must specify only ONE of the options (-i, -o, -l, -d)");
				return;
			}

			
			
			/* Atleast one action */
			if (!stdin && !stdout && !list && !delete)
			{
				printUsage("You must specify atleast ONE of the action options (-i, -o, -l, -d)");
				return;
			}
			
			
			/* These operations requre a bucket */
			if (stdin || stdout || delete)
			{
				
				if (bucket == null)
				{
					printUsage("You must specify a bucket/S3 archive name with the -b option.");
					return;
				}
				
			}
			
			
			
			
			/* Force verbose */
			if (list || delete)
			{
				verbose = true;
			}
			
			/* Ok, if we got here, then the options needed to perform an action are provided.  
			 * Lets go  */
			
			/* Initialize the Access class */
			Access.init(key, secret);
			
			/* Initialize the Logger */
			Log.init(verbose, true, true, debug);
			if (pretend)
			{
				Log.info("Pretend mode enabled. Forcing verbose\n");
			}
			

			
			/* Create the desired operation object */
			Operation op = null;
			
			if (stdin)
			{
				op = new StdinOperation(pretend, neverDie, useFile, bucket, System.in, size, tag);
			}
			else if (stdout)
			{
				op = new StdoutOperation(pretend, neverDie, useFile, bucket, System.out);
			}
			else if (list)
			{
				op = new ListOperation(bucket);
			}
			else if (delete)
			{
				op = new DeleteOperation(bucket);
			}
			else
			{
				/* This should NOT be possible */
				throw new Exception("Woh.. this can't happen.  We don't know what command to run.");
			}
			
			/* Start it up */
			op.start();
						
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}

		
		
		
	}
	
}

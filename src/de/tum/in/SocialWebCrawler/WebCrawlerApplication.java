package de.tum.in.SocialWebCrawler;

import java.io.IOException;
import java.io.File;

import de.tum.in.Util.Uninterrupted;


public abstract class WebCrawlerApplication {

	private class ShutdownHookThread extends Thread
	{
		@Override public void run()
		{
			terminateCrawl(true);
			closeDatabases();
		}
	}


	protected abstract void crawl(int startSid);

	protected WarcWriter archive;
	protected RelationshipDatabase relDb;
	protected CommentDatabase commentDb;
	protected ProcessingThread thr;
	protected Crawler crawler;
	protected int currentSid;

	private String archiveFileName;
	private String relDbFileName;
	private String commentDbFileName;


	private static void printUsageAndExit(String message, String app, int code)
	{
		if (message != null)
			System.err.println("error: " + message);
		System.err.printf("\nusage: %s [options]\n"+
				"valid options are:\n"+
				"\t-a file\tuse specified web archive file\n"+
				"\t-r file\tuse specified relationship DB file\n"+
				"\t-c file\tuse specified comment DB file\n"+
				"\t-i sid\tstart with the specified story ID\n"+
				"\t-h\tshow this text\n", app);
		
		System.exit(code);
	}

	public WebCrawlerApplication(String[] args)
	{
		archiveFileName = null;
		relDbFileName = null;
		commentDbFileName = null;
		currentSid = 0;

		String app = this.getClass().getSimpleName();

		int i = 0;
		while (i < args.length)
		{
			if ((args[i].length() > 1) && (args[i].charAt(0) == '-'))
			{
				switch (args[i].charAt(1))
				{
				case 'a':
					i++;
					if (i >= args.length)
						printUsageAndExit("missing argument", app, 1);
					archiveFileName = args[i];
					break;
				case 'r':
					i++;
					if (i >= args.length)
						printUsageAndExit("missing argument", app, 1);
					relDbFileName = args[i];
					break;
				case 'c':
					i++;
					if (i >= args.length)
						printUsageAndExit("missing argument", app, 1);
					commentDbFileName = args[i];
					break;
				case 'i':
					i++;
					if (i >= args.length)
						printUsageAndExit("missing argument", app, 1);
					currentSid = Integer.parseInt(args[i]);
					break;
				case 'h':
					printUsageAndExit(null, app, 0);
				default:
					printUsageAndExit("invalid option " + args[i], app, 1);
				}
			}
			else
				printUsageAndExit("invalid parameter " + args[i], app, 1);
			i++;
		}

		Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());
	}

	public void run()
	{
		openDatabases(archiveFileName, relDbFileName, commentDbFileName);
		crawl(currentSid);
		terminateCrawl(false);
		closeDatabases();
	}

	private void terminateCrawl(boolean force)
	{
		if (!force)
		{
			crawler.terminate();
			Uninterrupted.join(crawler);
			thr.terminate();
		}
		else
		{
			crawler.abort();
			thr.abort();
		}

		System.err.printf("crawl complete, next sid = %d\n", currentSid);		
	}

	private void openDatabases(String archiveFileName, String relDbFileName,
			String commentDbFileName)
	{
		try
		{
			if (archiveFileName == null)
			{
				archive = new WarcWriter("slashdot", WebCrawler.userAgent);
				System.err.printf("creating new archive \"%s\"\n",
						archive.getFileName());
			}
			else
			{
				archive = new WarcWriter(new File(archiveFileName),
						WebCrawler.userAgent);
			}
		}
		catch (IOException ex)
		{
			System.err.println("error creating archive: " + ex.getMessage());
			System.exit(1);
		}

		if (relDbFileName == null)
		{
			relDb = new RelationshipDatabase("relations");
			System.err.printf("creating new relationship database \"%s\"\n",
					relDb.getFileName());
		}
		else
			relDb = new RelationshipDatabase(new File(relDbFileName));
		if (commentDbFileName == null)
		{
			commentDb = new CommentDatabase("comments");
			System.err.printf("creating new comment database \"%s\"\n",
					commentDb.getFileName());
		}
		else
			commentDb = new CommentDatabase(new File(commentDbFileName));
	}

	protected void saveDatabases()
	{
		try
		{
			commentDb.write();
			relDb.write();
		}
		catch (IOException ex)
		{
			System.err.println("error writing databases: " +
					ex.getMessage());
		}	
	}

	private synchronized void closeDatabases()
	{
		saveDatabases();
		archive.close();
		System.err.println("all databases saved");
	}

}

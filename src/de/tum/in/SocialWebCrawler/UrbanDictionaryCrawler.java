package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.io.IOException;

import de.tum.in.Util.Time;
import de.tum.in.Util.Uninterrupted;


public class UrbanDictionaryCrawler {

	private class ShutdownHookThread extends Thread
	{
		@Override public void run()
		{
			crawler.abort();
			thr.abort();
			saveDatabases();
		}
	}

	public static final String baseUrl = "http://www.urbandictionary.com";
	
	private WarcWriter archive;
	private DefinitionDatabase db;
	private Crawler crawler;
	private ProcessingThread thr;

	public void run()
	{
		Runtime.getRuntime().addShutdownHook(new ShutdownHookThread());

		createDatabases();

		crawler = new WebCrawler(archive);
		crawler.start();

		// process robots.txt
		RobotRulesProcessor rrProc = new RobotRulesProcessor(
				WebCrawler.userAgent, 4);
		thr = new SingleResourceProcessingThread(rrProc);
		thr.start();
		crawler.addJob(baseUrl + "/robots.txt", thr);
		Uninterrupted.join(thr);

		int interval = rrProc.getFetchInterval();
		System.err.printf("robots.txt specifies a crawl delay of %d seconds, " +
				"waiting\n", interval);
		crawler.setFetchInterval(interval);
		Uninterrupted.sleep(interval * 1000);

		for (int i = 0; i < 27; i++)
		{
			String url = baseUrl + "/popular.php?character=";
			if (i < 26)
				url += (char) ('A' + i);
			else
				url += "%2A";

			UdPopularDefProcessor pdefProc =
				new UdPopularDefProcessor(db, crawler);
			thr = new SingleResourceProcessingThread(pdefProc);
			thr.start();
			crawler.addJob(url, thr);
			Uninterrupted.join(thr);
		}

		crawler.terminate();
		Uninterrupted.join(crawler);
		thr.terminate();

		saveDatabases();
	}

	private void createDatabases()
	{
		try
		{
			archive = new WarcWriter("ud", WebCrawler.userAgent);
		}
		catch (IOException ex)
		{
			System.err.printf("error creating WARC: %s\n", ex.getMessage());
			ex.printStackTrace();
			return;
		}
		System.err.printf("creating new archive \"%s\"\n",
				archive.getFileName());

		db = new DefinitionDatabase();
	}

	private void saveDatabases()
	{
		archive.close();

		// save definition database
		String fileName = "def-" + Time.getTimeStampGmt() + ".dat";
		try
		{
			db.writeToFile(new File(fileName));
		}
		catch (IOException ex)
		{
			System.err.printf("error writing DB: %s\n", ex.getMessage());
			ex.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		UrbanDictionaryCrawler udc = new UrbanDictionaryCrawler();
		udc.run();
	}

}

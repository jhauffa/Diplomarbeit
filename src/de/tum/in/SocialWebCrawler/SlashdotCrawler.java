package de.tum.in.SocialWebCrawler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;

import de.tum.in.Util.Uninterrupted;


public class SlashdotCrawler extends WebCrawlerApplication {

	private static final String baseUrl = "http://slashdot.org"; 
	private static final String statusFileName = "status.slash";

	private int currentOffset;


	public SlashdotCrawler(String[] args)
	{
		super(args); 
	}

	protected void crawl(int startSid)
	{
		crawler = new WebCrawler(archive);
		crawler.start();

		// process robots.txt
		RobotRulesProcessor rrProc = new RobotRulesProcessor(
				WebCrawler.userAgent);
		thr = new SingleResourceProcessingThread(rrProc);
		thr.start();
		crawler.addJob(baseUrl + "/robots.txt", thr);
		Uninterrupted.join(thr);

		// int interval = rrProc.getFetchInterval();
		int interval = rrProc.getFetchInterval() / 10;
		
		System.err.printf("robots.txt specifies a crawl delay of %d seconds, " +
				"waiting\n", interval);
		crawler.setFetchInterval(interval);
		Uninterrupted.sleep(interval * 1000);

		if (startSid == 0)
		{
			// try to read story ID from status file
			readStatusFile();
			if (currentSid == 0)
			{
				// get story ID from index page
				SlashdotIndexProcessor idxProc = new SlashdotIndexProcessor();
				thr = new SingleResourceProcessingThread(idxProc);
				thr.start();
				crawler.addJob(baseUrl, thr);
				Uninterrupted.join(thr);
				currentSid = idxProc.getOldestStoryId();
				System.err.printf("oldest story on index page = %d\n",
						currentSid);
			}
		}
		else
			currentSid = startSid;
		currentOffset = 0;

		// process comments pages (will initiate download and processing of all
		// new users' friend/foe pages)
		SlashdotCommentProcessor commentProc =
			new SlashdotCommentProcessor(commentDb, relDb, crawler);
		while (currentSid > 0)
		{
			thr = new SingleResourceProcessingThread(commentProc);
			thr.start();
			crawler.addJob(baseUrl + "/comments.pl?sid=" +
					Integer.toString(currentSid) +
					"&threshold=-1&mode=flat&no_d2=1&pid=0&startat=" +
					Integer.toString(currentOffset), thr);
			Uninterrupted.join(thr);

			saveDatabases();
			writeStatusFile();

			if (commentProc.getNumComments() == 0)
			{
				currentSid--;
				currentOffset = 0;
			}
			else
				currentOffset += 75;
		}
	}

	private void readStatusFile()
	{
		try
		{
			BufferedReader reader = new BufferedReader(
					new FileReader(statusFileName));
			String line = reader.readLine();
			if (line != null)
			{
				String[] parts = line.split(";");
				if (parts.length == 2)
				{
					currentSid = Integer.parseInt(parts[0]);
					currentOffset = Integer.parseInt(parts[1]);
				}
			}
		}
		catch (IOException ex)
		{
		}
	}

	private void writeStatusFile()
	{
		try
		{
			PrintWriter writer = new PrintWriter(
					new FileOutputStream(statusFileName, false));
			writer.printf("%d;%d\n", currentSid, currentOffset);
			writer.close();
		}
		catch (IOException ex)
		{
		}
	}


	public static void main(String[] args)
	{
		SlashdotCrawler crawlerApp = new SlashdotCrawler(args);
		crawlerApp.run();
	}

}

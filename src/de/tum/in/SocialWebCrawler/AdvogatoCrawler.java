package de.tum.in.SocialWebCrawler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;

import de.tum.in.Util.Uninterrupted;


public class AdvogatoCrawler extends WebCrawlerApplication {

	private static final String baseUrl = "http://www.advogato.org"; 
	private static final String statusFileName = "status.advog";


	public AdvogatoCrawler(String[] args)
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

		int interval = rrProc.getFetchInterval();

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
				AdvogatoIndexProcessor idxProc = new AdvogatoIndexProcessor();
				thr = new SingleResourceProcessingThread(idxProc);
				thr.start();
				crawler.addJob(baseUrl, thr);
				Uninterrupted.join(thr);
				currentSid = idxProc.getNewestStoryId();
				System.err.printf("newest story on index page = %d\n",
						currentSid);
			}
		}
		else
			currentSid = startSid;

		// process comments pages
		AdvogatoCommentProcessor commentProc =
			new AdvogatoCommentProcessor(commentDb);
		while (currentSid > 0)
		{
			thr = new SingleResourceProcessingThread(commentProc);
			thr.start();
			crawler.addJob(baseUrl + "/article/" +
					Integer.toString(currentSid) + ".html", thr,
					new Integer(currentSid));
			Uninterrupted.join(thr);

			saveDatabases();
			writeStatusFile();

			currentSid--;
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
				currentSid = Integer.parseInt(line);
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
			writer.printf("%d\n", currentSid);
			writer.close();
		}
		catch (IOException ex)
		{
		}
	}


	public static void main(String[] args)
	{
		AdvogatoCrawler crawlerApp = new AdvogatoCrawler(args);
		crawlerApp.run();
	}

}

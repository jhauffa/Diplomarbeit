package de.tum.in.SocialWebCrawler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tum.in.Util.RawCharSequence;
import de.tum.in.Util.Uninterrupted;


public class UdPopularDefProcessor extends Processor {

	private static final int numPages = 3;
	private static Pattern defPattern;

	private DefinitionDatabase db;
	private Crawler crawler;
	private MultiResourceProcessingThread thr;

	public UdPopularDefProcessor(DefinitionDatabase db, Crawler crawler)
	{
		this.db = db;
		this.crawler = crawler;
		ensureStaticMembers();
	}

	private synchronized void ensureStaticMembers()
	{
		if (defPattern == null)
			defPattern = Pattern.compile(
					"^<li><a href=\"/define\\.php\\?term=(.+?)\"",
					Pattern.MULTILINE);
	}

	@Override public void parseResource(WebResource res, Object param)
	{
		UdDefinitionProcessor defProc = new UdDefinitionProcessor(db);
		thr = new MultiResourceProcessingThread(defProc);
		thr.start();

		RawCharSequence buf = new RawCharSequence(res.content);
		Matcher matcher;

		matcher = defPattern.matcher(buf);
		while (matcher.find())
		{
			String term = matcher.group(1);
			for (int i = 1; i <= numPages; i++)
				crawler.addJob(UrbanDictionaryCrawler.baseUrl +
						"/define.php?term=" + term + "&page=" + i, thr, term);
		}

		crawler.addJobEnd(thr);
		Uninterrupted.join(thr);
	}

	@Override public void terminate()
	{
		if (thr != null)
			thr.terminate();
	}

	@Override public void abort()
	{
		if (thr != null)
			thr.abort();
	}

}

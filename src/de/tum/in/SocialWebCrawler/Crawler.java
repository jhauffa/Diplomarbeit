package de.tum.in.SocialWebCrawler;

public abstract class Crawler extends Thread implements Task {

	public abstract void setFetchInterval(int interval);
	public abstract int getFetchInterval();

	public void addJob(String url)
	{
		addJob(url, null, null);
	}

	public void addJob(String url, CrawlerNotificationSink sink)
	{
		addJob(url, sink, null);
	}

	public void addJobEnd(CrawlerNotificationSink sink)
	{
		// tell the processor that there will be no further jobs
		addJob(null, sink, null);
	}

	public abstract void addJob(String url, CrawlerNotificationSink sink,
			Object param);

}

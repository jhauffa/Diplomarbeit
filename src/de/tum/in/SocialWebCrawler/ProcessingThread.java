package de.tum.in.SocialWebCrawler;

public abstract class ProcessingThread extends Thread implements Task,
		CrawlerNotificationSink {

	public abstract void notify(WebResource res, Object param);

}

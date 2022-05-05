package de.tum.in.SocialWebCrawler;

public interface CrawlerNotificationSink {

	public void notify(WebResource res, Object param);

}

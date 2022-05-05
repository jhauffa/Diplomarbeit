package de.tum.in.SocialWebCrawler;

public abstract class Processor implements Task {

	public abstract void parseResource(WebResource res, Object param);

	public void terminate()
	{
	}

	public void abort()
	{
	}

}

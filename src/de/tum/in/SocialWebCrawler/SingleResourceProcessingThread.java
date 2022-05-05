package de.tum.in.SocialWebCrawler;

import java.net.HttpURLConnection;

public class SingleResourceProcessingThread extends ProcessingThread {

	private Object mutex;
	private WebResource res;
	private Object param;
	private Processor impl;

	public SingleResourceProcessingThread(Processor impl)
	{
		this.impl = impl;
		mutex = new Object();
		res = null;
	}

	@Override public void run()
	{
		synchronized (mutex)
		{
			while (res == null)
			{
				try
				{
					mutex.wait();
				}
				catch (InterruptedException ex)
				{
				}
			}
		}

		if ((res.resultCode == HttpURLConnection.HTTP_OK) &&
			(res.content != null))
			impl.parseResource(res, param);
		else
			System.err.printf("error %d retrieving %s, skipping\n",
					res.resultCode, res.url);
	}

	@Override public void notify(WebResource res, Object param)
	{
		synchronized (mutex)
		{
			this.res = res;
			this.param = param;
			mutex.notify();
		}
	}

	public void terminate()
	{
		impl.terminate();
	}

	public void abort()
	{
		impl.abort();
	}

}

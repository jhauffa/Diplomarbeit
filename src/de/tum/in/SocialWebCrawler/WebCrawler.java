package de.tum.in.SocialWebCrawler;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import de.tum.in.Util.Uninterrupted;


public class WebCrawler extends Crawler {

	private class Job
	{
		public URL url;
		public CrawlerNotificationSink sink;
		public Object param;

		public Job()
		{
			url = null;
			sink = null;
			param = null;
		}

		public Job(String url, CrawlerNotificationSink sink, Object param)
		{
			this.url = null;
			try
			{
				if (url != null)
					this.url = new URL(url);
			}
			catch (MalformedURLException ex)
			{
			}
			this.sink = sink;
			this.param = param;
		}

		public boolean isEmpty()
		{
			return (url == null);
		}

		public void done(WebResource res)
		{
			if (sink != null)
				sink.notify(res, param);
		}
	}

	private static final int blockSize = 4096;
	private static final int timeout = 10;  // seconds
	public static final String userAgent = "JavaWebCrawler";

	private LinkedList<Job> jobs;
	private boolean running;
	private boolean terminating;
	private int interval;
	private WarcWriter archive;

	public WebCrawler()
	{
		this(null);
	}
	
	public WebCrawler(WarcWriter archive)
	{
		jobs = new LinkedList<Job>();
		running = true;
		interval = 0;
		this.archive = archive;
	}

	private Job getNextJob()
	{
		Job job;
		synchronized (jobs)
		{
			while (jobs.size() == 0)
			{
				if (terminating)
				{
					running = false;
					return new Job();
				}
				try
				{
					jobs.wait();
				}
				catch (InterruptedException ex)
				{
				}
				if (!running)
					return new Job();
			}
			job = jobs.getFirst();
			jobs.removeFirst();
		}
		return job;
	}

	public void run()
	{
		while (running)
		{
			Job job = getNextJob();
			if (job.isEmpty())
			{
				job.done(null);
				continue;
			}

			System.err.println("trying to fetch " + job.url.toString());
			WebResource res = new WebResource(job.url.toString());
			try
			{
				HttpURLConnection conn =
					(HttpURLConnection) job.url.openConnection();
				conn.setConnectTimeout(timeout * 1000);
				conn.setReadTimeout(timeout * 1000);
				conn.setRequestProperty("User-Agent", userAgent);
				conn.setAllowUserInteraction(false);
				conn.setInstanceFollowRedirects(false);
				conn.setUseCaches(false);
				conn.connect();

				// read header
				res.resultCode = conn.getResponseCode();
				String headerField;
				int i = 0;
				while ((headerField = conn.getHeaderField(i)) != null)
				{
					String headerKey = conn.getHeaderFieldKey(i);
					res.addHeader(headerKey, headerField);
					i++;
				}

				// read content
				if (res.resultCode == HttpURLConnection.HTTP_OK)
				{
					InputStream in = conn.getInputStream();
					int contentLength = conn.getContentLength();
					ByteArrayOutputStream buf;
					if (contentLength > 0)
						buf = new ByteArrayOutputStream(contentLength);
					else
						buf = new ByteArrayOutputStream();
					byte[] blockBuf = new byte[blockSize];
					int len;
					while ((len = in.read(blockBuf, 0, blockSize)) != -1)
						buf.write(blockBuf, 0, len);
					res.setContent(buf.toByteArray());
					in.close();
				}

				conn.disconnect();

				try
				{
					if (archive != null)
						archive.write(res);
				}
				catch (IOException ex)
				{
					System.err.println("error writing to archive: " +
							ex.getMessage());
				}

				job.done(res);
			}
			catch (IOException ex)
			{
				res.resultCode = WebResource.INVALID_RESPONSE; 
				job.done(res);
			}

			System.err.println("done, waiting " + Integer.toString(interval) +
					" seconds");
			Uninterrupted.sleep(interval * 1000);
		}
	}

	public void abort()
	{
		synchronized (jobs)
		{
			running = false;
			jobs.notify();
		}
	}

	public void terminate()
	{
		synchronized (jobs)
		{
			terminating = true;
			jobs.notify();
		}		
	}

	public void addJob(String url, CrawlerNotificationSink sink, Object param)
	{
		Job job = new Job(url, sink, param);
		synchronized (jobs)
		{
			jobs.add(job);
			jobs.notify();
		}
	}

	public void setFetchInterval(int interval)
	{
		this.interval = interval;
	}

	public int getFetchInterval()
	{
		return interval;
	}

}

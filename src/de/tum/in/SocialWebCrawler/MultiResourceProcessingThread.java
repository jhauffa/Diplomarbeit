package de.tum.in.SocialWebCrawler;

import java.util.LinkedList;
import java.net.HttpURLConnection;

public class MultiResourceProcessingThread extends ProcessingThread {

	private class Job
	{
		public WebResource res;
		public Object param;

		public Job(WebResource res, Object param)
		{
			this.res = res;
			this.param = param;
		}
	}

	private LinkedList<Job> jobs;
	private boolean running;
	private boolean terminating;
	private Processor impl;

	public MultiResourceProcessingThread(Processor impl)
	{
		this.impl = impl;
		jobs = new LinkedList<Job>();
		running = true;
		terminating = false;
	}

	@Override public void run()
	{
		do
		{
			Job job;
			synchronized (jobs)
			{
				while (jobs.size() == 0)
				{
					if (terminating)
						return;
					try
					{
						jobs.wait();
					}
					catch (InterruptedException ex)
					{
					}
					if (!running)
						return;
				}
				job = jobs.getFirst();
				jobs.removeFirst();
			}

			if ((job.res.resultCode == HttpURLConnection.HTTP_OK) &&
				(job.res.content != null))
				impl.parseResource(job.res, job.param);
			else
				System.err.printf("error %d retrieving %s, skipping\n",
						job.res.resultCode,	job.res.url);
		} while (running);
	}

	@Override public void notify(WebResource res, Object param)
	{
		if (res != null)
		{
			synchronized (jobs)
			{
				jobs.add(new Job(res, param));
				jobs.notify();
			}
		}
		else
			terminate();
	}

	public void terminate()
	{
		synchronized (jobs)
		{
			terminating = true;
			jobs.notify();
		}
		impl.terminate();
	}

	public void abort()
	{
		synchronized (jobs)
		{
			running = false;
			jobs.notify();
		}
		impl.abort();
	}

}

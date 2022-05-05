package de.tum.in.SocialWebCrawler;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class RobotRulesProcessor extends Processor {

	private static final int defaultFetchInterval = 10;

	private String userAgent;
	private int interval;

	public RobotRulesProcessor(String userAgent)
	{
		this(userAgent, defaultFetchInterval);
	}

	public RobotRulesProcessor(String userAgent, int fallbackInterval)
	{
		this.userAgent = userAgent.toLowerCase();
		interval = fallbackInterval;
	}

	public void parseResource(WebResource res, Object param)
	{
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(res.content), "US-ASCII"));

			String currentUserAgent = "";
			int intervalAll = 0, intervalUserAgent = 0;
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.length() == 0)  // record delimiter
				{
					currentUserAgent = "";
					continue;
				}

				// strip comments
				String[] parts = line.split("#");

				// extract key and value
				int pos = parts[0].indexOf(':');
				if (pos < 0)
					continue;
				String key = parts[0].substring(0, pos);
				key = key.trim().toLowerCase();
				if (key.length() == 0)
					continue;
				String value = parts[0].substring(pos + 1);
				value = value.trim();

				if (key.equals("user-agent"))
					currentUserAgent = value.toLowerCase();
				else if (key.equals("crawl-delay"))
				{
					if (currentUserAgent.equals(userAgent))
						intervalUserAgent = Integer.parseInt(value);
					else if (currentUserAgent.equals("*"))
						intervalAll = Integer.parseInt(value);
				}
			}

			if (intervalUserAgent > 0)
				interval = intervalUserAgent;
			else if (intervalAll > 0)
				interval = intervalAll;
		}
		catch (Exception ex)
		{
		}
	}

	public int getFetchInterval()
	{
		return interval;
	}

}

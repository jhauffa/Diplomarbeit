package de.tum.in.SocialWebCrawler;

import java.util.TreeSet;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import de.tum.in.Util.RawCharSequence;


public class SlashdotIndexProcessor extends Processor {

	private TreeSet<Integer> storyIds;

	public SlashdotIndexProcessor()
	{
		storyIds = new TreeSet<Integer>();
	}

	public void parseResource(WebResource res, Object param)
	{
		// Assume that the encoding of the Slashdot front page is ISO Latin-1;
		// for more complex encodings, a charset decoder obtained as follows
		// should be used:
		//   CharsetDecoder dec = Charset.forName("whatever").newDecoder();
		RawCharSequence cs = new RawCharSequence(res.content);

		Pattern pattern = Pattern.compile("comments\\.pl\\?sid\\=(\\d+)\"");
		Matcher matcher = pattern.matcher(cs);
		while (matcher.find())
			storyIds.add(Integer.parseInt(matcher.group(1)));
	}

	public int getOldestStoryId()
	{
		try
		{
			return storyIds.first();
		}
		catch (NoSuchElementException ex)
		{
			return 0;
		}
	}

	public int getNewestStoryId()
	{
		try
		{
			return storyIds.last();
		}
		catch (NoSuchElementException ex)
		{
			return 0;
		}
	}

	public Set<Integer> getStoryIds()
	{
		return storyIds;
	}

}

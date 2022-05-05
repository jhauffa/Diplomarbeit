package de.tum.in.SocialWebCrawler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tum.in.Util.RawCharSequence;

public class AdvogatoCommentProcessor extends Processor {

	private static final int maxComments = 1000;

	private CommentDatabase commentDb;
	private Pattern commentPattern;	
	private int numComments;

	public AdvogatoCommentProcessor(CommentDatabase commentDb)
	{
		this.commentDb = commentDb;

		numComments = 0;
		commentPattern = Pattern.compile(
			"<a name\\=\"(\\d+)\"><b>(.+?)</b></a>, posted (.+?) by " +
			"<a href=\"/person/\\w+/\">(\\w+)</a>.*?</h4>\n" +
			"<blockquote>(.*?)</blockquote>", Pattern.DOTALL);
	}

	public void parseResource(WebResource res, Object param)
	{
		RawCharSequence buf = new RawCharSequence(res.content);
		Matcher matcher;
		int sid = (Integer) param;

		// parse comment
		numComments = 0;
		matcher = commentPattern.matcher(buf);
		while (matcher.find())
		{
			int id = (sid * maxComments) + Integer.parseInt(matcher.group(1));
			Comment c = new Comment(id);
			c.title = matcher.group(2);
			c.date = matcher.group(3);
			c.author = matcher.group(4);
			c.body = matcher.group(5);

			if (c.title.startsWith("Re: "))
			{
				String origTitle = c.title.substring(4);
				// look for closest possible parent comment
				for (int i = id - 1; i >= (sid * maxComments); i--)
				{
					Comment possibleParent = commentDb.query(i);
					if (possibleParent != null)
					{
						if (possibleParent.title.equals(c.title) ||
							possibleParent.title.equals(origTitle))
						{
							c.parentId = possibleParent.id;
							break;
						}
					}
				}
			}
			else
				c.parentId = 0;

			commentDb.append(c);
			numComments++;
		}
	}

	public int getNumComments()
	{
		return numComments;
	}

}

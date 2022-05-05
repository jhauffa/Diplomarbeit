package de.tum.in.SocialWebCrawler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeSet;

import de.tum.in.Util.RawCharSequence;
import de.tum.in.Util.Uninterrupted;


public class SlashdotCommentProcessor extends Processor {

	private CommentDatabase commentDb;
	private RelationshipDatabase relDb;
	private Crawler crawler;
	private TreeSet<String> visitedUsers;
	private MultiResourceProcessingThread thr;

	private Pattern idPattern;
	private Pattern titlePattern;
	private Pattern scorePattern;
	private Pattern headerPattern;
	private Pattern namePattern;
	private Pattern bodyPattern;
	private Pattern parentIdPattern;
	
	private int numComments;

	public SlashdotCommentProcessor(CommentDatabase commentDb,
			RelationshipDatabase relDb, Crawler crawler)
	{
		this.commentDb = commentDb;
		this.relDb = relDb;
		this.crawler = crawler;

		visitedUsers = new TreeSet<String>();
		numComments = 0;

		idPattern = Pattern.compile("comment_(\\d+)\"");
		titlePattern = Pattern.compile("a name\\=\"(\\d+)\">([^<]*)<");
		scorePattern = Pattern.compile("comment_score_(\\d+)\".*?Score:(-?\\d)</a>(, (\\w+))?\\)");
		headerPattern = Pattern.compile("\"by\">by (.*?)</span>.*?comment_otherdetails_(\\d+)\">.*?on ([^\\(]*)\\(", Pattern.DOTALL);
		namePattern = Pattern.compile(">(.*?) \\(");
		bodyPattern = Pattern.compile("comment_body_(\\d+)\">(.*?)<div class\\=\"commentSub", Pattern.DOTALL);
		parentIdPattern = Pattern.compile("pid\\=(\\d+)\">Reply to This</a></b></p></span> <span class\\=\"nbutton\".*?cid\\=(\\d+)\">Parent");
	}

	public void parseResource(WebResource res, Object param)
	{
		SlashdotRelationshipProcessor relProc =
			new SlashdotRelationshipProcessor(relDb);
		thr = new MultiResourceProcessingThread(relProc);
		thr.start();

		RawCharSequence buf = new RawCharSequence(res.content);
		Matcher matcher;

		// comment ID
		numComments = 0;
		matcher = idPattern.matcher(buf);
		while (matcher.find())
		{
			commentDb.append(new Comment(Integer.parseInt(matcher.group(1))));
			numComments++;
		}

		// title
		matcher = titlePattern.matcher(buf);
		while (matcher.find())
		{
			Comment c = commentDb.query(Integer.parseInt(matcher.group(1)));
			if (c == null)
				continue;
			c.title = matcher.group(2);
		}

		// score with score type
		matcher = scorePattern.matcher(buf);
		while (matcher.find())
		{
			Comment c = commentDb.query(Integer.parseInt(matcher.group(1)));
			if (c == null)
				continue;
			c.score = Integer.parseInt(matcher.group(2));
			if (matcher.group(4) != null)
				c.scoreType = matcher.group(4);
		}

		// user name & date
		int numUnknownUsers = 0;
		matcher = headerPattern.matcher(buf);
		while (matcher.find())
		{
			Comment c = commentDb.query(Integer.parseInt(matcher.group(2)));
			if (c == null)
				continue;
			if (!matcher.group(1).equals("Anonymous Coward"))
			{
				Matcher subMatcher = namePattern.matcher(matcher.group(1));
				if (subMatcher.find())
					c.author = subMatcher.group(1);

				// initiate processing of user's friend/foe pages
				if (!visitedUsers.contains(c.author))
				{
					if (relDb.query(c.author) == null)
					{
						String encodedAuthor = c.author.replace(' ', '+');
						SlashdotRelationshipProcessorParam relParam =
							new SlashdotRelationshipProcessorParam(c.author,
									"friend");
						crawler.addJob("http://slashdot.org/~" + encodedAuthor +
								"/friends", thr, relParam);
						relParam =
							new SlashdotRelationshipProcessorParam(c.author, "foe");
						crawler.addJob("http://slashdot.org/~" + encodedAuthor +
								"/foes", thr, relParam);
						numUnknownUsers++;
					}
					visitedUsers.add(c.author);
				}
			}
			c.date = matcher.group(3).trim();
		}
		System.err.printf("minimum download time = %d seconds\n",
				numUnknownUsers * crawler.getFetchInterval());

		// body
		matcher = bodyPattern.matcher(buf);
		while (matcher.find())
		{
			Comment c = commentDb.query(Integer.parseInt(matcher.group(1)));
			if (c == null)
				continue;
			c.body = matcher.group(2);
		}

		// parent comment ID
		matcher = parentIdPattern.matcher(buf);
		while (matcher.find())
		{
			Comment c = commentDb.query(Integer.parseInt(matcher.group(1)));
			if (c == null)
				continue;
			c.parentId = Integer.parseInt(matcher.group(2));
		}

		crawler.addJobEnd(thr);
		Uninterrupted.join(thr);
	}

	public int getNumComments()
	{
		return numComments;
	}

	public void terminate()
	{
		thr.terminate();
	}

	public void abort()
	{
		thr.abort();
	}

}

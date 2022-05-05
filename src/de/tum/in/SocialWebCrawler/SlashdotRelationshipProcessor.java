package de.tum.in.SocialWebCrawler;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import de.tum.in.Util.RawCharSequence;


public class SlashdotRelationshipProcessor extends Processor {

	private RelationshipDatabase db;
	private Pattern pattern;

	public SlashdotRelationshipProcessor(RelationshipDatabase db)
	{
		this.db = db;
		// pattern = Pattern.compile(">(\\w+) \\(\\d+\\)<");
		pattern = Pattern.compile("/\">(.+?) \\(\\d+\\)</a>");
	}

	public void parseResource(WebResource res, Object param)
	{
		SlashdotRelationshipProcessorParam p =
			(SlashdotRelationshipProcessorParam) param;

		// see comments in IndexPageProcessor.java
		RawCharSequence cs = new RawCharSequence(res.content);

		Matcher matcher = pattern.matcher(cs);
		int numMatches = 0;
		while (matcher.find())
		{
			db.append(new Relationship(p.userName, matcher.group(1),
					p.relationshipType));
			numMatches++;
		}
		if (numMatches == 0)
			db.appendEmpty(p.userName);
	}

}

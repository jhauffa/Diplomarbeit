package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import de.tum.in.Util.RawCharSequence;
import de.tum.in.SentimentAnalysis.Corpus;
import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Sentiment;
import de.tum.in.SentimentAnalysis.Word;
import de.tum.in.SentimentAnalysis.TextPolarityClassifier;


public class UdDefinitionProcessor extends Processor {

	private static final String[] paramFiles = {
		"./model/features.param",
		"./model/model.param",
		"./model/subj.features.param",
		"./model/subj.model.param"
	};

	private static Pattern defPattern;
	private static TextPolarityClassifier classifier;

	private DefinitionDatabase db;

	public UdDefinitionProcessor(DefinitionDatabase db)
	{
		this.db = db;
		ensureStaticMembers();
	}

	private synchronized void ensureStaticMembers()
	{
		if (defPattern == null)
			defPattern = Pattern.compile("<div class='definition'>(.+?)</div>",
					Pattern.DOTALL);
		if (classifier == null)
			classifier = new TextPolarityClassifier(
					new File(paramFiles[0]), new File(paramFiles[1]),
					new File(paramFiles[2]), new File(paramFiles[3]));
	}

	@Override public void parseResource(WebResource res, Object param)
	{
		String term = (String) param;
		term.replace("%20", " ");

		double polarity = 0.0;

		RawCharSequence cs = new RawCharSequence(res.content);
		Matcher matcher = defPattern.matcher(cs);
		int numDefinitions = 0;
		while (matcher.find())
		{
			String defBody = stripHtml(matcher.group(1));

			Corpus corpus = classifier.processText(defBody, true);
			int numPos = 0;
			int numNeg = 0;
			for (Sentence s : corpus)
			{			
				for (Word w : s)
				{
					Sentiment.Polarity p =
						Sentiment.ordinalToPolarity(w.getLabel());
					if (p == Sentiment.Polarity.POSITIVE)
						numPos++;
					else if (p == Sentiment.Polarity.NEGATIVE)
						numNeg++;
				}
			}

			int n = numPos + numNeg;
			if (n > 0)
				polarity += (((double) numPos / n) - 0.5) * 2.0;
			numDefinitions++;
		}

		if (numDefinitions > 0)
			polarity /= numDefinitions;

		Double prevPolarity = db.query(term);
		if (prevPolarity != null)
			polarity = (prevPolarity + polarity) / 2.0;
		db.append(term, polarity);
	}

	private static String stripHtml(String source)
	{
		StringBuffer buf = new StringBuffer(source.length());
		boolean inTag = false;
		boolean inEntity = false;
		StringBuffer tagBuffer = null;
		for (int i = 0; i < source.length(); i++)
		{
			char c = source.charAt(i);
			switch (c)
			{
			case '<':
				inTag = true;
				tagBuffer = new StringBuffer();
				break;
			case '>':
				inTag = false;
				if (tagBuffer != null)
				{
					String tag = tagBuffer.toString().substring(0, 2);
					if (tag.equalsIgnoreCase("br"))
						buf.append('\n');
					tagBuffer = null;
				}
				break;
			case '&':
				inEntity = true;
				break;
			case ';':
				inEntity = false;
				break;
			default:
				if (inTag)
					tagBuffer.append(c);
				else if (!inEntity)
					buf.append(c);
				break;
			}
		}
		return buf.toString();
	}

}

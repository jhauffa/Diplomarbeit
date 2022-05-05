package de.tum.in.MPQA;

import de.tum.in.SentimentAnalysis.Sentiment;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SentimentAnnotation implements Annotation {

	private static final String idDirectSubjective = "GATE_direct-subjective";
	private static final String idExpressiveSubjective =
		"GATE_expressive-subjectivity";

	private int start;
	private int end;
	private Sentiment sentiment;

	public SentimentAnnotation()
	{
		clear();
	}

	private void clear()
	{
		start = end = -1;
		sentiment = null;
	}

	public int getStart()
	{
		return start;
	}

	public int getEnd()
	{
		return end;
	}

	public Sentiment getSentiment()
	{
		return sentiment;
	}

	public boolean isEmpty()
	{
		return (sentiment == null);
	}

	public boolean parse(CorpusParticle owner, String line)
	{
		clear();

		String[] token = line.split("\\t");
		if (token.length < 4)
			return false;
		if (!token[2].equals("string"))
			return false;

		String[] value = token[1].split(",");
		if (value.length < 2)
			return false;
		try
		{
			start = Integer.valueOf(value[0]);
			end = Integer.valueOf(value[1]);
		}
		catch (NumberFormatException e)
		{
			return false;
		}

		if (owner.onlyDirect && !token[3].equals(idDirectSubjective))
			return true;
		if (owner.onlyExpressive && !token[3].equals(idExpressiveSubjective))
			return true;
		if (!token[3].equals(idDirectSubjective) &&
			!token[3].equals(idExpressiveSubjective))
			return true;  // empty annotation
		if (token.length < 5)
			return true;  // empty annotation

		// look for subjectivity annotations
		Sentiment.Polarity polarity = Sentiment.Polarity.NEUTRAL;
		double intensity = 1.0;
		double confidence = 1.0;

		Pattern pattern = Pattern.compile("(\\S+?)=\"(.+?)\"");
		Matcher matcher = pattern.matcher(token[4]);
		while (matcher.find())
		{
			String key = matcher.group(1);
			value = (matcher.group(2)).split(",\\s*");

			if (key.equals("intensity"))
			{
				if (value[0].equals("low"))
					intensity *= 0.25;
				else if (value[0].equals("medium"))
					intensity *= 0.5;
				else if (value[0].equals("high"))
					intensity *= 0.75;
				else if (value[0].equals("extreme"))
					intensity *= 1.0;
			}
			else if (key.equals("polarity"))
			{
				String part = value[0];
				if (part.startsWith("uncertain-"))
				{
					confidence *= 0.5;  // TODO
					part = part.substring(10);
				}

				if (part.equals("positive"))
					polarity = Sentiment.Polarity.POSITIVE;
				else if (part.equals("negative"))
					polarity = Sentiment.Polarity.NEGATIVE;
				else if (part.equals("both"))
					polarity = Sentiment.Polarity.BOTH;
				else if (part.equals("neutral"))
					polarity = Sentiment.Polarity.NEUTRAL;
			}
			else if (key.equals("annotation-uncertain") ||
					 key.equals("subjective-uncertain"))
			{
				// TODO: should they be handled separately?
				if (value[0].equals("somewhat-uncertain"))
					confidence *= 0.8;  // TODO
				else if (value[0].equals("very-uncertain"))
					confidence *= 0.5;  // TODO
			}
			else if (key.equals("expression-intensity"))
			{
				// TODO: neutral should be < low
				if (value[0].equals("neutral"))
					// intensity *= 1.0;
					intensity = 0.0;
				else if (value[0].equals("low"))
					intensity *= 0.8;
				else if (value[0].equals("medium"))
					intensity *= 0.9;
				else if (value[0].equals("high"))
					intensity *= 1.1;
				else if (value[0].equals("extreme"))
					intensity *= 1.2;

				if (intensity > 1.0)
					intensity = 1.0;
			}
		}

		sentiment = new Sentiment(polarity, intensity, confidence);
		return true;
	}

}

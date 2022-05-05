package de.tum.in.SocialNetworks;

import java.util.Arrays;

import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Sentiment;
import de.tum.in.SentimentAnalysis.Word;


public class WordPolarityFeature implements MessageFeature {

	public enum Range {
		WHOLE_TEXT, OUTER_N_PERCENT, OUTER_N_SENTENCES, INNER_N_PERCENT,
		REL_SENTENCES, GAUSSIAN_WEIGHTED
	};

	private static final String[] relIndicators = {
		"I", "me", "mine", "my", "our", "ours", "u", "us", "we", "ya", "you",
		"your", "yours" 
	};

	private boolean countPositive;
	private boolean countNegative;
	private boolean perSentence;
	private Range range;
	private double rangeParam1, rangeParam2, rangeParam3;

	public WordPolarityFeature(boolean countPositive, boolean countNegative,
			boolean perSentence)
	{
		this.countPositive = countPositive;
		this.countNegative = countNegative;
		this.perSentence = perSentence;

		range = Range.WHOLE_TEXT;
		rangeParam1 = 0.0;
		rangeParam2 = 0.0;
		rangeParam3 = 0.0;
	}

	public void setRange(Range type, double top, double bottom)
	{
		range = type;
		rangeParam1 = top;
		rangeParam2 = bottom;
	}

	public void setGaussianWeight(double scale, double slope, double shift)
	{
		range = Range.GAUSSIAN_WEIGHTED;
		rangeParam1 = scale;
		rangeParam2 = 2.0 * Math.pow(slope, 2.0);
		rangeParam3 = shift;
	}

	public String getDescription()
	{
		String desc = "relative frequency of ";
		if (countPositive && countNegative)
			desc += "polar ";
		else if (countPositive)
			desc += "positive ";
		else
			desc += "negative ";
		if (perSentence)
			desc += "sentences ";
		else
			desc += "words ";

		String rangeSizeStr = Double.toString(rangeParam1) + "/" +
				Double.toString(rangeParam2);
		switch (range)
		{
		case WHOLE_TEXT:
			desc += "(whole message)";
			break;
		case OUTER_N_PERCENT:
			desc += "(outer " + rangeSizeStr + "% of sentences)";
			break;
		case OUTER_N_SENTENCES:
			desc += "(outer " + rangeSizeStr + " sentences)";
			break;
		case INNER_N_PERCENT:
			desc += "(inner " + rangeSizeStr + "% of sentences)";
			break;
		case REL_SENTENCES:
			desc += "(sentences containing relationship cues)";
			break;
		case GAUSSIAN_WEIGHTED:
			desc += "(whole message, gaussian weight" + rangeParam1 + "/" +
				rangeParam2 + "/" + rangeParam3 + ")";
			break;
		}
		return desc;
	}

	public boolean shouldEvaluate(Sentence s, int i, int n)
	{
		float r = ((float) i / n) * 100.0f;
		switch (range)
		{
		case WHOLE_TEXT:
		case GAUSSIAN_WEIGHTED:
			return true;
		case OUTER_N_PERCENT:
			// always evaluate first and last sentence
			if ((i == 0) || (i == (n - 1)))
				return true;
			if ((r < rangeParam1) || (r > (100 - rangeParam2)))
				return true;
			break;
		case OUTER_N_SENTENCES:
			if ((i < rangeParam1) || ((n - i) <= rangeParam2))
				return true;
			break;
		case INNER_N_PERCENT:
			if ((r >= rangeParam1) || (r <= (100 - rangeParam2)))
				return true;
			break;
		case REL_SENTENCES:
			for (Word w : s)
			{
				int pos = Arrays.binarySearch(relIndicators, w.getWord(),
						String.CASE_INSENSITIVE_ORDER);
				if (pos >= 0)
					return true;
			}	
			break;
		}
		return false;
	}

	public double getValue(ProcessedMessage msg)
	{
		double score = 0.0;
		int num = 0;

		int len = msg.annotatedBody.size();
		int idx = -1;
		double weight = 1.0;
		for (Sentence s : msg.annotatedBody)
		{
			idx++;
			if (!shouldEvaluate(s, idx, len))
				continue;

			if (range == Range.GAUSSIAN_WEIGHTED)
			{
				double x = (double) idx / len;
				weight = 1.0 - (rangeParam1 *
						(Math.exp(-Math.pow((x - 0.5) + rangeParam3, 2.0) /
							rangeParam2)));
				// weight = Math.max(weight, 0.0);
			}

			int numInSentence = 0;
			for (Word w : s)
			{
				Sentiment.Polarity polarity =
					Sentiment.ordinalToPolarity(w.getLabel());

				if (countPositive && (polarity == Sentiment.Polarity.POSITIVE))
					numInSentence++;
				if (countNegative && (polarity == Sentiment.Polarity.NEGATIVE))
					numInSentence++;
			}

			if (perSentence)
			{
				if (numInSentence > 0)
					score += weight;
				num++;
			}
			else
			{
				score += numInSentence * weight;
				num += s.length();
			}
		}

		if (num > 0)
			return score / num;
		return 0.0;
	}

	public static Range stringToRangeType(String s)
	{
		if (s.equals("outer"))
			return Range.OUTER_N_PERCENT;
		else if (s.equals("outer-abs"))
			return Range.OUTER_N_SENTENCES;
		else if (s.equals("inner"))
			return Range.INNER_N_PERCENT;
		else if (s.equals("rel"))
			return Range.REL_SENTENCES;
		else if (s.equals("weighted"))
			return Range.GAUSSIAN_WEIGHTED;
		return Range.WHOLE_TEXT;
	}

}

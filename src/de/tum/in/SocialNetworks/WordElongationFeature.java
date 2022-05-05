package de.tum.in.SocialNetworks;

import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Word;

public class WordElongationFeature implements MessageFeature {

	private boolean perSentence;

	public WordElongationFeature(boolean perSentence)
	{
		this.perSentence = perSentence;
	}

	public String getDescription()
	{
		String desc = "relative frequency of ";
		if (perSentence)
			desc += "sentences containing ";
		desc += "elongated words";
		return desc;
	}

	public double getValue(ProcessedMessage msg)
	{
		int num = 0;
		int numElongated = 0;

		for (Sentence s : msg.annotatedBody)
		{
			int numInSentence = 0;
			for (Word w : s)
			{
				String word = w.getWord();
				int repeatCount = 0;
				char prevChar = '\0';
				for (int i = 0; i < word.length(); i++)
				{
					char curChar = word.charAt(i);
					if (prevChar == curChar)
					{
						if (++repeatCount > 2)
						{
							numInSentence++;
							break;
						}
					}
					else
						repeatCount = 0;
					prevChar = curChar;
				}
			}

			if (perSentence)
			{
				if (numInSentence > 0)
					numElongated++;
				num++;
			}
			else
			{
				numElongated += numInSentence;
				num += s.length();
			}
		}

		if (num > 0)
			return (double) numElongated / num;
		return 0.0;
	}

}

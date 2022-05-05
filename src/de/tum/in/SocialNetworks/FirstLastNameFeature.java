package de.tum.in.SocialNetworks;

import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Word;
import de.tum.in.Linguistics.PersonalName;

public class FirstLastNameFeature implements MessageFeature {

	public String getDescription()
	{
		return "ratio of frequencies of first name and last name";
	}

	public double getValue(ProcessedMessage msg)
	{
		int numFirst = 0;
		int numLastOrFull = 0;

		for (Sentence s : msg.annotatedBody)
		{
			int nameSequenceLength = 0;
			for (Word w : s)
			{
				String word = w.getWord();
				if (PersonalName.isName(word) || PersonalName.isTitle(word))
					nameSequenceLength++;
				else
				{
					// (name) vs. (title) (name)+ and (name)+ (name)
					if (nameSequenceLength == 1)
						numFirst++;
					else if (nameSequenceLength > 1)
						numLastOrFull++;
					nameSequenceLength = 0;
				}
			}
		}

		int sum = numFirst + numLastOrFull;
		if (sum > 0)
			return (double) numFirst / sum;
		return 0.0;
	}

}

package de.tum.in.SocialNetworks;

import de.tum.in.Linguistics.SimpleTokenizer;

public class WordObfuscationFeature implements MessageFeature {

	private boolean perSentence;

	public WordObfuscationFeature(boolean perSentence)
	{
		this.perSentence = perSentence;
	}

	public String getDescription()
	{
		String desc = "relative frequency of ";
		if (perSentence)
			desc += "sentences containing ";
		desc += "obfuscated words";
		return desc;
	}

	public double getValue(ProcessedMessage msg)
	{
		int num = 0;
		int numObfuscated = 0;
		int numObfuscatedSent = 0;

		SimpleTokenizer tok = new SimpleTokenizer(msg.message.getBody());
		while (tok.hasNext())
		{
			String word = tok.next();
			int numSymbols = 0;
			int numLetters = 0;
			for (int i = 0; i < word.length(); i++)
			{
				char c = word.charAt(i);
				if (Character.isLetter(c))
					numLetters++;
				else if (i != (word.length() - 1))
					numSymbols++;
				if (((numLetters >= 1) && (numSymbols >= 1)) ||
					(numSymbols > 3))
				{
					numObfuscated++;
					break;
				}
			}

			if (perSentence)
			{
				if (word.equals("."))
				{
					if (numObfuscated > 0)
					{
						numObfuscatedSent++;
						numObfuscated = 0;
					}
					num++;
				}
			}
			else
				num++;
		}
		if (perSentence)
		{
			if (numObfuscated > 0)
				numObfuscatedSent++;
			num++;
			numObfuscated = numObfuscatedSent;
		}

		if (num > 0)
			return (double) numObfuscated / num;
		return 0.0;
	}

}

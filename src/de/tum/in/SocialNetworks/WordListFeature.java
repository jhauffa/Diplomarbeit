package de.tum.in.SocialNetworks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Word;
import de.tum.in.Util.Trie;


public class WordListFeature implements MessageFeature {

	private String listName;
	private Trie<String, Double> expressions;
	private int maxExprLength;
	private boolean perSentence;

	public WordListFeature(File listFile, String listName, boolean perSentence,
			boolean invertPolarity)
	{
		this.listName = listName;
		this.perSentence = perSentence;
		expressions = new Trie<String, Double>();
		maxExprLength = 0;
		addWordList(listFile, invertPolarity);
	}

	public void addWordList(File listFile)
	{
		addWordList(listFile, false);
	}

	public void addWordList(File listFile, boolean invertPolarity)
	{
		try
		{
			BufferedReader reader =
				new BufferedReader(new FileReader(listFile));
			String line;
			while ((line = reader.readLine()) != null)
			{
				double polarity = 1.0;
				int sepIdx = line.indexOf(';');
				if ((sepIdx >= 0) && ((sepIdx + 1) < line.length()))
				{
					String polarityStr = line.substring(sepIdx + 1);
					try
					{
						polarity = Double.parseDouble(polarityStr);
					}
					catch (NumberFormatException ex)
					{
					}
					if (polarity < 0.0)
					{
						if (invertPolarity)
							polarity = -polarity;
						else
							continue;  // skip expressions with negative pol.
					}
					line = line.substring(0, sepIdx);
				}

				String[] words = line.split("\\s+");
				if (words.length > maxExprLength)
					maxExprLength = words.length;
				expressions.set(words, polarity);
			}
		}
		catch (IOException ex)
		{
			System.err.printf("error reading word list %s: %s\n",
					listFile.getName(), ex.getMessage());
		}
	}

	public String getDescription()
	{
		String desc = "relative frequency/score of ";
		if (perSentence)
			desc += "sentences containing ";
		desc += listName;
		return desc;
	}

	public double getValue(ProcessedMessage msg)
	{
		double score = 0.0;
		int num = 0;

		LinkedList<String> wordQueue = new LinkedList<String>();
		for (Sentence s : msg.annotatedBody)
		{
			double sentenceScore = 0.0;
			int numWords = 0;
			for (Word w : s)
			{
				String word = w.getWord();
				wordQueue.addLast(word);
				if (wordQueue.size() > maxExprLength)
					wordQueue.removeFirst();
				sentenceScore += lookupExpression(wordQueue);
				numWords++;
			}
			while (wordQueue.size() > 1)
			{
				wordQueue.removeFirst();
				sentenceScore += lookupExpression(wordQueue);
			}
			wordQueue.clear();

			if (perSentence)
			{
				if (numWords > 0)
					score += sentenceScore / numWords;
				num++;
			}
			else
			{
				score += sentenceScore;
				num += numWords;
			}
		}

		if (num > 0)
			return (double) score / num;
		return 0.0;
	}

	private double lookupExpression(LinkedList<String> wordQueue)
	{
		LinkedList<String> prefix = new LinkedList<String>(wordQueue);
		while (prefix.size() > 0)
		{
			String[] path = prefix.toArray(new String[prefix.size()]);
			Double v = expressions.get(path); 
			if (v != null)
				return path.length * v;
			prefix.removeLast();
		}
		return 0.0;
	}

}

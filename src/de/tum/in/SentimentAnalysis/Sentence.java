package de.tum.in.SentimentAnalysis;

import java.util.ArrayList;
import java.util.Iterator;


public class Sentence implements Iterable<Word> {

	protected ArrayList<Word> words;
	
	public Sentence()
	{
		words = new ArrayList<Word>();
	}

	public Sentence(Sentence other)
	{
		this();
		for (Word w : other.words)
			this.words.add(new Word(w));
	}

	public void append(Word word)
	{
		words.add(word);
	}

	public void append(String word, Sentiment trueSentiment,
			boolean startsBlock)
	{
		words.add(new Word(word, trueSentiment, startsBlock));
	}

	public boolean isEmpty()
	{
		return words.isEmpty();
	}

	public Iterator<Word> iterator()
	{
		return words.iterator();
	}

	public Word get(int index)
	{
		return words.get(index);
	}

	public int length()
	{
		return words.size();
	}

	public String getSentenceAsString()
	{
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < words.size(); i++)
		{
			if (i > 0)
				buf.append(" ");
			buf.append(words.get(i).getWord());
		}
		return buf.toString();
	}

	public boolean equalsAsString(Sentence other)
	{
		if (words.size() != other.words.size())
			return false;

		Iterator<Word> it1 = words.iterator();
		Iterator<Word> it2 = other.words.iterator();
		while (it1.hasNext())
			if (!it1.next().getWord().equals(it2.next().getWord()))
				return false;

		return true;
	}

	public int[] getTrueLabelDistribution()
	{
		int[] distr = new int[Sentiment.maxNumClasses];
		for (Word w : words)
			distr[w.getTrueLabel()]++;
		return distr;
	}

	public int[] getLabelDistribution()
	{
		int[] distr = new int[Sentiment.maxNumClasses];
		for (Word w : words)
			distr[w.getLabel()]++;
		return distr;		
	}

	public static int getMajorityClass(int[] labelDistr, boolean onlyPolar)
	{
		// Returns the class held by the most words in the sentence, including
		// the neutral class.
		int maxFreq = 0;
		int maxFreqClass = 0;
		int i = 0;
		if (onlyPolar)
			i++;
		for (; i < labelDistr.length; i++)
			if (labelDistr[i] > maxFreq)
			{
				maxFreq = labelDistr[i];
				maxFreqClass = i;
			}
		return maxFreqClass;
	}

	public static int computePolarity(int[] labelDistr, double neutralThreshold)
	{
		// Consider a sentence neutral if the ratio of polar to neutral words is
		// below the specified threshold. Otherwise, the sentence is classified
		// by the class held by the majority of its words. In case of a tie, the
		// class "both" is chosen.
		// Obviously, a higher threshold favors a classifier that is biased
		// towards the neutral class, but classifying only sentences without any
		// polar expressions as neutral might not be representative of reality.
		int numPolar = 0;
		for (int i = 1; i < labelDistr.length; i++)
			numPolar += labelDistr[i];
		if (((double) numPolar / labelDistr[0]) <= neutralThreshold)
			return 0;
		else
		{
			if ((labelDistr[1] > labelDistr[2]) &&
				(labelDistr[1] > labelDistr[3]))
				return 1;
			else if ((labelDistr[2] > labelDistr[1]) &&
					 (labelDistr[2] > labelDistr[3]))
				return 2;
			else  // label "both" most frequent or tie between "pos" and "neg"
				return 3;
		}
	}

}

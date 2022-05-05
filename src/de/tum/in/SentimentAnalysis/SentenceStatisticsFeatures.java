package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class SentenceStatisticsFeatures
	implements FeatureTemplate, Serializable {

	private static final int defaultMaxLength = 20;

	private int numClasses;
	private int maxLength;
	private String[] featureNames;

	public SentenceStatisticsFeatures(int numClasses)
	{
		this.numClasses = numClasses;
		maxLength = defaultMaxLength;

		featureNames = new String[(numClasses * 2) + 1];
		for (int i = 0; i < numClasses; i++)
		{
			featureNames[i] = "SST_length_" + Integer.toString(i + 1);
			featureNames[numClasses + i] = "SST_pos_" + Integer.toString(i + 1);
		}
		featureNames[numClasses * 2] = "SST_multiple_occurrence";
	}

	public int getNumFeatures()
	{
		return (numClasses * 2) + 1;
	}

	public String getFeatureName(int idx)
	{
		return featureNames[idx];
	}

	public String[] getFeatureNames()
	{
		return featureNames;
	}

	public void reset()
	{
		// nothing to do
	}

	public void train(Corpus corpus)
	{
		for (Sentence s : corpus)
		{
			int length = s.length();
			if (length > maxLength)
				maxLength = length;
		}
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int numFeatures = 2;

		// sentence length class
		int lengthClass = (int) Math.round(((float) s.length() / maxLength) *
				(numClasses - 1));
		lengthClass = Math.min(lengthClass, numClasses - 1);
		indices.add(lengthClass);
		values.add(1.0);

		// relative position class
		int posClass = (int) Math.round(((float) pos / s.length()) *
				(numClasses - 1));
		indices.add(numClasses + posClass);
		values.add(1.0);

		// multiple occurrences of the word at index "pos"
		int wordCount = 0;
		String word = s.get(pos).getWord().toLowerCase();
		for (Word w : s)
		{
			String curWord = w.getWord();
			curWord = curWord.toLowerCase();
			if (curWord.equals(word))
				wordCount++;
		}
		if (wordCount > 1)
		{
			indices.add(numClasses * 2);
			values.add(1.0);
			numFeatures++;
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("sentence statistics: length of sentence and position " +
				"of word (%d classes), multiple occurrence of words\n",
				numClasses);
		writer.flush();
	}

}

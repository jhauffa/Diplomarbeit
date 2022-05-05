package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class WordStatisticsFeatures implements FeatureTemplate, Serializable {

	private static final int defaultMaxLength = 20;

	private int numLengthClasses;
	private int maxLength;
	private String[] featureNames;

	public WordStatisticsFeatures(int numLengthClasses)
	{
		this.numLengthClasses = numLengthClasses;
		maxLength = defaultMaxLength;

		featureNames = new String[numLengthClasses];
		for (int i = 0; i < numLengthClasses; i++)
			featureNames[i] = "WST_length_" + Integer.toString(i + 1);
	}

	public int getNumFeatures()
	{
		return numLengthClasses;
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
			for (Word w : s)
			{
				int length = w.getWord().length();
				if (length > maxLength)
					maxLength = length;
			}
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int c = (int) Math.round(
				((float) s.get(pos).getWord().length() / maxLength) *
				(numLengthClasses - 1));
		c = Math.min(c, numLengthClasses - 1);
		indices.add(c);
		values.add(1.0);
		return 1;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("word statistics: length (%d classes)\n",
				numLengthClasses);
		writer.flush();
	}

}

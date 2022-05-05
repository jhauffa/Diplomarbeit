package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class WordShapeFeatures implements FeatureTemplate, Serializable {

	private static final String[] featureNames = {"WSH_capitalized"};

	public String getFeatureName(int idx)
	{
		return featureNames[idx];
	}

	public String[] getFeatureNames()
	{
		return featureNames;
	}

	public int getNumFeatures()
	{
		return featureNames.length;
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		String word = s.get(pos).getWord();
		if (Character.isUpperCase(word.charAt(0)))
		{
			indices.add(0);
			values.add(1.0);
			return 1;
		}
		return 0;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.println("word shape: capitalization");
		writer.flush();
	}

	public void reset()
	{
		// nothing to do
	}

	public void train(Corpus corpus)
	{
		// nothing to do
	}

}

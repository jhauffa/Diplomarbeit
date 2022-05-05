package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class SentenceStructureFeatures
		implements FeatureTemplate, Serializable {

	private static final String[] featureNames =
		{"STR_in_quotation", "STR_in_relative"};

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
		int numFeatures = 0;

		int numQuotationMarks = 0;
		int numCommas = 0;
		for (int i = 0; i < pos; i++)
		{
			String word = s.get(i).getWord(); 
			if (word.equals("\""))
				numQuotationMarks++;
			else if (word.equals(","))
				numCommas++;
		}

		if ((numQuotationMarks % 2) != 0)
		{
			indices.add(0);
			values.add(1.0);
			numFeatures++;
		}
		if ((numCommas % 2) != 0)
		{
			indices.add(1);
			values.add(1.0);
			numFeatures++;
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.println("sentence structure (word is quoted, word is part of " +
				"relative clause)");
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

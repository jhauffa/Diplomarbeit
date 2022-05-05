package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class SentenceTypeFeatures implements FeatureTemplate, Serializable {

	private static final String[] featureNames =
		{"STY_assertion", "STY_exclamation", "STY_question",
		 "STY_is_wh_question"};

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

		String lastWord = s.get(s.length() - 1).getWord();
		if (lastWord.startsWith("."))
		{
			indices.add(0);
			values.add(1.0);
			numFeatures++;
		}
		else if (lastWord.startsWith("!"))
		{
			indices.add(1);
			values.add(1.0);
			numFeatures++;
		}
		else if (lastWord.startsWith("?"))
		{
			indices.add(2);
			values.add(1.0);
			numFeatures++;

			String firstWord = s.get(0).getWord().toLowerCase();
			if (firstWord.startsWith("wh") || firstWord.equals("how"))
			{
				indices.add(3);
				values.add(1.0);
				numFeatures++;
			}
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.println("sentence type (assertion/exclamation/question), " +
				"is wh-question");
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

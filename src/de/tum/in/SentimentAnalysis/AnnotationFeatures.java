package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class AnnotationFeatures implements FeatureTemplate, Serializable {

	private String key;
	private String confStr;
	private int numAnnot;
	private int numTags;
	private String[] featureNames;

	public AnnotationFeatures(AnnotationProvider prov)
	{
		key = prov.getKey();
		numAnnot = prov.getNumAnnot();
		numTags = prov.getNumValues();
		confStr = prov.getConfigurationString();

		createFeatureNames();
	}

	public AnnotationFeatures(String key, int numAnnot, int numTags,
			String confStr)
	{
		this.key = key;
		this.numAnnot = numAnnot;
		this.numTags = numTags;
		this.confStr = confStr;

		createFeatureNames();
	}

	private void createFeatureNames()
	{
		featureNames = new String[numAnnot * numTags];
		for (int i = 0; i < numAnnot; i++)
			for (int j = 0; j < numTags; j++)
			{
				StringBuffer buf = new StringBuffer();
				buf.append("ANN_");
				buf.append(key);
				buf.append(i);
				buf.append('_');
				buf.append(j);
				featureNames[(i * numTags) + j] = buf.toString();
			}		
	}

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

		for (int i = 0; i < numAnnot; i++)
		{
			int annot = s.get(pos).getAnnotation(key + Integer.toString(i));
			if (annot >= 0)
			{
				indices.add((i * numTags) + annot);
				values.add(1.0);
				numFeatures++;
			}
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("%s annotations (%d sets)", key, numAnnot);
		if (!confStr.isEmpty())
			writer.printf(", %s", confStr);
		writer.println();
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

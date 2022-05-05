package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

public class FeaturePositionShifter implements FeatureTemplate, Serializable {

	private FeatureTemplate templ;
	private int offset;
	private transient String[] featureNames;

	public FeaturePositionShifter(FeatureTemplate templ, int offset)
	{
		this.templ = templ;
		this.offset = offset;
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

	public void reset()
	{
		// nothing to do
	}

	public void train(Corpus corpus)
	{
		initFeatures();
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int shiftedPos = pos + offset;
		if ((shiftedPos < 0) || (shiftedPos >= s.length()))
			return 0;
		return templ.getFeaturesForWord(s, shiftedPos, indices, values);
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("offset %d: ", offset);
		writer.flush();
		templ.printConfiguration(out);
	}

	private void readObject(ObjectInputStream ois)
    	throws ClassNotFoundException, IOException
    {
		ois.defaultReadObject();
		initFeatures();
	}

	private void initFeatures()
	{
		int numFeatures = templ.getNumFeatures();
		featureNames = new String[numFeatures];
		for (int i = 0; i < numFeatures; i++)
			featureNames[i] = templ.getFeatureName(i) + "_" +
				Integer.toString(offset);
	}

}

package de.tum.in.SentimentAnalysis;

import java.util.Vector;
import java.util.LinkedList;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


public class FeatureGenerator implements FeatureTemplate {

	private LinkedList<FeatureTemplate> templ;
	private Vector<String> featureNames;
	private int numFeatures;

	public FeatureGenerator()
	{
		templ = new LinkedList<FeatureTemplate>();
		featureNames = new Vector<String>();
	}

	private void clear()
	{
		numFeatures = 0;
		featureNames.clear();		
	}

	public void reset()
	{
		for (FeatureTemplate t : templ)
			t.reset();
		clear();
	}

	public void train(Corpus corpus)
	{
		for (FeatureTemplate t : templ)
			t.train(corpus);
		updateFeatures();
	}

	public void addFeatureTemplate(FeatureTemplate f)
	{
		assert (f != this);
		templ.add(f);
	}

	private void updateFeatures()
	{
		clear();
		for (FeatureTemplate t : templ)
		{
			numFeatures += t.getNumFeatures();
			featureNames.ensureCapacity(numFeatures);
			for (String s : t.getFeatureNames())
				featureNames.add(s);
		}
	}

	public int getNumFeatures()
	{
		return numFeatures;
	}

	public String getFeatureName(int idx)
	{
		return featureNames.elementAt(idx);
	}

	public String[] getFeatureNames()
	{
		return featureNames.toArray(new String[featureNames.size()]);
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int baseIdx = 0;
		int newFeaturesIdx = 0;
		for (FeatureTemplate t : templ)
		{
			int numCurFeatures = t.getFeaturesForWord(s, pos, indices, values);
			for (int i = newFeaturesIdx; i < (newFeaturesIdx + numCurFeatures);
				 i++)
				indices.set(i, baseIdx + indices.get(i));
			baseIdx += t.getNumFeatures();
			newFeaturesIdx += numCurFeatures;
		}
		return newFeaturesIdx;
	}

	public void printConfiguration(OutputStream out)
	{
		for (FeatureTemplate t : templ)
			t.printConfiguration(out);
	}

	public void loadParameters(InputStream in) throws IOException
	{
		ObjectInputStream ois = new ObjectInputStream(in);
		try
		{
			templ = (LinkedList<FeatureTemplate>) ois.readObject();
		}
		catch (ClassNotFoundException ex)
		{
			throw new IOException("malformed parameter file");
		}
		updateFeatures();
	}

	public void saveParameters(OutputStream out) throws IOException
	{
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(templ);
		oos.flush();
	}

}

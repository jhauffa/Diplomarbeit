package de.tum.in.SentimentAnalysis;

import java.util.Vector;
import java.io.OutputStream;

public interface FeatureTemplate {

	public void reset();
	public void train(Corpus corpus);
	public int getNumFeatures();
	public String getFeatureName(int idx);
	public String[] getFeatureNames();
	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values);
	public void printConfiguration(OutputStream out);

}

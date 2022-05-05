package de.tum.in.SentimentAnalysis;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Vector;

public interface Classifier {

	public void train(Corpus corpus, double regularizationParam)
			throws Exception;
	public void trainWithEarlyStopping(Corpus trainingSet, Corpus testSet,
			double regularizationParam)
		throws Exception, UnsupportedOperationException;
	public void process(Corpus corpus) throws Exception;

	public void setTransitionConstraints(boolean[][] constr)
		throws UnsupportedOperationException;

	public void getFeatureNames(Vector<String> names);
	public void getFeatureWeights(int fromState, int toState,
			Vector<Integer> indices, Vector<Double> weights);
	public void getFeatureWeights(Sentence sentence, int pos,
			Vector<Integer> indices, Vector<Double> weights);
	public boolean hasStoppedEarly();

	public enum BiasType { BIAS_FEATURE, BIAS_WEIGHTS_FIXED,
		BIAS_WEIGHTS_RELATIVE };
	public void applyClassBias(int classIndex, double biasValue, BiasType type)
		throws UnsupportedOperationException;

	public void loadModel(InputStream in)
		throws IOException, UnsupportedOperationException;
	public void saveModel(OutputStream out)
		throws IOException, UnsupportedOperationException;
	public void printModel(OutputStream out)
		throws IOException, UnsupportedOperationException;

}

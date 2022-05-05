package de.tum.in.SentimentAnalysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;

import iitb.CRF.CRF;
import iitb.CRF.DataSequence;
import iitb.CRF.DataIter;
import iitb.Model.EdgeFeatures;
import iitb.Model.FeatureGenImpl;
import iitb.Model.FeatureTypes;
import iitb.Model.FeatureImpl;


public class PolarityClassifierCRFSarawagi implements Classifier {

	private class SentenceWrapper implements DataSequence {

		private Sentence sentence;

		public SentenceWrapper(Sentence sentence)
		{
			this.sentence = sentence;
		}

		public int length()
		{
			return sentence.length();
		}

		public int y(int i)
		{
			return sentence.get(i).getTrueLabel();
		}

		public Object x(int i)
		{
			return sentence.get(i);
		}

		public void set_y(int i, int label)
		{
			sentence.get(i).setLabel(label);
		}

		public Sentence getSentence()
		{
			return sentence;
		}

	}

	private class CorpusWrapper implements DataIter {

		private Corpus corpus;
		private Iterator<Sentence> iter;
		
		public CorpusWrapper(Corpus corpus)
		{
			this.corpus = corpus;
		}
		
		public void startScan()
		{
			iter = corpus.iterator();
		}

		public boolean hasNext()
		{
			return iter.hasNext();
		}

		public DataSequence next()
		{
			return new SentenceWrapper(iter.next());
		}

	}

	private class FeatureVectorWrapper extends FeatureTypes {

		private int numStates;
		private FeatureGenerator featureGen;

		private int curFeatureIdx;
		private int curState;
		private int numFeatures;
		private Vector<Integer> indices;
		private Vector<Double> values;
		
		public FeatureVectorWrapper(FeatureGenImpl nativeFeatureGen,
				FeatureGenerator featureGen)
		{
			super(nativeFeatureGen);
			this.featureGen = featureGen;
			this.numStates = nativeFeatureGen.numStates();
		}

		@Override public boolean startScanFeaturesAt(DataSequence data,
				int prevPos, int pos)
		{
			curFeatureIdx = 0;
			curState = 0;
			indices = new Vector<Integer>();
			values = new Vector<Double>();
			numFeatures = featureGen.getFeaturesForWord(
					((SentenceWrapper) data).getSentence(), pos,
					indices, values);
			return (numFeatures > 0);
		}

		@Override public boolean hasNext()
		{
			return (curFeatureIdx < numFeatures);
		}

		@Override public void next(FeatureImpl f)
		{
			int realFeatureIdx = indices.get(curFeatureIdx);
			setFeatureIdentifier(realFeatureIdx * numStates + curState,
						curState, featureGen.getFeatureName(realFeatureIdx), f);

			f.yend = curState;
			f.ystart = -1;
			f.val = values.get(curFeatureIdx).floatValue();

			if (++curState >= numStates)
			{
				curState = 0;
				curFeatureIdx++;
			}
		}

	}

	private class FeatureGeneratorCRF extends FeatureGenImpl {

		public FeatureGeneratorCRF(int numLabels, FeatureGenerator featureGen)
			throws Exception
		{
			super("naive", numLabels, false);
			addFeature(new EdgeFeatures(this));
			addFeature(new FeatureVectorWrapper(this, featureGen));
		}

	}


	private FeatureGenerator featureGen;
	private CRF model;
	private Properties crfOptions;
	private FeatureGeneratorCRF nativeFeatureGen;

	public PolarityClassifierCRFSarawagi(FeatureGenerator featureGen)
	{
		this.featureGen = featureGen;

		crfOptions = new Properties();
		crfOptions.setProperty("maxIters", "10000");
		crfOptions.setProperty("epsForConvergence", "0.01");
		crfOptions.setProperty("invSigmaSquare", "0.01");

		// crfOptions.setProperty("trainer", "Collins");
		// crfOptions.setProperty("maxIters", "100");
	}

	public void setTransitionConstraints(boolean[][] constr)
		throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public void getFeatureNames(Vector<String> names)
	{
		for (String name : featureGen.getFeatureNames())
			names.add(name);
	}

	public void getFeatureWeights(int fromState, int toState,
			Vector<Integer> indices, Vector<Double> weights)
	{
		double[] modelWeights = model.learntWeights();
		for (int i = 0; i < modelWeights.length; i++)
		{
			indices.add(i);
			weights.add(modelWeights[i]);
		}
	}

	public void getFeatureWeights(Sentence sentence, int pos,
			Vector<Integer> indices, Vector<Double> weights)
	{
		double[] modelWeights = model.learntWeights();
		Vector<Double> values = new Vector<Double>();
		featureGen.getFeaturesForWord(sentence, pos, indices, values);
		for (Integer idx : indices)
			weights.add(modelWeights[idx]);
	}

	public void applyClassBias(int classIndex, double biasValue, BiasType type)
		throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public void train(Corpus corpus, double regularizationParam)
			throws Exception
	{
		featureGen.train(corpus);
		int numLabels = corpus.getNumClasses();
		nativeFeatureGen = new FeatureGeneratorCRF(numLabels, featureGen);
		nativeFeatureGen.train(new CorpusWrapper(corpus));
		model = new CRF(numLabels, nativeFeatureGen, crfOptions);
		model.train(new CorpusWrapper(corpus));
	}

	public void trainWithEarlyStopping(Corpus trainingSet, Corpus testSet,
			double regularizationParam)
		throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public boolean hasStoppedEarly()
	{
		return false;
	}

	public void process(Corpus corpus) throws Exception
	{
		for (Sentence s : corpus)
			model.apply(new SentenceWrapper(s));
	}

	public void saveModel(OutputStream out) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public void loadModel(InputStream in) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public void printModel(OutputStream out) throws IOException
	{
		PrintStream p = new PrintStream(out);
		p.println("options:");
		crfOptions.list(p);
		p.println();
		nativeFeatureGen.displayModel(model.learntWeights(), p);
		p.flush();
	}

}

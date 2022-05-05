package de.tum.in.SentimentAnalysis;

import java.util.LinkedList;
import java.util.Vector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;

import cc.mallet.pipe.Noop;
import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.FeatureVectorSequence;
import cc.mallet.types.SparseVector;
import cc.mallet.fst.CRF;
import cc.mallet.fst.CRFOptimizableByLabelLikelihood;
import cc.mallet.fst.CRFTrainerByL1LabelLikelihood;
import cc.mallet.fst.CRFTrainerByLabelLikelihood;


public class PolarityClassifierCRF implements Classifier {

	private static final int maxTrainingIter = Integer.MAX_VALUE;
	private static final int earlyStoppingInterval = 1;
	private static final int earlyStoppingSeqLength = 3;
	private static final String biasFeatureName = "BIAS";

	private FeatureGenerator featureGen;
	private LabelAlphabet labelAlphabet;
	private Alphabet featureAlphabet;
	private CRF model;
	private double sigma;
	private double logLikelihood;
	private boolean[][] constr;
	private int biasFeatureIndex;
	private String biasMethod;
	private boolean stoppedEarly;
	private boolean performFeatureInduction;


	public PolarityClassifierCRF(FeatureGenerator featureGen,
			boolean performFeatureInduction)
	{
		this.featureGen = featureGen;
		this.performFeatureInduction = performFeatureInduction;
	}

	public void setTransitionConstraints(boolean[][] constr)
	{
		this.constr = constr;
	}

	public void getFeatureNames(Vector<String> names)
	{
		Object[] entries = featureAlphabet.toArray();
		for (Object entry : entries)
			names.add((String) entry);
	}

	public void getFeatureWeights(int fromState, int toState,
			Vector<Integer> indices, Vector<Double> weights)
	{
		int weightsIdx = getWeightsIndex(fromState, toState);
		if (weightsIdx < 0)
			return;
		SparseVector transitionWeights = model.getWeights(weightsIdx);
		int[] featureIndices = transitionWeights.getIndices();
		for (int featureIdx : featureIndices)
		{
			indices.add(featureIdx);
			weights.add(transitionWeights.value(featureIdx));
		}
	}

	public void getFeatureWeights(Sentence sentence, int pos,
			Vector<Integer> indices, Vector<Double> weights)
	{
		// retrieve weights for transition
		int fromState = 0;
		if (pos > 0)
			fromState = sentence.get(pos - 1).getLabel();
		int toState = sentence.get(pos).getLabel();
		int weightsIdx = getWeightsIndex(fromState, toState);
		if (weightsIdx < 0)
			return;
		SparseVector transitionWeights = model.getWeights(weightsIdx);

		// get features
		Instance inst = induceFeaturesForSentence(sentence);
		FeatureVectorSequence featureVectors =
			(FeatureVectorSequence) inst.getData();
		FeatureVector features = featureVectors.get(pos);
		int[] featureIndices = features.getIndices();
		for (int featureIdx : featureIndices)
		{
			indices.add(featureIdx);
			weights.add(transitionWeights.value(featureIdx));
		}
	}

	public void train(Corpus corpus, double regularizationParam)
			throws Exception
	{
		trainWithEarlyStopping(corpus, null, regularizationParam);
	}

	public void trainWithEarlyStopping(Corpus trainingSet, Corpus testSet,
			double regularizationParam)
		throws Exception
	{
		featureGen.train(trainingSet);

		// disable bias feature, unless enabled by method applyClassBias
		biasFeatureIndex = -1;

		// create label alphabet
		labelAlphabet = new LabelAlphabet();
		for (int i = 0; i < trainingSet.getNumClasses(); i++)
			labelAlphabet.lookupIndex(Integer.toString(i), true);

		// create feature alphabet
		featureAlphabet = new Alphabet(featureGen.getFeatureNames());

		// transform training data into format expected by MALLET
		InstanceList trainingInst = createInstanceListFromCorpus(trainingSet);

		// create CRF model
		model = new CRF(featureAlphabet, labelAlphabet);
		model.getInputAlphabet().startGrowth();  // undo side effect of CRF()

		if (constr == null)
		{
			//model.addFullyConnectedStatesForThreeQuarterLabels(trainingInst);
			model.addFullyConnectedStatesForLabels();
			// model.addFullyConnectedStatesForBiLabels();
		}
		else
		{
			// add a state for each label and connect states according to the
			// specified transition constraints
			Vector<String> destLabels = new Vector<String>();
			for (int i = 0; i < labelAlphabet.size(); i++)
			{
				String srcLabel = (String) labelAlphabet.lookupObject(i);
				for (int j = 0; j < labelAlphabet.size(); j++)
					if (!constr[i][j])
						destLabels.add((String) labelAlphabet.lookupObject(j));
				if (destLabels.size() > 0)
				{
					String[] destLabelArray = new String[destLabels.size()];
					destLabels.toArray(destLabelArray);
					model.addState(srcLabel, destLabelArray);
				}
				destLabels.clear();
			}
		}

		model.setWeightsDimensionAsIn(trainingInst,
				/* useSomeUnsupportedTrick = */ false);

		// perform training
		CRFTrainerByL1LabelLikelihood trainer =
			new CRFTrainerByL1LabelLikelihood(model, regularizationParam);

//		sigma = 1.0;
//		trainer.setGaussianPriorVariance(sigma);
		sigma = trainer.getGaussianPriorVariance();  // use default

		stoppedEarly = false;
		if (testSet == null)
			invokeTrainer(trainer, trainingInst, maxTrainingIter);
		else
		{
			// train with early stopping
			LinkedList<CRF> modelHistory = new LinkedList<CRF>();
			double prevAccuracy = 0.0;
			int numFaults = 0;
			while (!invokeTrainer(trainer, trainingInst, earlyStoppingInterval))
			{
				process(testSet);
				Evaluator eval = new Evaluator();
				eval.addCorpus(testSet);
				eval.updateEvaluation();
				//double curAccuracy = eval.getAccuracy();
				double curAccuracy = eval.getAvgPolarFScore();
				if (curAccuracy < prevAccuracy)
				{
					if (++numFaults >= earlyStoppingSeqLength)
					{
						model = modelHistory.get(earlyStoppingSeqLength - 1);
						stoppedEarly = true;
						break;
					}
				}
				else
					numFaults = 0;
				prevAccuracy = curAccuracy;
				modelHistory.addFirst(new CRF(model));
				if (modelHistory.size() > earlyStoppingSeqLength)
					modelHistory.removeLast();
			}
		}

		// save log-likelihood
		CRFOptimizableByLabelLikelihood opt =
			trainer.getOptimizableCRF(trainingInst);
		logLikelihood = opt.getValue();
	}

	public boolean hasStoppedEarly()
	{
		return stoppedEarly;
	}

	private boolean invokeTrainer(CRFTrainerByLabelLikelihood trainer,
			InstanceList trainingInst, int numIter)
	{
		if (performFeatureInduction)
			return trainer.trainWithFeatureInduction(trainingInst, null,
					null, null, numIter, 10, 10, 500, 0.5,
					/*clustered = */ false, null, "info");
		else
			return trainer.train(trainingInst, numIter);
	}

	public void applyClassBias(int classIndex, double biasValue, BiasType type)
	{
		StringBuffer method = new StringBuffer();

		switch (type)
		{
		case BIAS_FEATURE:
			biasFeatureIndex = createBiasFeature(classIndex, biasValue);
			method.append("bias feature");
			break;
		case BIAS_WEIGHTS_FIXED:
			adjustFeatureWeights(classIndex, biasValue);
			method.append("biased weights (fixed)");
			break;
		case BIAS_WEIGHTS_RELATIVE:
			double r = getClassWeightRatio(classIndex);
			biasValue = (1.0 - r) / r;
			adjustFeatureWeights(classIndex, biasValue);
			method.append("biased weights (according to weight distribution)");
			break;
		}

		method.append(", class ");
		method.append(classIndex);
		method.append(", value ");
		method.append(biasValue);
		biasMethod = method.toString();
	}

	private int getWeightsIndex(int stateFrom, int stateTo)
	{
		if ((constr != null) && constr[stateFrom][stateTo])
			return -1;

		StringBuffer weightName = new StringBuffer(6);
		weightName.append(stateFrom);
		weightName.append("->");
		weightName.append(stateTo);
		weightName.append(':');
		weightName.append(stateTo);
		return model.getWeightsIndex(weightName.toString());
	}

	private int createBiasFeature(int classIndex, double biasValue)
	{
		// create bias feature
		int newFeatureIndex = model.getInputAlphabet().lookupIndex(
				biasFeatureName);

		// add feature to the weights vector of each state transition
		for (int i = 0; i < model.getOutputAlphabet().size(); i++)
		{
			int weightsIndex = getWeightsIndex(i, classIndex);
			if (weightsIndex < 0)
				continue;

			SparseVector weights = model.getWeights(weightsIndex);
			int numWeights = weights.numLocations();
			int[] indices = new int[numWeights + 1];
			double[] values = new double[numWeights + 1];
			System.arraycopy(weights.getIndices(), 0, indices, 0,
					numWeights);
			System.arraycopy(weights.getValues(), 0, values, 0, numWeights);
			indices[numWeights] = newFeatureIndex;
			values[numWeights] = biasValue;

			weights = new SparseVector(indices, values, false);
			model.setWeights(weightsIndex, weights);
		}

		model.weightsStructureChanged();
		return newFeatureIndex;
	}

	private void adjustFeatureWeights(int classIndex, double biasValue)
	{
		double[] defaultWeights = model.getDefaultWeights();

		for (int i = 0; i < model.getOutputAlphabet().size(); i++)
		{
			int weightsIndex = getWeightsIndex(i, classIndex);
			if (weightsIndex < 0)
				continue;

			SparseVector weights = model.getWeights(weightsIndex);
			weights.timesEquals(biasValue);
			defaultWeights[weightsIndex] *= biasValue;
		}

		model.setDefaultWeights(defaultWeights);
		model.weightsValueChanged();
	}

	private double getClassWeightRatio(int classIndex)
	{
		double classWeightSum = 0.0;
		double weightSum = 0.0;

		int numStates = model.getOutputAlphabet().size();
		for (int i = 0; i < numStates; i++)
			for (int j = 0; j < numStates; j++)
			{
				int weightsIndex = getWeightsIndex(i, j);
				if (weightsIndex < 0)
					continue;

				SparseVector weights = model.getWeights(weightsIndex);
				double sum = 0.0;
				for (Double v : weights.getValues())
					sum += v;
				weightSum += sum;
				if (j == classIndex)
					classWeightSum += sum;
			}

		return classWeightSum / weightSum;
	}

	public void process(Corpus corpus) throws Exception
	{
		for (Sentence s : corpus)
		{
			Instance inst = induceFeaturesForSentence(s);
			model.transduce(inst);

			Sequence labelSeq = (Sequence) inst.getData();
			assert (labelSeq.size() == s.length());
			for (int i = 0; i < labelSeq.size(); i++)
				s.get(i).setLabel(Integer.parseInt((String) labelSeq.get(i)));
		}
	}

	private Instance induceFeaturesForSentence(Sentence s)
	{
		InstanceList data = new InstanceList(new Noop());
		boolean addBiasFeature = (biasFeatureIndex > 0);
		data.add(createInstanceFromSentence(s, addBiasFeature));
		if (performFeatureInduction)
			model.induceFeaturesFor(data);
		Instance inst = data.get(0);
		inst.unLock();
		return inst;
	}

	private InstanceList createInstanceListFromCorpus(Corpus c)
	{
		InstanceList data = new InstanceList(new Noop());
		for (Sentence s : c)
			data.add(createInstanceFromSentence(s, false));
		return data;
	}

	private Instance createInstanceFromSentence(Sentence s,
			boolean addBiasFeature)
	{
		int length = s.length();
		AugmentableFeatureVector[] featureVectors =
			new AugmentableFeatureVector[length];
		int[] labels = new int[length];
		StringBuffer sentenceId = new StringBuffer();
		sentenceId.append("sentence: ");
		for (int i = 0; i < length; i++)
		{
			Word w = s.get(i);
			if (i < 3)
			{
				if (i > 0)
					sentenceId.append(' ');
				sentenceId.append(w.getWord());
			}

			Vector<Integer> indices = new Vector<Integer>();
			Vector<Double> values = new Vector<Double>();
			int numFeatures = featureGen.getFeaturesForWord(s, i, indices,
					values);
			if (addBiasFeature)
			{
				indices.add(biasFeatureIndex);
				values.add(1.0);
				numFeatures++;
			}

			if (numFeatures > 0)
			{
				int[] indexArray = new int[numFeatures];
				double[] valueArray = new double[numFeatures];
				for (int j = 0; j < numFeatures; j++)
				{
					indexArray[j] = indices.get(j);
					valueArray[j] = values.get(j);
				}

				if (performFeatureInduction)
				{
					// the feature induction algorithm of MALLET requires a
					// binary feature vector
					featureVectors[i] = new AugmentableFeatureVector(
							featureAlphabet, numFeatures, true);
					for (int index : indexArray)
						featureVectors[i].add(index);
				}
				else
					featureVectors[i] = new AugmentableFeatureVector(
							featureAlphabet, indexArray, valueArray,
							numFeatures);
			}
			else
			{
				// create empty feature vector (binary, if feature induction is
				// enabled)
				featureVectors[i] = new AugmentableFeatureVector(
						featureAlphabet, 0, performFeatureInduction);
			}

			labels[i] = w.getTrueLabel();
		}

		FeatureVectorSequence data = new FeatureVectorSequence(featureVectors);
		LabelSequence target = new LabelSequence(labelAlphabet, labels);
		return new Instance(data, target, sentenceId.toString(), "corpus");
	}

	public void saveModel(OutputStream out) throws IOException
	{
		ObjectOutputStream objectStream = new ObjectOutputStream(out);
		objectStream.writeInt(labelAlphabet.size());
		objectStream.writeObject(model);
	}

	public void loadModel(InputStream in) throws IOException
	{
		ObjectInputStream objectStream = new ObjectInputStream(in);
		int numClasses = 4;
		try
		{
			numClasses = objectStream.readInt();
			model = (CRF) objectStream.readObject();
		}
		catch (ClassNotFoundException ex)
		{
			throw new RuntimeException("unexpected data in model file");
		}

		biasFeatureIndex = -1;

		// create label alphabet
		labelAlphabet = new LabelAlphabet();
		for (int i = 0; i < numClasses; i++)
			labelAlphabet.lookupIndex(Integer.toString(i), true);

		// create feature alphabet
		featureAlphabet = new Alphabet(featureGen.getFeatureNames());
	}

	public void printModel(OutputStream out) throws IOException
	{
		PrintWriter writer = new PrintWriter(out);
		model.print(writer);
		writer.println();
		if (performFeatureInduction)
			writer.println("feature induction enabled");
		writer.printf("gaussian variance (sigma) = %.3f\n", sigma);
		if (biasMethod != null)
			writer.println(biasMethod);
		writer.printf("log-likelihood of training data = %.3f\n",
				logLikelihood);
		writer.flush();
	}

}

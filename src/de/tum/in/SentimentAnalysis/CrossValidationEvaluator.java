package de.tum.in.SentimentAnalysis;

import java.io.PrintStream;
import java.util.ArrayList;

public class CrossValidationEvaluator extends Evaluator {

	private ArrayList<Double>[] fScoreValues;
	private ArrayList<Double> accuracyValues;

	@Override public void addCorpus(Corpus corpus) throws Exception
	{
		super.addCorpus(corpus);

		Evaluator singleCorpusEval = new Evaluator();
		singleCorpusEval.addCorpus(corpus);
		singleCorpusEval.updateEvaluation();

		if (fScoreValues == null)
		{
			fScoreValues = new ArrayList[numLabels];
			for (int i = 0; i < numLabels; i++)
				fScoreValues[i] = new ArrayList<Double>();
			accuracyValues = new ArrayList<Double>();
		}
		for (int i = 0; i < numLabels; i++)
			fScoreValues[i].add(singleCorpusEval.fscore[i]);
		accuracyValues.add(singleCorpusEval.overallAccuracy);
	}

	@Override public void printResults(PrintStream out) throws Exception
	{
		super.printResults(out);
		out.printf("\n\nestimation of standard deviation across folds " +
				"(biased):\n\n");
		out.printf("accuracy: %f\n", getAccuracyStdDev());
		for (int i = 0; i < numLabels; i++)
			out.printf("f-score class %d: %f\n", i, getFScoreStdDev(i));
	}

	// The resulting standard deviation values are biased for two reasons: The
	// standard deviation is biased when estimated from samples, and the
	// variance estimation across different "folds" of cross validation is
	// biased, because the training datasets of each fold are not independent.

	public double getAccuracyStdDev()
	{
		return computeStdDev(accuracyValues);
	}

	public double getFScoreStdDev(int c)
	{
		return computeStdDev(fScoreValues[c]);
	}

	private double computeStdDev(ArrayList<Double> values)
	{
		int n = values.size();
		double mean = 0.0;
		for (Double v : values)
			mean += v;
		mean /= n;
		double stdDev = 0.0;
		for (Double v : values)
			stdDev += Math.pow(v - mean, 2.0);
		return Math.sqrt((1.0 / (n - 1)) * stdDev);
	}
}

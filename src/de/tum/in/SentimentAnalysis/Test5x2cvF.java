package de.tum.in.SentimentAnalysis;

import java.io.PrintStream;
import java.util.Iterator;

import de.tum.in.Math.Statistics;


public class Test5x2cvF {

	private static final int numEval = 2;
	private static final int numRounds = 5;
	private static final int numFolds = 2;

	private static final String[] evalDesc = {"accuracy", "macro-averaged F1"};

	private int numClasses;
	private double limit;
	private int curRound;
	private int curFold;
	private double[][][] p;
	private double[] f;
	private double realSg;

	public Test5x2cvF(int numPairwiseComparisons, int numClasses)
	{
		this.numClasses = numClasses;

		// Bonferroni correction for multiple pairwise tests
		realSg = 0.05 / numPairwiseComparisons;
		limit = Statistics.computeFLimit10and5(realSg);

		p = new double[numEval][numRounds][numFolds];
		curRound = 0;
		curFold = 0;
	}

	public void append(Corpus c1, Corpus c2)
	{
		if (curRound >= numRounds)
			throw new IllegalArgumentException();
		if (!c1.getDocumentId().equals(c2.getDocumentId()))
			throw new IllegalArgumentException();

		// count classification errors
		int n = 0;
		int[] e = new int[2];
		int[][] tp = new int[2][numClasses];
		int[][] fp = new int[2][numClasses];
		int[][] fn = new int[2][numClasses];

		Iterator<Sentence> itSentence1 = c1.iterator();
		Iterator<Sentence> itSentence2 = c2.iterator();
		while (itSentence1.hasNext() && itSentence2.hasNext())
		{
			Sentence s1 = itSentence1.next();
			Sentence s2 = itSentence2.next();
			Iterator<Word> itWord1 = s1.iterator();
			Iterator<Word> itWord2 = s2.iterator();
			while (itWord1.hasNext() && itWord2.hasNext())
			{
				Word w1 = itWord1.next();
				Word w2 = itWord2.next();
				int trueLabel = w1.getTrueLabel();

				int label = w1.getLabel();
				if (label != trueLabel)
				{
					e[0]++;
					fn[0][trueLabel]++;
					fp[0][label]++;
				}
				else
					tp[0][trueLabel]++;

				label = w2.getLabel();
				if (label != trueLabel)
				{
					e[1]++;
					fn[1][trueLabel]++;
					fp[1][label]++;
				}
				else
					tp[1][trueLabel]++;

				n++;
			}
		}

		// compute average f-score
		double[] macroAvgFScore = new double[2];
		for (int i = 0; i < 2; i++)
		{
			double macroAvgPrecision = 0.0;
			double macroAvgRecall = 0.0;
	
			for (int j = 0; j < numClasses; j++)
			{
				int sum = tp[i][j] + fp[i][j];
				if (sum > 0)
					macroAvgPrecision += (double) tp[i][j] / sum;
				sum = tp[i][j] + fn[i][j];
				if (sum > 0)
					macroAvgRecall += (double) tp[i][j] / sum;
			}

			macroAvgPrecision /= numClasses;
			macroAvgRecall /= numClasses;
			macroAvgFScore[i] =
				Statistics.harmonicMean(macroAvgPrecision, macroAvgRecall);
		}

		// save difference between evaluation results
		p[0][curRound][curFold] = (double) (e[0] - e[1]) / n;
		p[1][curRound][curFold] = macroAvgFScore[0] - macroAvgFScore[1];

		if (++curFold >= numFolds)
		{
			curRound++;
			curFold = 0;
		}
	}

	public void evaluate()
	{
		if (curRound < numRounds)
			throw new RuntimeException("not enough data");

		f = new double[numEval];
		for (int i = 0; i < numEval; i++)
		{
			double pSum = 0.0;
			for (int j = 0; j < numRounds; j++)
				for (int k = 0; k < numFolds; k++)
					pSum += p[i][j][k] * p[i][j][k];

			double sSum = 0.0;
			for (int j = 0; j < numRounds; j++)
			{
				double pAvg = 0;
				for (int k = 0; k < numFolds; k++)
					pAvg += p[i][j][k];
				pAvg /= numFolds;

				for (int k = 0; k < numFolds; k++)
				{
					double d = p[i][j][k] - pAvg;
					sSum += d * d;
				}
			}

			if (sSum != 0.0)
				f[i] = pSum / (2.0 * sSum);
		}
	}

	public void printResults(PrintStream out) throws Exception
	{
		if (f == null)
			return;

		out.println("\nresult of combined 5x2 cv F test:\n");
		for (int i = 0; i < numEval; i++)
		{
			for (int j = 0; j < numRounds; j++)
				out.printf("%f;%f ", p[i][j][0], p[i][j][1]);
			out.println();

			out.printf("%s\t", evalDesc[i]);
			out.printf("f = %f (limit for 'p < 0.05' = %f) => ", f[i], limit);
			if (f[i] > limit)
				out.println("different performance");
			else
				out.println("no difference");
		}
		out.printf("actual significance after Bonferroni correction: p < %f\n",
				realSg);
		out.println();
		out.flush();
	}

}

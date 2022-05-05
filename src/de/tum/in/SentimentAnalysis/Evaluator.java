package de.tum.in.SentimentAnalysis;

import java.io.PrintStream;

import de.tum.in.Math.Statistics;


public class Evaluator {

	protected int numLabels;
	protected int numWords;
	protected int numChunks;
	protected int numSentences;
	protected int numWordsCorrectlyClassified;
	protected int numChunksCorrectlyClassified;
	protected int[] numSentencesCorrectlyClassified;
	protected double overallAccuracy;
	protected double chunkAccuracy;
	protected double[] sentenceAccuracy;

	protected int[] tp;
	protected int[] tn;
	protected int[] fp;
	protected int[] fn;

	protected double[] accuracy;
	protected double[] recall;
	protected double[] precision;
	protected double[] fscore;

	protected double macroAvgRecall;
	protected double macroAvgPrecision;
	protected double macroAvgFScore;	

	protected int[][] confusion;

	protected double[] histTrueLabel;
	protected double[] histLabel;


	public Evaluator()
	{
		numLabels = 0;
		numWords = 0;
		numChunks = 0;
		numSentences = 0;
		numWordsCorrectlyClassified = 0;
		numChunksCorrectlyClassified = 0;
		numSentencesCorrectlyClassified = new int[4];
		sentenceAccuracy = new double[4];
	}

	private void init(int numLabels)
	{
		tp = new int[numLabels];
		tn = new int[numLabels];
		fp = new int[numLabels];
		fn = new int[numLabels];
		confusion = new int[numLabels][numLabels];
		histTrueLabel = new double[numLabels];
		histLabel = new double[numLabels];
		accuracy = new double[numLabels];
		recall = new double[numLabels];
		precision = new double[numLabels];
		fscore = new double[numLabels];

		this.numLabels = numLabels;
	}

	public void addCorpus(Corpus corpus) throws Exception
	{
		if (numLabels == 0)
			init(corpus.getNumClasses());
		else if (numLabels != corpus.getNumClasses())
			throw new Exception("corpuses not comparable");

		boolean chunkCorrectlyClassified;
		int prevTrueLabel = 0;
		for (Sentence s : corpus)
		{
			numSentences++;

			int[] distr = s.getLabelDistribution();
			int[] trueDistr = s.getTrueLabelDistribution();
			if (Sentence.computePolarity(distr, 0.0) ==
				Sentence.computePolarity(trueDistr, 0.0))
				numSentencesCorrectlyClassified[0]++;
			if (Sentence.computePolarity(distr, 0.1) ==
				Sentence.computePolarity(trueDistr, 0.1))
				numSentencesCorrectlyClassified[1]++;
			if (Sentence.getMajorityClass(distr, false) ==
				Sentence.getMajorityClass(trueDistr, false))
				numSentencesCorrectlyClassified[2]++;
			if (Sentence.getMajorityClass(distr, true) ==
				Sentence.getMajorityClass(trueDistr, true))
				numSentencesCorrectlyClassified[3]++;

			chunkCorrectlyClassified = false;

			for (Word w : s)
			{
				numWords++;

				int label = w.getLabel();
				int trueLabel = w.getTrueLabel();

				// start of a new chunk directly after the previous block OR
				// neutral element indicating the end of the previous block
				if (w.getStartsBlock() || (trueLabel != prevTrueLabel))
				{
					if (chunkCorrectlyClassified)
					{
						numChunksCorrectlyClassified++;
						chunkCorrectlyClassified = false;
					}
					if (w.getStartsBlock())
					{
						numChunks++;
						chunkCorrectlyClassified = true;
					}
				}

				confusion[trueLabel][label]++;
				histTrueLabel[trueLabel]++;
				histLabel[label]++;
				if (label == trueLabel)
				{
					numWordsCorrectlyClassified++;
					tp[trueLabel]++;
				}
				else
				{
					fn[trueLabel]++;
					fp[label]++;
					chunkCorrectlyClassified = false;
				}
				for (int i = 0; i < numLabels; i++)
					if ((i != label) && (i != trueLabel))
						tn[i]++;

				prevTrueLabel = trueLabel;
			}
		}
	}

	public void updateEvaluation()
	{
		overallAccuracy = (double) numWordsCorrectlyClassified / numWords;
		chunkAccuracy = (double) numChunksCorrectlyClassified / numChunks;
		for (int i = 0; i < 4; i++)
			sentenceAccuracy[i] =
				(double) numSentencesCorrectlyClassified[i] / numSentences;

		macroAvgPrecision = 0.0;
		macroAvgRecall = 0.0;
		for (int i = 0; i < numLabels; i++)
		{
			accuracy[i] = (double) (tp[i] + tn[i]) /
					(tp[i] + tn[i] + fp[i] + fn[i]);
			int n = tp[i] + fp[i];
			if (n > 0)
				precision[i] = (double) tp[i] / n;
			n = tp[i] + fn[i];
			if (n > 0)
				recall[i] = (double) tp[i] / n;
			fscore[i] = Statistics.harmonicMean(precision[i], recall[i]);

			macroAvgPrecision += precision[i];
			macroAvgRecall += recall[i];
		}

		macroAvgPrecision /= numLabels;
		macroAvgRecall /= numLabels;
		macroAvgFScore =
			Statistics.harmonicMean(macroAvgPrecision, macroAvgRecall);
	}

	public double getAccuracy()
	{
		return overallAccuracy;
	}

	public double getAvgPolarFScore()
	{
		double sumFScore = 0.0;
		for (int i = 1; i < fscore.length; i++)
			sumFScore += fscore[i];
		return sumFScore / (fscore.length - 1);
	}

	public void printResults(PrintStream out) throws Exception
	{
		out.printf("overall accuracy = %f\n", overallAccuracy);
		out.printf("overall chunk accuracy = %f\n", chunkAccuracy);
		out.printf("overall sentence accuracy (heuristic, thresh. 0.0) = %f\n",
				sentenceAccuracy[0]);
		out.printf("overall sentence accuracy (heuristic, thresh. 0.1) = %f\n",
				sentenceAccuracy[1]);
		out.printf("overall sentence accuracy (majority class) = %f\n",
				sentenceAccuracy[2]);
		out.printf("overall sentence accuracy (polar majority class) = %f\n",
				sentenceAccuracy[3]);

		out.printf("\nconfusion matrix (row index = true label):\n");
		for (int i = 0; i < numLabels; i++)
		{
			for (int j = 0; j < numLabels; j++)
				out.printf("%6d\t", confusion[i][j]);
			out.println();
		}

		out.printf("\nc\ttp    \ttn    \tfp    \tfn\n");
		for (int i = 0; i < numLabels; i++)
			out.printf("%d\t%6d\t%6d\t%6d\t%6d\n", i, tp[i], tn[i], fp[i],
					fn[i]);

		out.printf("\nc\taccuracy\tprecision\trecall\t\tf-score\n");
		for (int i = 0; i < numLabels; i++)
			out.printf("%d\t%f\t%f\t%f\t%f\n", i, accuracy[i], precision[i],
					recall[i], fscore[i]);

		out.printf("\navg.\tprecision\trecall\t\tf-score\n");
		out.printf("macro\t%f\t%f\t%f\n", macroAvgPrecision, macroAvgRecall,
				macroAvgFScore);

		out.printf("\nc\t%%true\t\t%%assigned\td\n");
		for (int i = 0; i < numLabels; i++)
		{
			double histLabelPercent = (histLabel[i] / numWords) * 100;
			double histTrueLabelPercent = (histTrueLabel[i] / numWords) * 100;
			out.printf("%d\t%f\t%f\t%f\n", i, histTrueLabelPercent,
					histLabelPercent, histLabelPercent - histTrueLabelPercent);
		}
	}

}

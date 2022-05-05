package de.tum.in.SentimentAnalysis;

public class CrossValidationProvider {

	private static final double[] chiSquareLimit =
		{0.0, 3.84146, 5.99146, 7.81473, 9.48773, 11.07050};
	private static final int maxIter = 100;

	private Corpus corpus;
	private Corpus[] partition;
	private Corpus trainingSet;
	private Corpus trainingTestSet;
	private Corpus testSet;

	private int n;
	private boolean stratified;
	private boolean provideTrainingTestSet;
	private int currentFold;

	public CrossValidationProvider(Corpus corpus, int n, boolean stratified,
			boolean provideTrainingTestSet)
	{
		this.corpus = corpus;
		this.n = n;
		this.stratified = stratified;
		this.provideTrainingTestSet = provideTrainingTestSet;
		this.currentFold = -1;
	}

	public void nextRound()
	{
		currentFold = -1;
		trainingSet = null;
		trainingTestSet = null;
		testSet = null;

		if (stratified && (n > 1))
		{
			double maxChiSquare;
			int numIter = 0;
			do
			{
				if (numIter++ >= maxIter)
					throw new RuntimeException("could not create a stratified" +
							" partition");

				partition = corpus.randomStratifiedPartition(n);

				double[] chiSquare = computeChiSquare(corpus, partition);
				maxChiSquare = 0.0;
				for (double v : chiSquare)
					if (v > maxChiSquare)
						maxChiSquare = v;
//				System.err.println(maxChiSquare);  // DEBUG
			} while (maxChiSquare > chiSquareLimit[corpus.getNumClasses() - 1]);
		}
		else
			partition = corpus.randomPartition(n);
/*
		int numTests = 20;
		double avgChiSquare = 0.0;
		for (int i = 0; i < numTests; i++)
		{
			partition = corpus.randomPartition(n);
			double[] chiSquare = computeChiSquare(corpus, partition);
			for (double v : chiSquare)
				avgChiSquare += v; 
		}
		avgChiSquare /= (numTests * n);
		System.err.printf("average chi^2 without stratification = %f\n",
				avgChiSquare);

		avgChiSquare = 0.0;
		for (int i = 0; i < numTests; i++)
		{
			partition = corpus.randomStratifiedPartition(n);
			double[] chiSquare = computeChiSquare(corpus, partition);
			for (double v : chiSquare)
				avgChiSquare += v;
		}
		avgChiSquare /= (numTests * n);
		System.err.printf("average chi^2 with stratification = %f\n",
				avgChiSquare);
*/
	}

	private double[] computeChiSquare(Corpus wholeCorpus, Corpus[] partitions)
	{
		// compute label distribution in whole corpus
		int numClasses = wholeCorpus.getNumClasses();
		double[] wholeCorpusDist = new double[numClasses];
		int numWords = 0;
		for (Sentence s : wholeCorpus)
			for (Word w : s)
			{
				wholeCorpusDist[w.getTrueLabel()]++;
				numWords++;
			}
		for (int i = 0; i < numClasses; i++)
			wholeCorpusDist[i] /= numWords;

		// for each partition...
		double[] chiSquare = new double[partitions.length];
		for (int i = 0; i < partitions.length; i++)
		{
			// compute label frequency
			int numPartWords = 0;
			int[] labelFreq = new int[numClasses];
			for (Sentence s : partitions[i])
				for (Word w : s)
				{
					labelFreq[w.getTrueLabel()]++;
					numPartWords++;
				}

			// compute chi^2
			for (int j = 0; j < numClasses; j++)
			{
				double expectedFreq = wholeCorpusDist[j] * numPartWords;
				chiSquare[i] += Math.pow(labelFreq[j] - expectedFreq, 2.0) /
					expectedFreq;
			}
		}
		return chiSquare;
	}

	public void nextFold()
	{
		if (++currentFold >= n)
			throw new AssertionError();

		if (n > 1)
		{
			Corpus tempSet = new Corpus(partition[0].getNumClasses());
			for (int i = 0; i < partition.length; i++)
				if (i != currentFold)
					tempSet.append(partition[i]);
			if (provideTrainingTestSet)
			{
				// use 10% of the training data as "training test set"
				Corpus[] tempPartition = tempSet.randomPartition(10);
				trainingSet = new Corpus(tempSet.getNumClasses());
				for (int i = 1; i < tempPartition.length; i++)
					trainingSet.append(tempPartition[i]);
				trainingTestSet = tempPartition[0];
			}
			else
				trainingSet = tempSet;
		}
		else
			trainingSet = partition[currentFold];

		testSet = new Corpus(partition[currentFold].getNumClasses());
		testSet.appendCopy(partition[currentFold]);
	}

	public Corpus getTrainingSet()
	{
		return trainingSet;
	}

	public Corpus getTrainingTestSet()
	{
		// test set to be used during training, e.g. for early stopping
		return trainingTestSet;
	}

	public Corpus getTestSet()
	{
		return testSet;
	}

	public String getConfigurationString()
	{
		StringBuffer buf = new StringBuffer();
		if (n > 1)
		{
			buf.append(n);
			buf.append("-fold ");
			if (stratified)
				buf.append("stratified ");
		}
		else
			buf.append("no ");
		buf.append("cross validation");
		return buf.toString();
	}

}

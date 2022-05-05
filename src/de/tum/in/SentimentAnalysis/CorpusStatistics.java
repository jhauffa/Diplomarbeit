package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileNotFoundException;

public class CorpusStatistics {

	private static void printCorpusStatistics(Corpus corpus)
	{
		int numLabels = 0;
		int numChunks = 0;
		int numClasses = corpus.getNumClasses();
		int[] numLabelsForClass = new int[numClasses];
		for (Sentence s : corpus)
		{
			for (Word w : s)
			{
				numLabelsForClass[w.getTrueLabel()]++;
				numLabels++;
				if (w.getStartsBlock())
					numChunks++;
			}
		}

		System.out.printf("number of sentences = %d\n", corpus.size());
		System.out.printf("number of classes = %d\n", numClasses);
		float avgChunkLength = 0.0f;
		for (int i = 0; i < numClasses; i++)
		{
			float labelsRel = ((float) numLabelsForClass[i] / numLabels) *
					100.0f; 
			System.out.printf("number of labels for class %d = %d, %.2f%%\n",
					i, numLabelsForClass[i], labelsRel);
			if (i > 0)
				avgChunkLength += numLabelsForClass[i];
		}
		avgChunkLength /= numChunks;
		System.out.printf("average chunk length = %.2f words\n",
				avgChunkLength);
	}

	private static void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.printf("error: %s\n", message);
		System.err.printf("\nusage: CorpusStatistics file\n"+
				"where \"file\" is a simplified MPQA corpus\n");
		System.exit(code);
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
			printUsageAndExit("not enough arguments", 1);

		Corpus corpus = null;
		try
		{
			corpus = new SimpleCorpus(new File(args[0]));
		}
		catch (FileNotFoundException ex)
		{
			printUsageAndExit("file not found: " + ex.getMessage(), 1);
		}

		printCorpusStatistics(corpus);
	}

}

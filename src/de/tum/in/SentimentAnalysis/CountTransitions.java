package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileNotFoundException;

public class CountTransitions {

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: CountTransitions corpus");
			return;
		}

		Corpus corpus = null;
		try
		{
			System.err.println("loading corpus...");
			corpus = new SimpleCorpus(new File(args[0]));
		}
		catch (FileNotFoundException ex)
		{
			System.err.println("file not found: " + ex.getMessage());
			return;
		}

		int numClasses = corpus.getNumClasses();
		int[][] numTrans = new int[numClasses][numClasses];
		int numPairs = 0;

		for (Sentence s : corpus)
		{
			int prevLabel = -1;
			for (Word w : s)
			{
				int curLabel = w.getTrueLabel();
				if (prevLabel >= 0)
				{
					numTrans[prevLabel][curLabel]++;
					numPairs++;
				}
				prevLabel = curLabel;
			}
		}

		for (int i = 0; i < numClasses; i++)
		{
			for (int j = 0; j < numClasses; j++)
				System.out.printf("%d (%.2f%%)\t",
						numTrans[i][j],
						((double) numTrans[i][j] / numPairs) * 100.0);
			System.out.println();
		}
	}

}

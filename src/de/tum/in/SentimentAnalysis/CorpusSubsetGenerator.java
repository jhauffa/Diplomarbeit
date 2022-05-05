package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileNotFoundException;

public class CorpusSubsetGenerator {

	private static void printUsageAndExit(String message, int code)
	{
		System.err.printf("error: %s\n\n" +
				"usage: CorpusSubsetGenerator corpus ratio\n" +
				"where \"corpus\" is a simplified MPQA corpus and\n" +
				"\"ratio\" specifies the size of the subset relative to the " +
				"size of the original corpus (in percent)",
				message);
		System.exit(code);
	}

	public static void main(String[] args)
	{
		if (args.length < 2)
			printUsageAndExit("not enough arguments", 1);

		File corpusFile = new File(args[0]);
		Corpus corpus = null;
		try
		{
			System.err.println("loading corpus...");
			corpus = new SimpleCorpus(corpusFile);
		}
		catch (FileNotFoundException ex)
		{
			System.err.println("file not found: " + ex.getMessage());
			System.exit(2);
		}

		double ratio = Double.parseDouble(args[1]) / 100.0;
		int n = (int) Math.round(1.0 / ratio);
		Corpus[] parts = corpus.randomPartition(n);
		((SimpleCorpus) parts[0]).write(System.out);
	}

}

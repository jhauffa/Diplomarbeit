package de.tum.in.SentimentAnalysis;

import java.io.PrintStream;
import java.util.Iterator;

import de.tum.in.Math.Statistics;


public class TestMcNemar {

	private double limit;
	private int[] n;
	private double chiSquare;

	public TestMcNemar(int numPairwiseComparisons)
	{
		// Bonferroni correction for multiple pairwise comparisons
		limit = Statistics.computeChiSquareLimit(1,
				0.05 / numPairwiseComparisons);

		n = new int[4];
	}

	public void performTest(Corpus c1, Corpus c2)
	{
		// corpora must be equal
		if (!c1.getDocumentId().equals(c2.getDocumentId()))
			throw new IllegalArgumentException();

		// count classification errors
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
				if (w1.getLabel() == w1.getTrueLabel())
				{
					if (w2.getLabel() == w2.getTrueLabel())
						n[3]++;
					else
						n[2]++;
				}
				else
				{
					if (w2.getLabel() == w2.getTrueLabel())
						n[1]++;
					else
						n[0]++;
				}
			}
		}

		// compute chi^2
		int f1 = Math.abs(n[1] - n[2]) - 1;
		f1 *= f1;
		int f2 = n[1] + n[2];
		if (f2 != 0)
			chiSquare = ((double) f1) / f2; 
	}

	public void printResults(PrintStream out) throws Exception
	{
		out.println("\nresult of McNemar's test:");
		for (int i = 0; i < n.length; i++)
			out.printf("n[%d] = %d\n", i + 1, n[i]);
		out.printf("chi^2 = %f (limit for 'p < 0.05' = %f) => ",
				chiSquare, limit);
		if (chiSquare > limit)
			out.println("different performance");
		else
			out.println("no difference");
		out.println();
		out.flush();
	}

}

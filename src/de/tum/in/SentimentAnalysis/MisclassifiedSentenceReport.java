package de.tum.in.SentimentAnalysis;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class MisclassifiedSentenceReport implements Report {

	private class SentenceStats
	{
		public Sentence sentence;
		public int numErrors;
	}

	private class CompareByNumErrors implements Comparator<SentenceStats>
	{
		public int compare(SentenceStats e1, SentenceStats e2)
		{
			return e2.numErrors - e1.numErrors;
		}
	}


	private Classifier model;
	private int maxSentences;
	private ArrayList<SentenceStats> stats;
	private Vector<String> featureNames;

	public MisclassifiedSentenceReport(Corpus corpus, Classifier model,
			int maxSentences)
	{
		this.maxSentences = maxSentences;
		this.model = model;

		featureNames = new Vector<String>();
		model.getFeatureNames(featureNames);

		stats = new ArrayList<SentenceStats>();
		for (Sentence s : corpus)
		{
			SentenceStats st = new SentenceStats();
			st.sentence = s;
			for (Word w : s)
				if (w.getLabel() != w.getTrueLabel())
					st.numErrors++;
			stats.add(st);
		}
		Collections.sort(stats, new CompareByNumErrors());
	}

	public void print(PrintStream out)
	{
		int n = maxSentences;
		if (n <= 0)
			n = stats.size();
		out.printf("top %d misclassified sentences:\n", n);
		for (int i = 0; i < n; i++)
		{
			SentenceStats st = stats.get(i);
			out.printf("%d\t", st.numErrors);
			int pos = 0;
			for (Word w : st.sentence)
			{
				out.printf("%s (L%d T%d", w.getWord(), w.getLabel(),
						w.getTrueLabel());
				printFeatures(out, st.sentence, pos);
				out.print(")\n\t");
				pos++;
			}
			out.println();
		}
	}

	private void printFeatures(PrintStream out, Sentence sentence, int pos)
	{
		Vector<Integer> indices = new Vector<Integer>();
		Vector<Double> weights = new Vector<Double>();
		model.getFeatureWeights(sentence, pos, indices, weights);
		for (int i = 0; i < Math.min(indices.size(), weights.size()); i++)
			out.printf(" %s=%.3f", featureNames.get(indices.get(i)),
					weights.get(i));
	}

}

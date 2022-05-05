package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;
import java.util.ArrayList;

import de.tum.in.Util.CharacterTrie;


public class CharacterNGramFeatures implements FeatureTemplate, Serializable {

	private class NGramIterator
	{
		private String word;
		private int length;
		private int pos;

		public NGramIterator(String word)
		{
			this.word = word;
			this.length = word.length();
			this.pos = 0;
		}

		public boolean hasNext()
		{
			if (overlapping)
				return (pos < (length - (n - 1)));
			else
				return (pos < length);
		}

		public String next()
		{
			String nGram = word.substring(pos, Math.min(pos + n, length));
			if (!caseSensitive)
				nGram = nGram.toLowerCase();
			if (overlapping)
				pos++;
			else
				pos += n;
			return nGram;
		}
	}
	
	private int n;
	private boolean overlapping;
	private boolean encodePosition;
	private boolean caseSensitive;

	private CharacterTrie<Integer> startNGrams;
	private CharacterTrie<Integer> midNGrams;
	private CharacterTrie<Integer> endNGrams;
	private ArrayList<String> featureNames;

	public CharacterNGramFeatures(int n, boolean overlapping,
			boolean encodePosition, boolean caseSensitive)
	{
		this.n = n;
		this.overlapping = overlapping;
		this.encodePosition = encodePosition;
		this.caseSensitive = caseSensitive;

		startNGrams = new CharacterTrie<Integer>();
		midNGrams = new CharacterTrie<Integer>();
		endNGrams = new CharacterTrie<Integer>();
		featureNames = new ArrayList<String>();
	}

	public int getNumFeatures()
	{
		return featureNames.size();
	}

	public String getFeatureName(int idx)
	{
		return featureNames.get(idx);
	}

	public String[] getFeatureNames()
	{
		return featureNames.toArray(new String[featureNames.size()]);
	}

	public void reset()
	{
		startNGrams.clear();
		midNGrams.clear();
		endNGrams.clear();
		featureNames.clear();
	}

	public void train(Corpus corpus)
	{
		for (Sentence s : corpus)
			for (Word w : s)
			{
				NGramIterator it = new NGramIterator(w.getWord());
				String nGram;
				if (encodePosition && it.hasNext())
				{
					nGram = it.next();
					if (startNGrams.get(nGram) == null)
					{
						startNGrams.set(nGram, featureNames.size());
						featureNames.add("CGR_start_" + nGram);
					}
				}
				while (it.hasNext())
				{
					nGram = it.next();
					if (encodePosition && !it.hasNext())
					{
						if (endNGrams.get(nGram) == null)
						{
							endNGrams.set(nGram, featureNames.size());
							featureNames.add("CGR_end_" + nGram);
						}
					}
					else
					{
						if (midNGrams.get(nGram) == null)
						{
							midNGrams.set(nGram, featureNames.size());
							featureNames.add("CGR_" + nGram);
						}
					}
				}
			}
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int numFeatures = 0;

		NGramIterator it = new NGramIterator(s.get(pos).getWord());
		String nGram;
		Integer featureIdx;
		if (encodePosition && it.hasNext())
		{
			nGram = it.next();
			featureIdx = startNGrams.get(nGram);
			if (featureIdx != null)
			{
				indices.add(featureIdx);
				values.add(1.0);
				numFeatures++;
			}
		}
		while (it.hasNext())
		{
			nGram = it.next();
			if (encodePosition && !it.hasNext())
				featureIdx = endNGrams.get(nGram);
			else
				featureIdx = midNGrams.get(nGram);
			if (featureIdx != null)
			{
				indices.add(featureIdx);
				values.add(1.0);
				numFeatures++;
			}
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("character %d-grams", n);
		if (overlapping)
			writer.print(", overlapping");
		if (encodePosition)
			writer.print(", position (start/mid/end) encoded");
		if (caseSensitive)
			writer.print(", case sensitive");
		writer.println();
		writer.flush();
	}

}

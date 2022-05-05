package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import de.tum.in.Util.CharacterTrie;


public class PrefixSuffixFeatures implements FeatureTemplate, Serializable {

	private int minLength;
	private int maxLength;
	private boolean caseSensitive;

	private CharacterTrie<Integer> startNGrams;
	private CharacterTrie<Integer> endNGrams;
	private ArrayList<String> featureNames;

	public PrefixSuffixFeatures(int minLength, int maxLength,
			boolean caseSensitive)
	{
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.caseSensitive = caseSensitive;

		startNGrams = new CharacterTrie<Integer>();
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
		endNGrams.clear();
		featureNames.clear();
	}

	public void train(Corpus corpus)
	{
		for (Sentence s : corpus)
			for (Word w : s)
			{
				String word = w.getWord();
				int len = minLength;
				while ((len <= maxLength) && (word.length() >= (2 * len)))
				{
					String nGram;

					// prefix
					nGram = word.substring(0, len);
					if (!caseSensitive)
						nGram = nGram.toLowerCase();
					if (startNGrams.get(nGram) == null)
					{
						startNGrams.set(nGram, featureNames.size());
						featureNames.add("PSX_prefix_" + nGram);
					}

					// suffix
					nGram = word.substring(word.length() - len, word.length());
					if (!caseSensitive)
						nGram = nGram.toLowerCase();
					if (endNGrams.get(nGram) == null)
					{
						endNGrams.set(nGram, featureNames.size());
						featureNames.add("PSX_suffix_" + nGram);
					}

					len++;
				}
			}
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int numFeatures = 0;

		String word = s.get(pos).getWord();
		int len = minLength;
		while ((len <= maxLength) && (word.length() >= (2 * len)))
		{
			String nGram;
			Integer featureIdx;

			// prefix
			nGram = word.substring(0, len);
			if (!caseSensitive)
				nGram = nGram.toLowerCase();
			featureIdx = startNGrams.get(nGram);
			if (featureIdx != null)
			{
				indices.add(featureIdx);
				values.add(1.0);
				numFeatures++;
			}

			// suffix
			nGram = word.substring(word.length() - len, word.length());
			if (!caseSensitive)
				nGram = nGram.toLowerCase();
			featureIdx = endNGrams.get(nGram);
			if (featureIdx != null)
			{
				indices.add(featureIdx);
				values.add(1.0);
				numFeatures++;
			}

			len++;
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("prefixes and suffixes, min. length = %d, max. " +
				"length = %d", minLength, maxLength);
		if (caseSensitive)
			writer.print(", case sensitive");
		writer.println();
		writer.flush();
	}

}

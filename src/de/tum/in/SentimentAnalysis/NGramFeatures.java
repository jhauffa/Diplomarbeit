package de.tum.in.SentimentAnalysis;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Collections;


public class NGramFeatures implements FeatureTemplate, Serializable {

	public enum Direction {FORWARD, BACKWARD};

	private class NGramData implements Serializable
	{
		public NGramData(int featureIndex)
		{
			this.featureIndex = featureIndex;
			this.frequency = 1;
			this.frequencyRank = 0;
		}

		public int featureIndex;
		public int frequency;
		public int frequencyRank;
	}

	private class CompareFrequency
		implements Comparator<Map.Entry<String, NGramData>>
	{
		public int compare(Map.Entry<String, NGramData> e1,
				Map.Entry<String, NGramData> e2)
		{
			return e2.getValue().frequency - e1.getValue().frequency;
		}
	}

	public static final String startSymbol = "<S>";
	public static final String endSymbol = "<E>";

	private final int n;
	private final boolean caseSensitive;
	private final boolean useBoundarySymbols;
	private final Direction direction;
	private final int offset;
	private final int freqThreshold;
	private final int minFreqRank;

	private HashMap<String, NGramData> nGramMap;
	private Vector<String> featureNames;
	private String featurePrefix;


	public NGramFeatures(int n, boolean caseSensitive,
			boolean useBoundarySymbols, Direction direction, int offset,
			int freqThreshold, int minFreqRank)
	{
		this.n = n;
		if (this.n < 1)
			throw new IllegalArgumentException();
		else if (this.n > 1)
			this.useBoundarySymbols = useBoundarySymbols;
		else  // n == 1
			this.useBoundarySymbols = false;
		this.caseSensitive = caseSensitive;
		this.direction = direction;
		this.offset = offset;
		this.freqThreshold = freqThreshold;
		this.minFreqRank = minFreqRank;

		nGramMap = new HashMap<String, NGramData>();
		featureNames = new Vector<String>();

		// NGR_%n%%dir%%offset%_
		StringBuffer buf = new StringBuffer(10);
		buf.append("NGR_");
		buf.append(Integer.toString(this.n));
		if (this.direction == Direction.BACKWARD)
			buf.append("B");
		else
			buf.append("F");
		buf.append(Integer.toString(this.offset));
		buf.append("_");
		featurePrefix = buf.toString();
	}

	public void reset()
	{
		featureNames.clear();
		nGramMap.clear();
	}

	public void train(Corpus corpus)
	{
		for (Sentence s : corpus)
		{
			int start = 0, end = s.length() - (n - 1);
			if (useBoundarySymbols)
			{
				start -= 1;
				end += 1;
			}

			if (offset > 0)
			{
				if (direction == Direction.FORWARD)
					end -= offset;
				else if (offset > (n - 1))
					end -= (offset - (n - 1));
			}
			else if (offset < 0)
			{
				if (direction == Direction.BACKWARD)
					start += -offset;
				else if (-offset > (n - 1))
					start += (-offset - (n - 1));
			}

			for (int i = start; i < end; i++)
			{
				int yPos = i;
				if (direction == Direction.FORWARD)
					yPos += n - 1;
				yPos += offset;
				yPos = Math.max(0, yPos);
				yPos = Math.min(s.length() - 1, yPos);

				String nGram = construct(s, i, n, caseSensitive);
				NGramData data = nGramMap.get(nGram);
				if (data == null)
				{
					data = new NGramData(featureNames.size());
					featureNames.add(featurePrefix + nGram);
					nGramMap.put(nGram, data);
				}
				else
					data.frequency++;
			}
		}
		rankNGrams();
	}

	private void rankNGrams()
	{
		// Rank n-grams by their frequency: the lowest rank is assigned to the
		// most frequent word.
		Vector<Map.Entry<String, NGramData>> entries =
			new Vector<Map.Entry<String, NGramData>>(nGramMap.entrySet());
		Collections.sort(entries, new CompareFrequency());
		for (int i = 0; i < entries.size(); i++)
			entries.get(i).getValue().frequencyRank = i;
	}

	public String getFeatureName(int idx)
	{
		return featureNames.get(idx);
	}

	public String[] getFeatureNames()
	{
		return featureNames.toArray(new String[featureNames.size()]);
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int nGramStart = pos - offset;
		if (direction == Direction.FORWARD)
			nGramStart -= (n - 1);

		if (nGramStart < -1)
			return 0;
		else if ((nGramStart + (n - 1)) > s.length())
			return 0;

		String nGram = construct(s, nGramStart, n, caseSensitive);
		NGramData data = nGramMap.get(nGram);
		if ((data != null) &&
			(data.frequency > freqThreshold) &&
			(data.frequencyRank >= minFreqRank))
		{
			indices.add(data.featureIndex);
			values.add(1.0);
			return 1;
		}
		return 0;
	}

	public int getNumFeatures()
	{
		return featureNames.size();
	}

	public void printConfiguration(OutputStream out)
	{
		String boundarySymbols = "";
		if (useBoundarySymbols)
			boundarySymbols = " + boundary symbols";
		PrintWriter writer = new PrintWriter(out);
		writer.printf("%d-grams%s, offset = %d, direction = %s, " +
				      "frequency threshold = %d, minimum rank = %d", n,
				      boundarySymbols, offset, direction.toString(),
				      freqThreshold, minFreqRank);
		if (caseSensitive)
			writer.print(", case sensitive");
		writer.println();
		writer.flush();
	}

	public static String construct(Sentence s, int startPos, int n,
			boolean caseSensitive)
	{
		StringBuffer nGram = new StringBuffer();
		for (int i = startPos; i < (startPos + n); i++)
		{
			if (i != startPos)
				nGram.append("_");

			if (i < 0)
				nGram.append(startSymbol);
			else if (i >= s.length())
				nGram.append(endSymbol);
			else
			{
				String word = s.get(i).getWord();
				if (!caseSensitive)
					word = word.toLowerCase();
				nGram.append(word);
			}
		}
		return nGram.toString();
	}

}

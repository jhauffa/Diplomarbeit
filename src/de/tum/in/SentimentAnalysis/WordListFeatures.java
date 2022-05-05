package de.tum.in.SentimentAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Vector;

public class WordListFeatures implements FeatureTemplate, Serializable {

	public enum NeighborhoodType { NEIGHBORHOOD_LEFT, NEIGHBORHOOD_BOTH,
		NEIGHBORHOOD_CHUNK };

	private HashSet<String> wordList;
	private String listName;
	private NeighborhoodType neighborhoodType;
	private int neighborhoodSize;
	private boolean membershipFeature;
	private boolean caseSensitive;
	private boolean matchStemForm;
	private String[] featureNames;

	public WordListFeatures(File listFile, String listName,
			NeighborhoodType neighborhoodType, int neighborhoodSize,
			boolean membershipFeature, boolean caseSensitive,
			boolean matchStemForm)
	{
		wordList = new HashSet<String>();
		readWordListFile(listFile);

		this.listName = listName;
		this.neighborhoodType = neighborhoodType;
		this.neighborhoodSize = neighborhoodSize;
		this.membershipFeature = membershipFeature;
		this.caseSensitive = caseSensitive;
		this.matchStemForm = matchStemForm;

		if (membershipFeature)
		{
			featureNames = new String[2];
			featureNames[1] = "LST_is_" + listName;
		}
		else
			featureNames = new String[1];
		featureNames[0] = getFeatureName(neighborhoodType) + listName;
	}

	private static String getFeatureName(NeighborhoodType type)
	{
		switch (type)
		{
		case NEIGHBORHOOD_LEFT:
			return "LST_preceded_by_";
		case NEIGHBORHOOD_BOTH:
			return "LST_neighborhood_contains_";
		case NEIGHBORHOOD_CHUNK:
			return "LST_chunk_contains_";
		}
		throw new AssertionError("invalid neighborhood type");
	}

	private void readWordListFile(File wordListFile)
	{
		try
		{
			BufferedReader reader = new BufferedReader(
					new FileReader(wordListFile));
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (caseSensitive)
					wordList.add(line);
				else
					wordList.add(line.toLowerCase());
			}
		}
		catch (IOException ex)
		{
			System.err.println("Error reading " + wordListFile.getPath() +
					": " + ex.getMessage());
		}
	}

	public String getFeatureName(int idx)
	{
		return featureNames[idx];
	}

	public String[] getFeatureNames()
	{
		return featureNames;
	}

	public int getNumFeatures()
	{
		return featureNames.length;
	}

	public void reset()
	{
		// nothing to do
	}

	public void train(Corpus corpus)
	{
		// nothing to do
	}

	public int getFeaturesForWord(Sentence s, int pos, Vector<Integer> indices,
			Vector<Double> values)
	{
		int numFeatures = 0;

		int startPos = pos;
		int endPos = pos;
		switch (neighborhoodType)
		{
		case NEIGHBORHOOD_BOTH:
			endPos += neighborhoodSize;
			endPos = Math.min(endPos, s.length() - 1);
			// fall through
		case NEIGHBORHOOD_LEFT:
			startPos -= neighborhoodSize;
			startPos = Math.max(startPos, 0);
			break;
		case NEIGHBORHOOD_CHUNK:
			int numTags = ChunkAnnotationProvider.getNumTagsUnencoded();
			int chunkType = s.get(pos).getAnnotation("chunk0");

			if (chunkType < numTags)
			{
				// not BIO2 encoded or not start tag; scan backwards
				while (startPos > 0)
				{
					startPos--;
					int curChunkType = s.get(startPos).getAnnotation("chunk0");
					if (curChunkType < numTags)
					{
						if (curChunkType != chunkType)
						{
							startPos++;
							break;
						}
					}
					else  // found start tag
						break;
				}
			}
			else
				chunkType -= numTags;

			// scan forward
			while (endPos < (s.length() - 1))
			{
				endPos++;
				int curChunkType = s.get(endPos).getAnnotation("chunk0");
				if (curChunkType != chunkType)
				{
					endPos--;
					break;
				}
			}
			break;
		}

		String curWord;
		String curWordStem = null;
		for (int i = startPos; i <= endPos; i++)
		{
			if (i == pos)
				continue;
			curWord = s.get(i).getWord();
			if (!caseSensitive)
				curWord = curWord.toLowerCase();
			if (matchStemForm)
				curWordStem = WordStemFeatures.getStemForm(curWord);
			if (wordList.contains(curWord) ||
				(matchStemForm && wordList.contains(curWordStem)))
			{
				indices.add(0);
				values.add(1.0);
				numFeatures++;
			}
		}

		if (membershipFeature)
		{
			curWord = s.get(pos).getWord();
			if (!caseSensitive)
				curWord = curWord.toLowerCase();
			if (matchStemForm)
				curWordStem = WordStemFeatures.getStemForm(curWord);
			if (wordList.contains(curWord) ||
				(matchStemForm && wordList.contains(curWordStem)))
			{
				indices.add(1);
				values.add(1.0);
				numFeatures++;
			}
		}

		return numFeatures;
	}

	public void printConfiguration(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.printf("word list \"%s\": ", listName);
		if (membershipFeature)
			writer.print("current word in list, ");
		switch (neighborhoodType)
		{
		case NEIGHBORHOOD_LEFT:
			writer.printf("one of the %d preceding words in list",
					neighborhoodSize);
			break;
		case NEIGHBORHOOD_BOTH:
			writer.printf("one of the %d surrounding words in list",
					neighborhoodSize);
			break;
		case NEIGHBORHOOD_CHUNK:
			writer.print("one of the words in the current chunk in list");
			break;
		}
		if (matchStemForm)
			writer.print(" (+ stem form)");
		writer.println();
		writer.flush();
	}

}

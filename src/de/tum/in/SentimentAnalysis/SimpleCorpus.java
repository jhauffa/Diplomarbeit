package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;


public class SimpleCorpus extends Corpus {

	public static final Character wordSeparator = ' ';
	public static final Character attributeSeparator = '/';
	public static final Character blockStartMarker = 's';
	public static final Character blockOtherMarker = 'x';

	public SimpleCorpus(int numClasses)
	{
		super(numClasses);
	}

	public SimpleCorpus(File corpusFile) throws FileNotFoundException
	{
		super();
		BufferedReader reader = new BufferedReader(new FileReader(corpusFile));
		String line;
		try
		{
			// read number of classes
			line = reader.readLine();
			setNumClasses(Integer.parseInt(line));

			// read sentences
			while ((line = reader.readLine()) != null)
			{
				Sentence s = parseSentence(line);
				if (s != null)
					add(s);
			}
		}
		catch (IOException ex)
		{
			System.err.println("Error reading " + corpusFile.getPath() + ": " +
					ex.getMessage());
		}
	}

	private Sentence parseSentence(String line)
	{
		Sentence s = new Sentence();

		for (String annotatedWord : line.split(wordSeparator.toString()))
		{
			String[] part = annotatedWord.split(attributeSeparator.toString());

			boolean startsBlock = false;
			Sentiment.Polarity polarity = Sentiment.Polarity.NEUTRAL;
			double intensity = 0.0;
			double confidence = 1.0;

			if (part.length < 1)
				continue;
			if (part.length > 1)
			{
				if (blockStartMarker.equals(part[1].charAt(0)))
					startsBlock = true;
				polarity = Sentiment.ordinalToPolarity(
						Integer.parseInt(part[1].substring(1)));
				if (part.length > 2)
				{
					intensity = Double.parseDouble(part[2]);
					if (part.length > 3)
						confidence = Double.parseDouble(part[3]);
				}
			}

			s.append(part[0], new Sentiment(polarity, intensity, confidence),
					startsBlock);
		}

		if (!s.isEmpty())
			return s;
		return null;
	}

	public void write(OutputStream out)
	{
		PrintWriter writer = new PrintWriter(out);
		writer.println(getNumClasses());
		for (Sentence s : sentences)
		{
			for (int i = 0; i < s.length(); i++)
			{
				Word w = s.get(i);
				if (i > 0)
					writer.print(wordSeparator);

				writer.printf("%s%c", w.getWord(), attributeSeparator);
				if (w.getStartsBlock())
					writer.print(blockStartMarker);
				else
					writer.print(blockOtherMarker);

				Sentiment sentiment = w.getTrueSentiment();
				writer.printf("%d%c%s%c%s", w.getTrueLabel(),
						attributeSeparator,
						Double.toString(sentiment.getIntensity()),
						attributeSeparator,
						Double.toString(sentiment.getConfidence()));
			}
			writer.println();
		}
		writer.flush();	
	}

}

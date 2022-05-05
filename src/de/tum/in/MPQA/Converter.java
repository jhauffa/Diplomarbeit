package de.tum.in.MPQA;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;

import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Word;
import de.tum.in.SentimentAnalysis.Sentiment;
import de.tum.in.SentimentAnalysis.SimpleCorpus;


public class Converter {

	private static boolean isNeutral(Sentence s)
	{
		for (Word w : s)
			if (w.getTrueSentiment().getPolarity() !=
				Sentiment.Polarity.NEUTRAL)
				return false;
		return true;
	}

	private static void convertCorpus(File corpusDir, boolean printPolarity,
			boolean printIntensity, boolean printConfidence, boolean onlyDirect,
			boolean onlyExpressive)
		throws FileNotFoundException
	{
		System.out.println(4);  // number of classes

		Corpus corpus = new Corpus(corpusDir, onlyDirect, onlyExpressive);

		int num = 0;
		int numNeutralIntensity = 0;

		for (Sentence s : corpus)
		{
			if (isNeutral(s))  // discard all completely neutral sentences
				continue;

			Iterator<Word> it = s.iterator();
			while (it.hasNext())
			{
				Word w = it.next();

				num++;
				if ((w.getTrueSentiment().getIntensity() == 0.0) &&
					(w.getTrueSentiment().getPolarity() != Sentiment.Polarity.NEUTRAL))
					numNeutralIntensity++;

				String word = w.getWord();
				// Tokenizer generates one token per punctuation character, so
				// it is sufficient to check the first character.
				if (word.charAt(0) == SimpleCorpus.attributeSeparator)
					word = "-";
				System.out.print(word);

				if (printPolarity)
				{
					System.out.print(SimpleCorpus.attributeSeparator);
					if (w.getStartsBlock())
						System.out.print(SimpleCorpus.blockStartMarker);
					else
						System.out.print(SimpleCorpus.blockOtherMarker);
					System.out.print(w.getTrueLabel());
				}
				if (printIntensity)
					System.out.printf("%c%s", SimpleCorpus.attributeSeparator,
						Double.toString(w.getTrueSentiment().getIntensity()));
				if (printConfidence)
					System.out.printf("%c%s", SimpleCorpus.attributeSeparator,
					   Double.toString(w.getTrueSentiment().getConfidence()));

				if (it.hasNext())
					System.out.print(SimpleCorpus.wordSeparator);
 			}
			System.out.println();
		}

		System.err.printf("neutral intensity: %d / %d\n", numNeutralIntensity, num);
	}

	private static void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.printf("error: %s\n", message);
		System.err.printf("\nusage: Converter [options] path\n"+
				"where path identifies the root directory of the MPQA corpus\n"+
				"valid options are:\n"+
				"\t-i\tappend the intensity value to the generated label\n"+
				"\t-c\tappend the confidence value to the generated label\n"+
				"\t\t(implies -i)\n"+
				"\t-u\tgenerate untagged corpus ('.' at the end of "+
				"each sentence)\n"+
				"\t-d\textract only \"direct subjective\" annotations\n"+
				"\t-e\textract only \"expressive subjectivity\" annotations\n"+
				"\t-h\tshow this text\n");
		System.exit(code);
	}
	
	public static void main(String[] args)
	{
		if (args.length < 1)
			printUsageAndExit("not enough arguments", 1);

		File corpusDir = null;
		boolean printPolarity = true;
		boolean printIntensity = false;
		boolean printConfidence = false;
		boolean onlyDirect = false;
		boolean onlyExpressive = false;

		for (String arg : args)
		{
			if ((arg.length() > 1) && (arg.charAt(0) == '-'))
			{
				switch (arg.charAt(1))
				{
				case 'c':
					printConfidence = true;
					// fall through
				case 'i':
					printIntensity = true;
					break;
				case 'u':
					printPolarity = false;
					printIntensity = false;
					printConfidence = false;
					break;
				case 'd':
					onlyDirect = true;
					break;
				case 'e':
					onlyExpressive = true;
					break;
				case 'h':
					printUsageAndExit(null, 0);
				default:
					printUsageAndExit("invalid option " + arg, 1);
				}
			}
			else
				corpusDir = new File(arg);
		}

		if (corpusDir == null)
			printUsageAndExit("corpus directory not specified", 1);

		try
		{
			convertCorpus(corpusDir, printPolarity, printIntensity,
					printConfidence, onlyDirect, onlyExpressive);
		}
		catch (Exception ex)
		{
			System.err.println("error: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(2);
		}
	}

}

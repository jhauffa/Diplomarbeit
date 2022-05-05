package de.tum.in.Linguistics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// import de.tum.in.Util.Statistics;


public class LanguageIdentifierUnigram {

	public static final double[] EN = {0.07444952384665621, 0.01519596853586502,
		0.022525903326737692, 0.04071427875196163, 0.1184973633678711,
		0.01996237560067451, 0.015507880654625559, 0.06065715984521361,
		0.07007300693029739, 0.001627791369781564, 0.00829491290828809,
		0.04701100464944002, 0.027311804898969717, 0.06409794040529081,
		0.08876823954850721, 0.013675396956907391, 0.0014620880566900275,
		0.06378602828653027, 0.062216720439016306, 0.08645814041893697,
		0.03507061885313813, 0.01092667141033014, 0.023071749534568635,
		0.001832483697718168, 0.026473541079800766, 3.314066261830729E-4};

	private static final char[] umlauts = {'Š', 'Ÿ', 'š', '§'};
	private static final int numLetters = 26;

	public static boolean isLanguage(String text, double[] languageCharDist,
			boolean penalizeUmlauts)
	{
		int[] dist = new int[numLetters];
		int numChars = 0;
		for (int i = 0; i < text.length(); i++)
		{
			char c = Character.toLowerCase(text.charAt(i));
			int charIdx = c - 'a';
			if ((charIdx >= 0) && (charIdx < numLetters))
			{
				dist[charIdx]++;
				numChars++;
			}
			if (penalizeUmlauts)
				for (int j = 0; j < umlauts.length; j++)
					if (c == umlauts[j])
						numChars += 100;
		}

		if (numChars == 0)
			return false;

		double sumSqDiff = 0.0;
		for (int i = 0; i < numLetters; i++)
			sumSqDiff += Math.pow(((double) dist[i] / numChars) -
					languageCharDist[i], 2.0);
		// System.err.printf("d = %f\n", sumSqDiff);
		return (sumSqDiff < 0.005);  // was 0.02
		// return !Statistics.chiSquare(dist, languageCharDist, true);
	}


	public static void main(String[] args)
	{
		int[] dist = new int[numLetters];
		int n = 0;

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(args[0]));
			int c;
			while ((c = reader.read()) >= 0)
			{
				int charIdx = Character.toLowerCase(c) - 'a';
				if ((charIdx >= 0) && (charIdx < numLetters))
				{
					dist[charIdx]++;
					n++;
				}
			}
			
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}

		for (int i = 0; i < numLetters; i++)
		{
			if (i > 0)
				System.out.print(", ");
			System.out.print((double) dist[i] / n);
		}
	}

}

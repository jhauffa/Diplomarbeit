package de.tum.in.UnitTests;

import junit.framework.TestCase;
import de.tum.in.SentimentAnalysis.LabelEncoding;
import de.tum.in.SentimentAnalysis.Sentiment;
import de.tum.in.SentimentAnalysis.Word;

public class LabelEncodingTest extends TestCase {

	private static final int numLabels = 3;
	private static final int[] labels =
		{ 0,  1,  0,  0,  2,  2,  2,  1,  0,  0,  0};
	private static final boolean[] startsBlock =
		{false, true, false, false, true, false, true, true, false, false,
		 false};

	private static final int[] expectedBIO1 =
		{ 0,  1,  0,  0,  2,  2,  4,  1,  0,  0,  0};
	private static final int[] expectedBIO2 =
		{ 0,  3,  0,  0,  4,  2,  4,  3,  0,  0,  0};
	private static final int[] expectedBIOW =
		{ 0,  5,  0,  0,  4,  2,  6,  5,  0,  0,  0};
	private static final int[] expectedBMEO =
		{ 0,  3,  0,  0,  4,  6,  4,  3,  0,  0,  0};
	private static final int[] expectedBMEOW =
		{ 0,  7,  0,  0,  4,  6,  8,  7,  0,  0,  0};
	private static final int[] expectedBMEOWplus =
		{11,  7,  9, 12,  4,  6,  8,  7,  9,  0,  0};


	private Word createWord(int label, boolean startsBlock)
	{
		return new Word("foo", new Sentiment(Sentiment.ordinalToPolarity(label),
				1.0, 1.0), startsBlock);
	}

	private void compareSequence(String scheme, int[] expected)
	{
		LabelEncoding enc = new LabelEncoding(numLabels, scheme);
		for (int i = 0; i < labels.length; i++)
		{
			Word prev = null;
			if (i > 0)
				prev = createWord(labels[i - 1], startsBlock[i - 1]); 
			Word cur = createWord(labels[i], startsBlock[i]);
			Word next = null;
			if ((i + 1) < labels.length)
				next = createWord(labels[i + 1], startsBlock[i + 1]);

			int newLabel = enc.encode(prev, cur, next);
			assertEquals(expected[i], newLabel);
		}
	}

	public void testLabelEncoding()
	{
		compareSequence("no", labels);
		compareSequence("BIO1", expectedBIO1);
		compareSequence("BIO2", expectedBIO2);
		compareSequence("BIOW", expectedBIOW);
		compareSequence("BMEO", expectedBMEO);
		compareSequence("BMEOW", expectedBMEOW);
		compareSequence("BMEOWplus", expectedBMEOWplus);
	}

}

package de.tum.in.UnitTests;

import junit.framework.TestCase;
import java.util.Iterator;
import java.io.File;
import java.io.FileNotFoundException;
import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Word;
import de.tum.in.SentimentAnalysis.Sentiment;

public class MPQATest extends TestCase {

/*
	private void dumpSentence(Sentence s)
	{
		for (Word w : s)
			System.out.printf("%s ", w.word);
		System.out.println();
	}
*/

	public void testCorpus()
	{
		de.tum.in.MPQA.Corpus corpus = null;
		try
		{
			corpus = new de.tum.in.MPQA.Corpus(new File("./testcorpus"),
					false, false);
		}
		catch (FileNotFoundException ex)
		{
			fail(ex.getMessage());
		}

		Iterator<Sentence> itCorpus = corpus.iterator();
		assertTrue(itCorpus.hasNext());

		Sentence sentence = itCorpus.next();
		assertNotNull(sentence);

		Iterator<Word> itSentence = sentence.iterator();
		assertTrue(itSentence.hasNext());

		Word word = itSentence.next();
		assertEquals("The", word.getWord());
		assertEquals(Sentiment.Polarity.NEUTRAL,
				word.getTrueSentiment().getPolarity());
		assertEquals(0.0, word.getTrueSentiment().getIntensity());
		assertEquals(1.0, word.getTrueSentiment().getConfidence());

		// locate word with non-trivial polarity annotation
		for (int i = 0; i < 4; i++)
		{
			assertTrue(itCorpus.hasNext());
			sentence = itCorpus.next();
		}
		itSentence = sentence.iterator();
		for (int i = 0; i < 13; i++)
		{
			assertTrue(itSentence.hasNext());
			word = itSentence.next();
		}

		assertEquals("suffering", word.getWord());
		assertEquals(Sentiment.Polarity.NEUTRAL,
				word.getTrueSentiment().getPolarity());
		assertEquals(0.5, word.getTrueSentiment().getIntensity());
		assertEquals(1.0, word.getTrueSentiment().getConfidence());

		// locate sentence in different "corpus particle"
		for (int i = 0; i < 11; i++)
		{
			assertTrue(itCorpus.hasNext());
			sentence = itCorpus.next();
		}

		itSentence = sentence.iterator();
		assertTrue(itSentence.hasNext());

		word = itSentence.next();
		assertEquals("Beijing", word.getWord());
		assertEquals(Sentiment.Polarity.NEUTRAL,
				word.getTrueSentiment().getPolarity());
		assertEquals(0.0, word.getTrueSentiment().getIntensity());
		assertEquals(1.0, word.getTrueSentiment().getConfidence());
	}

}

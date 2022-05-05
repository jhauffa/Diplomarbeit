package de.tum.in.UnitTests;

import junit.framework.TestCase;

import de.tum.in.Linguistics.Tokenizer;


public class TokenizerTest extends TestCase {

	private static final String text1 = "He won't go, he cannot go -- " +
		"it's {00000000-0000-0000-C000-000000000046}.";
	private static final String[] tokens1 = {"He", "wo", "n't", "go", ",", "he",
		"can", "not", "go", "-", "-", "it", "'s",
		"{00000000-0000-0000-C000-000000000046}", "."};
	private static final int[] tokenOffsets1 = {0, 3, 5, 9, 11, 13, 16, 19, 23,
		26, 27, 29, 31, 34, 72};

	private static final String text2 = "Foo bar\nfoo foo\r\nfoo\n\nfoo";
	private static final String[] tokens2 = {"Foo", "bar",
		Tokenizer.lineEndToken, "foo", "foo", Tokenizer.lineEndToken, "foo",
		Tokenizer.lineEndToken, Tokenizer.lineEndToken, "foo"};
	private static final int[] tokenOffsets2 = {0, 4, 7, 8, 12, 16, 17, 20, 21,
		22};

	private void performTest(Tokenizer t, String[] tokens, int[] offsets)
	{
		int c = 0;
		while (t.hasNext() && (c < tokens.length))
		{
			assertEquals(tokens[c], t.next());
			assertEquals(offsets[c], t.getOffset());
			c++;
		}
		assertEquals(tokens.length, c);
		assertFalse(t.hasNext());		
	}

	public void testTokenization()
	{
		Tokenizer tokenIterator = new Tokenizer(text1, false);
		performTest(tokenIterator, tokens1, tokenOffsets1);
		tokenIterator = new Tokenizer(text2, true);
		performTest(tokenIterator, tokens2, tokenOffsets2);
	}

}

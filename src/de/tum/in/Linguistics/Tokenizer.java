package de.tum.in.Linguistics;

import java.util.Iterator;

public class Tokenizer implements Iterator<String> {

	private enum CharacterClass { CHAR_SPACE, CHAR_WORD, CHAR_PUNCT };

	private static CharacterClass getCharacterClass(char c)
	{
		CharacterClass charClass;
		if (Character.isWhitespace(c))
			charClass = CharacterClass.CHAR_SPACE;
		else if (Character.isLetterOrDigit(c))
			charClass = CharacterClass.CHAR_WORD;
		else
			charClass = CharacterClass.CHAR_PUNCT;
		return charClass;
	}

	public static final String lineEndToken = "<E>";

	private String text;
	private String curToken;
	private int curTokenStart;
	private int prevTokenStart;
	private int prevTokenEnd;
	private boolean reportLineEnd;

	public Tokenizer(String text, boolean reportLineEnd)
	{
		this.text = text;
		this.reportLineEnd = reportLineEnd;

		prevTokenStart = -1;
		prevTokenEnd = 0;
		readNextWord();
	}

	public boolean hasNext()
	{
		return (curToken != null);
	}

	public String next()
	{
		prevTokenStart = curTokenStart;
		String token = curToken;
		readNextWord();
		return token;
	}

	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	public int getOffset()
	{
		return prevTokenStart;
	}


	// This method tries to match the tokenization style of the Penn TreeBank,
	// so that tokens produced by the converter and the external PoS tagger
	// match up.

	private void readNextWord()
	{
		curTokenStart = -1;
		char prevChar = '\0';
		CharacterClass prevClass = CharacterClass.CHAR_SPACE;

		int pos = prevTokenEnd;
		while (pos < text.length())
		{
			char curChar = text.charAt(pos);
			CharacterClass curClass = getCharacterClass(curChar);

			if ((prevChar == '\n') && reportLineEnd)
			{
				curTokenStart = pos - 1;
				prevTokenEnd = pos;
				curToken = lineEndToken;
				return;
			}

			if ((curClass != prevClass) ||
				(curClass == CharacterClass.CHAR_PUNCT))
			{
				if (curTokenStart >= 0)
				{
					// special cases:
					// treat "'t" as part of the preceding word, all other
					// apostrophes as part of the following word
					boolean ignoreBoundary = false;
					if (curChar == '\'')
					{
						if (((pos + 1) < text.length()) &&
							(text.charAt(pos + 1) == 't'))
							ignoreBoundary = true;
					}
					else if (prevChar == '{')
					{
						// extract UUID as single token
						if (((pos + 36) < text.length()) &&
							(text.charAt(pos + 36) == '}'))
						{
							curTokenStart = pos - 1;
							pos += 37;
						}
					}
					else if (prevChar == '\'')
					{
						if (curClass != CharacterClass.CHAR_SPACE)
							ignoreBoundary = true;
					}

					if (!ignoreBoundary)
					{
						String token = text.substring(curTokenStart, pos);
						int prefixLength = 0;
						if (token.equals("cannot"))
							prefixLength = 3;
						else if (token.endsWith("n't"))
							prefixLength = token.length() - 3;

						if (prefixLength == 0)
						{
							curToken = token;
							prevTokenEnd = pos;
						}
						else
						{
							curToken = token.substring(0, prefixLength);
							prevTokenEnd = (pos - token.length())+prefixLength;
						}
						return;
					}
				}
				else if (curClass != CharacterClass.CHAR_SPACE)
					curTokenStart = pos;
			}

			prevChar = curChar;
			prevClass = curClass;
			pos++;
		}

		if (curTokenStart >= 0)
			curToken = text.substring(curTokenStart);
		else
			curToken = null;
		prevTokenEnd = pos;
	}

}

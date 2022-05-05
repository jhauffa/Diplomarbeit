package de.tum.in.Linguistics;

import java.util.Iterator;

public class SimpleTokenizer implements Iterator<String> {

	private String text;
	private String curToken;
	private int curPos;

	public SimpleTokenizer(String text)
	{
		this.text = text;
		curPos = 0;
		readNextWord();
	}

	public boolean hasNext()
	{
		return (curToken != null);
	}

	public String next()
	{
		String token = curToken;
		readNextWord();
		return token;
	}

	public void remove()
	{
		throw new UnsupportedOperationException();
	}

	private void readNextWord()
	{
		int tokenStart = -1;
		curToken = null;
		while (curPos < text.length())
		{
			char c = text.charAt(curPos);
			if (Character.isWhitespace(c))
			{
				if (tokenStart >= 0)
				{
					curToken = text.substring(tokenStart, curPos);
					break;
				}
			}
			else
			{
				if (tokenStart < 0)
					tokenStart = curPos;
			}
			curPos++;
		}
	}

}

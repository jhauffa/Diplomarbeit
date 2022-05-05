package de.tum.in.Util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;

public class RawCharSequence implements CharSequence {

	private byte[] data;
	private int offset;
	private int length;
	private CharsetDecoder dec;

	public RawCharSequence(byte[] data)
	{
		this(data, 0, data.length, null);
	}

	public RawCharSequence(byte[] data, int offset, int length)
	{
		this(data, offset, length, null);
	}

	public RawCharSequence(byte[] data, int offset, int length, String encoding)
	{
		this.data = data;
		this.offset = offset;
		this.length = length;

		if (encoding != null)
		{
			dec = Charset.forName(encoding).newDecoder();
			if ((dec.averageCharsPerByte() != 1.0) ||
				(dec.maxCharsPerByte() != 1.0))
				throw new RuntimeException(encoding +
						" is not a single byte encoding");
			dec.onMalformedInput(CodingErrorAction.REPLACE);
			dec.onUnmappableCharacter(CodingErrorAction.REPLACE);
		}
		else
			dec = null;
	}

	public char charAt(int index)
	{
		if ((index < 0) || (index >= length))
			throw new IndexOutOfBoundsException();

		if (dec == null)
			return fakeDecode(data[offset + index]);

		ByteBuffer buf = ByteBuffer.wrap(data, offset + index, 1);
		CharBuffer cbuf;
		try
		{
			cbuf = dec.decode(buf);
		}
		catch (CharacterCodingException ex)
		{
			// should not happen
			return '?';
		}
		return cbuf.charAt(0);
	}

	private char fakeDecode(byte b)
	{
		if (b < 0)
			return (char) (b + 256);
		return (char) b;
	}

	public int length()
	{
		return length;
	}

	public String subSequence(int start, int end)
	{
		if ((start < 0) || (end < 0) || (start > end) || (end > length))
			throw new IndexOutOfBoundsException();

		int len = end - start;
		if (dec == null)
		{
			StringBuffer buf = new StringBuffer(len);
			for (int i = 0; i < len; i++)
				buf.append(fakeDecode(data[offset + start + i]));
			return buf.toString();
		}
		else
		{
			ByteBuffer buf = ByteBuffer.wrap(data, offset + start,offset + len);
			CharBuffer cbuf = null;
			try
			{
				cbuf = dec.decode(buf);
			}
			catch (CharacterCodingException ex)
			{
				// should not happen
				return null;
			}
			return cbuf.toString();
		}
	}

	public String toString()
	{
		return subSequence(0, length());
	}

	public int findAsciiString(String s, int startOffset)
	{
		int strOffset = 0;
		for (int i = startOffset; i < length; i++)
		{
			if (charAt(i) == s.charAt(strOffset))
			{
				if (++strOffset == s.length())
					return i - (s.length() - 1);
			}
			else
				strOffset = 0;
		}
		return -1;
	}

}

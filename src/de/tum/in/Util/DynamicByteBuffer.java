package de.tum.in.Util;

public class DynamicByteBuffer {

	private static final int defaultBufferSize = 1024;

	private byte[] buf;
	private int pos;
	private int limit;

	public DynamicByteBuffer()
	{
		this(defaultBufferSize);
	}

	public DynamicByteBuffer(int bufferSize)
	{
		buf = new byte[bufferSize];
		limit = bufferSize;
		pos = 0;
	}

	public void append(byte b)
	{
		if (pos >= limit)
			grow();
		buf[pos] = b;
		pos++;
	}

	public void append(byte[] b)
	{
		append(b, b.length);
	}

	public void append(byte[] b, int n)
	{
		while ((pos + n) > limit)
			grow();
		System.arraycopy(b, 0, buf, pos, n);
		pos += n;		
	}

	public void append(DynamicByteBuffer other)
	{
		append(other.getBuffer(), other.size());
	}

	private void grow()
	{
		limit *= 2;
		byte[] newBuf;
		try
		{
			newBuf = new byte[limit];
		}
		catch (OutOfMemoryError ex)
		{
			System.err.printf("not enough memory to grow buffer to %d bytes\n",
					limit);
			throw ex;
		}
		System.arraycopy(buf, 0, newBuf, 0, buf.length);
		buf = newBuf;
	}

	public int size()
	{
		return pos;
	}

	public byte[] getBuffer()
	{
		return buf;
	}

	public void clear()
	{
		pos = 0;
	}

	public boolean startsWithAscii(String s)
	{
		int len = s.length();
		if (len > pos)
			return false;
		for (int i = 0; i < len; i++)
		{
			char c = s.charAt(i);
			if ((c < 0) || (c > 127))
				return false;
			if (c != buf[i])
				return false;
		}
		return true;
	}

	public byte get(int idx)
	{
		if (idx >= pos)
			throw new IndexOutOfBoundsException();
		return buf[idx];
	}

	public void printAsAscii()
	{
		for (int i = 0; i < pos; i++)
		{
			if ((buf[i] >= 0) && (buf[i] < 128))
				System.err.print((char) buf[i]);
			else
				System.err.print('?');
		}
	}

}

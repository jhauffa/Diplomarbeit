package de.tum.in.EMail;

import java.util.HashMap;
import java.io.UnsupportedEncodingException;

public class Header {

	private int bodyOffset;
	private HashMap<String, StringBuffer> headerElements;

	public Header(byte[] data, int offset, int length)
	{
		// find end of headers / start of body
		bodyOffset = offset + 2;
		while (bodyOffset <= (offset + length))
		{
			if ((data[bodyOffset - 1] == MessageEncoding.LF) &&
				(data[bodyOffset - 2] == MessageEncoding.LF))
				break;
			else if ((bodyOffset > (offset + 3)) &&
					 (data[bodyOffset - 1] == MessageEncoding.LF) &&
					 (data[bodyOffset - 2] == MessageEncoding.CR) &&
					 (data[bodyOffset - 3] == MessageEncoding.LF) &&
					 (data[bodyOffset - 4] == MessageEncoding.CR))
				break;
			bodyOffset++;
		}

		// extract headers; assume ASCII encoding
		String rawHeader = "";
		try
		{
			rawHeader = new String(data, offset, (bodyOffset - 2) - offset,
				"US-ASCII");
		}
		catch (UnsupportedEncodingException ex)
		{
			// cannot happen
		}

		String[] lines = rawHeader.split("\n");
		headerElements = new HashMap<String, StringBuffer>();
		StringBuffer curBuf = null;
		for (String line : lines)
		{
			int sepIdx = line.indexOf(':');
			if (sepIdx >= 0)
			{
				String key = line.substring(0, sepIdx);
				String value = line.substring(sepIdx + 1).trim();
				curBuf = headerElements.get(key);
				if (curBuf == null)
				{
					curBuf = new StringBuffer();
					curBuf.append(value);
					headerElements.put(key, curBuf);
				}
				else
					curBuf.append(value);
			}
			else if (curBuf != null)
				curBuf.append(line);
		}
	}

	public int getBodyOffset()
	{
		return bodyOffset;
	}

	public String get(String key)
	{
		StringBuffer buf = headerElements.get(key);
		if (buf == null)
			return "";
		return buf.toString();
	}

}

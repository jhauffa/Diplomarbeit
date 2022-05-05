package de.tum.in.EMail;

import java.io.UnsupportedEncodingException;

import de.tum.in.Util.RawCharSequence;


public class MimeDecoder {

	StringBuffer plainText;

	public MimeDecoder(byte[] data, int offset, int length, String contentType,
			String transferEncoding)
	{
		plainText = new StringBuffer();
		parseMimePart(data, offset, length, contentType, transferEncoding);
	}

	private boolean parseMimePart(byte[] data, int offset, int length,
			String contentType, String transferEncoding)
	{
		String encoding = getParameter(contentType, "charset");
		if (encoding == null)
			encoding = "US-ASCII";
		else
			encoding = encoding.toUpperCase();

		boolean hasText = false;
		if (contentType.startsWith("text/plain") || contentType.isEmpty())
		{
			hasText = processTextPart(data, offset, length, encoding,
					transferEncoding, plainText);
		}
		else if (contentType.startsWith("text/html"))
		{
			StringBuffer htmlContent = new StringBuffer();
			hasText = processTextPart(data, offset, length, encoding,
					transferEncoding, htmlContent);
			if (hasText)
				plainText.append(stripHtmlTags(htmlContent));
		}
		else if (contentType.startsWith("multipart/"))
		{
			String boundary = "--";
			String boundarySuffix = getParameter(contentType, "boundary");
			if (boundarySuffix != null)
				boundary += boundarySuffix;

			boolean isAlternative = contentType.startsWith("alternative", 10);

			hasText = processMultiPart(data, offset, length, isAlternative,
					boundary);
		}
		return hasText;
	}

	private boolean processTextPart(byte[] data, int offset, int length,
			String contentEncoding, String transferEncoding,
			StringBuffer targetBuf)
	{
		try
		{
			String s;
			if (transferEncoding.equals("quoted-printable"))
			{
				CharSequence seq = new RawCharSequence(data, offset, length);
				s = MessageEncoding.decodeQuotedPrintable(seq, contentEncoding,
						false);
			}
			else if (transferEncoding.equals("base64"))
			{
				CharSequence seq = new RawCharSequence(data, offset, length);
				s = MessageEncoding.decodeBase64(seq, contentEncoding);
			}
			else
			{
				s = new String(data, offset, length, contentEncoding);
			}
			targetBuf.append(s);
		}
		catch (UnsupportedEncodingException ex)
		{
			return false;
		}
		return true;
	}

	private boolean processMultiPart(byte[] data, int offset, int length,
			boolean isAlternative, String boundary)
	{
		boolean hasText = false;
		RawCharSequence seq = new RawCharSequence(data, offset, length);
		int startBoundaryOffset = seq.findAsciiString(boundary, 0);
		while ((startBoundaryOffset >= 0) &&
			   ((startBoundaryOffset + 1) < length))
		{
			int endBoundaryOffset = seq.findAsciiString(boundary,
					startBoundaryOffset + 1);
			if (endBoundaryOffset >= 0)
			{
				int headerOffset = offset + startBoundaryOffset +
					boundary.length() + 1;
				Header header = new Header(data, headerOffset,
						(offset + endBoundaryOffset) - headerOffset);

				int bodyLength = (offset + endBoundaryOffset) -
						header.getBodyOffset();
				if (bodyLength > 0)
				{
					boolean curPartHasText = parseMimePart(data,
							header.getBodyOffset(), bodyLength,
							header.get("Content-Type"),
							header.get("Content-Transfer-Encoding"));
					if (curPartHasText)
					{
						hasText = true;
						if (isAlternative)
							break;
					}
				}
			}
			startBoundaryOffset = endBoundaryOffset;
		}

		return hasText;
	}

	private StringBuffer stripHtmlTags(StringBuffer htmlData)
	{
		StringBuffer text = new StringBuffer();

		boolean insideTag = false;
		for (int i = 0; i < htmlData.length(); i++)
		{
			char c = htmlData.charAt(i);
			if (c == '<')
				insideTag = true;
			else if (c == '>')
				insideTag = false;
			else if (!insideTag)
				text.append(c);
		}

		return text;
	}

	private String getParameter(String header, String parameterName)
	{
		String[] parts = header.split(";");
		for (int i = 1; i < parts.length; i++)
		{
			int valueSepIdx = parts[i].indexOf('=');
			String key = "";
			if (valueSepIdx > 0)
				key = parts[i].substring(0, valueSepIdx).trim();
			if (key.equals(parameterName))
			{
				int quoteStart = parts[i].indexOf('"');
				if ((quoteStart >= 0) && ((quoteStart + 1) < parts[i].length()))
				{
					int quoteEnd = parts[i].indexOf('"', quoteStart + 1);
					if (quoteEnd >= 0)
						return parts[i].substring(quoteStart + 1, quoteEnd);
				}
				if ((valueSepIdx + 1) < parts[i].length())
					return parts[i].substring(valueSepIdx + 1);
			}
		}
		return null;
	}

	public String getPlainText()
	{
		return plainText.toString();
	}

}

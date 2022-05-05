package de.tum.in.EMail;

import java.io.UnsupportedEncodingException;
import de.tum.in.Util.DynamicByteBuffer;

public class MessageEncoding {

	public static final byte CR = 0x0D;
	public static final byte LF = 0x0A;

	private static final int[] base64Map = {62, -1, -1, -1, 63, 52, 53, 54, 55,
		56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3,
		4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
		23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34,
		35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51};

	public static String encodeQuotedPrintable(CharSequence s,
			String destEncoding, boolean inHeader)
	{
		StringBuffer enc = new StringBuffer();
		try
		{
			byte[] data = s.toString().getBytes(destEncoding);
			for (byte b : data)
			{
				if (((b >= 33) && (b <= 60)) ||
					((b >= 62) && (b <= 126)) ||
					(b == '\n'))
				{
					enc.append((char) b);
				}
				else if (b == ' ')
				{
					if (inHeader)
						enc.append('_');
					else
						enc.append(' ');
				}
				else
				{
					enc.append('=');
					int x = b;
					if (x < 0)
						x += 256;
					enc.append(Integer.toHexString(x).toUpperCase());
				}
			}
		}
		catch (UnsupportedEncodingException ex)
		{
		}
		return enc.toString();
	}

	public static String decodeQuotedPrintable(CharSequence s,
			String sourceEncoding, boolean inHeader)
		throws UnsupportedEncodingException
	{
		DynamicByteBuffer buf = new DynamicByteBuffer(s.length());

		int i = 0;
		while (i < s.length())
		{
			char c = s.charAt(i);
			if (inHeader && (c == '_'))
			{
				buf.append((byte) ' ');
				i++;
			}
			else if (c == '=')
			{				
				if ((i + 2) < s.length())
				{
					CharSequence entity = s.subSequence(i + 1, i + 3);
					if (entity.charAt(0) == '\n')
						i += 2;
					else if ((entity.charAt(0) == '\r') &&
							 (entity.charAt(1) == '\n'))
						i += 3;
					else
					{
						try
						{
							buf.append((byte) Integer.parseInt(
									entity.toString(), 16));
						}
						catch (NumberFormatException ex)
						{
							// malformed entity
							buf.append((byte) '=');
							buf.append((byte) entity.charAt(0));
							buf.append((byte) entity.charAt(1));
						}
						i += 3;
					}
				}
			}
			else
			{
				buf.append((byte) c);
				i++;
			}
		}

		return new String(buf.getBuffer(), 0, buf.size(), sourceEncoding);
	}

	public static String decodeBase64(CharSequence s, String sourceEncoding)
		throws UnsupportedEncodingException
	{
		DynamicByteBuffer buf = new DynamicByteBuffer((s.length() / 4) * 3);

		int i = 0;
		int encIdx = 0;
		int numPadBytes = 0;
		byte[] values = new byte[4];
		while (i < s.length())
		{
			char c = s.charAt(i);
			switch (c)
			{
			case '=':
				values[encIdx++] = 0;
				numPadBytes++;
				break;
			case '\r':
			case '\n':
				break;
			default:
				int code = c - '+';
				int v = -1;
				if ((code >= 0) && (code < base64Map.length))
					v = base64Map[code];
				if (v < 0)
				{
					buf.append((byte) '?');
					i += 4 - encIdx;  // skip to next group
					encIdx = 0;
				}
				else
					values[encIdx++] = (byte) v;
				break;
			}

			if (encIdx > 3)
			{
				buf.append((byte)((values[0] << 2)+(values[1] >> 4)));
				buf.append((byte)(((values[1] & 15) << 4)+(values[2] >>2)));
				buf.append((byte)(((values[2] & 3) << 6)+values[3]));
				encIdx = 0;
			}
			i++;
		}

		return new String(buf.getBuffer(), 0, buf.size() - numPadBytes,
				sourceEncoding);
	}

}

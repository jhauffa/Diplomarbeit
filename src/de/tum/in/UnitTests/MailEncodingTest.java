package de.tum.in.UnitTests;

import junit.framework.TestCase;
import de.tum.in.EMail.MessageEncoding;
import java.io.UnsupportedEncodingException;

public class MailEncodingTest extends TestCase {

	private static final String base64Enc = "UG9seWZvbiB6d2l0c2NoZXJuZCBhw59" +
			"lbiBNw6R4Y2hlbnMgVsO2Z2VsIFLDvGJlbiwgSm9n\r\naHVydCB1bmQgUXVhcms=";
	private static final String base64Dec = "Polyfon zwitschernd aßen " +
			"Mäxchens Vögel Rüben, Joghurt und Quark";

	private static final String quotedPrintableEnc = "Saturday July 25 in the" +
			" Netherlands. We will be sailing again on the =\n\nFrisian " +
			"vessel the St=EAd Sleat";
	private static final String quotedPrintableDec = "Saturday July 25 in the" +
			" Netherlands. We will be sailing again on the \nFrisian " +
			"vessel the Stêd Sleat";

	public void testDecoder()
	{
		try
		{
			String dec = MessageEncoding.decodeBase64(base64Enc, "UTF-8");
			assertEquals(dec, base64Dec);

			dec = MessageEncoding.decodeQuotedPrintable(quotedPrintableEnc,
					"ISO-8859-1", false);
			assertEquals(dec, quotedPrintableDec);
		}
		catch (UnsupportedEncodingException ex)
		{
			fail();
		}
	}

}

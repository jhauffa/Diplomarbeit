package de.tum.in.EMail.Tools;

import java.io.File;
import java.io.IOException;

import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageDatabase;
import de.tum.in.Linguistics.LanguageIdentifierBigram;
import de.tum.in.Util.Time;


public class VerifyLanguage {

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: VerifyLanguage mailbox");
			return;
		}

		MessageDatabase dbIn;
		try
		{
			dbIn = MessageDatabase.readFromFile(new File(args[0]));
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}

		MessageDatabase dbOut = new MessageDatabase();
		int numSkip = 0;
		for (Message m : dbIn)
		{
			if (LanguageIdentifierBigram.isLanguage(m.getBody(),
					LanguageIdentifierBigram.EN, LanguageIdentifierBigram.DE))
				dbOut.add(m, true);
			else
				numSkip++;
		}

		System.out.printf("removed %d of %d messages\n", numSkip, dbIn.size());
		
		try
		{
			MessageDatabase.writeToFile(
					new File("./out-" + Time.getTimeStamp() + ".dat"), dbOut);
		}
		catch (IOException ex)
		{
			System.err.printf("write error: %s\n", ex.getMessage());
		}
	}

}

package de.tum.in.EMail.Tools;

import java.io.File;
import java.io.IOException;

import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageDatabase;

public class ExtractWorkingSet {

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("not enough arguments");
			return;
		}

		int targetSize = 50;
		if (args.length > 1)
			targetSize = Integer.parseInt(args[1]);

		MessageDatabase dbIn;
		try
		{
			dbIn = MessageDatabase.readFromFile(new File(args[0]));
		}
		catch (IOException ex)
		{
			System.err.println("read error: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}

		double p = (double) targetSize / dbIn.size();
		MessageDatabase dbOut = new MessageDatabase();
		int n = 0;
		for (Message m : dbIn)
		{
			if (Math.random() < p)
			{
				dbOut.add(m, true);
				if (++n >= targetSize)
					break;
			}
		}

		String fileName = args[0] + ".out";
		try
		{
			MessageDatabase.writeToFile(new File(fileName), dbOut);
		}
		catch (IOException ex)
		{
			System.err.println("write error: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}
		System.out.println("working set written to " + fileName);
	}

}

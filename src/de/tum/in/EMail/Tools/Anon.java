package de.tum.in.EMail.Tools;

import java.io.File;
import java.io.IOException;

import de.tum.in.EMail.MessageDatabase;


public class Anon {

	public static void main(String[] args)
	{
		MessageDatabase db;
		try
		{
			db = MessageDatabase.readFromFile(new File(args[0]));
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}

		db.anonymize(true);

		try
		{
			MessageDatabase.writeToFile(new File("clean.dat"), db);
		}
		catch (IOException ex)
		{
			System.err.printf("write error: %s\n", ex.getMessage());
		}
	}

}

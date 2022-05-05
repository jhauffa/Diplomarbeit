package de.tum.in.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class StreamWatcher extends Thread {

	private BufferedReader reader;

	public StreamWatcher(InputStream in)
	{
		reader = new BufferedReader(new InputStreamReader(in));
	}

	@Override public void run()
	{
		String line;
		try
		{
			while ((line = reader.readLine()) != null)
				System.err.printf("OUT: %s\n", line);
			reader.close();
		}
		catch (IOException ex)
		{
			System.err.printf("IO error: %s\n", ex.getMessage());
		}
	}

}

package de.tum.in.SocialWebCrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class DefinitionDatabase {

	private HashMap<String, Double> definitions;

	public DefinitionDatabase()
	{
		definitions = new HashMap<String, Double>();		
	}

	public synchronized void append(String word, double polarity)
	{
		definitions.put(word, polarity);
	}

	public synchronized Double query(String word)
	{
		return definitions.get(word);
	}

	public void readFromFile(File file) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null)
		{
			String[] part = line.split(";");
			if (part.length != 2)
				continue;

			double value;
			try
			{
				value = Double.parseDouble(part[1]);
			}
			catch (NumberFormatException ex)
			{
				continue;
			}
			append(part[0], value);
		}
		reader.close();
	}

	public void writeToFile(File file) throws IOException
	{
		PrintWriter writer = new PrintWriter(new FileOutputStream(file, false));
		for (Map.Entry<String, Double> e : definitions.entrySet())
		{
			String word = e.getKey();
			word.replace(';', ' ');
			writer.printf("%s;%f\n", word, e.getValue());
		}
		writer.close();
	}

}

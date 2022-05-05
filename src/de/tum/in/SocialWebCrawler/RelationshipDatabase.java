package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Iterator;

import de.tum.in.Util.Time;


public class RelationshipDatabase
		implements Iterable<Map.Entry<String, LinkedList<Relationship>>> {

	private HashMap<String, LinkedList<Relationship>> data;
	private File file;

	public RelationshipDatabase(String prefix)
	{
		// create new database, use given prefix and timestamp as file name
		data = new HashMap<String, LinkedList<Relationship>>();
		String fileName = prefix + "-" + Time.getTimeStampGmt() + ".csv";
		file = new File(fileName);
	}

	public RelationshipDatabase(File file)
	{
		// open existing database; if file does not exist, write() will create a
		// new database
		this.data = new HashMap<String, LinkedList<Relationship>>();
		this.file = file;
		if (this.file.exists())
			read();
	}

	public synchronized void append(Relationship r)
	{
		LinkedList<Relationship> list = data.get(r.from);
		if (list == null)
		{
			list = new LinkedList<Relationship>();
			data.put(r.from, list);
		}
		list.add(r);
	}

	public synchronized void append(LinkedList<Relationship> list)
	{
		if (list.isEmpty())
			return;
		String userName = list.get(0).from;
		LinkedList<Relationship> prevList = data.get(userName);
		if (prevList != null)
		{
			for (Relationship r : list)
				prevList.add(r);
		}
		else
			data.put(userName, list);
	}

	public synchronized void appendEmpty(String userName)
	{
		data.put(userName, new LinkedList<Relationship>());
	}

	public synchronized LinkedList<Relationship> query(String userName)
	{
		return data.get(userName);
	}

	private void read()
	{
		int numRecords = 0;
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null)
			{
				String[] part = line.split(";");
				if (part.length == 3)
					append(new Relationship(part[0], part[1], part[2]));
				else if (part.length == 1)
					appendEmpty(part[0]);
				else
					continue;
				numRecords++;
			}
			reader.close();
		}
		catch (IOException ex)
		{
			System.err.printf("error reading from %s: %s\n", file.getName(),
					ex.getMessage());
		}
		System.err.printf("read %d relationship records\n", numRecords);
	}

	public void write() throws IOException
	{
		PrintWriter writer = new PrintWriter(new FileOutputStream(file, false));
		for (Entry<String,LinkedList<Relationship>> entry : data.entrySet())
		{
			LinkedList<Relationship> list = entry.getValue();
			if (list.size() > 0)
			{
				for (Relationship r : list)
					writer.printf("%s;%s;%s\n", r.from, r.to, r.type);
			}
			else
				writer.printf("%s;;\n", entry.getKey());
		}
		writer.close();
	}

	public String getFileName()
	{
		return file.getPath();
	}

	public Iterator<Map.Entry<String, LinkedList<Relationship>>> iterator()
	{
		return data.entrySet().iterator();
	}

}

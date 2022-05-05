package de.tum.in.SocialWebCrawler;

import java.util.HashMap;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import de.tum.in.Util.Time;


public class CommentDatabase implements Iterable<Comment> {

	private HashMap<Integer, Comment> data;
	private File file;

	public CommentDatabase(String prefix)
	{
		// create new database, use given prefix and timestamp as file name
		data = new HashMap<Integer, Comment>();
		String fileName = prefix + "-" + Time.getTimeStampGmt() + ".dat";
		file = new File(fileName);
	}

	public CommentDatabase(File file)
	{
		// open existing database; if file does not exist, write() will create a
		// new database
		this.data = new HashMap<Integer, Comment>();
		this.file = file;
		if (this.file.exists())
			read();
	}

	public synchronized void append(Comment c)
	{
		data.put(c.id, c);
	}

	public synchronized Comment query(int id)
	{
		return data.get(id);
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
				if (part.length != 8)
					continue;

				Comment c = new Comment(Integer.parseInt(part[0]));
				c.parentId = Integer.parseInt(part[1]);
				c.title = unescape(part[2]);
				c.score = Integer.parseInt(part[3]);
				c.scoreType = part[4];
				c.author = part[5];
				c.date = part[6];

				// read comment body
				int bodyLength = Integer.parseInt(part[7]);
				char[] buf = new char[bodyLength];
				reader.read(buf, 0, bodyLength);
				c.body = unescape(new String(buf));

				append(c);
				numRecords++;

				// discard all characters up to and including the next line
				// break
				reader.readLine();
			}
			reader.close();
		}
		catch (IOException ex)
		{
			System.err.printf("error reading from %s: %s\n", file.getName(),
					ex.getMessage());
		}
		System.err.printf("read %d comment records\n", numRecords);
	}

	public void write() throws IOException
	{
		PrintWriter writer = new PrintWriter(new FileOutputStream(file, false));
		for (Comment c : data.values())
		{
			String body = escape(c.body);
			writer.printf("%d;%d;%s;%d;%s;%s;%s;%d\n", c.id, c.parentId,
					escape(c.title), c.score, c.scoreType, c.author, c.date,
					body.length());
			writer.printf("%s\n", body);
		}
		writer.close();
	}

	private String escape(String s)
	{
		return s.replaceAll(";", "%!%");
	}

	private String unescape(String s)
	{
		return s.replaceAll("%!%", ";");
	}

	public String getFileName()
	{
		return file.getPath();
	}

	public Iterator<Comment> iterator()
	{
		return data.values().iterator();
	}

}

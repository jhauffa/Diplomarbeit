package de.tum.in.EMail.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.MessageFile;
import de.tum.in.EMail.Person;
import de.tum.in.EMail.PersonDatabase;
import de.tum.in.SocialNetworks.Relationship;
import de.tum.in.Util.Time;


public class DataConverterBA {

	private static class RatingStats
	{
		public int sum;
		public int n;
	}

	private static class Mailbox
	{
		public MessageDatabase db;
		public HashMap<String, RatingStats> ratings;

		public Mailbox()
		{
			db = new MessageDatabase();
			ratings = new HashMap<String, RatingStats>();
		}
	}

	private static void parseAnnotFile(File f, HashMap<String, Integer> annot)
			throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		while ((line = reader.readLine()) != null)
		{
			String[] parts = line.split(";");
			if (parts.length != 2)
				continue;

			int v = 0;
			try
			{
				v = Integer.parseInt(parts[1]);
			}
			catch (NumberFormatException ex)
			{
				System.err.printf("parse error: %s\n", parts[1]);
			}

			annot.put(parts[0], v);
		}
		reader.close();
	}

	private static void addToMailbox(HashMap<String, Mailbox> mailboxes,
			Person recipient, Message msg, int annotValue)
	{
		String recipientId = recipient.getId();
		Mailbox m = mailboxes.get(recipientId);
		if (m == null)
		{
			m = new Mailbox();
			mailboxes.put(recipientId, m);
		}

		m.db.add(new Message(msg), true);

		String senderId = msg.getSender().getId();
		RatingStats r = m.ratings.get(senderId);
		if (r == null)
		{
			r = new RatingStats();
			m.ratings.put(senderId, r);
		}
		r.sum += annotValue;
		r.n++;
	}

	private static void processEnronCorpusRecursive(File f,
			HashMap<String, Integer> annot, HashMap<String, Mailbox> mailboxes)
	{
		if (f.isDirectory())
		{
			// recursively traverse all subdirectories
			System.err.printf("processing %s\n", f.getPath());  // DBG
			for (File item : f.listFiles())
				processEnronCorpusRecursive(item, annot, mailboxes);
		}
		else
		{
			try
			{
				// Read message ID; this functionality cannot be easily added to
				// class Message, as it would change the on-disk structure for
				// serialization.
				BufferedReader reader = new BufferedReader(new FileReader(f));
				String firstLine = reader.readLine();
				reader.close();
				if (firstLine == null)
					return;
				if (!firstLine.startsWith("Message-ID: <"))
					return;
				String id = firstLine.substring(13, firstLine.length() - 1);

				// Is there an annotation for the message ID?
				Integer annotValue = annot.get(id);
				if (annotValue != null)
				{
					MessageDatabase msgFile = new MessageFile(f);
					for (Message m : msgFile)
					{
						for (Person p : m.getRecipients())
							addToMailbox(mailboxes, p, m, annotValue);
						for (Person p : m.getRecipientsCc())
							addToMailbox(mailboxes, p, m, annotValue);
						for (Person p : m.getRecipientsBcc())
							addToMailbox(mailboxes, p, m, annotValue);
					}
				}
			}
			catch (IOException ex)
			{
				System.err.printf("error reading %s: %s\n", f.getAbsoluteFile(),
						ex.getMessage());
			}
		}
	}

	private static void processEnronCorpus(File baseDir,
			HashMap<String, Integer> annot)
	{
		HashMap<String, Mailbox> mailboxes = new HashMap<String, Mailbox>();
		processEnronCorpusRecursive(baseDir, annot, mailboxes);

		int idx = 1;
		for (Map.Entry<String, Mailbox> e : mailboxes.entrySet())
		{
			Mailbox m = e.getValue();
			if (m.db.size() < 2)
			{
				System.err.println("skipping mailbox with less than 2 mails");
				continue;
			}

			PersonDatabase personDb = m.db.getPersonDatabase();
			m.db.setOwner(personDb.queryById(e.getKey()));
			for (Map.Entry<String, RatingStats> r : m.ratings.entrySet())
			{
				Person p = personDb.queryById(r.getKey());
				if (p != null)
				{
					assert Relationship.attributeDbMin[1] == -1.0;
					assert Relationship.attributeDbMax[1] == 1.0;
					RatingStats stats = r.getValue();
					p.setRating(Relationship.attributeNames[1],
							(double) stats.sum / stats.n);
					p.setRating(Relationship.attributeNames[0], 0.5);
				}
			}

			// anonymize, write to disk
			m.db.anonymize(true);
			File f = new File("ba-" + Time.getTimeStamp() + "-" + idx + ".dat");
			try
			{
				MessageDatabase.writeToFile(f, m.db);
				idx++;
			}
			catch (IOException ex)
			{
				System.err.printf("write error: %s\n", ex.getMessage());
			}
		}
	}

	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.err.println("usage: DataConverterBA enron annotations");
			return;
		}

		HashMap<String, Integer> annot = new HashMap<String, Integer>();
		try
		{
			parseAnnotFile(new File(args[1]), annot);
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}

		processEnronCorpus(new File(args[0]), annot);
	}

}

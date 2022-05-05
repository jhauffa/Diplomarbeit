package de.tum.in.EMail.Tools;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import de.tum.in.EMail.MboxFile;
import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.Person;
import de.tum.in.Linguistics.LanguageIdentifierBigram;
import de.tum.in.SocialNetworks.Relationship;
import de.tum.in.Util.Time;


public class DataConverterSEP {

	private static void parseAnnotationFile(File f,
			HashMap<String, Double> annot) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		while ((line = reader.readLine()) != null)
		{
			String[] parts = line.split(";");
			if (parts.length != 2)
				continue;

			double value = 0.0;
			try
			{
				// parse double according to current locale
				value =
					NumberFormat.getInstance().parse(parts[1]).doubleValue();
			}
			catch (ParseException ex)
			{
				System.err.printf("parse error: %s\n", parts[1]);
			}

			annot.put(parts[0], value);
		}
		reader.close();
	}

	private static void processMailbox(MessageDatabase mboxIn,
			Map<String, Double> annot)
	{
		// transfer ratings into person database
		double defaultValence =
			(Relationship.attributeDbMin[1] + Relationship.attributeDbMax[1]) /
			2.0;
		for (Map.Entry<String, Double> e : annot.entrySet())
		{
			for (Person p : mboxIn.getPersonDatabase())
				if (p.hasEMailAddress(e.getKey()))
				{
					double intensity = e.getValue() / 10.0;
					intensity = Relationship.attributeDbMin[0] + (intensity *
							(Relationship.attributeDbMax[0] -
							 Relationship.attributeDbMin[0]));
					p.setRating(Relationship.attributeNames[0], intensity);
					p.setRating(Relationship.attributeNames[1], defaultValence);
				}
		}

		// copy relevant messages into new database
		MessageDatabase mboxOut = new MessageDatabase();
		HashMap<String, Integer> ownerStatistics =
			new HashMap<String, Integer>();
		for (Message m : mboxIn)
		{
			if (LanguageIdentifierBigram.isLanguage(m.getBody(),
					LanguageIdentifierBigram.EN, LanguageIdentifierBigram.DE))
			{
				boolean isRelevant = false;
				Person sender = m.getSender();
				if (sender.hasRating(Relationship.attributeNames[0]))
				{
					isRelevant = true;
					for (Person p : m.getRecipients())
						updateOwnerStatistics(ownerStatistics, p);
					for (Person p : m.getRecipientsCc())
						updateOwnerStatistics(ownerStatistics, p);
					for (Person p : m.getRecipientsBcc())
						updateOwnerStatistics(ownerStatistics, p);
				}
				else
				{
					for (Person p : m.getRecipients())
						if (p.hasRating(Relationship.attributeNames[0]))
							isRelevant = true;
					for (Person p : m.getRecipientsCc())
						if (p.hasRating(Relationship.attributeNames[0]))
							isRelevant = true;
					for (Person p : m.getRecipientsBcc())
						if (p.hasRating(Relationship.attributeNames[0]))
							isRelevant = true;
					if (isRelevant)
						updateOwnerStatistics(ownerStatistics, sender);
				}

				if (isRelevant)
					mboxOut.add(m, true);
			}
			else
				System.err.printf("discarding message \"%s\" from \"%s\"\n",
						m.getSubject(), m.getSender().getDisplayName());
		}

		// determine owner
		String candidate = "";
		int maxMsg = 0;
		for (Map.Entry<String, Integer> e : ownerStatistics.entrySet())
		{
			if (e.getValue() > maxMsg)
			{
				candidate = e.getKey();
				maxMsg = e.getValue();
			}
		}
		mboxOut.setOwner(mboxOut.getPersonDatabase().queryById(candidate));

		// anonymize, write to disk
		mboxOut.anonymize(true);
		File f = new File("sep-" + Time.getTimeStamp() + ".dat");
		try
		{
			MessageDatabase.writeToFile(f, mboxOut);
		}
		catch (IOException ex)
		{
			System.err.printf("write error: %s\n", ex.getMessage());
		}
	}

	private static void updateOwnerStatistics(HashMap<String, Integer> stats,
			Person candidate)
	{
		String id = candidate.getId();
		Integer c = stats.get(id);
		if (c != null)
			stats.put(id, c + 1);
		else
			stats.put(id, 1);
	}
	
	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.err.println("usage: DataConverterSEP mbox annotations");
			return;
		}

		HashMap<String, Double> annotations = new HashMap<String, Double>();
		MessageDatabase mbox = null;
		try
		{
			parseAnnotationFile(new File(args[1]), annotations);
			mbox = new MboxFile(new File(args[0]));
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}

		processMailbox(mbox, annotations);
	}

}

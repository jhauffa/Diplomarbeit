package de.tum.in.EMail.Tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.Person;
import de.tum.in.SocialNetworks.Relationship;
import de.tum.in.SocialNetworks.SocialNetwork;


public class DumpMessageDatabase {

	public static void main(String[] args)
	{
		MessageDatabase db;
		try
		{
			db = MessageDatabase.readFromFile(new File(args[0]));
		}
		catch (IOException ex)
		{
			System.err.println("read error: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}

		Person owner = db.getOwner();
		System.err.printf("owner = %s\n\n", owner.getId());

		HashSet<String> senders = new HashSet<String>();
		for (Message m : db)
			senders.add(m.getSender().getId());
		int numMsg = db.size();
		int numPersons = senders.size();
		System.out.printf("DB: %d messages, %d persons => %f messages per " +
				"person\n", numMsg, numPersons, (float) numMsg / numPersons);

		int numIn = 0;
		int numOut = 0;
		
		HashMap<String, Integer> recipients = new HashMap<String, Integer>();
		for (Message m : db)
		{
			for (Person p : m.getRecipients())
				countPerson(recipients, p);
			for (Person p : m.getRecipientsCc())
				countPerson(recipients, p);
			for (Person p : m.getRecipientsBcc())
				countPerson(recipients, p);

			if (m.getSender().getId().equals(owner.getId()))
				numOut++;
			else
				numIn++;
		}
		System.out.printf("in = %d, out = %d\n", numIn, numOut);

		for (Map.Entry<String, Integer> entry : recipients.entrySet())
			System.err.printf("%s\t%d\n", entry.getKey(), entry.getValue());

/*
		for (Message m : db)
		{
			System.out.println("From: " + formatId(m.getSender()));
			System.out.print("To: ");
			for (Person p : m.getRecipients())
				System.out.print(formatId(p) + " ");
			System.out.println();
*/
			/*
			System.out.print("CC: ");
			for (Person p : m.getRecipientsCc())
				System.out.print(p.getId() + " ");
			System.out.println();
			System.out.print("BCC: ");
			for (Person p : m.getRecipientsBcc())
				System.out.print(p.getId() + " ");
			System.out.println();
			System.out.println("Body:");
			System.out.println(m.getBody());
			*/
/*
			System.out.println("\n----------------------\n");			
		}
*/

		SocialNetwork net = new SocialNetwork(db, false);
		for (Relationship r : net)
			System.out.printf("%f;%f\n",
					r.trueAttributes[0], r.trueAttributes[1]);
	}

	private static void countPerson(HashMap<String, Integer> map, Person p)
	{
		Integer count = map.get(p.getId());
		if (count == null)
			count = 0;
		map.put(p.getId(), count + 1);
	}

/*
	private static String formatId(Person p)
	{
		String id = p.getId() + "-";
		if (p.hasRating("intensity"))
			id += "i";
		if (p.hasRating("valence"))
			id += "v";
		return id;
	}
*/

}

package de.tum.in.SocialNetworks;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.Person;
import de.tum.in.Math.RowDataSet;


public class SocialNetwork implements Iterable<Relationship> {

	private ArrayList<Relationship> edges;

	public SocialNetwork()
	{
		edges = new ArrayList<Relationship>();
	}

	public SocialNetwork(MessageDatabase db)
	{
		this(db, true);
	}

	public SocialNetwork(MessageDatabase db, boolean enablePreprocessing)
	{
		this();

		HashMap<String, Relationship> edgeMap =
			new HashMap<String, Relationship>();

		MessagePreprocessor proc = null;
		if (enablePreprocessing)
			proc = new MessagePreprocessor(true, true);

		// extract relationships, assign messages to relationships
		Person owner = db.getOwner();
		for (Message m : db)
		{
			Person sender = m.getSender();
			ArrayList<Person> recipients =
					new ArrayList<Person>(m.getRecipients());
			recipients.addAll(m.getRecipientsCc());
			recipients.addAll(m.getRecipientsBcc());

			boolean senderIsOwner = sender.getId().equals(owner.getId());
			boolean senderHasRatings = hasRatings(sender);
			int knownRecipients = 0;
			for (Person p : recipients)
			{
				boolean recipientIsOwner = p.getId().equals(owner.getId());
				boolean recipientHasRatings = hasRatings(p);
				if (recipientIsOwner && senderHasRatings)
				{
					addMessage(edgeMap, owner, sender, m, proc);
					knownRecipients++;
				}
				else if (senderIsOwner && recipientHasRatings)
				{
					addMessage(edgeMap, owner, p, m, proc);
					knownRecipients++;
				}
			}

			// assign messages with unknown recipients to mailbox owner
			if ((knownRecipients == 0) && !senderIsOwner && senderHasRatings)
				addMessage(edgeMap, owner, sender, m, proc);
		}

		edges.addAll(edgeMap.values());
	}

	private static void addMessage(HashMap<String, Relationship> edgeMap,
			Person source, Person dest, Message m, MessagePreprocessor proc)
	{
		String key = source.getId() + dest.getId();
		Relationship r = edgeMap.get(key);
		if (r == null)
		{
			r = new Relationship(source.getId(), dest.getId());
			for (int i = 0; i < Relationship.numAttributes; i++)
			{
				r.trueAttributes[i] =
					(dest.getRating(Relationship.attributeNames[i]) -
							Relationship.attributeDbMin[i]) /
								(Relationship.attributeDbMax[i] -
								 Relationship.attributeDbMin[i]);
			}
			edgeMap.put(key, r);
		}
		if (proc != null)
			r.messages.add(proc.process(m));
		else
			r.messages.add(new ProcessedMessage(m));
	}

	private static boolean hasRatings(Person p)
	{
		for (String attr : Relationship.attributeNames)
			if (!p.hasRating(attr))
				return false;
		return true;
	}


	public Iterator<Relationship> iterator()
	{
		return edges.iterator();
	}

	public int getNumEdges()
	{
		return edges.size();
	}

	public Relationship get(int index)
	{
		return edges.get(index);
	}

	public void printEdges(PrintStream out)
	{
		out.println("\nrelationship edges:");
		for (Relationship r : edges)
			out.printf("%s -> %s\n", r.sourceId, r.destId);
		out.println();
		out.flush();
	}

	public RowDataSet getTrueRatings(int idx)
	{
		RowDataSet ratings = new RowDataSet(1, edges.size());
		int pos = 0;
		for (Relationship r : edges)
			ratings.set(0, pos++, r.trueAttributes[idx]);
		return ratings;
	}

	public void resetExclusion()
	{
		for (Relationship r : edges)
			r.excludeFromEvaluation = false;
	}

}

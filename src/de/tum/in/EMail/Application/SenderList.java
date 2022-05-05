package de.tum.in.EMail.Application;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.io.PrintWriter;

import de.tum.in.EMail.Person;
import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.Message;


public class SenderList {

	private class SortByMessageCount implements Comparator<Person>
	{
		public int compare(Person p1, Person p2)
		{
			int c1 = 0;
			ArrayList<Message> messageList = senderData.get(p1);
			if (messageList != null)
				c1 = messageList.size();
			int c2 = 0;
			messageList = senderData.get(p2);
			if (messageList != null)
				c2 = messageList.size();
			return c2 - c1;
		}
	}

	private IdentityHashMap<Person, ArrayList<Message>> senderData;
	private ArrayList<Person> senders;

	public SenderList()
	{
		senderData = new IdentityHashMap<Person, ArrayList<Message>>();
		senders = new ArrayList<Person>();
	}

	public SenderList(MessageDatabase source)
	{
		this();
		addIncomingMessages(source);
		sort();
	}

	public void addIncomingMessages(MessageDatabase source)
	{
		for (Message m : source)
			addMessage(m.getSender(), m);
	}

	public void addOutgoingMessages(MessageDatabase source)
	{
		for (Message m : source)
		{
			for (Person p : m.getRecipients())
				addMessage(p, m);
			for (Person p : m.getRecipientsCc())
				addMessage(p, m);
			for (Person p : m.getRecipientsBcc())
				addMessage(p, m);
		}		
	}

	private void addMessage(Person p, Message m)
	{
		ArrayList<Message> messageList = senderData.get(p);
		if (messageList == null)
		{
			messageList = new ArrayList<Message>();
			senderData.put(p, messageList);		
		}
		messageList.add(m);
	}

	public void sort()
	{
		senders.clear();
		senders.addAll(senderData.keySet());
		Collections.sort(senders, new SortByMessageCount());
	}

	public int size()
	{
		return senders.size();
	}

	public Person get(int idx)
	{
		return senders.get(idx);
	}

	public List<Person> get()
	{
		return senders;
	}

	public void copyMessages(Person sender, MessageDatabase dest)
	{
		ArrayList<Message> messageList = senderData.get(sender);
		if (messageList != null)
			for (Message m : messageList)
				dest.add(m, true);
	}

	public void dump(PrintWriter log)
	{
		for (Map.Entry<Person, ArrayList<Message>> e : senderData.entrySet())
			log.printf("%s: %d\n", e.getKey().getId(), e.getValue().size());
	}

}

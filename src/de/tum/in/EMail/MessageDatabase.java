package de.tum.in.EMail;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.PrintStream;

public class MessageDatabase implements Iterable<Message>, Serializable {

	private static final long serialVersionUID = 1002;
	private static final String keyMessageDb = "msgdb";
	private static final String keyOwnerId = "ownerid";
	
	protected ArrayList<Message> messages;
	protected PersonDatabase personDb;
	protected transient Person owner;

	public MessageDatabase()
	{
		this(new PersonDatabase());
	}

	public MessageDatabase(PersonDatabase personDb)
	{
		messages = new ArrayList<Message>();
		this.personDb = personDb;
	}

	public void add(Message m, boolean addPersons)
	{
		messages.add(m);
		if (addPersons)
		{
			m.setSender(personDb.addUnique(m.getSender()));
			copyPersonList(m.getRecipients(), personDb);
			copyPersonList(m.getRecipientsCc(), personDb);
			copyPersonList(m.getRecipientsBcc(), personDb);
		}
	}

	private void copyPersonList(Vector<Person> list, PersonDatabase db)
	{
		Vector<Person> alreadyInDb = new Vector<Person>();
		Iterator<Person> it = list.iterator();
		while (it.hasNext())
		{
			Person p = it.next();
			Person pd = db.addUnique(p);
			if (pd != p)
			{
				alreadyInDb.add(pd);
				it.remove();
			}
		}
		list.addAll(alreadyInDb);
	}

	public Iterator<Message> iterator()
	{
		return messages.iterator();
	}

	public PersonDatabase getPersonDatabase()
	{
		return personDb;
	}

	public void anonymize(boolean checkWholeDb)
	{
		PersonDatabase db = null;
		if (checkWholeDb)
			db = personDb;

		for (Message m : messages)
			m.anonymize(db);
		personDb.anonymize();
	}

	public int size()
	{
		return messages.size();
	}

	public void clear()
	{
		messages.clear();
	}

	public void setOwner(Person p)
	{
		this.owner = p;
	}

	public Person getOwner()
	{
		return owner;
	}

	public static void writeToFile(File f, MessageDatabase db)
			throws IOException
	{
		ZipOutputStream compressedOut = new ZipOutputStream(
				new FileOutputStream(f));

		compressedOut.putNextEntry(new ZipEntry(keyMessageDb));
		ObjectOutputStream objOut = new ObjectOutputStream(compressedOut);
		objOut.writeObject(db);
		objOut.flush();
		compressedOut.closeEntry();

		if (db.owner != null)
		{
			compressedOut.putNextEntry(new ZipEntry(keyOwnerId));
			PrintStream prnOut = new PrintStream(compressedOut);
			prnOut.print(db.owner.getId());
			prnOut.flush();
			compressedOut.closeEntry();
		}

		compressedOut.close();
	}

	public static MessageDatabase readFromFile(File f) throws IOException
	{
		ZipInputStream compressedIn = new ZipInputStream(
				new FileInputStream(f));

		MessageDatabase db = null;
		String ownerId = null;
		ZipEntry entry = compressedIn.getNextEntry();
		while (entry != null)
		{
			String entryName = entry.getName();
			if (entryName.equals(keyMessageDb))
			{
				ObjectInputStream in = new ObjectInputStream(compressedIn);
				try
				{
					db = (MessageDatabase) in.readObject();
				}
				catch (ClassNotFoundException ex)
				{
				}
			}
			else if (entryName.equals(keyOwnerId))
			{
				BufferedReader in = new BufferedReader(
						new InputStreamReader(compressedIn));
				ownerId = in.readLine();
			}

			compressedIn.closeEntry();
			entry = compressedIn.getNextEntry();			
		}

		compressedIn.close();

		if (db == null)
			throw new IOException("archive does not contain a message " +
					"database");
		if (ownerId != null)
			db.owner = db.personDb.queryById(ownerId);
		if (db.owner == null)
		{
			System.err.println("warning: owner not in db!");
			db.owner = new Person(ownerId);
		}
		return db;
	}

}

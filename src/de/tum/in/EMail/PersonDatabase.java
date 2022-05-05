package de.tum.in.EMail;

import java.util.ArrayList;
import java.util.Iterator;
import java.io.Serializable;

public class PersonDatabase implements Iterable<Person>, Serializable {

	private static final long serialVersionUID = 1004;

	private ArrayList<Person> persons;

	public PersonDatabase()
	{
		persons = new ArrayList<Person>();
	}

	public void add(Person p)
	{
		persons.add(p);
	}

	public Person addUnique(Person p)
	{
		Person q = find(p);
		if (q != null)
		{
			q.merge(p);
			return q;
		}
		else
		{
			add(p);
			return p;
		}
	}

	public Person queryByName(String name)
	{
		for (Person p : persons)
			if (p.hasName(name))
				return p;
		return null;
	}

	public Person queryByEMailAddress(String address)
	{
		for (Person p : persons)
			if (p.hasEMailAddress(address))
				return p;
		return null;
	}

	public Person queryById(String id)
	{
		for (Person p : persons)
			if (p.getId().equals(id))
				return p;
		return null;
	}

	public Person find(Person p)
	{
		String[] entities = p.getNames();
		for (String name : entities)
		{
			Person q = queryByName(name);
			if (q != null)
				return q;
		}
		entities = p.getEMailAddresses();
		for (String address : entities)
		{
			Person q = queryByEMailAddress(address);
			if (q != null)
				return q;
		}
		return null;
	}

	public void anonymize()
	{
		for (Person p : persons)
			p.anonymize();
	}

	public Iterator<Person> iterator()
	{
		return persons.iterator();
	}

	public Person get(int idx)
	{
		return persons.get(idx);
	}

	public int size()
	{
		return persons.size();
	}

}

package de.tum.in.EMail;

import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Map;
import java.io.Serializable;

public class Person implements Serializable {

	private static final long serialVersionUID = 1003;

	private String id;
	private HashSet<String> names;
	private HashSet<String> emailAddresses;
	private HashMap<String, Double> ratings;
	private transient ArrayList<String> nameParts;

	public Person()
	{
		this("{" + UUID.randomUUID().toString() + "}");
	}

	public Person(String id)
	{
		this.id = id;
		names = new HashSet<String>();
		emailAddresses = new HashSet<String>();
		ratings = new HashMap<String, Double>();
	}

	public Person(Person other)
	{
		this(other.id);
		names.addAll(other.names);
		emailAddresses.addAll(other.emailAddresses);
		for (Map.Entry<String, Double> r : other.ratings.entrySet())
			ratings.put(r.getKey(), r.getValue());
	}

	public void addName(String name)
	{
		assert !name.isEmpty();
		names.add(name);
	}

	public void addEMailAddress(String address)
	{
		assert !address.isEmpty();
		emailAddresses.add(address);
	}

	public String getId()
	{
		return id;
	}

	public String[] getNames()
	{
		return names.toArray(new String[names.size()]);
	}

	public String[] getEMailAddresses()
	{
		return emailAddresses.toArray(new String[emailAddresses.size()]);
	}

	public String getDisplayName()
	{
		for (String name : names)
			return name;
		for (String addr : emailAddresses)
			return addr;
		return id;
	}

	public boolean hasName(String name)
	{
		return names.contains(name);
	}

	public boolean hasEMailAddress(String address)
	{
		return emailAddresses.contains(address);
	}

	public boolean isIdentifiedBy(String s)
	{
		if (nameParts == null)
		{
			nameParts = new ArrayList<String>();
			for (String name : names)
			{
				String[] parts = name.split("\\s");
				for (String part : parts)
					if (part.length() > 2)
						nameParts.add(canonicalize(part));
			}
			for (String addr : emailAddresses)
				nameParts.add(canonicalize(addr));
		}

		s = canonicalize(s);
		for (String p : nameParts)
			if (p.equals(s))
				return true;
		for (String addr : emailAddresses)
			if (addr.equals(s))
				return true;
		return false;
	}

	private String canonicalize(String s)
	{
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (Character.isLetter(c))
				buf.append(Character.toLowerCase(c));
		}
		return buf.toString();
	}

	public void merge(Person other)
	{
		names.addAll(other.names);
		emailAddresses.addAll(other.emailAddresses);
	}

	public void anonymize()
	{
		names.clear();
		emailAddresses.clear();
	}

	public void setRating(String key, Double value)
	{
		ratings.put(key, value);
	}

	public double getRating(String key)
	{
		Double value = ratings.get(key);
		if (value == null)
			return 0.0;
		return value;
	}

	public boolean hasRating(String key)
	{
		return ratings.containsKey(key);
	}

}

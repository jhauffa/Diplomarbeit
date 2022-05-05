package de.tum.in.EMail;

import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.util.Date;
import java.util.Locale;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;


public class Message implements Serializable {

	private static final String[] dateFormatPatterns = {
		"EEE, d MMM yyyy HH:mm:ss Z",
		"d MMM yyyy HH:mm:ss Z",
		"d MMM yyyy HH:mm Z",
		"EEE MMM d HH:mm:ss Z yyyy",
		"EEE, d MMM yyyy HH:mm:ss",
		"d MMM yyyy HH:mm:ss"
	};

	private static final long serialVersionUID = 1001;

	private Person sender;
	private Vector<Person> recipients;
	private Vector<Person> recipientsCc;
	private Vector<Person> recipientsBcc;
	private Date date;
	private String subject;
	private String body;

	public Message()
	{
		recipients = new Vector<Person>();
		recipientsCc = new Vector<Person>();
		recipientsBcc = new Vector<Person>();
		date = new Date(0);
	}

	public Message(Message other)
	{
		this();
		sender = new Person(other.sender);
		for (Person p : other.recipients)
			recipients.add(new Person(p));
		for (Person p : other.recipientsCc)
			recipients.add(new Person(p));
		for (Person p : other.recipientsBcc)
			recipients.add(new Person(p));
		date = other.date;
		subject = other.subject;
		body = other.body;
	}

	public Message(PersonDatabase personDb, byte[] data, int offset, int length)
	{
		// extract metadata from header
		Header header = new Header(data, offset, length);

		Vector<Person> pv = extractMultiplePersons(header.get("From"),personDb);
		if (!pv.isEmpty())
			sender = pv.firstElement();
		else
		{
			sender = extractSinglePerson(header.get("Sender"), personDb);
			if (sender == null)
				sender = new Person();
		}
		recipients = extractMultiplePersons(header.get("To"), personDb);
		recipientsCc = extractMultiplePersons(header.get("Cc"), personDb);
		recipientsBcc = extractMultiplePersons(header.get("Bcc"), personDb);
		date = extractDate(header.get("Date"));
		subject = decodeHeaderString(header.get("Subject"));

		// decode MIME data in body
		int bodyLength = (offset + length) - header.getBodyOffset();
		if (bodyLength > 0)
		{
			MimeDecoder dec = new MimeDecoder(data, header.getBodyOffset(),
					bodyLength, header.get("Content-Type"),
					header.get("Content-Transfer-Encoding"));
			body = dec.getPlainText();
		}
		else
			body = "";
	}

	private Person extractSinglePerson(String data, PersonDatabase db)
	{
		if (data.isEmpty())
			return null;

		Person p = new Person();
		parsePersonIdentifier(data, 0, p);
		return db.addUnique(p);
	}

	private Vector<Person> extractMultiplePersons(String data,
			PersonDatabase db)
	{
		Vector<Person> persons = new Vector<Person>();
		if (data.isEmpty())
			return persons;

		int offset = 0;
		while (offset < data.length())
		{
			Person p = new Person();
			offset = parsePersonIdentifier(data, offset, p);
			persons.add(db.addUnique(p));
		}

		return persons;
	}

	private int parsePersonIdentifier(String data, int offset, Person p)
	{
		// TODO: static patterns
		Pattern pattern = Pattern.compile("\\s*\"?(.*?)\"?\\s*<(.*?)>\\s*,?",
				Pattern.DOTALL);
		Matcher matcher = pattern.matcher(data);
		if (matcher.find(offset))
		{
			String name = decodeHeaderString(matcher.group(1));
			if (!name.isEmpty())
				p.addName(name);
			String address = matcher.group(2);
			if (!address.isEmpty())
				p.addEMailAddress(address);
			return matcher.end();
		}
		else
		{
			pattern = Pattern.compile("\\s*([\\S&&[^,]]*)\\s*,?",
					Pattern.DOTALL);
			matcher = pattern.matcher(data);
			if (matcher.find(offset))
			{
				String address = matcher.group(1);
				if (!address.isEmpty())
					p.addEMailAddress(address);
				return matcher.end();
			}
		}

		return data.length();  // malformed string, skip to end
	}

	private Date extractDate(String data)
	{
		Date d = new Date(0);
		if (data.isEmpty())
			return d;

		boolean success = false;
		for (String pattern : dateFormatPatterns)
		{
			DateFormat fmt = new SimpleDateFormat(pattern, Locale.US);
			try
			{
				d = fmt.parse(data);
				success = true;
				break;
			}
			catch (ParseException ex)
			{
			}
		}

		if (!success)
			System.err.printf("invalid date: %s\n", data);

		return d;
	}

	private String decodeHeaderString(String data)
	{
		String[] parts = data.split("\\s");
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < parts.length; i++)
		{
			if (i > 0)
				buf.append(' ');
			buf.append(decodeHeaderField(parts[i]));
		}
		return buf.toString();
	}

	private String decodeHeaderField(String data)
	{
		if (data.isEmpty() || (data.charAt(0) != '='))
			return data;
		String[] parts = data.split("\\?");
		if ((parts.length != 5) ||
			!parts[0].equals("=") || !parts[4].equals("="))
			return data;

		String characterEncoding = parts[1].toUpperCase();
		try
		{
			switch (Character.toLowerCase(parts[2].charAt(0)))
			{
			case 'q':
				return MessageEncoding.decodeQuotedPrintable(parts[3],
						characterEncoding, true);
			case 'b':
				return MessageEncoding.decodeBase64(parts[3],
						characterEncoding);
			}
		}
		catch (UnsupportedEncodingException ex)
		{
			// fall through
		}

		// -> unknown encoding
		return parts[3];
	}

	public Person getSender()
	{
		return sender;
	}

	public void setSender(Person p)
	{
		sender = p;
	}

	public Vector<Person> getRecipients()
	{
		return recipients;
	}

	public Vector<Person> getRecipientsCc()
	{
		return recipientsCc;
	}

	public Vector<Person> getRecipientsBcc()
	{
		return recipientsBcc;
	}

	public Date getDate()
	{
		return date;
	}

	public void setDate(Date d)
	{
		date = d;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String s)
	{
		subject = s;
	}

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	public void anonymize(PersonDatabase db)
	{
		if (body.isEmpty())
			return;

		StringBuffer anonymizedBody = new StringBuffer();
		int tokenStart = 0;
		boolean tokenIsWhitespace = Character.isWhitespace(body.charAt(0));
		for (int i = 1; i < body.length(); i++)
		{
			char c = body.charAt(i);
			boolean charIsWhitespace = Character.isWhitespace(c);

			if (tokenIsWhitespace ^ charIsWhitespace)
			{
				if (tokenIsWhitespace)
					anonymizedBody.append(body, tokenStart, i);
				else
				{
					String token = body.substring(tokenStart, i);

					Person identifiedByToken = null;
					if (sender.isIdentifiedBy(token))
						identifiedByToken = sender;
					if (identifiedByToken == null)
						identifiedByToken = getPersonIdentifiedBy(recipients,
								token);
					if (identifiedByToken == null)
						identifiedByToken = getPersonIdentifiedBy(recipientsCc,
								token);
					if (identifiedByToken == null)
						identifiedByToken = getPersonIdentifiedBy(recipientsBcc,
								token);
					if ((identifiedByToken == null) && (db != null))
						identifiedByToken = getPersonIdentifiedBy(db, token);

					if (identifiedByToken == null)
						anonymizedBody.append(token);
					else
						anonymizedBody.append(identifiedByToken.getId());
				}

				tokenStart = i;
				tokenIsWhitespace = charIsWhitespace;
			}
		}

		body = anonymizedBody.toString();
	}

	private Person getPersonIdentifiedBy(Iterable<Person> persons, String s)
	{
		Person personIdentifiedBy = null;
		Iterator<Person> it = persons.iterator();
		while ((personIdentifiedBy == null) && it.hasNext())
		{
			Person p = it.next();
			if (p.isIdentifiedBy(s))
				personIdentifiedBy = p;
		}
		return personIdentifiedBy;
	}

}

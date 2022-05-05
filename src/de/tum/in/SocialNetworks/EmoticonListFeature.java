package de.tum.in.SocialNetworks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import de.tum.in.Linguistics.SimpleTokenizer;


public class EmoticonListFeature implements MessageFeature {

	private String listName;
	private boolean perSentence;
	private HashSet<String> emoticons;

	public EmoticonListFeature(String listName, boolean perSentence)
	{
		this.listName = listName;
		this.perSentence = perSentence;
		emoticons = new HashSet<String>();
	}

	public EmoticonListFeature(File listFile, String listName,
			boolean perSentence)
	{
		this(listName, perSentence);
		addEmoticonList(listFile);		
	}

	public void addEmoticonList(File listFile)
	{
		try
		{
			BufferedReader reader =
				new BufferedReader(new FileReader(listFile));
			String line;
			while ((line = reader.readLine()) != null)
				emoticons.add(line);
		}
		catch (IOException ex)
		{
			System.err.printf("error reading emoticon list %s: %s\n",
					listFile.getName(), ex.getMessage());
		}
	}

	public String getDescription()
	{
		String desc = "relative frequency/score of ";
		if (perSentence)
			desc += "sentences containing ";
		desc += listName;
		return desc;
	}

	public double getValue(ProcessedMessage msg)
	{
		int numMatch = 0;
		int numMatchSent = 0;
		int num = 0;

		SimpleTokenizer tok = new SimpleTokenizer(msg.message.getBody());
		while (tok.hasNext())
		{
			String word = tok.next();

			if (emoticons.contains(word))
				numMatch++;

			if (perSentence)
			{
				// Doesn't match the sentence splitting rules in UntaggedCorpus,
				// but in this case a coarse approximation is good enough.
				if (word.equals("."))
				{
					if (numMatch > 0)
					{
						numMatchSent++;
						numMatch = 0;
					}
					num++;
				}
			}
			else
				num++;
		}
		if (perSentence)
		{
			if (numMatch > 0)
				numMatchSent++;
			num++;
			numMatch = numMatchSent;
		}

		if (num > 0)
			return (double) numMatch / num;
		return 0.0;
	}

}

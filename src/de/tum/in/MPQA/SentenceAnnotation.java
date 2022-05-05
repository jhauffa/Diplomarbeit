package de.tum.in.MPQA;

public class SentenceAnnotation implements Annotation {

	private int start;
	private int end;

	public SentenceAnnotation()
	{
		clear();
	}

	private void clear()
	{
		start = end = -1;
	}

	public int getStart()
	{
		return start;
	}
	
	public int getEnd()
	{
		return end;
	}

	public boolean isEmpty()
	{
		return ((start < 0) || (end < 0));
	}

	public boolean parse(CorpusParticle owner, String line)
	{
		clear();

		String[] token = line.split("\\t");
		if (token.length < 4)
			return false;
		if (!token[2].equals("string") || !token[3].equals("GATE_sentence"))
			return false;

		String[] value = token[1].split(",");
		if (value.length < 2)
			return false;
		try
		{
			start = Integer.valueOf(value[0]);
			end = Integer.valueOf(value[1]);
		}
		catch (NumberFormatException e)
		{
			return false;
		}

		return true;
	}

}

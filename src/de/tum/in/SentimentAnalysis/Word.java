package de.tum.in.SentimentAnalysis;

import java.util.LinkedList;

public class Word {

	private class Annotation
	{
		public String key;
		public int value;

		public Annotation(String key, int value)
		{
			this.key = key;
			this.value = value;
		}

		public Annotation(Annotation other)
		{
			this.key = other.key;
			this.value = other.value;
		}
	}

	private LinkedList<Annotation> annotations;
	private String word;
	private Sentiment trueSentiment;
	private Sentiment sentiment;
	private boolean startsBlock;
	private int label;
	private int trueLabel;

	public Word(String word, Sentiment trueSentiment, boolean startsBlock)
	{
		this.word = word;
		this.startsBlock = startsBlock;

		setTrueSentiment(trueSentiment);
		setSentiment(new Sentiment(trueSentiment));

		annotations = new LinkedList<Annotation>();
	}

	public Word(Word other)
	{
		this.annotations = new LinkedList<Annotation>();
		for (Annotation a : other.annotations)
			this.annotations.add(new Annotation(a));
		this.word = new String(other.word);
		this.trueSentiment = new Sentiment(other.trueSentiment);
		this.sentiment = new Sentiment(other.sentiment);
		this.startsBlock = other.startsBlock;
		this.label = other.label;
		this.trueLabel = other.trueLabel;
	}

	public String getWord()
	{
		return word;
	}

	public int getAnnotation(String key)
	{
		for (Annotation a : annotations)
			if (a.key.equals(key))
				return a.value;
		return -1;
	}

	public void setAnnotation(String key, int value)
	{
		for (Annotation a : annotations)
			if (a.key.equals(key))
			{
				a.value = value;
				return;
			}
		annotations.add(new Annotation(key, value));
	}

	public int getLabel()
	{
		return label;
	}

	public void setLabel(int label)
	{
		this.label = label;
		// cannot convert to sentiment, label might be encoded
	}

	public int getTrueLabel()
	{
		return trueLabel;
	}

	public void setTrueLabel(int label)
	{
		this.trueLabel = label;
		// cannot convert to sentiment, label might be encoded
	}

	public Sentiment getSentiment()
	{
		return sentiment;
	}

	public void setSentiment(Sentiment sentiment)
	{
		this.sentiment = sentiment;
		this.label = Sentiment.polarityToOrdinal(this.sentiment.getPolarity());
	}

	public Sentiment getTrueSentiment()
	{
		return trueSentiment;
	}

	private void setTrueSentiment(Sentiment sentiment)
	{
		this.trueSentiment = sentiment;
		this.trueLabel = Sentiment.polarityToOrdinal(
				this.trueSentiment.getPolarity());
	}

	public boolean getStartsBlock()
	{
		return startsBlock;
	}

}

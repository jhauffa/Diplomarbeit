package de.tum.in.SentimentAnalysis;

public class Sentiment {

	public enum Polarity {NEUTRAL, POSITIVE, NEGATIVE, BOTH};
	public final static int maxNumClasses = 4;
	
	private Polarity polarity;
	private double intensity;
	private double confidence;

	public Sentiment()
	{
		polarity = Polarity.NEUTRAL;
		intensity = 0.0;
		confidence = 1.0;
	}

	public Sentiment(Sentiment other)
	{
		clone(other);
	}

	public Sentiment(Polarity direction, double intensity, double confidence)
	{
		setPolarity(direction);
		setIntensity(intensity);
		setConfidence(confidence);
	}

	public void clone(Sentiment other)
	{
		polarity = other.polarity;
		intensity = other.intensity;
		confidence = other.confidence;
	}

	public void setPolarity(Polarity direction)
	{
		this.polarity = direction;
	}

	public Polarity getPolarity()
	{
		return polarity;
	}
	
	public void setIntensity(double intensity)
	{
		if ((intensity < 0.0) || (intensity > 1.0))
			throw new IllegalArgumentException();
		this.intensity = intensity;
	}

	public double getIntensity()
	{
		// intensity of subjectivity
		return intensity;
	}

	public void setConfidence(double confidence)
	{
		if ((confidence < 0.0) || (confidence > 1.0))
			throw new IllegalArgumentException();
		this.confidence = confidence;
	}

	public double getConfidence()
	{
		// estimate of P(annotation correct) = 0.5 + (confidence / 2)
		return confidence;
	}


	public static int mapPolarityClass(int c, int numClasses)
	{
		if (c >= numClasses)
		{
			switch (numClasses)
			{
			case 2:  // neutral/polar
				c = 1;  // anything not neutral is polar
				break;
			case 3:  // neutral/positive/negative
				c = 0;  // both -> neutral
				break;
			default:
				System.err.printf("c = %d, numClasses = %d\n", c, numClasses);
				throw new AssertionError();
			}
		}
		return c;
	}

	public static int polarityToOrdinal(Polarity polarity)
	{
		switch (polarity)
		{
		case NEUTRAL:
			return 0;
		case POSITIVE:
			return 1;
		case NEGATIVE:
			return 2;
		case BOTH:
			return 3;
		}
		throw new AssertionError();
	}

	public static Polarity ordinalToPolarity(int ordinal)
	{
		switch (ordinal)
		{
		case 0:
			return Polarity.NEUTRAL;
		case 1:
			return Polarity.POSITIVE;
		case 2:
			return Polarity.NEGATIVE;
		case 3:
			return Polarity.BOTH;
		}
		throw new AssertionError();
	}

}

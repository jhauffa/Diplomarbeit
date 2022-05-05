package de.tum.in.SocialNetworks;

public class InvertedFeature implements MessageFeature {

	private MessageFeature f;

	public InvertedFeature(MessageFeature f)
	{
		this.f = f;
	}

	public String getDescription()
	{
		return f.getDescription() + " (inv.)";
	}

	public double getValue(ProcessedMessage msg)
	{
		return 1.0 - f.getValue(msg);
	}

}

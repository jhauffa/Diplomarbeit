package de.tum.in.SocialNetworks;

public class MessageFrequencyFeature implements RelationshipFeature {

	private int numMessages;

	public MessageFrequencyFeature(SocialNetwork net)
	{
		numMessages = 0;
		for (Relationship r : net)
			numMessages += r.messages.size();
	}

	public String getDescription()
	{
		return "percentage of overall number of messages";
	}

	public double getValue(Relationship r)
	{
		return (double) r.messages.size() / numMessages;
	}

}

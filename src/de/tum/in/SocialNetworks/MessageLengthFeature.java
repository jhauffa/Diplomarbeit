package de.tum.in.SocialNetworks;

public class MessageLengthFeature implements MessageFeature {

	private int maxMessageLength;

	public MessageLengthFeature(SocialNetwork net)
	{
		maxMessageLength = 1;
		for (Relationship r : net)
		{
			for (ProcessedMessage m : r.messages)
			{
				int length = m.message.getBody().length();
				if (length > maxMessageLength)
					maxMessageLength = length;
			}
		}
	}

	public String getDescription()
	{
		return "length of message relative to longest message";
	}

	public double getValue(ProcessedMessage msg)
	{
		return (double) msg.message.getBody().length() / maxMessageLength;
	}

}

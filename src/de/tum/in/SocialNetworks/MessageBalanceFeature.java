package de.tum.in.SocialNetworks;

public class MessageBalanceFeature implements RelationshipFeature {

	public String getDescription()
	{
		return "balance of sent and received messages";
	}

	public double getValue(Relationship r)
	{
		int numSent = 0;
		for (ProcessedMessage msg : r.messages)
			if (msg.message.getSender().getId() == r.sourceId)
				numSent++;
		return 1.0 -
			(Math.abs(((double) numSent / r.messages.size()) - 0.5) * 2.0);
	}

}

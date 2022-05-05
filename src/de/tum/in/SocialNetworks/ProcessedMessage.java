package de.tum.in.SocialNetworks;

import de.tum.in.EMail.Message;
import de.tum.in.SentimentAnalysis.Corpus;

public class ProcessedMessage {

	public Message message;
	public Corpus annotatedBody;

	public ProcessedMessage(Message message, Corpus annotatedBody)
	{
		this.message = message;
		this.annotatedBody = annotatedBody;
	}

	public ProcessedMessage(Message message)
	{
		this.message = message;
		this.annotatedBody = null;
	}

}

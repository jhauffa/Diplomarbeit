package de.tum.in.SocialNetworks;

import java.util.ArrayList;

public class Relationship {

	public static final int numAttributes = 2;
	public static final String[] attributeNames = {"intensity", "valence"};
	public static final double[] attributeDbMin = {0.0, -1.0};
	public static final double[] attributeDbMax = {1.0,  1.0};
	public static final double[] attributeMin = {0.0, 0.0};
	public static final double[] attributeMax = {1.0, 1.0};

	public String sourceId;
	public String destId;

	public double[] trueAttributes;

	public boolean excludeFromEvaluation;

	public ArrayList<ProcessedMessage> messages;

	public Relationship(String sourceId, String destId)
	{
		this.sourceId = sourceId;
		this.destId = destId;

		trueAttributes = new double[numAttributes];
		excludeFromEvaluation = false;

		messages = new ArrayList<ProcessedMessage>();
	}

}

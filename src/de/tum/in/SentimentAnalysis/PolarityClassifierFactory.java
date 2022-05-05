package de.tum.in.SentimentAnalysis;

public class PolarityClassifierFactory {

	public static Classifier create(String type, FeatureGenerator featureGen,
			boolean performFeatureInduction)
	{
		if (type.equals("HMM"))
			return new PolarityClassifierHMM();
		else if (type.equals("CRF"))
			return new PolarityClassifierCRF(featureGen,
					performFeatureInduction);
		else if (type.equals("CRFSarawagi"))
			return new PolarityClassifierCRFSarawagi(featureGen);
		else
			throw new IllegalArgumentException("unknown model " + type);
	}

	public static String getModelNames()
	{
		return "HMM, CRF, CRFSarawagi";
	}

	public static String getDefaultModelName()
	{
		return "HMM";
	}

}

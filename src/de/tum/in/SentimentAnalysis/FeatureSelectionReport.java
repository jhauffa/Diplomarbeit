package de.tum.in.SentimentAnalysis;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class FeatureSelectionReport implements Report {

	private static final double eps = 0.00001;

	private Classifier model;
	private FeatureGenerator featureGen;
	private int numStates;
	private boolean printAnnotationDetails;

	public FeatureSelectionReport(Classifier model, FeatureGenerator featureGen,
			int numStates, boolean printAnnotationDetails)
	{
		this.model = model;
		this.featureGen = featureGen;
		this.numStates = numStates;
		this.printAnnotationDetails = printAnnotationDetails;
	}

	public void print(PrintStream out)
	{
		// get feature distribution before selection by class, indicated by the
		// prefix of the feature's name 
		String[] origFeatureNames = featureGen.getFeatureNames();
		HashMap<String, Integer> origFeatureDist =
			new HashMap<String, Integer>();
		for (String name : origFeatureNames)
			updateFeatureDist(origFeatureDist, getFeaturePrefix(name));

		out.println("overall feature distribution:");
		for (Map.Entry<String, Integer> e : origFeatureDist.entrySet())
			out.printf("%-20s\t%.2f%%\n", e.getKey(),
					((double) e.getValue() / origFeatureNames.length) * 100.0);
		out.println();

		// get feature distribution and feature weight sums for each transition
		Vector<String> modelFeatureNames = new Vector<String>();
		model.getFeatureNames(modelFeatureNames);

		for (int i = 0; i < numStates; i++)
			for (int j = 0; j < numStates; j++)
			{
				HashMap<String, Integer> observedFeatureDist =
					new HashMap<String, Integer>();
				HashMap<String, Integer> nonZeroFeatureDist =
					new HashMap<String, Integer>();
				HashMap<String, Double> featureWeightSum =
					new HashMap<String, Double>();
				double globalWeightSum = 0.0;

				Vector<Integer> indices = new Vector<Integer>();
				Vector<Double> weights = new Vector<Double>();
				model.getFeatureWeights(i, j, indices, weights);

				for (int k = 0; k < indices.size(); k++)
				{
					int idx = indices.get(k);
					double weight = Math.abs(weights.get(k));
					globalWeightSum += weight;						

					String name = modelFeatureNames.get(idx);
					String[] parts = name.split("_&_");
					for (String part : parts)
					{
						String prefix = getFeaturePrefix(part);

						updateFeatureDist(observedFeatureDist, prefix);
						if (Math.abs(weight) > eps)
							updateFeatureDist(nonZeroFeatureDist, prefix);
						Double sum = featureWeightSum.get(prefix);
						if (sum == null)
							featureWeightSum.put(prefix, weight);
						else
							featureWeightSum.put(prefix, sum + weight);
					}
				}

				out.printf("state transition %d -> %d:\n", i + 1, j + 1);
				out.println("observation/survival rate:");
				for (String key : observedFeatureDist.keySet())
				{
					Integer obsValue = observedFeatureDist.get(key);
					if (obsValue == null)
						obsValue = 0;
					Integer nonZeroValue = nonZeroFeatureDist.get(key);
					if (nonZeroValue == null)
						nonZeroValue = 0;
					Integer origValue = origFeatureDist.get(key);
					if (origValue == null)
					{
						System.err.println("key " + key + " not in original " +
								"feature distribution!");
						origValue = 0;
					}
					out.printf("%-20s\t%.2f%%\t%.2f%%\n", key,
							((double) obsValue / origValue) * 100.0,
							((double) nonZeroValue / obsValue) * 100.0);
				}
				out.println("feature weight ratio (whole class/avg. per " +
						"feature):");
				for (Map.Entry<String, Double> e : featureWeightSum.entrySet())
				{
					double perClassWeightSum = e.getValue();

					double perFeatureWeightSum = perClassWeightSum;
					Integer numFeatures = observedFeatureDist.get(e.getKey());
					if (numFeatures != null)
						perFeatureWeightSum /= numFeatures;
					else
						perFeatureWeightSum = 0.0;

					out.printf("%-20s\t%.2f%%\t%.5f%%\n", e.getKey(),
							(perClassWeightSum / globalWeightSum) * 100.0,
							(perFeatureWeightSum / globalWeightSum) * 100.0);
				}
				out.println();
			}
	}

	private String getFeaturePrefix(String name)
	{
		int sepIdx = name.indexOf('_');
		if (sepIdx < 0)
			return "other";
		String prefix = name.substring(0, sepIdx);
		if (printAnnotationDetails && prefix.equals("ANN"))
			prefix = name.substring(0, name.indexOf('_', sepIdx + 1));
		return prefix;
	}

	private void updateFeatureDist(HashMap<String, Integer> dist, String prefix)
	{
		Integer count = dist.get(prefix);
		if (count == null)
			dist.put(prefix, 1);
		else
			dist.put(prefix, count + 1);		
	}

}

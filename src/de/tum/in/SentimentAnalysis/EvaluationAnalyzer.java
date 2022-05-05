package de.tum.in.SentimentAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EvaluationAnalyzer {

	private static class FeatureWeightComparator
			implements Comparator<Map.Entry<String, Double>>
	{
		public int compare(Map.Entry<String, Double> f1,
				Map.Entry<String, Double> f2)
		{
			return Double.compare(f2.getValue(), f1.getValue());
		}
	}

	private static double getUnigramFeatureWeight(String name, String weightStr)
	{
		// ignore position shifted features
		if (name.indexOf('_') >= 0)
			return 0.0;

		double weight = 0.0;
		try
		{
			weight = Double.parseDouble(weightStr);
		}
		catch (NumberFormatException ex)
		{
		}
		return weight;
	}

	private static List<Map.Entry<String, Double>>[] processReport(File f)
			throws IOException
	{
		ArrayList<HashMap<String, Double>> unigramWeights =
			new ArrayList<HashMap<String, Double>>();

		// parse report; read unigram feature weights and count CRF models
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line;
		int numModels = 0;
		int numFeatures = 0;
		int transitionIdx = -1;
		HashMap<String, Double> curWeightMap = null;
		while ((line = in.readLine()) != null)
		{
			if (line.startsWith(": "))
				numFeatures++;

			if (line.startsWith(": NGR_1F0_"))
			{
				String[] parts = line.substring(10).split(" = ");
				if (parts.length == 2)
				{
					double weight = getUnigramFeatureWeight(parts[0], parts[1]);
					if (weight != 0.0)
					{
						Double weightSum = curWeightMap.get(parts[0]);
						if (weightSum != null)
							weightSum += weight;
						else
							weightSum = weight;
						curWeightMap.put(parts[0], weightSum);
					}
				}
			}
			else if (line.startsWith("*** CRF WEIGHTS"))
			{
				numModels++;
				transitionIdx = -1;
			}
			else if (line.startsWith("WEIGHTS NAME"))
			{
				transitionIdx++;
				if (transitionIdx >= unigramWeights.size())
				{
					curWeightMap = new HashMap<String, Double>();
					unigramWeights.add(curWeightMap);
				}
			}
		}
		in.close();

		numFeatures /= numModels;
		System.out.printf("avg. num. features = %d\n", numFeatures);  // HACK

		// sort by descending mean feature weight
		List<Map.Entry<String, Double>>[] sortedUnigramWeights =
			new List[unigramWeights.size()];
		for (int i = 0; i < unigramWeights.size(); i++)
		{
			HashMap<String, Double> map = unigramWeights.get(i);

			// compute mean
			if (numModels > 0)
				for (Map.Entry<String, Double> e : map.entrySet())
					e.setValue(e.getValue() / numModels);

			ArrayList<Map.Entry<String, Double>> sortedTransWeights =
				new ArrayList<Map.Entry<String, Double>>(map.entrySet());
			Collections.sort(sortedTransWeights, new FeatureWeightComparator());
			sortedUnigramWeights[i] = sortedTransWeights;
		}

		return sortedUnigramWeights;
	}

	private static void printResults(
			List<Map.Entry<String, Double>>[] featureWeights)
	{
		for (int i = 0; i < featureWeights.length; i++)
			for (Map.Entry<String, Double> e : featureWeights[i])
				System.out.printf("%d;%s;%f\n", i, e.getKey(), e.getValue());
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: EvaluationAnalyzer file");
			return;
		}

		List<Map.Entry<String, Double>>[] featureWeights = null;
		try
		{
			File f = new File(args[0]);
			featureWeights = processReport(f);
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}

		if (featureWeights != null)
			printResults(featureWeights);
	}

}

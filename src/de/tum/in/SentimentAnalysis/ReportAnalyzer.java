package de.tum.in.SentimentAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.text.ParseException;

public class ReportAnalyzer {

	private static class FeatureGroupStats {
		public double sumObservationRate;
		public int numObservationRate;
		public double sumSurvivalRate;
		public int numSurvivalRate;
		public double sumWeightRatio;
		public int numWeightRatio;
		public double sumPerFeatureWeight;
		public int numPerFeatureWeight;
	}

	private static final int numTransitions = 16;
	private static HashMap<String, FeatureGroupStats[]> transData;
	private static HashMap<String, Double> distrData;
	private static int numFiles;

	private static double parseValue(String s)
	{
		double v = 0.0;
		String sn = s.substring(0, s.length() - 1);  // remove percent sign
		try
		{
			// parse double according to current locale
			v = NumberFormat.getInstance().parse(sn).doubleValue();
		}
		catch (ParseException ex)
		{
			// System.err.printf("parse error: %s\n", sn);
		}
		return v;
	}

	private static void addTransData(int transitionIdx, int groupIdx,
			String[] rawData)
	{
		FeatureGroupStats[] stats = transData.get(rawData[0]);
		if (stats == null)
		{
			stats = new FeatureGroupStats[numTransitions];
			transData.put(rawData[0], stats);
		}
		if (stats[transitionIdx] == null)
			stats[transitionIdx] = new FeatureGroupStats();

		if (groupIdx == 0)
		{
			stats[transitionIdx].sumObservationRate += parseValue(rawData[1]);
			stats[transitionIdx].numObservationRate++;
			stats[transitionIdx].sumSurvivalRate += parseValue(rawData[2]);
			stats[transitionIdx].numSurvivalRate++;
		}
		else
		{
			stats[transitionIdx].sumWeightRatio += parseValue(rawData[1]);
			stats[transitionIdx].numWeightRatio++;
			stats[transitionIdx].sumPerFeatureWeight += parseValue(rawData[2]);
			stats[transitionIdx].numPerFeatureWeight++;
		}
	}

	private static void addDistrData(String[] rawData)
	{
		double cv = parseValue(rawData[1]);
		Double v = distrData.get(rawData[0]);
		if (v == null)
			distrData.put(rawData[0], cv);
		else
			distrData.put(rawData[0], v + cv);
	}

	private static void printSingleResult(String groupName, int transitionIdx,
			FeatureGroupStats stats)
	{
		// compute arithmetic mean
		if (stats.numObservationRate > 0)
			stats.sumObservationRate /= stats.numObservationRate;
		else
			stats.sumObservationRate = 0.0;
		if (stats.numSurvivalRate > 0)
			stats.sumSurvivalRate /= stats.numSurvivalRate;
		else
			stats.sumSurvivalRate = 0.0;
		if (stats.numWeightRatio > 0)
			stats.sumWeightRatio /= stats.numWeightRatio;
		else
			stats.sumWeightRatio = 0.0;
		if (stats.numPerFeatureWeight > 0)
			stats.sumPerFeatureWeight /= stats.numPerFeatureWeight;
		else
			stats.sumPerFeatureWeight = 0.0;

		// print
		System.out.printf("%s;%d;%f;%f;%f;%f\n", groupName, transitionIdx,
				stats.sumObservationRate, stats.sumSurvivalRate,
				stats.sumWeightRatio, stats.sumPerFeatureWeight);
	}

	private static void printResults()
	{
		// feature distribution data
		for (Map.Entry<String, Double> e : distrData.entrySet())
		{
			Double v = e.getValue();
			if (v != null)
				System.out.printf("%s;-1;;;%f;\n", e.getKey(), v / numFiles);
		}

		// transition data
		for (Map.Entry<String, FeatureGroupStats[]> e : transData.entrySet())
		{
			FeatureGroupStats[] stats = e.getValue();
			if (stats != null)
			{
				for (int i = 0; i < numTransitions; i++)
				{
					if (stats[i] != null)
						printSingleResult(e.getKey(), i, stats[i]);
				}
			}
		}
	}

	private static void processReport(File f) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(f));
		String line;
		int transitionIdx = -1;
		int groupIdx = 0;
		while ((line = in.readLine()) != null)
		{
			if (line.startsWith("state transition") ||
				line.startsWith("overall feature"))
			{
				transitionIdx++;
				groupIdx = 0;
			}
			else if (line.startsWith("feature weight"))
			{
				groupIdx++;
			}
			else if (transitionIdx == 0)
			{
				// parse overall distribution
				String[] parts = line.split("\\s+");
				if (parts.length == 2)
					addDistrData(parts);
			}
			else if (transitionIdx > 0)
			{
				// parse per-transition distribution
				String[] parts = line.split("\\s+");
				if (parts.length == 3)
					addTransData(transitionIdx - 1, groupIdx, parts);
			}
		}
		numFiles++;
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: ReportAnalyzer basename");
			return;
		}

		transData = new HashMap<String, FeatureGroupStats[]>();
		distrData = new HashMap<String, Double>();
		numFiles = 0;

		int repIdx = 1;
		int foldIdx = 1;
		boolean filesRemaining = true;
		while (filesRemaining)
		{
			try
			{
				String fileName = args[0] + "-"+repIdx+"-"+foldIdx + ".log";
				// System.err.printf("trying to read \"%s\"\n", fileName);
				File f = new File(fileName);
				processReport(f);
				foldIdx++;
			}
			catch (FileNotFoundException ex)
			{
				if (foldIdx > 1)
				{
					repIdx++;
					foldIdx = 1;
				}
				else
					filesRemaining = false;
			}
			catch (IOException ex)
			{
				System.err.printf("read error: %s\n", ex.getMessage());
				return;
			}
		}

		printResults();
	}

}

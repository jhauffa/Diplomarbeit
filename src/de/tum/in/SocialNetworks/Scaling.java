package de.tum.in.SocialNetworks;

import java.io.PrintStream;

import de.tum.in.Math.RowDataSet;


public class Scaling {

	private static final double defaultSdf = 3.0;
	public static final int numSamples = /*3*/ 2;

	public enum Method { SEMI, NORMAL, MINMAX };

	private Method method;
	private double sdf;
	private SocialNetwork net;

	public Scaling(String method, SocialNetwork net)
	{
		this(parseMethodString(method), net);
	}

	public Scaling(Method method, SocialNetwork net)
	{
		this(method, defaultSdf, net);
	}

	public Scaling(String method, double sdf, SocialNetwork net)
	{
		this(parseMethodString(method), sdf, net);
	}

	public Scaling(Method method, double sdf, SocialNetwork net)
	{
		this.method = method;
		this.sdf = sdf;
		this.net = net;
	}

	private static Method parseMethodString(String s)
	{
		if (s.equals("semi"))
			return Method.SEMI;
		else if (s.equals("normal"))
			return Method.NORMAL;
		else
			return Method.MINMAX;		
	}

	public void printConfiguration(PrintStream log)
	{
		switch (method)
		{
		case SEMI:
			log.println("semi-supervised (input/output range computed " + 
					"from small sample of from relationships with extreme" +
					"ratings)");
			break;
		case NORMAL:
			log.printf("assuming normal distribution, input range = mean +/-" +
					"%f*SD\n", sdf);
			break;
		default:
			log.println("input range = minimum/maximum rating");
			break;
		}
		log.flush();
	}

	public void scaleRatings(RowDataSet ratings, int attrIdx)
	{
		if (method == Method.SEMI)
		{
			// find relationship(s) with minimal/maximal value of attribute

			Relationship[] minRel = new Relationship[numSamples];
			int[] minRelIdx = new int[numSamples];
			double[] minRelAttr = new double[numSamples];
			Relationship[] maxRel = new Relationship[numSamples];
			int[] maxRelIdx = new int[numSamples];
			double[] maxRelAttr = new double[numSamples];
			for (int i = 0; i < numSamples; i++)
			{
				minRelIdx[i] = -1;
				minRelAttr[i] = Double.MAX_VALUE;
				maxRelIdx[i] = -1;
				maxRelAttr[i] = -Double.MAX_VALUE;
			}

			int idx = 0;
			for (Relationship r : net)
			{
				for (int i = 0; i < numSamples; i++)
					if (r.trueAttributes[attrIdx] < minRelAttr[i])
					{
						minRelAttr[i] = r.trueAttributes[attrIdx];
						minRelIdx[i] = idx;
						minRel[i] = r;
						break;
					}
				for (int i = 0; i < numSamples; i++)
					if (r.trueAttributes[attrIdx] > maxRelAttr[i])
					{
						maxRelAttr[i] = r.trueAttributes[attrIdx];
						maxRelIdx[i] = idx;
						maxRel[i] = r;
						break;
					}
				idx++;
			}

			double minIn = 0.0;
			double minOut = 0.0;
			double maxIn = 0.0;
			double maxOut = 0.0;
			for (int i = 0; i < numSamples; i++)
			{
				minRel[i].excludeFromEvaluation = true;
				maxRel[i].excludeFromEvaluation = true;
				minIn += ratings.get(0, minRelIdx[i]);
				maxIn += ratings.get(0, maxRelIdx[i]);
				minOut += minRelAttr[i];
				maxOut += maxRelAttr[i];
			}
			minIn /= numSamples;
			maxIn /= numSamples;
			minOut /= numSamples;
			maxOut /= numSamples;

			performScaling(ratings, minIn, minOut, maxIn, maxOut);
		}
		else if (method == Method.NORMAL)
		{
/*
			// Scaling to 0..1 followed by scaling under the assumption of a
			// normal distribution with mean 0.5 yields better RMSE when
			// predicting the polarity of the tiny development set. This is
			// probably an artifact of the data, which has little variance
			// around a mean of approximately 0.5. 
			double minIn = Double.MAX_VALUE;
			double maxIn = -Double.MAX_VALUE;
			for (int j = 0; j < ratings.getNumPoints(); j++)
			{
				double v = ratings.get(0, j);
				if (v < minIn)
					minIn = v;
				if (v > maxIn)
					maxIn = v;
			}
			performScaling(ratings, minIn, 0.0, maxIn, 1.0);
			double sd = Math.sqrt(ratings.variance(0, 0.5));
			performScaling(ratings,
					0.5 - (sdf * sd), 0.0, 0.5 + (sdf * sd), 1.0);
*/

			double mean = ratings.mean(0);
			double sd = Math.sqrt(ratings.variance(0, mean));
			performScaling(ratings,
					mean - (sdf * sd), 0.0, mean + (sdf * sd), 1.0);
		}
		else
		{
			double minIn = Double.MAX_VALUE;
			double maxIn = -Double.MAX_VALUE;
			for (int j = 0; j < ratings.getNumPoints(); j++)
			{
				double v = ratings.get(0, j);
				if (v < minIn)
					minIn = v;
				if (v > maxIn)
					maxIn = v;
			}

			performScaling(ratings, minIn, 0.0, maxIn, 1.0);
		}
	}


	private static void performScaling(RowDataSet ratings,
			double minIn, double minOut, double maxIn, double maxOut)
	{
		assert (ratings.getNumDimensions() == 1);

		// System.err.printf("min: %f -> %f; max: %f -> %f\n", minIn, minOut,
		//		maxIn, maxOut);
		// sanity checks
		if (Math.abs(maxIn - minIn) < 0.01)
//			throw new RuntimeException("d_in < 0.01");
			System.err.println("Warning: d_in < 0.01");
		if (Math.abs(maxOut - minOut) < 0.01)
//			throw new RuntimeException("d_out < 0.01");
			System.err.println("Warning: d_out < 0.01");

		for (int i = 0; i < ratings.getNumPoints(); i++)
		{
			double v = (ratings.get(0, i) - minIn) / (maxIn - minIn);
			v = minOut + (v * (maxOut - minOut));  // no-op for interval 0..1
			v = Math.min(v, 1.0);
			v = Math.max(v, 0.0);
			ratings.set(0, i, v);
		}
	}
	
}

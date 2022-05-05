package de.tum.in.SocialNetworks;

import java.io.PrintStream;

import de.tum.in.Math.RowDataSet;
import de.tum.in.Math.Matrix;


public class Evaluator {

	private static final int maxQuant = 15;

	private PrintStream out;
	private SocialNetwork net;

	public Evaluator(PrintStream out, SocialNetwork net)
	{
		this.out = out;
		this.net = net;
	}

	public void printRMSE(RowDataSet ratings, RowDataSet trueRatings)
	{
		int n = ratings.getNumPoints();

		double rmse = 0.0;
		int[] quantErrCount = new int[maxQuant-1]; 

		for (int i = 0; i < n; i++)
		{
			Relationship r = net.get(i);
			if (r.excludeFromEvaluation)
				continue;

			rmse += Math.pow(trueRatings.get(0, i) - ratings.get(0, i), 2.0);
			for (int j = 2; j <= maxQuant; j++)
			{
				double diff = Math.floor(trueRatings.get(0, i) * j) -
						(ratings.get(0, i) * j);
				if ((diff < 0.0) || (diff >= 1.0))
					quantErrCount[j-2]++;
			}
		}

		rmse = Math.sqrt(rmse / n);
		System.err.printf("RMSE = %f\n", rmse);
		out.printf("RMSE = %f\n", rmse);
		out.println();

		out.println("accuracy with n classes:");
		for (int j = 2; j <= maxQuant; j++)
		{
			double accuracy = 1.0 - ((double) quantErrCount[j-2] / n);
			double dr = accuracy - (1.0 / j);
			out.printf("%d\t%.2f%%\t%.2f\n", j, accuracy * 100.0, dr * 100.0);
		}
		out.println();

		out.flush();
	}

	public void printCorrelationOfRatings(RowDataSet ratings,
			RowDataSet trueRatings, boolean printVariance)
	{
		RowDataSet d = new RowDataSet(2, ratings.getNumPoints());
		for (int i = 0; i < ratings.getNumPoints(); i++)
		{
			d.set(0, i, ratings.get(0, i));
			d.set(1, i, trueRatings.get(0, i));
		}

		if (printVariance)
		{
			double meanOut = d.mean(0);
			double varOut = d.variance(0);
			double varIn = d.variance(1);
			out.printf("mean out = %f, variance in = %f, variance out = %f\n",
					meanOut, varIn, varOut);
		}

		double pearsonCoeff = d.correlation(0, 1);
		out.printf("Pearson's correlation coefficient = %f\n", pearsonCoeff);

		d.transformToRanks();
		double spearmanCoeff = d.correlation(0, 1);
		out.printf("Spearman's rank correlation coefficient = %f\n",
				spearmanCoeff);

		out.println();
		out.flush();
	}

	public void printCorrelationMatrix(RowDataSet data)
	{
		RowDataSet d = new RowDataSet(data.duplicate());
		printCorrelationMatrixInternal(d);
	}

	public void printCorrelationMatrix(RowDataSet[] data)
	{
		int numPoints = data[0].getNumPoints();
		RowDataSet d = new RowDataSet(data.length, numPoints);
		for (int i = 0; i < data.length; i++)
		{
			assert(data[i].getNumPoints() == numPoints);
			for (int j = 0; j < numPoints; j++)
				d.set(i, j, data[i].get(0, j));
		}
		printCorrelationMatrixInternal(d);
	}

	private void printCorrelationMatrixInternal(RowDataSet data)
	{
		out.println("Pearson's correlation coefficient:");
		Matrix c = data.correlationMatrix();
		c.print(out);
		data.transformToRanks();
		c = data.correlationMatrix();
		out.println("Spearman's rank correlation coefficient:");
		c.print(out);
	}

	public void printCorrelationOfFeaturesRatings(RowDataSet featureVectors,
			RowDataSet ratings)
	{
		assert(ratings.getNumPoints() == featureVectors.getNumPoints());

		for (int i = 0; i < featureVectors.getNumDimensions(); i++)
		{
			RowDataSet d = new RowDataSet(2, ratings.getNumPoints());
			for (int j = 0; j < ratings.getNumPoints(); j++)
			{
				d.set(0, j, ratings.get(0, j));
				d.set(1, j, featureVectors.get(i, j));
			}

			double pearsonCoeff = d.correlation(0, 1);
			out.printf("Pearson's correlation coefficient = %f\n",
					pearsonCoeff);

			d.transformToRanks();
			double spearmanCoeff = d.correlation(0, 1);
			out.printf("Spearman's rank correlation coefficient = %f\n",
					spearmanCoeff);	
		}
	}

	public void printVectors(RowDataSet vectors)
	{
		for (int i = 0; i < vectors.getNumDimensions(); i++)
		{
			out.printf("%d;", i + 1);

			for (int j = 0; j < vectors.getNumPoints(); j++)
				out.printf("%f;", vectors.get(i, j));
			out.println();
		}
		out.println();
		out.flush();
	}

	public void printNetworkStats()
	{
		int n = 0;
		int sumMails = 0;
		for (Relationship r : net)
		{
			sumMails += r.messages.size();
			n++;
		}

		out.printf("%d relationships, %d messages, %.2f messages per " +
				"relationship\n", n, sumMails, (double) sumMails / n);
		out.println();
		out.flush();
	}

}

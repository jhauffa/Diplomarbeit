package de.tum.in.SocialNetworks;

import de.tum.in.Math.Matrix;
import de.tum.in.Math.Vector;
import de.tum.in.Math.RowDataSet;

public class PrincipalComponentAnalysis {

	private RowDataSet adjData;
	private Matrix eigenVectors;
	private Vector eigenValues;

	public PrincipalComponentAnalysis(RowDataSet data)
	{
		// subtract mean from each data point
		adjData = new RowDataSet(data.duplicate());
		adjData.transformMean(0.0);

		Matrix cov = adjData.covarianceMatrix();
		eigenValues = new Vector(data.getNumDimensions());
		eigenVectors = cov.eigen(eigenValues);
	}

	public Matrix getEigenVectors()
	{
		return eigenVectors;
	}

	public Vector getEigenValues()
	{
		return eigenValues;
	}

	public double getRelVariance(int first, int last)
	{
		return getRelVariance(first, last, 0, eigenValues.size() - 1);
	}

	public double getRelVariance(int firstPart, int lastPart,
			int firstWhole, int lastWhole)
	{
		double partSum = 0.0;
		double sum = 0.0;
		for (int i = 0; i < eigenValues.size(); i++)
		{
			double v = eigenValues.get(i);
			if ((i >= firstPart) && (i <= lastPart))
				partSum += v;
			if ((i >= firstWhole) && (i <= lastWhole))
				sum += v;
		}
		return partSum / sum;
	}

	public RowDataSet transform(int dim)
	{
		int n = adjData.getNumDimensions();
		Matrix features = new Matrix(dim, n);

		// copy and transpose first dim eigenvectors
		for (int i = 0; i < dim; i++)
			for (int j = 0; j < n; j++)
				features.set(i, j, eigenVectors.get(j, i));
		return new RowDataSet(Matrix.mult(features, adjData));
	}

}

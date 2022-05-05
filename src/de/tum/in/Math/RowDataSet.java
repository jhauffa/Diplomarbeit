package de.tum.in.Math;

public class RowDataSet extends Matrix {

	public RowDataSet(int numDimensions, int numPoints)
	{
		super(numDimensions, numPoints);
	}

	public RowDataSet(Matrix data)
	{
		super(data);
	}

	public int getNumDimensions()
	{
		return height;
	}

	public int getNumPoints()
	{
		return width;
	}

	public double mean(int row)
	{
		double mean = 0.0;
		for (int i = 0; i < width; i++)
			mean += data[row][i];
		mean /= width;
		return mean;
	}

	public Vector mean()
	{
		Vector m = new Vector(height);
		for (int i = 0; i < height; i++)
			m.set(i, mean(i));
		return m;
	}

	public double variance(int row, double mean)
	{
		if (width <= 1)
			return 0.0;

		double sum = 0.0;
		for (int i = 0; i < width; i++)
		{
			double x = data[row][i] - mean;
			sum += x * x;
		}

		return (sum / (width - 1));
	}

	public double variance(int row)
	{
		double mean = mean(row);
		return variance(row, mean);
	}

	public double stdDev(int row)
	{
		return Math.sqrt(variance(row));
	}

	public double correlation(int row1, int row2)
	{
		double n = stdDev(row1) * stdDev(row2);
		if (n != 0.0)
			return covariance(row1, row2) / n;
		return 0.0;
	}

	public double covariance(int row1, int row2)
	{
		if (width <= 1)
			return 0.0;

		double mean1 = mean(row1);
		double mean2 = mean(row2);

		double sum = 0.0;
		for (int i = 0; i < width; i++)
			sum += (data[row1][i] - mean1) * (data[row2][i] - mean2);

		return sum / (width - 1);
	}
	
	public Matrix covarianceMatrix()
	{
		Matrix m = new Matrix(height, height);

		for (int i = 0; i < height; i++)
			m.set(i, i, variance(i));

		for (int i = 0; i < (height - 1); i++)
		{
			for (int j = i + 1; j < height; j++)
			{
				double covar = covariance(i, j);
				m.set(i, j, covar);
				m.set(j, i, covar);
			}
		}

		return m;
	}

	public void transformToRanks()
	{
		for (int i = 0; i < height; i++)
		{
			double[] rank = new double[width];
			for (int j = 0; j < width; j++)
			{
				// TODO: could be made more efficient by using Arrays.sort and
				//   a custom Comparator
				int numGreater = 0;
				int numEqual = 0;
				for (int k = 0; k < width; k++)
				{
					if (data[i][k] > data[i][j])
						numGreater++;
					else if (data[i][k] == data[i][j])
						numEqual++;
				}
				int minRank = numGreater + 1;
				int maxRank = numGreater + numEqual;
				int sum = ((maxRank + minRank) * (maxRank - minRank + 1)) / 2;
				rank[j] = (double) sum / numEqual; 
			}
			data[i] = rank;
		}
	}

	public void transformMean(int row, double mean)
	{
		double d = mean(row) - mean;
		for (int i = 0; i < width; i++)
			data[row][i] = data[row][i] - d;
	}

	public void transformMean(double mean)
	{
		for (int i = 0; i < height; i++)
			transformMean(i, mean);
	}

	public Matrix correlationMatrix()
	{
		Matrix m = new Matrix(height, height);

		for (int i = 0; i < height; i++)
			m.set(i, i, 1.0);

		for (int i = 0; i < (height - 1); i++)
		{
			for (int j = i + 1; j < height; j++)
			{
				double correlation = correlation(i, j);
				m.set(i, j, correlation);
				m.set(j, i, correlation);
			}
		}

		return m;
	}

}

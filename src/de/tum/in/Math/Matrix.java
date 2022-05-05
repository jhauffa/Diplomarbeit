package de.tum.in.Math;

import java.io.OutputStream;
import java.io.PrintStream;

public class Matrix {

	protected int height;
	protected int width;
	protected double[][] data;

	public Matrix(int height, int width)
	{
		this.height = height;
		this.width = width;
		data = new double[height][];
		for (int i = 0; i < height; i++)
			data[i] = new double[width];
	}

	public Matrix(double[][] data)
	{
		height = data.length;
		width = data[0].length;
		this.data = data;
	}

	public Matrix(Matrix other)
	{
		this(other.data);
	}

	public Matrix duplicate()
	{
		double[][] dataCopy = new double[height][];
		for (int i = 0; i < height; i++)
		{
			dataCopy[i] = new double[width];
			System.arraycopy(data[i], 0, dataCopy[i], 0, width);
		}
		return new Matrix(dataCopy);
	}

	public int height()
	{
		return height;
	}

	public int width()
	{
		return width;
	}

	public double get(int i, int j)
	{
		return data[i][j];
	}

	public void set(int i, int j, double value)
	{
		data[i][j] = value;
	}

	public Vector getRow(int r)
	{
		return new Vector(data[r]);
	}

	public Vector copyColumn(int c)
	{
		Vector v = new Vector(height);
		for (int i = 0; i < height; i++)
			v.set(i, data[i][c]);
		return v;
	}

	public void setColumn(int c, Vector v)
	{
		int n = v.size();
		assert (n == height);

		for (int i = 0; i < n; i++)
			data[i][c] = v.get(i);
	}

	public Vector mult(Vector v)
	{
		// m = m * v
		assert (v.size() == width);
		Vector p = new Vector(height);
		for (int i = 0; i < height; i++)
		{
			double sum = 0.0;
			for (int j = 0; j < width; j++)
				sum += data[i][j] * v.get(j); 
			p.set(i, sum);
		}
		return p;
	}

	public static Matrix mult(Matrix m1, Matrix m2)
	{
		int n = m1.width;
		assert (n == m2.height);
		
		int h = m1.height;
		int w = m2.width;
		Matrix product = new Matrix(h, w);
		
		for (int i = 0; i < h; i++)
			for (int j = 0; j < w; j++)
				for (int k = 0; k < n; k++)
					product.data[i][j] += m1.data[i][k] * m2.data[k][j];
		
		return product;
	}


	protected void swapColumns(int c1, int c2)
	{
		for (int i = 0; i < height; i++)
		{
			double tmp = data[i][c2];
			data[i][c2] = data[i][c1];
			data[i][c1] = tmp;
		}
	}

	private static final int MAX_ROTATIONS = 50;

	// Compute eigenvectors of a matrix using the Jacobi rotation algorithm.
	// Adapted from Press et al, "Numerical Recipes in C", 2nd edition.
	// Each column of the returned matrix contains one eigenvector. Eigenvectors
	// are sorted by eigenvalue in descending order.
	public Matrix eigen(Vector values)
	{
		int n = height;
		if (n != width)
			throw new RuntimeException("matrix not symmetric");
		if (values.size() != n)
			throw new RuntimeException("invalid size of output vector");

		Matrix vectors = new Matrix(n, n);
		int i;
		for (i = 0; i < n; i++)
		{
			for (int j = 0; j < n; j++)
			{
				if (i == j)
					vectors.set(i, j, 1.0);
				else
					vectors.set(i, j, 0.0);
			}
		}

		Matrix a = duplicate();
		Vector b = new Vector(n);
		Vector z = new Vector(n);
		for (i = 0; i < n; i++)
		{
			double tmp = a.get(i, i);
			b.set(i, tmp);
			values.set(i, tmp);
		}

		for (i = 0; i < MAX_ROTATIONS; i++) 
		{
			double sum = 0.0;
			for (int j = 0; j < (n - 1); j++)
				for (int k = j + 1; k < n; k++)
					sum += Math.abs(a.get(j, k));
			if (sum == 0.0)
				break;

			double threshold = 0.0;
			if (i < 3)
				threshold = 0.2 * sum / (n * n);

			for (int j = 0; j < (n - 1); j++)
			{
				for (int k = j + 1; k < n; k++)
				{
					double g = 100.0 * Math.abs(a.get(j, k));
					if ((i > 3) &&
						((Math.abs(values.get(j)) + g) ==
						  Math.abs(values.get(j))) &&
						((Math.abs(values.get(k)) + g) ==
						  Math.abs(values.get(k))))
					{
						a.set(j, k, 0.0);
					}
					else if (Math.abs(a.get(j, k)) > threshold) 
					{
						double h = values.get(k) - values.get(j);
						double t;
						if ((Math.abs(h) + g) == Math.abs(h))
						{
							t = a.get(j, k) / h;
						}
						else
						{
							double theta = 0.5 * h / a.get(j, k);
							t = 1.0 / (Math.abs(theta) +
									Math.sqrt(1.0 + theta * theta));
							if (theta < 0.0)
								t = -t;
						}

						double c = 1.0 / Math.sqrt(1.0 + t * t);
						double s = t * c;
						double tau = s / (1.0 + c);
						h = t * a.get(j, k);
						z.set(j, z.get(j) - h);
						z.set(k, z.get(k) + h);
						values.set(j, values.get(j) - h);
						values.set(k, values.get(k) + h);
						a.set(j, k, 0.0);

						for (int l = 0; l <= (j - 1); l++)
						{
							g = a.get(l, j);
							h = a.get(l, k);
							a.set(l, j, g - s * (h + g * tau));
							a.set(l, k, h + s * (g - h * tau));
						}
						for (int l = j + 1; l <= (k - 1); l++)
						{
							g = a.get(j, l);
							h = a.get(l, k);
							a.set(j, l, g - s * (h + g * tau));
							a.set(l, k, h + s * (g - h * tau));
						}
						for (int l = k + 1; l < n; l++)
						{
							g = a.get(j, l);
							h = a.get(k, l);
							a.set(j, l, g - s * (h + g * tau));
							a.set(k, l, h + s * (g - h * tau));
						}
						for (int l = 0; l < n; l++)
						{
							g = vectors.get(l, j);
							h = vectors.get(l, k);
							vectors.set(l, j, g - s * (h + g * tau));
							vectors.set(l, k, h + s * (g - h * tau));
						}
					}
				}
			}

			for (int j = 0; j < n; j++)
			{
				b.set(j, b.get(j) + z.get(j));
				values.set(j, b.get(j));
				z.set(j, 0.0);
			}
		}

		if (i >= MAX_ROTATIONS)
			throw new RuntimeException("Matrix did not converge after maximum" +
					" number of rotations.");

		for (int j = 0; j < vectors.height(); j++)
			for (int k = 0; k < vectors.width(); k++)
				vectors.set(j, k, -1.0 * vectors.get(j, k));

		// sort by descending eigenvalues
		for (int j = 0; j < (n - 1); j++)
		{
			int maxIdx = j;
			for (int k = j + 1; k < n; k++)
				if (values.get(k) > values.get(maxIdx))
					maxIdx = k;
			if (maxIdx != j)
			{
				double tmp = values.get(j);
				values.set(j, values.get(maxIdx));
				values.set(maxIdx, tmp);
				vectors.swapColumns(j, maxIdx);
			}
		}

		return vectors;
	}

	public void print(OutputStream out)
	{
		PrintStream p = new PrintStream(out);
		for (int i = 0; i < height; i++)
		{
			for (int j = 0; j < width; j++)
				p.printf("%f\t", data[i][j]);
			p.println();
		}
		p.flush();
	}

}

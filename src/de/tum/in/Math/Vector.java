package de.tum.in.Math;

public class Vector {

	protected int size;
	protected double[] data;

	public Vector(int dim)
	{
		size = dim;
		data = new double[dim];
	}
	
	public Vector(double[] data)
	{
		size = data.length;
		this.data = data;
	}

	public Vector(Vector other)
	{
		this(other.data);
	}

	public Vector duplicate()
	{
		double[] dataCopy = new double[size];
		System.arraycopy(data, 0, dataCopy, 0, size);
		return new Vector(dataCopy);
	}

	public int size()
	{
		return size;
	}

	public double get(int i)
	{
		return data[i];
	}
	
	public void set(int i, double value)
	{
		data[i] = value;
	}

	public double euclideanDist(Vector v)
	{
		return Math.sqrt(euclideanDistSqr(v));
	}

	public double euclideanDistSqr(Vector v)
	{
		assert (size == v.size);

		double d = 0.0;
		for (int i = 0; i < size; i++)
		{
			double x = data[i] - v.data[i];
			d += x * x;
		}

		return d;
	}

	public void add(Vector v)
	{
		assert (size == v.size);
		for (int i = 0; i < size; i++)
			data[i] += v.data[i];
	}

	public void sub(Vector v)
	{
		assert (size == v.size);
		for (int i = 0; i < size; i++)
			data[i] -= v.data[i];
	}

	public void mult(double x)
	{
		for (int i = 0; i < size; i++)
			data[i] *= x;
	}

	public static Vector add(Vector v1, Vector v2)
	{
		// v1 + v2
		Vector result = v1.duplicate();
		result.add(v2);
		return result;		
	}

	public static Vector sub(Vector v1, Vector v2)
	{
		// v1 - v2
		Vector result = v1.duplicate();
		result.sub(v2);
		return result;
	}

	public static Vector mult(double x, Vector v)
	{
		// x * v
		Vector result = v.duplicate();
		result.mult(x);
		return result;
	}

}

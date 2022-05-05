package de.tum.in.Math;

import java.util.Arrays;

public class ImageDataSet extends Matrix {

	public ImageDataSet(int x, int y)
	{
		super(y, x);
	}

	public ImageDataSet(Matrix data)
	{
		super(data);
	}

	public int getPixel(int idx)
	{
		return (int) data[idx / width][idx % width];
	}

	public void setPixel(int idx, int v)
	{
		data[idx / width][idx % width] = v;
	}

	public int[] get4NeighborhoodOffsets()
	{
		int[] offsets = new int[4];
		offsets[0] = -1;
		offsets[1] =  1;
		offsets[2] = -width;
		offsets[3] =  width;
		return offsets;
	}

	public int[] get8NeighborhoodOffsets()
	{
		int[] offsets = new int[8];
		offsets[0] = -1;
		offsets[1] =  1;
		offsets[2] = -width;
		offsets[3] =  width;
		offsets[4] = -width - 1;
		offsets[5] = -width + 1;
		offsets[6] =  width - 1;
		offsets[7] =  width + 1;
		return offsets;		
	}

	public void fill(double v)
	{
		for (int i = 0; i < height; i++)
			Arrays.fill(data[i], v);
	}

	public void quantize(int numSteps)
	{
		numSteps--;
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				data[i][j] = Math.round(data[i][j] * numSteps);
	}

	public void stretchHistogram()
	{
		double min = Double.MAX_VALUE;
		double max = -Double.MAX_VALUE;
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
			{
				double v = data[i][j];
				if (v < min)
					min = v;
				if (v > max)
					max = v;
			}

		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				data[i][j] = (data[i][j] - min) / (max - min);
	}

}

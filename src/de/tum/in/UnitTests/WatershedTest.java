package de.tum.in.UnitTests;

import de.tum.in.Math.Matrix;
import de.tum.in.Math.ImageDataSet;
import de.tum.in.SocialNetworks.WatershedTransform;

public class WatershedTest {

	private static final double data[][] = {
		{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
		{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
		{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
		{1.0, 0.7, 0.5, 0.5, 0.7, 1.0, 0.7, 0.5, 0.5, 0.7},
		{1.0, 0.7, 0.0, 0.0, 0.8, 1.0, 0.8, 0.0, 0.0, 0.7},
		{1.0, 0.7, 0.0, 0.0, 0.8, 1.0, 0.8, 0.0, 0.0, 0.7},
		{1.0, 0.7, 0.5, 0.5, 0.7, 1.0, 0.7, 0.5, 0.5, 0.7},
		{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
		{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
		{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}
	};

	public static void main(String[] args)
	{
		ImageDataSet img = new ImageDataSet(new Matrix(data));
		img.quantize(256);
		ImageDataSet wsh = WatershedTransform.transform(img);
		wsh.print(System.out);
	}

}

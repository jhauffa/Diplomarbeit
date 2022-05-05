package de.tum.in.UnitTests;

import junit.framework.TestCase;

import de.tum.in.Math.Matrix;
import de.tum.in.Math.Vector;
import de.tum.in.Math.RowDataSet;
import de.tum.in.SocialNetworks.PrincipalComponentAnalysis;


public class MathTest extends TestCase {

	private static final double EPSILON = 0.0000001;

	private static boolean isEqual(Vector v1, Vector v2)
	{
		if (v1.size() != v2.size())
			return false;
		
		for (int i = 0; i < v1.size(); i++)
			if (Math.abs(v1.get(i) - v2.get(i)) > EPSILON)
				return false;
	
		return true;
	}

	private static boolean isEqual(Matrix m1, Matrix m2)
	{
		if ((m1.width() != m2.width()) || (m1.height() != m2.height()))
			return false;

		for (int i = 0; i < m1.height(); i++)
			for (int j = 0; j < m1.width(); j++)
				if (Math.abs(m1.get(i, j) - m2.get(i, j)) > EPSILON)
				{
/*
					// DBG
					System.err.printf("%d,%d: %f, exp. %f\n", i, j,
							m2.get(i, j), m1.get(i, j));
*/
					return false;
				}

		return true;
	}

	public void testEigenDecomposition()
	{
		// matrix has 4 real eigenvectors
		double data[][] = {
				{4.0, -30.0, 60.0, -35.0},
				{-30.0, 300.0, -675.0, 420.0},
				{60.0, -675.0, 1620.0, -1050.0},
				{-35.0, 420.0, -1050.0, 700.0}
		};
		Matrix m = new Matrix(data);

		Matrix eigenVectors;
		Vector eigenValues = new Vector(4);
		try
		{
			eigenVectors = m.eigen(eigenValues);
		}
		catch (Exception ex)
		{
			fail("Matrix.eigen() threw exception: " + ex.getMessage());
			return;
		}
		assertEquals(4, eigenVectors.height());
		assertEquals(4, eigenVectors.width());

		for (int i = 0; i < 4; i++)
		{
			// M * eigenVector[i] = eigenValue[i] * eigenVector[i]
			Vector v1 = eigenVectors.copyColumn(i);
			Vector v2 = m.mult(v1);
			v1.mult(eigenValues.get(i));
			assertTrue(isEqual(v1, v2));
		}
	}

	public void testPCA()
	{
		double data[][] = {
				{2.5, 0.5, 2.2, 1.9, 3.1, 2.3, 2.0, 1.0, 1.5, 1.1},
				{2.4, 0.7, 2.9, 2.2, 3.0, 2.7, 1.6, 1.1, 1.6, 0.9}
		};
		double expectedData[][] = {
				{-0.827970186, 1.77758033, -0.992197494, -0.274210416,
				 -1.67580142, -0.912949103, 0.0991094375, 1.14457216,
				 0.438046137, 1.22382056},
				{-0.175115307, 0.142857227, 0.384374989, 0.130417207,
				 -0.209498461, 0.175282444, -0.349824698, 0.0464172582,
				 0.0177646297, -0.162675287}
		};

		RowDataSet dataSet = new RowDataSet(new Matrix(data));
		PrincipalComponentAnalysis pca =
			new PrincipalComponentAnalysis(dataSet);
		RowDataSet transformedDataSet = pca.transform(2);

		RowDataSet expectedDataSet = new RowDataSet(new Matrix(expectedData));
		assertTrue(isEqual(expectedDataSet, transformedDataSet));
	}

	public void testRanks()
	{
		double data[][] = {{2.0, 6.0, 1.0, 1.0, 3.0, 1.0, 7.0, 2.0, 1.0, 2.0}};
		double rank[][] = {{5.0, 2.0, 8.5, 8.5, 3.0, 8.5, 1.0, 5.0, 8.5, 5.0}};
		RowDataSet dataSet = new RowDataSet(new Matrix(data));
		dataSet.transformToRanks();
		RowDataSet expectedDataSet = new RowDataSet(new Matrix(rank));
		assertTrue(isEqual(expectedDataSet, dataSet));
	}

}

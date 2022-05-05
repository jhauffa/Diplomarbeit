package de.tum.in.UnitTests;

import junit.framework.TestCase;

import de.tum.in.Math.Statistics;

public class StatisticsTest extends TestCase {

	public void testChiSquareLimits()
	{
		double limit = Statistics.computeChiSquareLimit(1, 0.05);
		assertEquals(3.841, limit, 0.001);
		limit = Statistics.computeChiSquareLimit(10, 0.01);
		assertEquals(23.209, limit, 0.001);
		limit = Statistics.computeChiSquareLimit(20, 0.005);
		assertEquals(39.997, limit, 0.001);
		limit = Statistics.computeChiSquareLimit(1, 0.03);
		assertEquals(4.71, limit, 0.1);		
	}

}

package de.tum.in.Math;

public class Statistics {

	private static final double[] chiSquareConf = {0.005, 0.01, 0.05};
	private static final int chiSquare99 = 1;
	private static final int chiSquare95 = 2;
	private static final double[][] chiSquareLimits = {
		{7.879, 10.597, 12.838, 14.860, 16.750, 18.548, 20.278, 21.955, 23.589,
		 25.188, 26.757, 28.300, 29.819, 31.319, 32.801, 34.267, 35.718, 37.156,
		 38.582, 39.997, 41.401, 42.796, 44.181, 45.559, 46.928, 48.290, 49.645,
		 50.993, 52.336, 53.672},
		{6.635, 9.210, 11.345, 13.277, 15.086, 16.812, 18.475, 20.090, 21.666,
		 23.209, 24.725, 26.217, 27.688, 29.141, 30.578, 32.000, 33.409, 34.805,
		 36.191, 37.566, 38.932, 40.289, 41.638, 42.980, 44.314, 45.642, 46.963,
		 48.278, 49.588, 50.892},
		{3.841, 5.991, 7.815, 9.488, 11.070, 12.592, 14.067, 15.507, 16.919,
		 18.307, 19.675, 21.026, 22.362, 23.685, 24.996, 26.296, 27.587, 28.869,
		 30.144, 31.410, 32.671, 33.924, 35.172, 36.415, 37.652, 38.885, 40.113,
		 41.337, 42.557, 43.773}
	};

	private static final double[] fConf = {0.01, 0.05, 0.1};
	private static final double[] fLimits10and5 = {10.051, 4.735, 3.297};

	private static double interplLog(double x, double x1, double x2,
			double y1, double y2)
	{
		double base = x2 / x1;
		double k = Math.log(x / x1) / Math.log(base);
		return k * y2 + (1.0 - k) * y1;
	}

	public static double computeChiSquareLimit(int deg, double conf)
	{
		int upperIdx = 1;
		while ((conf > chiSquareConf[upperIdx]) &&
			   (upperIdx < (chiSquareConf.length - 1)))
			upperIdx++;

		return interplLog(conf, chiSquareConf[upperIdx - 1],
				chiSquareConf[upperIdx], chiSquareLimits[upperIdx - 1][deg - 1],
				chiSquareLimits[upperIdx][deg - 1]);
	}

	public static double computeFLimit10and5(double conf)
	{
		int upperIdx = 1;
		while ((conf > fConf[upperIdx]) &&
			   (upperIdx < (fConf.length - 1)))
			upperIdx++;

		return interplLog(conf, fConf[upperIdx - 1], fConf[upperIdx],
				fLimits10and5[upperIdx - 1], fLimits10and5[upperIdx]);
	}

	public static boolean chiSquare(int[] dist, double[] expectedNormDist,
			boolean conf99)
	{
		int dim = dist.length;
		assert dim == expectedNormDist.length;

		int distSum = 0;
		for (int i = 0; i < dim; i++)
			distSum += dist[i];

		double c = 0.0;
		for (int i = 0; i < dim; i++)
		{
			double expectedFreq = expectedNormDist[i] * distSum;
			c += Math.pow(dist[i] - expectedFreq, 2.0) / expectedFreq;
		}

		if (conf99)
			return (c > chiSquareLimits[chiSquare99][dim - 1]);
		return (c > chiSquareLimits[chiSquare95][dim - 1]);
	}

	public static double harmonicMean(double x1, double x2)
	{
		double sum = x1 + x2;
		if (sum > 0.0)
			return 2.0 * ((x1 * x2) / sum);
		return 0.0;
	}

}

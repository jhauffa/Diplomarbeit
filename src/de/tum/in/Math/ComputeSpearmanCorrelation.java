package de.tum.in.Math;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

public class ComputeSpearmanCorrelation {

	private static double parseValue(String s)
	{
		double v = 0.0;
		try
		{
			// parse double according to current locale
			v = NumberFormat.getInstance().parse(s).doubleValue();
		}
		catch (ParseException ex)
		{
			System.err.printf("parse error: %s\n", s);
		}
		return v;
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: ComputeSpearmanCorrelation csv");
			return;
		}

		ArrayList<Double> val1 = new ArrayList<Double>();
		ArrayList<Double> val2 = new ArrayList<Double>();
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(
					new File(args[0])));
			String line;
			while ((line = in.readLine()) != null)
			{
				String[] parts = line.split(";");
				if (parts.length == 2)
				{
					val1.add(parseValue(parts[0]));
					val2.add(parseValue(parts[1]));
				}
			}
			in.close();
		}
		catch (IOException ex)
		{
			System.err.printf("read error: %s\n", ex.getMessage());
			return;
		}
		catch (NumberFormatException ex)
		{
			System.err.printf("parse error: %s\n", ex.getMessage());
			return;
		}

		int n = val1.size();
		Matrix data = new Matrix(2, n);
		for (int i = 0; i < n; i++)
		{
			data.set(0, i, val1.get(i));
			data.set(1, i, val2.get(i));
		}
		RowDataSet ds = new RowDataSet(data);
		ds.transformToRanks();
		double cor = ds.correlation(0, 1);
		System.out.printf("Spearman's rank correlation coeff. = %f\n", cor);
	}

}

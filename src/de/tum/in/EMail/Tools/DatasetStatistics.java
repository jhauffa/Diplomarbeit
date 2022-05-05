package de.tum.in.EMail.Tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import de.tum.in.EMail.MessageDatabase;


public class DatasetStatistics {

	private static class DatasetInfo
	{
		String name;
		int size;
	}

	private static class DatasetInfoComparator
			implements Comparator<DatasetInfo>
	{
		public int compare(DatasetInfo d1, DatasetInfo d2)
		{
			return d1.size - d2.size;
		}
	}

	private static void processDataset(File f, List<DatasetInfo> list)
	{
		MessageDatabase db;
		try
		{
			db = MessageDatabase.readFromFile(f);
		}
		catch (IOException ex)
		{
			System.err.printf("error reading %s: %s\n",
					f.getName(), ex.getMessage());
			return;
		}

		DatasetInfo info = new DatasetInfo();
		info.name = f.getName();
		info.size = db.size();
		list.add(info);
	}

	private static void printList(List<DatasetInfo> list)
	{
		Collections.sort(list, new DatasetInfoComparator());
		for (DatasetInfo info : list)
			System.out.printf("%s\t%d\n", info.name, info.size);
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("usage: DatasetStatistics basedir");
			return;
		}

		ArrayList<DatasetInfo> stats = new ArrayList<DatasetInfo>(); 
		File f = new File(args[0]);
		if (f.isDirectory())
		{
			for (File item : f.listFiles())
				processDataset(item, stats);
		}

		printList(stats);
	}

}

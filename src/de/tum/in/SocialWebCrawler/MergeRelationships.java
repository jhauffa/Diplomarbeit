package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

public class MergeRelationships {

	private static void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.println("error: " + message);
		System.err.printf("\nusage: MergeRelationships reldb reldb\n"+
				"where\n"+
				"reldb\tis a relationship database\n");
		System.exit(code);
	}

	public static void main(String[] args)
	{
		if (args.length < 2)
			printUsageAndExit("not enough arguments", 1);

		RelationshipDatabase relDb1 =
			new RelationshipDatabase(new File(args[0]));
		RelationshipDatabase relDb2 =
			new RelationshipDatabase(new File(args[1]));

		RelationshipDatabase mergedRelDb = new RelationshipDatabase("merged");

		int numRecords = merge(relDb1, relDb2, mergedRelDb);

		try
		{
			mergedRelDb.write();
		}
		catch (IOException ex)
		{
			System.err.printf("error writing database: %s\n", ex.getMessage());
		}
		System.err.printf("%d records\n", numRecords);
		System.err.printf("result written to \"%s\"\n",
				mergedRelDb.getFileName());
	}

	private static int merge(RelationshipDatabase relDb1,
			RelationshipDatabase relDb2, RelationshipDatabase mergedRelDb)
	{
		int numRecords = appendDatabase(relDb1, mergedRelDb);
		numRecords += appendDatabase(relDb2, mergedRelDb);
		return numRecords;
	}

	private static int appendDatabase(RelationshipDatabase relDb,
			RelationshipDatabase mergedRelDb)
	{
		int numRecords = 0;
		for (Map.Entry<String, LinkedList<Relationship>> entry : relDb)
		{
			LinkedList<Relationship> list = entry.getValue();
			if (list.size() > 0)
			{
				for (Relationship r : list)
				{
					// relationship record already in database?
					boolean found = false;
					LinkedList<Relationship> result = mergedRelDb.query(r.from);
					if (result != null)
					{
						for (Relationship resultRel : result)
							if (resultRel.to.equals(r.to) &&
								resultRel.type.equals(r.type))
							{
								found = true;
								break;
							}
					}

					if (!found)
					{
						mergedRelDb.append(r);
						numRecords++;
					}
				}
			}
			else
			{
				String person = entry.getKey();
				if (mergedRelDb.query(person) == null)
					mergedRelDb.appendEmpty(person);
				numRecords++;
			}
		}
		return numRecords;
	}

}

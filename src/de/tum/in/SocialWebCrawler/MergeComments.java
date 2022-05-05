package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.io.IOException;

public class MergeComments {

	private static void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.println("error: " + message);
		System.err.printf("\nusage: MergeComments commentdb commentdb\n"+
				"where\n"+
				"commentdb\tis a comment database\n");
		System.exit(code);
	}

	public static void main(String[] args)
	{
		if (args.length < 2)
			printUsageAndExit("not enough arguments", 1);

		CommentDatabase commentDb1 = new CommentDatabase(new File(args[0]));
		CommentDatabase commentDb2 = new CommentDatabase(new File(args[1]));

		CommentDatabase mergedCommentDb = new CommentDatabase("merged");

		int numRecords = merge(commentDb1, commentDb2, mergedCommentDb);

		try
		{
			mergedCommentDb.write();
		}
		catch (IOException ex)
		{
			System.err.printf("error writing database: %s\n", ex.getMessage());
		}
		System.err.printf("%d records\n", numRecords);
		System.err.printf("result written to \"%s\"\n",
				mergedCommentDb.getFileName());
	}

	private static int merge(CommentDatabase commentDb1,
			CommentDatabase commentDb2, CommentDatabase mergedCommentDb)
	{
		int numRecords = 0;

		for (Comment c : commentDb1)
		{
			mergedCommentDb.append(c);
			numRecords++;
		}
		for (Comment c : commentDb2)
		{
			mergedCommentDb.append(c);
			numRecords++;
		}

		return numRecords;
	}

}

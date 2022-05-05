package de.tum.in.SocialWebCrawler;

import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.io.File;
import java.io.IOException;

public class PruneComments {

	private static void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.println("error: " + message);
		System.err.printf("\nusage: PruneComments reldb commentdb\n"+
				"where\n"+
				"reldb\tis a relationship database and\n"+
				"commentdb\tis a comment database\n");
		System.exit(code);
	}

	public static void main(String[] args)
	{
		if (args.length < 2)
			printUsageAndExit("not enough arguments", 1);

		RelationshipDatabase relDb= new RelationshipDatabase(new File(args[0]));
		CommentDatabase commentDb = new CommentDatabase(new File(args[1]));

		CommentDatabase prunedCommentDb = new CommentDatabase("pruned");
		RelationshipDatabase prunedRelDb = new RelationshipDatabase("pruned");
		
		prune(relDb, commentDb, prunedRelDb, prunedCommentDb);

		try
		{
			prunedCommentDb.write();
			prunedRelDb.write();
		}
		catch (IOException ex)
		{
			System.err.printf("error writing database: %s\n", ex.getMessage());
		}
		System.err.printf("result written to \"%s\" and \"%s\"\n",
				prunedCommentDb.getFileName(), prunedRelDb.getFileName());
	}

	private static void prune(RelationshipDatabase relDb, 
			CommentDatabase commentDb, RelationshipDatabase prunedRelDb,
			CommentDatabase prunedCommentDb)
	{
		int numUsefulRel = 0;
		int numUsefulComments = 0;

		for (Comment c : commentDb)
		{
			if (c.parentId == 0)
				continue;

			Comment parent = commentDb.query(c.parentId);
			if (parent == null)
			{
				System.err.printf("warning: parent comment %d of comment %d " +
						"not in DB\n", c.parentId, c.id);
				continue;
			}

			if (parent.author.equals(c.author))  // replying to self
				continue;

			Vector<Relationship> relAuthors = new Vector<Relationship>(2);
			Relationship relChildParent = findRelationship(relDb, c.author,
					parent.author);
			if (relChildParent != null)
				relAuthors.add(relChildParent);
			Relationship relParentChild = findRelationship(relDb, parent.author,
					c.author);
			if (relParentChild != null)
				relAuthors.add(relParentChild);

			if (!relAuthors.isEmpty())
			{
				prunedCommentDb.append(commentDb.query(c.id));
				prunedCommentDb.append(commentDb.query(parent.id));
				numUsefulComments += 2;

				for (Relationship rel : relAuthors)
				{
					if (findRelationship(prunedRelDb, rel.from, rel.to) == null)
					{
						prunedRelDb.append(rel);
						numUsefulRel++;
					}
				}
			}
		}

		System.err.printf("found %d useful relationships, %s useful comments\n",
				numUsefulRel, numUsefulComments);
	}

	private static Relationship findRelationship(RelationshipDatabase relDb, 
			String from, String to)
	{
		Relationship rel = null;
		List<Relationship> list = relDb.query(from);
		if (list != null)
		{
			Iterator<Relationship> it = list.iterator();
			while ((rel == null) && it.hasNext())
			{
				Relationship curRel = it.next();
				if (curRel.to.equals(to))
					rel = curRel;
			}
		}
		return rel;
	}
	
}

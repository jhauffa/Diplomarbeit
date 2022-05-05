package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.Comparator;


public class EvaluateComments {

	private class Edge
	{
		public String source;
		public String dest;
		private boolean directed;

		public Edge(String source, String dest, boolean directed)
		{
			this.source = source;
			this.dest = dest;
			this.directed = directed;
		}

		public boolean equals(Object other)
		{
			if (!(other instanceof Edge))
				return false;
			Edge otherEdge = (Edge) other;
			if (directed)
				return (otherEdge.source.equals(this.source) &&
						otherEdge.dest.equals(this.dest));
			else
				return ((otherEdge.source.equals(this.source) &&
						 otherEdge.dest.equals(this.dest)) ||
						(otherEdge.source.equals(this.dest) &&
						 otherEdge.dest.equals(this.source)));		
		}

		public int hashCode()
		{
			int hash;
			if (directed)
			{
				hash = source.hashCode() + 0x9e3779b9;
				hash ^= dest.hashCode() + 0x9e3779b9 + (hash << 6)+(hash >> 2);
			}
			else
				hash = source.hashCode() ^ dest.hashCode();
			return hash;
		}
	}

	private class CompareCount implements Comparator<Map.Entry<Edge,Integer>>
	{
		public int compare(Map.Entry<Edge,Integer> e1,
				Map.Entry<Edge,Integer> e2)
		{
			return e2.getValue() - e1.getValue();
		}
	}

	private static void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.println("error: " + message);
		System.err.printf("\nusage: EvaluateComments commentdb\n"+
				"where\n"+
				"commentdb\tis a comment database\n");
		System.exit(code);
	}

	public static void main(String[] args)
	{
		if (args.length < 1)
			printUsageAndExit("not enough arguments", 1);

		CommentDatabase commentDb = new CommentDatabase(new File(args[0]));
		EvaluateComments eval = new EvaluateComments();
		eval.evaluate(commentDb);
	}

	public void evaluate(CommentDatabase commentDb)
	{
		HashMap<Edge, Integer> commentCount = new HashMap<Edge, Integer>();
		for (Comment c : commentDb)
		{
			if ((c.parentId != 0) && !c.author.isEmpty())
			{
				Comment p = commentDb.query(c.parentId);
				if (!p.author.isEmpty())
				{
					Edge e = new Edge(c.author, p.author, true);
					Integer curCnt = commentCount.get(e);
					if (curCnt != null)
						commentCount.put(e, curCnt + 1);
					else
						commentCount.put(e, 1);
				}
			}
		}

		System.out.println("directed relationship edges:");
		printSorted(commentCount);
		System.out.println();

		HashMap<Edge, Integer> commentCountUndir = new HashMap<Edge, Integer>();
		for (Map.Entry<Edge, Integer> entry : commentCount.entrySet())
		{
			Edge e = new Edge(entry.getKey().source, entry.getKey().dest,false);
			Integer curCnt = commentCountUndir.get(e);
			if (curCnt != null)
				commentCountUndir.put(e, curCnt + entry.getValue());
			else
				commentCountUndir.put(e, entry.getValue());
		}

		System.out.println("undirected relationship edges:");
		printSorted(commentCountUndir);
	}

	private void printSorted(HashMap<Edge, Integer> commentCount)
	{
		Vector<Map.Entry<Edge, Integer>> v =
			new Vector<Map.Entry<Edge, Integer>>(commentCount.entrySet());
		Collections.sort(v, new CompareCount());
		for (Map.Entry<Edge, Integer> entry : v)
		{
			if (entry.getValue() <= 1)
				break;
			System.out.printf("%d\t\"%s\",\"%s\"\n", entry.getValue(),
					entry.getKey().source, entry.getKey().dest);
		}
	}

}

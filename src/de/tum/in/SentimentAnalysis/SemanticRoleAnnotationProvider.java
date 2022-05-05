package de.tum.in.SentimentAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import de.tum.in.Util.StreamWatcher;


public class SemanticRoleAnnotationProvider extends AnnotationProvider {

	private class OutputStreamParser extends Thread
	{
		private BufferedReader reader;

		public OutputStreamParser(InputStream in)
		{
			reader = new BufferedReader(new InputStreamReader(in));
		}

		@Override public void run()
		{
			try
			{
				Iterator<Sentence> data = corpus.iterator();
				if (!data.hasNext())
				{
					System.err.println("empty corpus");
					return;
				}
				Sentence s = data.next();

				String line;
				int[] curChunk = new int[maxDepth];
				ArrayList<Integer>[] tagSequence = new ArrayList[maxDepth];
				for (int i = 0; i < maxDepth; i++)
					tagSequence[i] = new ArrayList<Integer>();
				while ((line = reader.readLine()) != null)
				{
					if (line.length() < 1)
					{
						// empty line is sentence delimiter
						for (int i = 0; i < maxDepth; i++)
						{
							int[] tmp = new int[tagSequence[i].size()];
							for (int j = 0; j < tagSequence[i].size(); j++)
								tmp[j] = tagSequence[i].get(j);
							tagSequence[i].clear();
							annotateSentence(s, tmp, i);

							curChunk[i] = 0;
						}

						if (!data.hasNext())
							break;
						s = data.next();
					}
					else
					{
						String[] column = line.split("\\t\\t");
						if (column.length < 2)
						{
							System.err.printf("invalid SRL output \"%s\"\n",
									line);
							continue;
						}

						int d = Math.min(column.length - 1, maxDepth);
						for (int i = 0; i < d; i++)
						{
							if (column[i+1].startsWith("("))
							{
								String tag = column[i+1].substring(1,
										column[i+1].indexOf('*'));

								// cut off suffix of tags starting with C and R
								if ((tag.charAt(0) == 'C') ||
									(tag.charAt(0) == 'R'))
								{
									int dashIdx = tag.indexOf('-');
									if (dashIdx > 0)
										tag = tag.substring(0, dashIdx);
								}

								int tagIdx = Arrays.binarySearch(tags, tag);
								if (tagIdx < 0)
								{
									System.err.printf("unknown tag \"%s\"\n",
											tag);
									tagIdx = 0;
								}
								else
									tagIdx++;
								curChunk[i] = tagIdx;
								if (!ignoreBoundaryTags && (tagIdx > 0))
									tagIdx += tags.length;
								tagSequence[i].add(tagIdx);
							}
							else
								tagSequence[i].add(curChunk[i]);
							if (column[i+1].endsWith(")"))
								curChunk[i] = 0;
						}
					}
				}
			}
			catch (IOException ex)
			{
				System.err.printf("IO error: %s\n", ex.getMessage());
			}
		}		
	}


	private static final String[] srlEnv = {"WNHOME=./external"};
	private static final String[] srlBaseCmd = {
		"./external/swirl_parse_classify", "./external/model_swirl",
		"./external/model_charniak"};
	private static final String[] tags = {"A0", "A1", "A2", "A3", "A4", "A5",
		"AA", "AM-ADV", "AM-CAU", "AM-DIR", "AM-DIS", "AM-EXT", "AM-LOC",
		"AM-MNR", "AM-MOD", "AM-NEG", "AM-PNC", "AM-TMP", "C", "R", "V"};
	//private static final int maxSentLength = 120;
	private static final int maxSentLength = 60;

	private int maxDepth;
	private boolean ignoreBoundaryTags;

	public SemanticRoleAnnotationProvider(Corpus corpus, int maxDepth,
			boolean ignoreBoundaryTags)
	{
		super(corpus);
		this.maxDepth = Math.max(1, maxDepth);
		this.ignoreBoundaryTags = ignoreBoundaryTags;
	}

	@Override public String getKey()
	{
		return "SRL";
	}

	@Override public int getNumValues()
	{
		if (ignoreBoundaryTags)
			return tags.length + 1;
		else
			return (tags.length * 2) + 1;
	}

	@Override public int getNumAnnot()
	{
		return maxDepth;
	}

	@Override protected void performAnnotation()
	{
		// create input file
		File inputFile = new File(corpus.getDocumentId() + ".tmp");
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(new FileOutputStream(inputFile));
		}
		catch (IOException ex)
		{
			System.err.printf("error creating input file for SRL: %s\n",
					ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException();
		}

		for (Sentence s : corpus)
		{
			writer.print("3 ");
			for (int i = 0; i < Math.min(s.length(), maxSentLength); i++)
			{
				if (i > 0)
					writer.print(' ');
				String word = s.get(i).getWord();
				if (word.equals("\""))
					writer.print("\\\"");
				else
					writer.print(s.get(i).getWord());
			}
			writer.println();
		}
		writer.close();

		// run SwiRL
		Runtime env = Runtime.getRuntime();
		Process proc = null;
		String[] cmd = new String[srlBaseCmd.length + 1];
		System.arraycopy(srlBaseCmd, 0, cmd, 0, srlBaseCmd.length);
		cmd[srlBaseCmd.length] = inputFile.getPath();
		try
		{
			proc = env.exec(cmd, srlEnv);
		}
		catch (IOException ex)
		{
			System.err.printf("error starting SRL: %s", ex.getMessage());
			inputFile.delete();
			// return;
			throw new RuntimeException();
		}

		StreamWatcher stderrWatcher = new StreamWatcher(proc.getErrorStream());
		stderrWatcher.start();
		OutputStreamParser stdoutParser = new OutputStreamParser(
				proc.getInputStream());
		stdoutParser.start();

		try
		{
			proc.waitFor();
		}
		catch (InterruptedException ex)
		{
			proc.destroy();
		}
		finally
		{
			Thread.interrupted();
		}

		inputFile.delete();
	}

	@Override public String getConfigurationString()
	{
		StringBuffer buf = new StringBuffer();
		buf.append("max. depth = ");
		buf.append(maxDepth);
		if (!ignoreBoundaryTags)
			buf.append(", BIO2 encoding");
		return buf.toString();
	}

}

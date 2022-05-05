package de.tum.in.SentimentAnalysis;

import de.tum.in.Util.StreamWatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


public class ChunkAnnotationProvider extends AnnotationProvider {

	private class InputStreamFeeder extends Thread
	{
		private BufferedWriter writer;
		private Corpus corpus;

		public InputStreamFeeder(OutputStream out, Corpus corpus)
		{
			writer = new BufferedWriter(new OutputStreamWriter(out));
			this.corpus = corpus;
		}

		@Override public void run()
		{
			try
			{
				for (Sentence s : corpus)
				{
					for (Word w : s)
					{
						String tag = PoSAnnotationProvider.getTagName(
								w.getAnnotation("PoS0"));
						writer.write(w.getWord() + "\t" + tag);
						writer.newLine();
					}
					writer.newLine();
					writer.flush();
				}
				writer.close();
			}
			catch (IOException ex)
			{
				System.err.printf("IO error: %s\n", ex.getMessage());
			}
		}
	}

	private class OutputStreamParser extends Thread
	{
		private BufferedReader reader;
		private Corpus corpus;

		public OutputStreamParser(InputStream in, Corpus corpus)
		{
			reader = new BufferedReader(new InputStreamReader(in));
			this.corpus = corpus;
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
				ArrayList<Integer> tagSequence = new ArrayList<Integer>();
				while ((line = reader.readLine()) != null)
				{
					if (line.length() < 1)
					{
						// empty line is sentence delimiter
						int[] tmp = new int[tagSequence.size()];
						for (int i = 0; i < tagSequence.size(); i++)
							tmp[i] = tagSequence.get(i);
						annotateSentence(s, tmp, 0);

						if (!data.hasNext())
							break;
						s = data.next();
						tagSequence = new ArrayList<Integer>();
					}
					else
					{
						String[] column = line.split("\\s");
						if (column.length != 3)
						{
							System.err.printf("invalid chunker output \"%s\"\n",
									line);
							continue;
						}

						String[] parts = column[2].split("\\-");
						if (parts.length == 2)
						{
							int tagIdx = Arrays.binarySearch(tags, parts[1]);
							if (tagIdx < 0)
							{
								System.err.printf("unknown tag \"%s\"\n",
										parts[1]);
								continue;
							}
							if (ignoreBoundaryTags || (parts[0].equals("B")))
								tagSequence.add(tagIdx + 1);
							else
								tagSequence.add(tagIdx + tags.length + 1);
						}
						else
							tagSequence.add(0);  // "O"
					}
				}
			}
			catch (IOException ex)
			{
				System.err.printf("IO error: %s\n", ex.getMessage());
			}
		}		
	}


	private static final String chunkerCmd =
			"./external/yamcha -m ./external/general.model";

	private static final String[] tags = {"ADJP", "ADVP", "CONJP", "INTJ",
			"LST", "NP", "PP", "PRT", "SBAR", "UCP", "VP"};

	private boolean ignoreBoundaryTags;

	public ChunkAnnotationProvider(Corpus corpus, boolean ignoreBoundaryTags)
	{
		super(corpus);
		this.ignoreBoundaryTags = ignoreBoundaryTags;
	}

	@Override public String getKey()
	{
		return "chunk";
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
		return 1;
	}

	@Override protected void performAnnotation()
	{
		Runtime env = Runtime.getRuntime();
		Process proc = null;
		try
		{
			proc = env.exec(chunkerCmd);
		}
		catch (IOException ex)
		{
			System.err.println("error starting chunker");
			return;
		}

		InputStreamFeeder stdinFeeder = new InputStreamFeeder(
				proc.getOutputStream(),	corpus);
		stdinFeeder.start();
		StreamWatcher stderrWatcher = new StreamWatcher(proc.getErrorStream());
		stderrWatcher.start();
		OutputStreamParser stdoutParser = new OutputStreamParser(
				proc.getInputStream(), corpus);
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
	}

	@Override public String getConfigurationString()
	{
		String conf = "CoNLL 2000 shared task tagset";
		if (!ignoreBoundaryTags)
			conf += ", BIO2 encoding";
		return conf;
	}

	public static int getNumTagsUnencoded()
	{
		return tags.length;
	}

}

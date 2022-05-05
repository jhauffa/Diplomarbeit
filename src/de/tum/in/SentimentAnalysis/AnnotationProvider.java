package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public abstract class AnnotationProvider {

	protected Corpus corpus;

	private final String annotationSeparator = " ";
	private BufferedWriter cacheWriter;

	public AnnotationProvider(Corpus corpus)
	{
		this.corpus = corpus;
	}

	public abstract String getKey();
	public abstract int getNumValues();
	public abstract int getNumAnnot();
	protected abstract void performAnnotation();

	public String getConfigurationString()
	{
		return null;
	}

	public void retrieveAnnotations()
	{
		File annotCache = new File(getKey() + Integer.toString(getNumAnnot()) +
				"-" + corpus.getDocumentId() + ".cache");
		if (annotCache.exists())
			loadFromCache(annotCache);
		else
			annotateAndCacheResult(annotCache);
	}

	protected boolean annotateSentence(Sentence s, int[] annotations,
			int annotIdx)
	{
		boolean match = true;
		if ((annotations == null) || (annotations.length != s.length()))
		{
			if ((annotations != null) && (annotations.length > 0))
			{
				System.err.printf("\nannotation mismatch\n");
				System.err.printf("original sentence: \"%s\"\n",
						s.getSentenceAsString());
			}

			// prepare dummy annotation sequence, used as placeholder in cache
			annotations = new int[s.length()];
			Arrays.fill(annotations, -1);
			match = false;
		}
		else
		{
			String curAnnotKey = getKey() + Integer.toString(annotIdx);
			int idx = 0;
			for (Word w : s)
			{
				if (annotations[idx] >= 0)
					w.setAnnotation(curAnnotKey, annotations[idx]);
				idx++;
			}
		}

		if (cacheWriter != null)
			writeSentenceAnnotations(cacheWriter, annotations);
		return match;
	}

	private void annotateAndCacheResult(File cache)
	{
		try
		{
			cacheWriter = new BufferedWriter(new FileWriter(cache));
			System.err.printf("performing %s annotation...\n", getKey());
			performAnnotation();
			cacheWriter.close();
		}
		catch (IOException ex)
		{
			System.err.printf("Warning: could not create cache file \"%s\"\n",
					cache.getName());
		}
	}

	private void writeSentenceAnnotations(BufferedWriter writer,
			int[] annotations)
	{
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < annotations.length; i++)
		{
			if (i > 0)
				buf.append(" ");
			buf.append(annotations[i]);
		}

		try
		{
			writer.write(buf.toString());
			writer.newLine();
		}
		catch (IOException ex)
		{
			System.err.printf("Warning: cache write error \"%s\"\n",
					ex.getMessage());
		}
	}

	private void loadFromCache(File cache)
	{
		Iterator<Sentence> data = corpus.iterator();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(cache));
			int l = 0;
			while (data.hasNext())
			{
				Sentence s = data.next();

				for (int i = 0; i < getNumAnnot(); i++)
				{
					String[] annotations = null;
					String line = reader.readLine();
					l++;
					if (line == null)
					{
						System.err.printf("Warning: unexpected end of file, " +
								"%d. annotation \"%s\"\n", i + 1, getKey());
						reader.close();
						return;
					}
					annotations = line.split(annotationSeparator);
					int[] annotationValues = new int[annotations.length];
					for (int j = 0; j < annotations.length; j++)
						annotationValues[j] = Integer.parseInt(annotations[j]);
					if (!annotateSentence(s, annotationValues, i))
					{
						System.err.printf("Warning: mismatch between corpus " +
								"and cache file for annotation \"%s\" on line" +
								" %d\n", getKey(), l);
						reader.close();
						return;
					}
				}
			}
			reader.close();
		}
		catch (Exception ex)
		{
			System.err.printf("Warning: error reading cache file for " +
					"annotation \"%s\", error \"%s\"\n", getKey(),
					ex.getMessage());
			ex.printStackTrace();
		}
	}

}

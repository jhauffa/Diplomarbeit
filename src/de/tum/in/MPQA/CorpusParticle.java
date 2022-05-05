package de.tum.in.MPQA;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import de.tum.in.SentimentAnalysis.Sentence;


public class CorpusParticle implements Iterable<Sentence> {

	public class SentenceIterator implements Iterator<Sentence>
	{
		private BufferedReader document;
		private int currentDocumentOffset;
		private Sentence currentSentence;
		private int sentenceIdx;

		public SentenceIterator()
		{
			try
			{
				document = new BufferedReader(new FileReader(documentFile));
			}
			catch (FileNotFoundException e)
			{
				// File has vanished between construction of CorpusParticle and
				// call to CorpusParticle.iterator...
				currentSentence = null;
				return;
			}

			sentenceIdx = 0;
			currentDocumentOffset = 0;
			readNextSentence();
		}

		private String readFromDocument(int offset, int length)
		{
			int numSkip = offset - currentDocumentOffset;
			if (numSkip < 0)
				return null;

			char[] buf = new char[length];
			int numRead = 0;
			try
			{
				document.skip(numSkip);
				numRead = document.read(buf, 0, length);
				currentDocumentOffset += numSkip + numRead;
			}
			catch (IOException e)
			{
				return null;
			}

			if (numRead != length)
				return null;

			return new String(buf);
		}
		
		private void readNextSentence()
		{
			currentSentence = null;
			while ((currentSentence == null) &&
				   (sentenceIdx < sentenceAnnotations.size()))
			{
				SentenceAnnotation r = sentenceAnnotations.get(sentenceIdx);
				int offset = r.getStart(); 
				int length = r.getEnd() - offset;

				String rawSentence = readFromDocument(offset, length);
				if (rawSentence != null)
					currentSentence = AnnotationMatcher.processSentence(
							rawSentence, offset, sentimentAnnotations);

				sentenceIdx++;
			}
		}

		public boolean hasNext()
		{
			return (currentSentence != null);
		}

		public Sentence next()
		{
			Sentence s = currentSentence;
			if (s != null)
				readNextSentence();
			return s;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}


	private final File documentFile;
	public final boolean onlyDirect;
	public final boolean onlyExpressive;

	private ArrayList<SentenceAnnotation> sentenceAnnotations;
	private ArrayList<SentimentAnnotation> sentimentAnnotations;


	public CorpusParticle(File documentDir, File annotDir, String particleName,
			boolean onlyDirect, boolean onlyExpressive)
		throws FileNotFoundException, SyntaxError
	{
		this.onlyDirect = onlyDirect;
		this.onlyExpressive = onlyExpressive;

		documentFile = new File(documentDir, particleName);
		if (!documentFile.exists())
			throw new FileNotFoundException(documentFile.getPath());

		File annotSentencesFile = new File(annotDir, particleName +
				File.separator + "gatesentences.mpqa.2.0");
		sentenceAnnotations = parseAnnotationFile(annotSentencesFile,
				SentenceAnnotation.class);

		File annotAttributesFile = new File(annotDir, particleName +
				File.separator + "gateman.mpqa.lre.2.0");
		sentimentAnnotations = parseAnnotationFile(annotAttributesFile,
				SentimentAnnotation.class);
	}

	private <T extends Annotation> ArrayList<T> parseAnnotationFile(File file,
		Class<T> c)
		throws FileNotFoundException, SyntaxError
	{
		ArrayList<T> rangeList = new ArrayList<T>();
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line;
		int lineNumber = 0;
		try
		{
			while ((line = reader.readLine()) != null)
			{
				if (line.charAt(0) != '#')
				{
					T annotation;
					try
					{
						annotation = c.newInstance();
					}
					catch (Exception e)
					{
						throw new RuntimeException(e.getMessage());
					}

					if (!annotation.parse(this, line))
						throw new SyntaxError(file, lineNumber);
					if (!annotation.isEmpty())
						rangeList.add(annotation);
				}
				lineNumber++;
			}
		}
		catch (IOException e)
		{
			throw new SyntaxError(file, lineNumber);
		}

		Collections.sort(rangeList, new CompareAnnotations());
		return rangeList;
	}

	public Iterator<Sentence> iterator()
	{
		return new SentenceIterator();
	}

}

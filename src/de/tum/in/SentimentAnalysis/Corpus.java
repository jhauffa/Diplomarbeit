package de.tum.in.SentimentAnalysis;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Corpus implements Iterable<Sentence> {

	protected ArrayList<Sentence> sentences;

	private String documentId;
	private int numClasses;

	
	protected Corpus()
	{
		this(0);
	}

	public Corpus(int numClasses)
	{
		this.sentences = new ArrayList<Sentence>();
		this.numClasses = numClasses;
	}

	public Corpus(Corpus other)
	{
		this.sentences = new ArrayList<Sentence>();
		this.numClasses = other.numClasses;
		this.documentId = other.documentId;
		appendCopy(other);
	}

	protected void add(Sentence s)
	{
		sentences.add(s);
		documentId = null;
	}

	public String getDocumentId()
	{
		if (documentId == null)
		{
			// initialize MD5 algorithm
			MessageDigest algo = null;
			try
			{
				algo = MessageDigest.getInstance("MD5");
			}
			catch (NoSuchAlgorithmException ex)
			{
				throw new RuntimeException(ex.getMessage());
			}
			algo.reset();

			// process sentences
			for (Sentence s : sentences)
				algo.update(s.getSentenceAsString().getBytes());

			// retrieve MD5 digest as hex string
			byte[] md5Digest = algo.digest();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < md5Digest.length; i++)
				buf.append(Integer.toHexString(md5Digest[i] & 0xff));
			documentId = buf.toString();
		}
		return documentId;
	}

	public int size()
	{
		return sentences.size();
	}

	public void append(Iterable<Sentence> source)
	{
		for (Sentence s : source)
			add(s);
	}

	public void appendCopy(Iterable<Sentence> source)
	{
		for (Sentence s : source)
			add(new Sentence(s));
	}

	public Iterator<Sentence> iterator()
	{
		return sentences.iterator();
	}

	public int getNumClasses()
	{
		return numClasses;
	}

	protected void setNumClasses(int num)
	{
		numClasses = num;
	}

	public void shuffle()
	{
		Collections.shuffle(sentences);
		documentId = null;
	}

	public Corpus[] randomPartition(int n)
	{
		if (n <= 0)
			throw new IllegalArgumentException();
		int subsetSize = sentences.size() / n;
		if (subsetSize == 0)
			throw new IllegalArgumentException("not enough sentences");

		ArrayList<Sentence> shuffledSentences =
			new ArrayList<Sentence>(sentences);
		Collections.shuffle(shuffledSentences);

		Corpus[] parts = new Corpus[n];
		for (int i = 0; i < n; i++)
		{
			ArrayList<Sentence> subset = new ArrayList(
					shuffledSentences.subList(i * subsetSize,
							(i+1) * subsetSize));
			parts[i] = new Corpus(numClasses);
			parts[i].append(subset);
		}

		return parts;
	}


	private static final int numStratQuantSteps = 3;  // min = 2
	private static final int maxSentenceLength = 60;

	public Corpus[] randomStratifiedPartition(int n)
	{
		if (n <= 0)
			throw new IllegalArgumentException();
		int subsetSize = sentences.size() / n;
		if (subsetSize == 0)
			throw new IllegalArgumentException("not enough sentences");

		// sort sentences into buckets according to their label distribution;
		// one bucket for each combination of sentence length and distribution
		// of classes; all values are quantized into numStratQuantSteps steps
		int numBuckets = (int) Math.pow(numStratQuantSteps, numClasses);
		ArrayList<Sentence>[] buckets = new ArrayList[numBuckets];
		for (int i = 0; i < numBuckets; i++)
			buckets[i] = new ArrayList<Sentence>();
		int[] distr = new int[numClasses];
		for (Sentence s : sentences)
		{
			// generate histogram of label distribution
			for (int i = 0; i < numClasses; i++)
				distr[i] = 0;
			int len = 0;
			for (Word w : s)
			{
				distr[w.getTrueLabel()]++;
				len++;
			}

			// find best matching bucket
			double v = len / maxSentenceLength;
			if (v > 1.0)
				v = 1.0;
			int bucketIdx = (int) Math.round(v * (numStratQuantSteps - 1));
			int idxBase = numStratQuantSteps;
			for (int i = 0; i < (numClasses - 1); i++)
			{
				if (len > 0)
					v = (double) distr[i] / len;
				else
					v = 0.0;
				len -= distr[i];
				int q = (int) Math.round(v * (numStratQuantSteps - 1));
				if (q > 0)
					bucketIdx += idxBase + q - 1;
				idxBase *= numStratQuantSteps;
			}
			buckets[bucketIdx].add(s);
		}

		// shuffle each bucket
		for (ArrayList<Sentence> bucket : buckets)
			Collections.shuffle(bucket);

		// create subsets that contain equal amounts of sentences from each
		// bucket; the sentences in the resulting subsets will be sorted by
		// label distribution, but that should not affect the performance of the
		// learning algorithms used
		ArrayList<Sentence>[] subsets = new ArrayList[n];
		for (int i = 0; i < n; i++)
			subsets[i] = new ArrayList<Sentence>();
		for (ArrayList<Sentence> bucket : buckets)
		{
			// choose first subset randomly to deal with bucket sizes that are
			// not evenly divisible by n
			int idx = (int) Math.floor(n * Math.random());
			for (Sentence s : bucket)
			{
				subsets[idx].add(s);
				if (++idx >= n)
					idx = 0;
			}
		}

		Corpus[] parts = new Corpus[subsets.length];
		for (int i = 0; i < parts.length; i++)
		{
			parts[i] = new Corpus(numClasses);
			parts[i].append(subsets[i]);
		}
		return parts;
	}

}

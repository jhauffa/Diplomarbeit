package de.tum.in.SentimentAnalysis;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.Locale;
import java.util.Vector;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;


public class PolarityClassifierHMM implements Classifier {

	private class WordStat
	{
		public double p[];
		
		public WordStat()
		{
			p = new double[numStates];
		}

		public WordStat(int seenInState)
		{
			this();
			p[seenInState] = 1.0;
		}
	}

	private class LatticeNode
	{
		public int prev;
		public double p;

		public LatticeNode(int prev, double p)
		{
			this.prev = prev;
			this.p = p;
		}
	}

	private static final double LOG0 = -1.0 * Double.MAX_VALUE;

	private int numStates;
	private double[] pStart;
	private double[][] pTransit;
	private Hashtable<String, WordStat> words;
	private boolean[][] constr;


	public void setTransitionConstraints(boolean[][] constr)
	{
		this.constr = constr;
	}

	public void getFeatureNames(Vector<String> names)
	{
		// nothing to do
	}

	public void getFeatureWeights(int fromState, int toState,
			Vector<Integer> indices, Vector<Double> weights)
	{
		// nothing to do
	}

	public void getFeatureWeights(Sentence sentence, int pos,
			Vector<Integer> indices, Vector<Double> weights)
	{
		// nothing to do
	}

	public void applyClassBias(int classIndex, double biasValue, BiasType type)
		throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public void train(Corpus corpus, double regularizationParam)
			throws Exception
	{
		numStates = corpus.getNumClasses();
		pStart = new double[numStates];
		pTransit = new double[numStates][numStates];
		words = new Hashtable<String, WordStat>();

		int fTransitFrom[] = new int[numStates];
		int fState[] = new int[numStates];
		int numSentences = 0;

		for (Sentence s : corpus)
		{
			if (s.length() <= 0)
				continue;
			pStart[s.get(0).getTrueLabel()] += 1.0;
			for (int i = 0; i < s.length(); i++)
			{
				int trueLabel = s.get(i).getTrueLabel();
				if (i < (s.length() - 1))
				{
					pTransit[trueLabel][s.get(i+1).getTrueLabel()] += 1.0;
					fTransitFrom[trueLabel]++;
				}

				String word = s.get(i).getWord();
				WordStat stat = words.get(word);
				if (stat != null)
					stat.p[trueLabel] += 1.0;
				else
					words.put(word, new WordStat(trueLabel));

				fState[trueLabel]++;
			}
			numSentences++;
		}
		if (numSentences <= 0)
			throw new Exception("empty corpus");

		for (int i = 0; i < numStates; i++)
		{
			if (pStart[i] > 0.0)
				pStart[i] = Math.log(pStart[i] / numSentences);
			else
				pStart[i] = LOG0;

			for (int j = 0; j < numStates; j++)
			{
				if ((constr != null) && (constr[i][j]))
					pTransit[i][j] = Double.NEGATIVE_INFINITY;
				else if (fTransitFrom[i] > 0)
					pTransit[i][j] = Math.log(pTransit[i][j] / fTransitFrom[i]);
				else
					pTransit[i][j] = LOG0;
			}
		}

		Iterator<WordStat> it = words.values().iterator();
		while (it.hasNext())
		{
			WordStat stat = it.next();
			for (int i = 0; i < numStates; i++)
			{
				if (fState[i] > 0)
					stat.p[i] = Math.log(stat.p[i] / fState[i]);
				else
					stat.p[i] = LOG0;
			}
		}
	}

	public void trainWithEarlyStopping(Corpus trainingSet, Corpus testSet,
			double regularizationParam)
		throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}

	public boolean hasStoppedEarly()
	{
		return false;
	}

	public void process(Corpus corpus)
	{
		if (numStates == 0)
			throw new RuntimeException("HMM not trained");

		for (Sentence s : corpus)
		{
			LatticeNode lattice[][] =
				new LatticeNode[s.length() + 1][numStates];
			for (int i = 0; i < numStates; i++)
				lattice[0][i] = new LatticeNode(-1, pStart[i]);
			for (int i = 0; i < s.length(); i++)
			{
				for (int j = 0; j < numStates; j++)
				{
					int bestPrev = 0;
					double bestProb = LOG0;

					for (int k = 0; k < numStates; k++)
					{
						double pEmit;
						WordStat stat = words.get(s.get(i).getWord());
						if (stat != null)
							pEmit = stat.p[j];
						else
							pEmit = LOG0;
						double p = lattice[i][k].p + pTransit[k][j] + pEmit;
						if (p > bestProb)
						{
							bestProb = p;
							bestPrev = k;
						}
					}

					lattice[i+1][j] = new LatticeNode(bestPrev, bestProb);
				}
			}
			
			int pos = s.length();
			int bestState = 0;
			double bestProb = LOG0;
			for (int i = 0; i < numStates; i++)
			{
				if (lattice[pos][i].p > bestProb)
				{
					bestProb = lattice[pos][i].p;
					bestState = i;
				}
			}
			s.get(pos - 1).setLabel(bestState);
			for (int i = pos - 1; i > 0; i--)
			{
				bestState = lattice[i][bestState].prev;
				s.get(i - 1).setLabel(bestState);
			}	
		}
	}

	public void loadModel(InputStream in) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		numStates = Integer.parseInt(reader.readLine());

		String[] parts = reader.readLine().split("\\s");
		if (parts.length != numStates)
			throw new IOException("invalid data: start probabilities");
		for (int i = 0; i < numStates; i++)
			pStart[i] = Double.parseDouble(parts[i]);

		for (int i = 0; i < numStates; i++)
		{
			parts = reader.readLine().split("\\s");
			if (parts.length != numStates)
				throw new IOException("invalid data: transition probabilities");
			for (int j = 0; j < numStates; j++)
				pTransit[i][j] = Double.parseDouble(parts[j]);			
		}

		String line;
		while ((line = reader.readLine()) != null)
		{
			parts = line.split("\\s");
			if (parts.length != (numStates + 1))
				throw new IOException("invalid data: emission probabilities");
			WordStat w = new WordStat();
			words.put(parts[0], w);
			for (int i = 0; i < numStates; i++)
				w.p[i] = Double.parseDouble(parts[i + 1]);
		}
	}

	public void saveModel(OutputStream out) throws IOException
	{
        PrintStream p = new PrintStream(out);
        p.println(numStates);
        for (int i = 0; i < numStates; i++)
        	p.printf(Locale.US, "%f ", pStart[i]);
        p.println();
        for (int i = 0; i < numStates; i++)
        {
        	for (int j = 0; j < numStates; j++)
        		p.printf(Locale.US, "%f ", pTransit[i][j]);
        	p.println();
        }
		Set<Map.Entry<String, WordStat>> emissions = words.entrySet();
		for (Map.Entry<String, WordStat> e : emissions)
		{
			p.printf("%s ", e.getKey());
			for (int i = 0; i < numStates; i++)
				p.printf(Locale.US, "%f ", e.getValue().p[i]);
			p.println();
		}
	}

	public void printModel(OutputStream out) throws IOException
	{
		PrintStream p = new PrintStream(out);
		p.printf("number of hidden states = %d\n", numStates);
		p.println("start probabilities:");
		for (int i = 0; i < numStates; i++)
			p.printf("%f ", Math.exp(pStart[i]));
		p.println();
		p.println("transition probabilities:");
		for (int i = 0; i < numStates; i++)
		{
			for (int j = 0; j < numStates; j++)
				p.printf("%f ", Math.exp(pTransit[i][j]));
			p.println();
		}
		p.println();
		p.println("emission probabilities:");
		Set<Map.Entry<String, WordStat>> emissions = words.entrySet();
		for (Map.Entry<String, WordStat> e : emissions)
		{
			p.printf("%s: ", e.getKey());
			for (int i = 0; i < numStates; i++)
				p.printf("%f ", Math.exp(e.getValue().p[i]));
			p.println();
		}
	}

}

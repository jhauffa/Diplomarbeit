package de.tum.in.SentimentAnalysis;

public class SampledCorpus extends Corpus {

	SampledCorpus(Corpus originalCorpus, boolean subSample,
			int neighborhoodSize, boolean stratifyPosNeg)
	{
		super(originalCorpus.getNumClasses());

		Corpus[] minorityClassSamples = null;
		if (stratifyPosNeg)
		{
			int numClasses = originalCorpus.getNumClasses();
			minorityClassSamples = new Corpus[numClasses - 1];
			for (int i = 0; i < minorityClassSamples.length; i++)
				minorityClassSamples[i] = new Corpus(numClasses);
		}

		for (Sentence s : originalCorpus)
		{
			int polarStart = -1;
			for (int i = 0; i < s.length(); i++)
			{
				if (s.get(i).getTrueLabel() != 0)
				{
					if (polarStart == -1)
						polarStart = i;
				}
				else if (polarStart >= 0)
				{
					if (neighborhoodSize == -1)
						neighborhoodSize =
							(int) Math.round((i - polarStart) / 4.0);

					int extractStart = Math.max(0, polarStart-neighborhoodSize);
					int p = polarStart - 1;
					while ((p >= extractStart) &&
						   (s.get(p).getTrueLabel() == 0))
						p--;
					extractStart = p + 1;

					int extractEnd = Math.min(i + neighborhoodSize, s.length());
					p = i;
					while ((p < extractEnd) && (s.get(p).getTrueLabel() == 0))
						p++;
					extractEnd = p;

					Sentence sample = new Sentence();
					for (int j = extractStart; j < extractEnd; j++)
						sample.append(s.get(j));

					if (stratifyPosNeg)
					{
						int c = s.get(polarStart).getTrueLabel();
						minorityClassSamples[c - 1].add(sample);
					}
					else
						add(sample);
					if (!subSample)
						add(s);

					polarStart = -1;
				}
			}
		}

		if (stratifyPosNeg)
		{
			for (Corpus c : minorityClassSamples)
				append(c);
			int diff = minorityClassSamples[0].size() -
					minorityClassSamples[1].size();
			if (diff > 0)
			{
				minorityClassSamples[1].shuffle();
				for (int i = 0; i < diff; i++)
					add(minorityClassSamples[1].sentences.get(i));
			}
			else if (diff < 0)
			{
				minorityClassSamples[0].shuffle();
				for (int i = 0; i < -diff; i++)
					add(minorityClassSamples[0].sentences.get(i));
			}
			shuffle();
		}
	}

}

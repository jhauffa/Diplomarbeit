package de.tum.in.SentimentAnalysis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public class SubjectivityAnnotationGenerator {

	private static final String annotKey = "subjectivity0";

	private double biasValue;
	private FeatureGenerator featureGen;
	private Classifier classifier;

	public SubjectivityAnnotationGenerator()
	{
		this(0.0);
	}

	public SubjectivityAnnotationGenerator(double biasValue)
	{
		this.biasValue = biasValue;

		featureGen = createFeatureGenerator();
		classifier = PolarityClassifierFactory.create("CRF", featureGen, false);
	}

	public void train(Corpus corpus)
	{
		// Use one half of the training data to train the subjectivity
		// classifier. This will still result in an overestimation of the
		// classifier's accuracy, but it is a necessary compromise considering
		// the small training corpus.
		Corpus parts[] = corpus.randomPartition(2);
		Corpus trainingCorpus = convertCorpus(parts[0]);

		try
		{
			System.err.println("\ntraining subjectivity classifier...");
			classifier.train(trainingCorpus, 0.0);
			System.err.println("done!");
			if (biasValue != 0.0)
				classifier.applyClassBias(0, biasValue,
						Classifier.BiasType.BIAS_FEATURE);
		}
		catch (Exception ex)
		{
			System.err.printf("error training subjectivity classifier: %s\n",
					ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void generateAnnotations(Corpus corpus)
	{
		Corpus annotCorpus = convertCorpus(corpus);

		try
		{
			classifier.process(annotCorpus);
		}
		catch (Exception ex)
		{
			System.err.printf("error generating subjectivity annotations: %s\n",
					ex.getMessage());
			ex.printStackTrace();
			return;
		}

		Iterator<Sentence> origIter = corpus.iterator();
		Iterator<Sentence> subjIter = annotCorpus.iterator();
		while (origIter.hasNext())
		{
			assert subjIter.hasNext();
			Sentence origSentence = origIter.next();
			Sentence subjSentence = subjIter.next();

			assert origSentence.length() == subjSentence.length();
			for (int i = 0; i < subjSentence.length(); i++)
				origSentence.get(i).setAnnotation(annotKey,
						subjSentence.get(i).getLabel());
		}

	}

	private Corpus convertCorpus(Corpus sourceCorpus)
	{
		Corpus destCorpus = new Corpus(sourceCorpus.getNumClasses());
		destCorpus.appendCopy(sourceCorpus);
		LabelEncoding enc = new LabelEncoding(2, "no");
		enc.encode(destCorpus);
		return destCorpus;
	}

	private static FeatureGenerator createFeatureGenerator()
	{
		FeatureGenerator featureGen = new FeatureGenerator();
		NGramFeatures unigramFeatures = new NGramFeatures(1, false, false,
				NGramFeatures.Direction.FORWARD, 0, 0, 0);
		featureGen.addFeatureTemplate(unigramFeatures);
		featureGen.addFeatureTemplate(
				new FeaturePositionShifter(unigramFeatures, -1));
		featureGen.addFeatureTemplate(
				new FeaturePositionShifter(unigramFeatures, 1));
		return featureGen;
	}

	public void loadParametersAndModel(InputStream paramIn, InputStream modelIn)
			throws IOException
	{
		featureGen.loadParameters(paramIn);
		classifier.loadModel(modelIn);
	}

	public void saveParametersAndModel(OutputStream paramOut,
			OutputStream modelOut) throws IOException
	{
		featureGen.saveParameters(paramOut);
		classifier.saveModel(modelOut);
	}

}

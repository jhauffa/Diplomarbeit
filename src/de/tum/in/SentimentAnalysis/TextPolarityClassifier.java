package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextPolarityClassifier {

	private FeatureGenerator featureGen;
	private Classifier classifier;
	private SubjectivityAnnotationGenerator subjAnnotGen;

	public TextPolarityClassifier(File featureParameters, File modelParameters,
			File subjParameters, File subjModel)
	{
		featureGen = new FeatureGenerator();
		classifier = PolarityClassifierFactory.create("CRF", featureGen, false);
		subjAnnotGen = new SubjectivityAnnotationGenerator();

		try
		{
			featureGen.loadParameters(new FileInputStream(featureParameters));
			classifier.loadModel(new FileInputStream(modelParameters));
			subjAnnotGen.loadParametersAndModel(
					new FileInputStream(subjParameters),
					new FileInputStream(subjModel));
		}
		catch (IOException ex)
		{
			System.err.printf("error loading model: %s\n", ex.getMessage());
		}
	}

	public Corpus processText(String text, boolean lineEndIsSentenceEnd)
	{
		Corpus corpus = new UntaggedCorpus(text, 4, lineEndIsSentenceEnd);
		subjAnnotGen.generateAnnotations(corpus);
		try
		{
			classifier.process(corpus);
		}
		catch (Exception ex)
		{
			System.err.printf("classification error: %s\n", ex.getMessage());
		}
		return corpus;
	}

	public static void main(String[] args)
	{
		// load training corpus
		if (args.length < 1)
		{
			System.err.println("not enough arguments");
			return;
		}
		File corpusFile = new File(args[0]);
		Corpus trainingCorpus;
		try
		{
			trainingCorpus = new SimpleCorpus(corpusFile);
		}
		catch (IOException ex)
		{
			System.err.printf("error loading corpus: %s\n", ex.getMessage());
			return;
		}

		// create feature generator
		PolarityClassificationFeatureSet featureSet =
				new PolarityClassificationFeatureSet(true);
		featureSet.setNGramMinimumRank(50);
		featureSet.setNGramWindowSize(1);
		featureSet.setWordListNeighborhoodType(
				WordListFeatures.NeighborhoodType.NEIGHBORHOOD_BOTH);
		featureSet.setWordListNeighborhoodSize(1);
		FeatureGenerator featureGen =
				featureSet.createFeatureGenerator(trainingCorpus); 

		// generate prior subjectivity annotations
		SubjectivityAnnotationGenerator subjAnnotGen =
			new SubjectivityAnnotationGenerator(0.7);
		subjAnnotGen.train(trainingCorpus);
		subjAnnotGen.generateAnnotations(trainingCorpus);

		// create and train classifier
		Classifier classifier = PolarityClassifierFactory.create("CRF",
				featureGen, false);
		try
		{
			classifier.train(trainingCorpus, 0.05);
			classifier.saveModel(new FileOutputStream("./model.param"));
			featureGen.saveParameters(new FileOutputStream("./features.param"));
			subjAnnotGen.saveParametersAndModel(
					new FileOutputStream("./subj.features.param"),
					new FileOutputStream("./subj.model.param"));
		}
		catch (Exception ex)
		{
			System.err.printf("error: %s\n", ex.getMessage());
			ex.printStackTrace();
		}
	}

}

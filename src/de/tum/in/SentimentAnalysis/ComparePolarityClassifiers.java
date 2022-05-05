package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import de.tum.in.Util.ArgumentParser;
import de.tum.in.Util.ArgumentParser.ArgumentType;
import de.tum.in.Util.Time;


public class ComparePolarityClassifiers {

	private static final int numClassifiers = 2;
	private static final int numCrossValidationRounds = 5;  // 5x2cv
	private static final int numCrossValidationSteps = 2;

	private static final int defaultNumPairwiseComp = 1;

	private static final char keyNumPairwiseComp = 'n';
	private static final char keyModelName = 'm';
	private static final char keyFeatures = 'f';
	private static final char keyMinFreqRank = 'x';
	private static final char keyNeighborhoodType = 'k';
	private static final char keyNeighborhoodSize = 'l';
	private static final char keyPriorSubjBias = 'j';
	private static final char keyRegularizationParam = 'z';
	private static final char keyBiasFeatureWeight = 'b';
	private static final char keyBiasWeightFactor = 'w';
	private static final char keyNGramWindowSize = 's';
	private static final char keyPolarityWordLists = 'p';

	private static ArgumentParser parseArguments(String[] args)
	{
		ArgumentParser parser =
			new ArgumentParser("ComparePolarityClassifiers");
		parser.addArgument(keyNumPairwiseComp,
				"overall number of pairwise comparisons", ArgumentType.INT);
		parser.addArgument(keyModelName,
				"sequence classification model for classifier 1/2:\n\t\t\t" +
				PolarityClassifierFactory.getModelNames() + "\n\t\t\tdefault: "+
				PolarityClassifierFactory.getDefaultModelName(),
				ArgumentType.STRING, numClassifiers);
		parser.addArgument(keyFeatures,
				"choose composition of feature set for classifier 1/2:\n" +
				"\t\t\tset is a string that may contain the letters\n" +
				"\t\t\t'1' (statistics), '2' (morphology), '3' (syntax),\n"+
				"\t\t\t'4' (semantics), and 's' (simplified features)\n" +
				"\t\t\tdefault: 1234", ArgumentType.STRING, numClassifiers);
		parser.addArgument(keyBiasFeatureWeight,
				"introduce bias feature to all transitions to the\n" +
				"\t\t\t\"neutral\" state, weight = x", ArgumentType.DOUBLE,
				numClassifiers);
		parser.addArgument(keyBiasWeightFactor,
				"multiply all features for transitions to the \"neutral\"\n" +
				"\t\t\tstate by x", ArgumentType.DOUBLE, numClassifiers);
		parser.addArgument(keyMinFreqRank,
				"do not generate features for n-grams with frequency\n" +
				"\t\t\trank < x\n\t\t\tdefault = 0", ArgumentType.INT,
				numClassifiers);
		parser.addArgument(keyNeighborhoodType,
				"word list neighborhood type\n" +
				"\t\t\tpossible values = l(eft), b(oth), c(hunk); default = c",
				ArgumentType.STRING, numClassifiers);
		parser.addArgument(keyNeighborhoodSize,
				"word list neighborhood length\n\t\t\tdefault = 0",
				ArgumentType.INT, numClassifiers);
		parser.addArgument(keyPriorSubjBias,
				"bias of prior subjectivity classifier\n\t\t\tdefault = 0.0",
				ArgumentType.DOUBLE, numClassifiers);
		parser.addArgument(keyRegularizationParam,
				"L1 regularization parameter\n\t\t\tdefault = 0.0",
				ArgumentType.DOUBLE, numClassifiers);
		parser.addArgument(keyNGramWindowSize,
				"n-gram window size\n\t\t\tdefault = 1",
				ArgumentType.INT, numClassifiers);
		parser.addArgument(keyPolarityWordLists,
				"use prior polarity word lists",
				ArgumentType.BOOLEAN, numClassifiers);
		parser.addAnonymousArgument("file",
				"\"file\" is a simplified MPQA corpus");

		parser.parse(args);
		return parser;
	}

	public static void main(String[] args)
	{
		ArgumentParser conf = parseArguments(args);
		File corpusFile = new File(conf.getAnonArgument(0));
		Corpus fullCorpus = null;
		try
		{
			System.err.println("loading corpus...");
			fullCorpus = new SimpleCorpus(corpusFile);
		}
		catch (FileNotFoundException ex)
		{
			System.err.println("file not found: " + ex.getMessage());
			System.exit(2);
		}

		// create feature generator and evaluator for each classifier
		FeatureGenerator[] featureGen = new FeatureGenerator[numClassifiers];
		CrossValidationEvaluator[] eval =
			new CrossValidationEvaluator[numClassifiers];
		String[] modelName = new String[numClassifiers];
		Classifier.BiasType biasType[] =
			new Classifier.BiasType[numClassifiers];
		double[] biasValue = new double[numClassifiers];
		for (int i = 0; i < numClassifiers; i++)
		{
			PolarityClassificationFeatureSet featureSet =
				new PolarityClassificationFeatureSet(false);
			String featureDesc = conf.getStringValue(keyFeatures, i, "1234");
			for (int j = 0; j < featureDesc.length(); j++)
			{
				char c = featureDesc.charAt(j);
				int idx = c - '1';
				if ((idx >= 0) && (idx < 4))
					featureSet.setGenerateLevelNFeatures(idx, true);
				else if (c == 's')
					featureSet.setGenerateSimpleFeatures(true);
			}

			featureSet.setNGramMinimumRank(
					conf.getIntValue(keyMinFreqRank, i, 0));
			featureSet.setNGramWindowSize(
					conf.getIntValue(keyNGramWindowSize, i, 1));
			String neighborhoodType = conf.getStringValue(keyNeighborhoodType,
					i, "c");
			switch (neighborhoodType.charAt(0))
			{
			case 'c':
				featureSet.setWordListNeighborhoodType(
						WordListFeatures.NeighborhoodType.NEIGHBORHOOD_CHUNK);
				break;
			case 'l':
				featureSet.setWordListNeighborhoodType(
						WordListFeatures.NeighborhoodType.NEIGHBORHOOD_LEFT);
				break;
			case 'b':
				featureSet.setWordListNeighborhoodType(
						WordListFeatures.NeighborhoodType.NEIGHBORHOOD_BOTH);
				break;
			}
			featureSet.setWordListNeighborhoodSize(
					conf.getIntValue(keyNeighborhoodSize, i, 0));
			featureSet.setIncludePolarityWordLists(
					conf.getBooleanValue(keyPolarityWordLists, i, false));

			featureGen[i] = featureSet.createFeatureGenerator(fullCorpus);

			eval[i] = new CrossValidationEvaluator();
			modelName[i] = conf.getStringValue(keyModelName, i,
					PolarityClassifierFactory.getDefaultModelName());

			biasValue[i] = conf.getDoubleValue(keyBiasFeatureWeight, i, 0.0);
			if (biasValue[i] == 0.0)
			{
				biasValue[i] = conf.getDoubleValue(keyBiasWeightFactor, i, 0.0);
				if (biasValue[i] != 0.0)
					biasType[i] = Classifier.BiasType.BIAS_WEIGHTS_FIXED;
			}
			else
				biasType[i] = Classifier.BiasType.BIAS_FEATURE;
		}

		try
		{
			// write report header
			String now = Time.getTimeStamp();
			String fileName = "cmp-" + now + ".log";
			PrintStream log = new PrintStream(fileName);
			log.printf("Polarity Classifier Comparison\n%s\n", now);
			for (int i = 0; i < numClassifiers; i++)
			{
				log.printf("\nfeatures of classifier %d (%s):\n\n", i + 1,
						modelName[i]);
				featureGen[i].printConfiguration(log);
			}

			// perform 5x2cv f-test
			Test5x2cvF testF = new Test5x2cvF(
					conf.getIntValue(keyNumPairwiseComp,
							defaultNumPairwiseComp), 4);
			CrossValidationProvider cv = new CrossValidationProvider(
					fullCorpus, numCrossValidationSteps, false, false);
			for (int i = 0; i < numCrossValidationRounds; i++)
			{
				cv.nextRound();
				System.err.printf("performing %s (%d/%d)\n",
						cv.getConfigurationString(), i + 1,
						numCrossValidationRounds);

				for (int j = 0; j < numCrossValidationSteps; j++)
				{
					System.err.printf("training instance %d...\n", j + 1);
					cv.nextFold();

					Corpus trainingCorpus = cv.getTrainingSet();
					Corpus[] testCorpusInst = new Corpus[numClassifiers];
					testCorpusInst[0] = cv.getTestSet();

					for (int k = 0; k < numClassifiers; k++)
					{
						System.err.printf("classifier %d/%d...\n", k + 1,
								numClassifiers);
						featureGen[k].reset();
						System.gc();

						SubjectivityAnnotationGenerator subjAnnotGen =
							new SubjectivityAnnotationGenerator(
									conf.getDoubleValue(keyPriorSubjBias, k,
											0.0));

						Classifier classifier =
							PolarityClassifierFactory.create(
									modelName[k], featureGen[k], false);

						System.err.println("training...");
						subjAnnotGen.train(trainingCorpus);
						subjAnnotGen.generateAnnotations(trainingCorpus);
						classifier.train(trainingCorpus,
								conf.getDoubleValue(keyRegularizationParam, k,
										0.0));

						System.err.println("processing...");
						if (biasValue[k] != 0.0)
							classifier.applyClassBias(0, biasValue[k],
									biasType[k]);
						if (k > 0)
							testCorpusInst[k] = new Corpus(testCorpusInst[0]);
						subjAnnotGen.generateAnnotations(testCorpusInst[k]);
						classifier.process(testCorpusInst[k]);
						eval[k].addCorpus(testCorpusInst[k]);
					}

					testF.append(testCorpusInst[0], testCorpusInst[1]);
				}
			}

			System.err.println("done!");

			// print result of 5x2cv f-test
			log.println();
			testF.evaluate();
			testF.printResults(log);

			// print general evaluation
			for (int i = 0; i < numClassifiers; i++)
			{
				log.printf("\nevaluation of classifier %d:\n", i + 1);
				eval[i].updateEvaluation();
				eval[i].printResults(log);
			}

			log.close();
			System.err.printf("report written to %s\n", fileName);
		}
		catch (Exception ex)
		{
			System.err.println("error: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(2);
		}
	}

}

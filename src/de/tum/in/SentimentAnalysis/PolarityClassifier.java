package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Vector;

import de.tum.in.Util.ArgumentParser;
import de.tum.in.Util.ArgumentParser.ArgumentType;
import de.tum.in.Util.Time;


public class PolarityClassifier {

	private static final int defaultNumLabels = 4;
	private static final int defaultNumCrossValidationSteps = 3;
	private static final int defaultNumCrossValidationRounds = 1;
	private static final String defaultEncodingScheme = "no";

	private static final char keyModelName = 'm';
	private static final char keyFeatures = 'f';
	private static final char keyMinFreqRank = 'x';
	private static final char keyNeighborhoodType = 'k';
	private static final char keyNeighborhoodSize = 'l';
	private static final char keyPriorSubjBias = 'j';
	private static final char keyRegularizationParam = 'z';
	private static final char keyBiasFeatureWeight = 'b';
	private static final char keyBiasWeightFactor = 'w';
	private static final char keyNumClasses = 'n';
	private static final char keyTagEncoding = 'e';
	private static final char keyCVSteps = 'c';
	private static final char keyCVRounds = 'r';
	private static final char keySubSampling = 's';
	private static final char keySuperSampling = 'p';
	private static final char keyBiasWeightVariable = 'v';
	private static final char keyFeatureInduction = 'i';
	private static final char keyEarlyStopping = 't';

	private static ArgumentParser parseArguments(String[] args)
	{
		ArgumentParser parser =
			new ArgumentParser("ComparePolarityClassifiers");
		parser.addArgument(keyModelName,
				"sequence classification model:\n\t\t" +
				PolarityClassifierFactory.getModelNames() + "\n\t\tdefault: " +
				PolarityClassifierFactory.getDefaultModelName(),
				ArgumentType.STRING);
		parser.addArgument(keyNumClasses,
				"number of polarity classes; valid values for x are:\n" +
				"\t\t2 (neutral/polar), 3 (neutral/positive/negative),\n" +
				"\t\t4 (neutral/positive/negative/both)\n\t\tdefault: " +
				Integer.toString(defaultNumLabels), ArgumentType.INT);
		parser.addArgument(keyTagEncoding,
				"encoding of tags; valid values for x are:\n\t\t" +
				LabelEncoding.getSchemeNames() + "\n\t\tdefault: " +
				defaultEncodingScheme, ArgumentType.STRING);
		parser.addArgument(keyCVSteps,
				"number of cross validation \"folds\"\n\t\tdefault: " +
				Integer.toString(defaultNumCrossValidationSteps),
				ArgumentType.INT);
		parser.addArgument(keyCVRounds,
				"number of cross validation rounds\n\t\tdefault: " +
				Integer.toString(defaultNumCrossValidationRounds),
				ArgumentType.INT);
		parser.addArgument(keyFeatures,
				"choose composition of feature set:\n" +
				"\t\tset is a string that may contain the letters\n" +
				"\t\t'1' (statistics), '2' (morphology), '3' (syntax),\n" +
				"\t\t'4' (semantics), and 's' (simplified features)\n" +
				"\t\tdefault: 1234", ArgumentType.STRING);
		parser.addArgument(keySubSampling,
				"subsample the neutral polarity class,\n" +
				"\t\tx is the neighborhood size; disabled by default",
				ArgumentType.INT);
		parser.addArgument(keySuperSampling,
				"supersample, see above", ArgumentType.INT);
		parser.addArgument(keyFeatureInduction,
				"enable feature induction", ArgumentType.BOOLEAN);
		parser.addArgument(keyEarlyStopping,
				"enable early stopping", ArgumentType.BOOLEAN);
		parser.addArgument(keyBiasFeatureWeight,
				"introduce bias feature to all transitions to the\n"+
				"\t\t\"neutral\" state, weight = x", ArgumentType.DOUBLE);
		parser.addArgument(keyBiasWeightFactor,
				"multiply all features for transitions to the \"neutral\" " +
				"state\n\t\tby x", ArgumentType.DOUBLE);
		parser.addArgument(keyBiasWeightVariable,
				"multiply all features for transitions to the \"neutral\" " +
				"state\n\t\tby a value, so that the sum of the weights " +
				"equals the sum of\n\t\tall weights for \"polar\" " +
				"features", ArgumentType.BOOLEAN);
		parser.addArgument(keyMinFreqRank,
				"do not generate features for n-grams with frequency rank < " +
				"x\n\t\tdefault = 0", ArgumentType.INT);
		parser.addArgument(keyNeighborhoodType,
				"word list neighborhood type\n" +
				"\t\tpossible values = l(eft), b(oth), c(hunk); default = c",
				ArgumentType.STRING);
		parser.addArgument(keyNeighborhoodSize,
				"word list neighborhood length\n\t\tdefault = 0",
				ArgumentType.INT);
		parser.addArgument(keyPriorSubjBias,
				"bias of prior subjectivity classifier\n\t\tdefault = 0.0",
				ArgumentType.DOUBLE);
		parser.addArgument(keyRegularizationParam,
				"L1 regularization parameter\n\t\tdefault = 0.0",
				ArgumentType.DOUBLE);
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

		String modelName = conf.getStringValue(keyModelName,
				PolarityClassifierFactory.getDefaultModelName());

		double biasValue = conf.getDoubleValue(keyBiasFeatureWeight, 0.0);
		Classifier.BiasType biasType = Classifier.BiasType.BIAS_FEATURE;
		if (biasValue == 0.0)
		{
			biasValue = conf.getDoubleValue(keyBiasWeightFactor, 0.0);
			if (biasValue != 0.0)
				biasType = Classifier.BiasType.BIAS_WEIGHTS_FIXED;
		}

		boolean performSubSampling = false;
		boolean performSuperSampling = false;
		int samplingNeighborhoodSize = conf.getIntValue(keySubSampling, -1);
		if (samplingNeighborhoodSize < 0)
		{
			samplingNeighborhoodSize = conf.getIntValue(keySuperSampling, -1);
			if (samplingNeighborhoodSize >= 0)
				performSuperSampling = true;
		}
		else
			performSubSampling = true;

		// apply encoding scheme
		LabelEncoding enc = new LabelEncoding(
				conf.getIntValue(keyNumClasses, defaultNumLabels),
				conf.getStringValue(keyTagEncoding, defaultEncodingScheme));
		enc.encode(fullCorpus);

		// create feature generator
		PolarityClassificationFeatureSet featureSet =
			new PolarityClassificationFeatureSet(false);
		String featureDesc = conf.getStringValue(keyFeatures, "1234");
		for (int j = 0; j < featureDesc.length(); j++)
		{
			char c = featureDesc.charAt(j);
			int idx = c - '1';
			if ((idx >= 0) && (idx < 4))
				featureSet.setGenerateLevelNFeatures(idx, true);
			else if (c == 's')
				featureSet.setGenerateSimpleFeatures(true);
		}

		featureSet.setNGramMinimumRank(conf.getIntValue(keyMinFreqRank, 0));
		featureSet.setNGramWindowSize(1);

		String neighborhoodType = conf.getStringValue(keyNeighborhoodType, "c");
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
				conf.getIntValue(keyNeighborhoodSize, 0));
		FeatureGenerator featureGen =
			featureSet.createFeatureGenerator(fullCorpus); 

		try
		{
			// write report header
			String now = Time.getTimeStamp();
			String fileName = "results-" + now + ".log";
			PrintStream log = new PrintStream(fileName);
			log.printf("Polarity Classification\n%s\n" +
					   "model = %s\n" +
					   "label encoding = %s\n\n",
					   now, modelName, enc.getSchemeDescription());
			log.printf("features:\n\n");
			featureGen.printConfiguration(log);

			int numEarlyStops = 0;

			Classifier classifier;
			CrossValidationEvaluator eval = new CrossValidationEvaluator();
			int numCrossValidationSteps = conf.getIntValue(keyCVSteps,
					defaultNumCrossValidationSteps);
			int numCrossValidationRounds = conf.getIntValue(keyCVRounds,
					defaultNumCrossValidationRounds);
			boolean performEarlyStopping =
				conf.getBooleanValue(keyEarlyStopping, false);
			CrossValidationProvider cv = new CrossValidationProvider(
					fullCorpus, numCrossValidationSteps, false,
					performEarlyStopping);
			for (int i = 0; i < numCrossValidationRounds; i++)
			{
				cv.nextRound();
				System.err.printf("performing %s (%d/%d)\n",
						cv.getConfigurationString(), i + 1,
						numCrossValidationRounds);

				for (int j = 0; j < numCrossValidationSteps; j++)
				{
					System.err.printf("training instance %d...", j + 1);
					cv.nextFold();

					classifier = null;
					featureGen.reset();
					System.gc();

					classifier = PolarityClassifierFactory.create(
							modelName, featureGen,
							conf.getBooleanValue(keyFeatureInduction, false));
					// set transition constraints according to label encoding
					boolean[][] constr = enc.getTransitionConstraints();
					if (constr != null)
						classifier.setTransitionConstraints(constr);

					SubjectivityAnnotationGenerator subjAnnotGen =
						new SubjectivityAnnotationGenerator(
								conf.getDoubleValue(keyPriorSubjBias, 0.0));

					Corpus trainingCorpus = cv.getTrainingSet();
					if (performSubSampling || performSuperSampling)
						trainingCorpus = new SampledCorpus(trainingCorpus,
								performSubSampling, samplingNeighborhoodSize,
								false);
					subjAnnotGen.train(trainingCorpus);
					subjAnnotGen.generateAnnotations(trainingCorpus);
					double regularizationParam =
						conf.getDoubleValue(keyRegularizationParam, 0.0);
					if (performEarlyStopping)
					{
						classifier.trainWithEarlyStopping(trainingCorpus,
								cv.getTrainingTestSet(), regularizationParam);
						if (classifier.hasStoppedEarly())
							numEarlyStops++;
					}
					else
						classifier.train(trainingCorpus, regularizationParam);

					System.err.println("processing...");
					Corpus testCorpus = cv.getTestSet();

					subjAnnotGen.generateAnnotations(testCorpus);
					if (biasValue != 0.0)
						classifier.applyClassBias(0, biasValue, biasType);

					classifier.process(testCorpus);
					generateReports(now, i, j, testCorpus, classifier,
							featureGen);
					enc.decode(testCorpus);
					eval.addCorpus(testCorpus);

					log.printf("\n\nmodel parameters (run %d,%d):\n\n",i+1,j+1);
					classifier.printModel(log);
				}
			}

			eval.updateEvaluation();
			System.err.println("done!");
			System.err.printf("overall accuracy = %.2f%%\n",
					eval.getAccuracy() * 100.0);

			if (performSubSampling || performSuperSampling)
			{
				if (performSubSampling)
					log.printf("\n\nsubsampled the neutral class");
				else
					log.printf("\n\nsupersampled the polar classes");
				log.printf(", neighborhood size = %d\n\n",
						samplingNeighborhoodSize);
			}
			if (performEarlyStopping)
				log.printf("training stopped early in %d of %d instances\n",
						numEarlyStops,
						numCrossValidationRounds * numCrossValidationSteps);
			log.printf("\n\nresults of %d rounds of %s:\n\n",
					numCrossValidationRounds, cv.getConfigurationString());
			eval.printResults(log);
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

	private static void generateReports(String now, int roundIdx, int foldIdx,
			Corpus corpus, Classifier classifier, FeatureGenerator featureGen)
		throws Exception
	{
		Vector<Report> reports = new Vector<Report>();
		reports.add(new MisclassifiedSentenceReport(corpus, classifier, 100));
		reports.add(new FeatureSelectionReport(classifier, featureGen,
				corpus.getNumClasses(), true));

		StringBuffer reportFileName = new StringBuffer();
		reportFileName.append("report-");
		reportFileName.append(now);
		reportFileName.append('-');
		reportFileName.append(roundIdx + 1);
		reportFileName.append('-');
		reportFileName.append(foldIdx + 1);
		reportFileName.append(".log");
		PrintStream reportOut = new PrintStream(reportFileName.toString());

		for (Report r : reports)
		{
			r.print(reportOut);
			reportOut.println();
		}

		reportOut.close();
	}

}

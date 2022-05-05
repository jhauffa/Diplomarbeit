package de.tum.in.SocialNetworks;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.Person;
import de.tum.in.Math.RowDataSet;
import de.tum.in.Math.Vector;
import de.tum.in.Util.ArgumentParser;
import de.tum.in.Util.Time;
import de.tum.in.Util.ArgumentParser.ArgumentType;


public class AnalyzeMailbox {

	private static final int defaultMaxSomIter = 10000;
	private static final int defaultSomNeurons = 11;

	private static final String[][] slangWordLists = {
		{"./lists/slang-pos.txt", "positive slang"},
		{"./lists/slang-neg.txt", "negative slang"}
	};
	private static final String[][] emoticonLists = {
		{"./lists/emoticons-pos.txt", "positive emoticons"},
		{"./lists/emoticons-neg.txt", "negative emoticons"}
	};
	private static final boolean[] slangWordListPolarity = {
		true, false
	};
	private static final boolean[] emoticonListPolarity = {
		true, false
	};
	private static final String slangWordListWeighted = "./lists/slang-ud.txt";

	private static final char tagMethod = 'm';
	private static final char tagScalingSdf = 'd';
	private static final char tagSomIter = 'i';
	private static final char tagSomNeurons = 'n';
	private static final char tagSomCluster = 'c';
	private static final char tagSomWidth = 'w';
	private static final char tagSomConstr = 'z';
	private static final char tagOwnerId = 'o';
	private static final char tagPerSentence = 'p';
	private static final char tagPolFilter = 'f';
	private static final char tagPolFilterTop = 't';
	private static final char tagPolFilterBottom = 'b';
	private static final char tagPolFilterScale = 'u';
	private static final char tagPolFilterWidth = 'v';
	private static final char tagPolFilterShift = 's';
	private static final char tagSlangPolar = 'j';
	private static final char tagSlangWeighted = 'k';
	private static final char tagNoSentiment = 'x';

	private static ArgumentParser parseArguments(String[] args)
	{
		ArgumentParser parser = new ArgumentParser("AnalyzeMailbox");
		parser.addArgument(tagMethod, "method of dimensionality reduction\n" +
				"\t\tpossible values are PCA, SOM, avg, random, const; " +
				"default is PCA", ArgumentType.STRING);
		parser.addArgument(tagScalingSdf, "for scaling method \"normal\", x" +
				"specifies the SDS that is mapped to 0/1\n\t\tdefault is 3.0",
				ArgumentType.DOUBLE);
		parser.addArgument(tagSomIter, "number of SOM iterations, default = " +
				Integer.toString(defaultMaxSomIter), ArgumentType.INT);
		parser.addArgument(tagSomNeurons, "number of SOM neurons, default = " +
				Integer.toString(defaultSomNeurons), ArgumentType.INT);
		parser.addArgument(tagSomWidth, "width of SOM, default = 1",
				ArgumentType.INT);
		parser.addArgument(tagSomCluster, "SOM: identify clusters in U matrix",
				ArgumentType.BOOLEAN);
		parser.addArgument(tagSomConstr, "SOM: apply topology constraints",
				ArgumentType.BOOLEAN);
		parser.addArgument(tagOwnerId, "override mailbox owner, x = UUID",
				ArgumentType.STRING);
		parser.addArgument(tagPerSentence, "count sentences instead of words",
				ArgumentType.BOOLEAN);
		parser.addArgument(tagPolFilter, "apply polarity classification to a " +
				"subset of sentences only\n\t\tpossible values are: all, " +
				"outer, outer-abs, inner, rel (sentences containing " +
				"relationship cues), weighted\n\t\tdefault = all",
				ArgumentType.STRING);
		parser.addArgument(tagPolFilterTop, "apply polarity classification " +
				"to the first x (% of) sentences\n\t\tdefault = 10",
				ArgumentType.INT);
		parser.addArgument(tagPolFilterBottom, "apply polarity classification "+
				"to the last x (% of) sentences\n\t\tdefault = first x",
				ArgumentType.INT);
		parser.addArgument(tagPolFilterScale, "scale factor for gaussian " +
				"weight function for polarity classification",
				ArgumentType.DOUBLE);
		parser.addArgument(tagPolFilterWidth, "width parameter of gaussian " +
				"weight function for polarity classification",
				ArgumentType.DOUBLE);
		parser.addArgument(tagPolFilterShift, "shift parameter of gaussian " +
				"weight function for polarity classification",
				ArgumentType.DOUBLE);
		parser.addArgument(tagSlangPolar, "use lists of slang words sorted by" +
				" polarity for predicting polarity", ArgumentType.BOOLEAN);
		parser.addArgument(tagSlangWeighted, "use lists of slang words " +
				"weighted by polarity for predicting polarity",
				ArgumentType.BOOLEAN);
		parser.addArgument(tagNoSentiment, "do not perform sentiment analysis",
				ArgumentType.BOOLEAN);
		parser.addAnonymousArgument("file", "\"file\" is a message database");
		parser.addAnonymousArgument("suffix", "\"suffix\" is appended to the " +
				"output file name");
		parser.parse(args);
		return parser;
	}

	public static void main(String[] args)
	{
		ArgumentParser conf = parseArguments(args);

		String now = Time.getTimeStamp();
		String logFileName = "mbox-" + now + "-" + conf.getAnonArgument(1) +
			".log";
		PrintStream log = null;

		File dbFile = new File(conf.getAnonArgument(0));
		MessageDatabase db;
		try
		{
			log = new PrintStream(logFileName);
			log.printf("reconstructing Social Network from %s\n%s\n\n",
					dbFile.getName(), now);

			db = MessageDatabase.readFromFile(dbFile);
		}
		catch (IOException ex)
		{
			System.err.println("I/O error: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}

		// find mailbox owner
		Person owner;
		String ownerId = conf.getStringValue(tagOwnerId, null);
		if (ownerId != null)
		{
			owner = db.getPersonDatabase().queryById(ownerId);
			if (owner != null)
				db.setOwner(owner);
		}
		else
			owner = db.getOwner();
		if (owner == null)
		{
			System.err.println("Owner of message database unknown!");
			return;
		}
		log.printf("mailbox owner = %s\n", owner.getId());

		// generate social network graph from mailbox data
		SocialNetwork net = new SocialNetwork(db);
		Evaluator eval = new Evaluator(log, net);
		net.printEdges(log);
		eval.printNetworkStats();

		// initialize feature generators
		FeatureGenerator[] featureGen =
			new FeatureGenerator[Relationship.numAttributes];
		featureGen[0] = createFeatureGenIntensity(owner, net, conf);
		featureGen[1] = createFeatureGenValence(conf);

		RowDataSet[] predictedRatings =
			new RowDataSet[Relationship.numAttributes];
		RowDataSet[] trueRatings = new RowDataSet[Relationship.numAttributes];
		for (int i = 0; i < Relationship.numAttributes; i++)
			trueRatings[i] = net.getTrueRatings(i);

		boolean performScaling = true;
		
		for (int i = 0; i < Relationship.numAttributes; i++)
		{
			// extract features
			RowDataSet featureVectors = featureGen[i].getFeatureVectors(net);
			log.printf("\n\nfeature vectors for attribute \"%s\":\n",
					Relationship.attributeNames[i]);
			featureGen[i].printFeatureVectors(log, featureVectors, true);

			log.println("correlation of features:");
			eval.printCorrelationMatrix(featureVectors);
			log.println("\ncorrelation of features and true ratings:");
			eval.printCorrelationOfFeaturesRatings(featureVectors,
					trueRatings[i]);
			log.print("\n\n");

			// dimensionality reduction
			String method = conf.getStringValue(tagMethod, "PCA"); 
			if (method.equals("PCA"))
			{
				log.println("PCA");
				PrincipalComponentAnalysis pca =
						new PrincipalComponentAnalysis(featureVectors);

				int lastCompIdx = featureVectors.height() - 1; 
				double varianceLoss = pca.getRelVariance(1, lastCompIdx);
				log.printf("rel. variance loss = %f\n", varianceLoss);
				double v2 = pca.getRelVariance(1, 1, 1, lastCompIdx);
				log.printf("second component explains %f of remaining " +
						"variance\n", v2);

				predictedRatings[i] = pca.transform(1);
			}
			else if (method.equals("SOM"))
			{
				predictedRatings[i] =
					new RowDataSet(1, featureVectors.getNumPoints());

				int numSomNeurons =
						conf.getIntValue(tagSomNeurons, defaultSomNeurons);
				int width = conf.getIntValue(tagSomWidth, 1);
				int numIter = conf.getIntValue(tagSomIter, defaultMaxSomIter);
				log.printf("SOM, %d neurons, %d iterations\n", numSomNeurons,
						numIter);
				boolean findClusters = conf.getBooleanValue(tagSomCluster,
						false);
				if (findClusters)
					log.println("identifying clusters in U matrix");
				boolean constrainedTopology = conf.getBooleanValue(tagSomConstr,
						false);
				if (constrainedTopology)
					log.println("constrained topology");

				SelfOrganizingMap som = new SelfOrganizingMap(featureVectors,
						numSomNeurons, width, numIter, findClusters,
						constrainedTopology);
				som.sortClusters();

				// dump U matrix
				log.println("\nU matrix:");
				som.getUMatrix().print(log);
				log.println();

				int maxClusterIdx = som.getNumClusters() - 1;
				Vector assoc = som.getAssociationVector();
				for (int j = 0; j < assoc.size(); j++)
					predictedRatings[i].set(0, j, assoc.get(j) / maxClusterIdx);
			}
			else if (method.equals("avg"))
			{
				log.println("average of feature vector");
				int width = featureVectors.getNumPoints();
				predictedRatings[i] = new RowDataSet(1, width);
				int idx = 0;
				for (int j = 0; j < width; j++)
				{
					double v = 0.0;
					int height = featureVectors.getNumDimensions();
					for (int k = 0; k < height; k++)
						v += featureVectors.get(k, idx);
					predictedRatings[i].set(0, idx, v / height);
					idx++;
				}
			}
			else if (method.equals("random"))
			{
				log.println("generating random values (baseline)");
				int n = featureVectors.getNumPoints();
				predictedRatings[i] = new RowDataSet(1, n);
				for (int j = 0; j < n; j++)
					predictedRatings[i].set(0, j, Math.random());
			}
			else
			{
				log.println("generating constant values (baseline)");
				int n = featureVectors.getNumPoints();
				predictedRatings[i] = new RowDataSet(1, n);
				for (int j = 0; j < n; j++)
					predictedRatings[i].set(0, j, 0.5);
				performScaling = false;
			}

			log.println("unscaled ratings:");
			eval.printVectors(predictedRatings[i]);
			eval.printCorrelationOfRatings(predictedRatings[i], trueRatings[i],
					true);
			eval.printRMSE(predictedRatings[i], trueRatings[i]);

			// perform scaling
			if (performScaling)
			{
				double sdf = conf.getDoubleValue(tagScalingSdf, 3.0);
				for (Scaling.Method scalingMethod : Scaling.Method.values())
				{
					Scaling sc = new Scaling(scalingMethod, sdf, net);
					log.print("\nscaling: ");
					sc.printConfiguration(log);

					RowDataSet scaledRatings =
						new RowDataSet(predictedRatings[i].duplicate());
					if ((scalingMethod == Scaling.Method.SEMI) &&
						(net.getNumEdges() <= (2 * Scaling.numSamples)))
						log.println("not enough relationships, skipping");
					else
						sc.scaleRatings(scaledRatings, i);
					eval.printCorrelationOfRatings(scaledRatings,
							trueRatings[i], true);

					// evaluate, print results
					log.println();
					eval.printRMSE(scaledRatings, trueRatings[i]);
					net.resetExclusion();

					if (scalingMethod == Scaling.Method.NORMAL)
					{
						RowDataSet scaledTrueRatings =
							new RowDataSet(trueRatings[i].duplicate());
						sc.scaleRatings(scaledTrueRatings, -1);

						log.println("scaling true ratings:");
						eval.printRMSE(scaledRatings, scaledTrueRatings);				
					}
				}
			}
			else
				log.println("no scaling");
		}

		log.println("correlation of true attributes:");
		eval.printCorrelationMatrix(trueRatings);
		log.println("correlation of predicted attributes:");
		eval.printCorrelationMatrix(predictedRatings);

		log.close();
		System.err.printf("\ndone, results written to %s\n", logFileName);
	}

	private static void setRangeParams(WordPolarityFeature wpf,
			ArgumentParser conf)
	{
		WordPolarityFeature.Range rangeType =
			WordPolarityFeature.stringToRangeType(
					conf.getStringValue(tagPolFilter, "all"));
		double rangeParam1;
		double rangeParam2;
		if (rangeType == WordPolarityFeature.Range.GAUSSIAN_WEIGHTED)
		{
			rangeParam1 = conf.getDoubleValue(tagPolFilterScale, 0.8);
			rangeParam2 = conf.getDoubleValue(tagPolFilterWidth, 0.1);
			double rangeParam3 = conf.getDoubleValue(tagPolFilterShift, 0.0);
			wpf.setGaussianWeight(rangeParam1, rangeParam2, rangeParam3);
		}
		else
		{
			rangeParam1 = conf.getIntValue(tagPolFilterTop, 10);
			rangeParam2 = conf.getIntValue(tagPolFilterBottom,
					(int) rangeParam1);			
			wpf.setRange(rangeType, rangeParam1, rangeParam2);
		}
	}

	private static FeatureGenerator createFeatureGenIntensity(Person owner,
			SocialNetwork net, ArgumentParser conf)
	{
		FeatureGenerator featureGen = new FeatureGenerator();
		boolean perSentence = conf.getBooleanValue(tagPerSentence, false);

		WordListFeature colloquialExpFeature =
			new WordListFeature(new File("./lists/abbrev.txt"),
					"colloquial expressions", perSentence, false);
		for (String[] entry : slangWordLists)
			colloquialExpFeature.addWordList(new File(entry[0]));
		featureGen.addMsgFeature(colloquialExpFeature);

		EmoticonListFeature emoticonFeature =
			new EmoticonListFeature("emoticons", perSentence);
		for (String[] entry : emoticonLists)
			emoticonFeature.addEmoticonList(new File(entry[0]));
		featureGen.addMsgFeature(emoticonFeature);
		
		featureGen.addMsgFeature(new WordElongationFeature(perSentence));
		featureGen.addMsgFeature(new InvertedFeature(
				new WordObfuscationFeature(perSentence)));
		featureGen.addMsgFeature(new MessageLengthFeature(net));
		featureGen.addMsgFeature(new FirstLastNameFeature());

		if (!conf.getBooleanValue(tagNoSentiment, false))
		{
			WordPolarityFeature wpf =
				new WordPolarityFeature(true, true, perSentence);
			setRangeParams(wpf, conf);
			featureGen.addMsgFeature(wpf);
		}

		featureGen.addRelFeature(new MessageFrequencyFeature(net));
		featureGen.addRelFeature(new MessageBalanceFeature());
		return featureGen;
	}

	private static FeatureGenerator createFeatureGenValence(ArgumentParser conf)
	{
		FeatureGenerator featureGen = new FeatureGenerator();
		boolean perSentence = conf.getBooleanValue(tagPerSentence, false);

		boolean useSlangPolar = conf.getBooleanValue(tagSlangPolar, false);
		boolean useSlangWeighted =conf.getBooleanValue(tagSlangWeighted, false);
		if (useSlangPolar)
		{
			for (int i = 0; i < slangWordLists.length; i++)
			{
				String[] entry = slangWordLists[i];
				WordListFeature wordFeature = new WordListFeature(
						new File(entry[0]), entry[1], perSentence, false);
				if (slangWordListPolarity[i])
					featureGen.addMsgFeature(wordFeature);
				else
					featureGen.addMsgFeature(new InvertedFeature(wordFeature));
			}
		}
		if (useSlangWeighted)
		{
			featureGen.addMsgFeature(new WordListFeature(
					new File(slangWordListWeighted),
					"weighted positive slang words", perSentence, false));			
			featureGen.addMsgFeature(new InvertedFeature(new WordListFeature(
					new File(slangWordListWeighted),
					"weighted negative slang words", perSentence, true)));			
		}

		for (int i = 0; i < emoticonLists.length; i++)
		{
			String[] entry = emoticonLists[i];
			EmoticonListFeature emoticonFeature = new EmoticonListFeature(
					new File(entry[0]), entry[1], perSentence);
			if (emoticonListPolarity[i])
				featureGen.addMsgFeature(emoticonFeature);
			else
				featureGen.addMsgFeature(new InvertedFeature(emoticonFeature));
		}

		if (!conf.getBooleanValue(tagNoSentiment, false))
		{
			WordPolarityFeature wpf =
				new WordPolarityFeature(true, false, perSentence);
			setRangeParams(wpf, conf);
			featureGen.addMsgFeature(wpf);
			wpf = new WordPolarityFeature(false, true, perSentence);
			setRangeParams(wpf, conf);
			featureGen.addMsgFeature(new InvertedFeature(wpf));
		}
		return featureGen;
	}

}

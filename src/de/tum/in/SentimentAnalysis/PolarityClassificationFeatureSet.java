package de.tum.in.SentimentAnalysis;

import java.io.File;
import java.util.Vector;

public class PolarityClassificationFeatureSet {

	private static final int numFeatureLevels = 4;
	private static final String[][] wordLists = {
		{"./lists/coord.lst", "coordination"},
		{"./lists/GI.Negate.lst", "negation_GI"},
		{"./lists/GI.Ovrst.lst", "intensifier_GI"},
		{"./lists/GI.Undrst.lst", "weakener_GI"},
		{"./lists/intensifiers.lst", "intensifier"},
		{"./lists/modal.lst", "modal_operator"},
		{"./lists/negation.direct.lst", "direct_negation"},
		{"./lists/negation.indirect.lst", "indirect_negation"},
		{"./lists/weakeners.lst", "weakener"}
	};
	private static final String[][] wordListsPolarity = {
		{"./lists/GI.Positiv.lst", "positive"},
		{"./lists/GI.Negativ.lst", "negative"}
	};

	private boolean[] generateLevelNFeatures;
	private boolean simple;
	private boolean includePolarityWordLists;
	private int nGramMinRank;
	private WordListFeatures.NeighborhoodType wordListNeighborhoodType;
	private int wordListNeighborhoodSize;
	private int nGramWindowSize;

	public PolarityClassificationFeatureSet(boolean enableAllFeatures)
	{
		generateLevelNFeatures = new boolean[numFeatureLevels];
		if (enableAllFeatures)
			for (int i = 0; i < generateLevelNFeatures.length; i++)
				generateLevelNFeatures[i] = true;

		wordListNeighborhoodType =
			WordListFeatures.NeighborhoodType.NEIGHBORHOOD_CHUNK;
		// all other parameters initialized to 0 / false
	}

	public void setGenerateLevelNFeatures(int n, boolean enabled)
	{
		generateLevelNFeatures[n] = enabled;
	}

	public void setGenerateSimpleFeatures(boolean enabled)
	{
		simple = enabled;
	}

	public void setNGramMinimumRank(int rank)
	{
		nGramMinRank = rank;
	}

	public void setWordListNeighborhoodType(
			WordListFeatures.NeighborhoodType type)
	{
		wordListNeighborhoodType = type;
	}

	public void setWordListNeighborhoodSize(int size)
	{
		wordListNeighborhoodSize = size;
	}

	public void setNGramWindowSize(int size)
	{
		nGramWindowSize = size;
	}

	public void setIncludePolarityWordLists(boolean enabled)
	{
		includePolarityWordLists = enabled;
	}

	public FeatureGenerator createLegacyFeatureGenerator(Corpus fullCorpus)
	{
		FeatureGenerator featureGen = new FeatureGenerator();

		NGramFeatures unigramFeatures = new NGramFeatures(1, false, false,
				NGramFeatures.Direction.FORWARD, 0, 0, 0);
		addFeatureTemplate(featureGen, unigramFeatures, 1);

		featureGen.addFeatureTemplate(new WordStatisticsFeatures(5));
		featureGen.addFeatureTemplate(new WordShapeFeatures());
		featureGen.addFeatureTemplate(new SentenceStatisticsFeatures(5));
		featureGen.addFeatureTemplate(new SentenceTypeFeatures());
		featureGen.addFeatureTemplate(new SentenceStructureFeatures());

		featureGen.addFeatureTemplate(new PrefixSuffixFeatures(2, 3, false));
		featureGen.addFeatureTemplate(new WordStemFeatures(0, 0));

		Vector<AnnotationProvider> annotProv = new Vector<AnnotationProvider>();
		annotProv.add(new PoSAnnotationProvider(fullCorpus, false));
		annotProv.add(new ChunkAnnotationProvider(fullCorpus, true));
		annotProv.add(new SemanticRoleAnnotationProvider(fullCorpus, 2, true));
		for (AnnotationProvider p : annotProv)
		{
			p.retrieveAnnotations();
			AnnotationFeatures annotationFeatures = new AnnotationFeatures(p);
			addFeatureTemplate(featureGen, annotationFeatures, 1);
		}

		// subjectivity annotations
		AnnotationFeatures annotationFeatures =
			new AnnotationFeatures("subjectivity", 1, 2, "");
		addFeatureTemplate(featureGen, annotationFeatures, 1);

		return featureGen;
	}

	public FeatureGenerator createFeatureGenerator(Corpus fullCorpus)
	{
		FeatureGenerator featureGen = new FeatureGenerator();
		FeatureTemplate templ;
		Vector<AnnotationProvider> annotProv = new Vector<AnnotationProvider>();

		int windowSize = 0;
		if (!simple)
			windowSize = 1;

		if (generateLevelNFeatures[0])
		{
			if (simple)
				templ = new CharacterNGramFeatures(3, true, true, false);
			else
				templ = new NGramFeatures(1, false, false,
						NGramFeatures.Direction.FORWARD, 0, 0, nGramMinRank);
			addFeatureTemplate(featureGen, templ, nGramWindowSize);

			templ = new WordStatisticsFeatures(5);
			addFeatureTemplate(featureGen, templ, windowSize);
			templ = new SentenceStatisticsFeatures(5);
			addFeatureTemplate(featureGen, templ, windowSize);
		}

		if (generateLevelNFeatures[1])
		{
			annotProv.add(new PoSAnnotationProvider(fullCorpus, simple));

			templ = new WordShapeFeatures();
			addFeatureTemplate(featureGen, templ, windowSize);
			if (!simple)
			{
				templ = new PrefixSuffixFeatures(2, 3, false);
				addFeatureTemplate(featureGen, templ, 1);
				templ = new WordStemFeatures(0, 0);
				addFeatureTemplate(featureGen, templ, 1);
			}
		}

		if (generateLevelNFeatures[2])
		{
			annotProv.add(new ChunkAnnotationProvider(fullCorpus, simple));

			templ = new SentenceTypeFeatures();
			addFeatureTemplate(featureGen, templ, windowSize);
			templ = new SentenceStructureFeatures();
			addFeatureTemplate(featureGen, templ, windowSize);
		}

		if (generateLevelNFeatures[3])
		{
			int srlDepth = simple ? 1 : 4;
			annotProv.add(new SemanticRoleAnnotationProvider(fullCorpus,
					srlDepth, simple));

			templ = new AnnotationFeatures("subjectivity", 1, 2, "");
			addFeatureTemplate(featureGen, templ, 1);

			if (!simple)
			{
				addFeaturesForWordLists(wordLists, featureGen, windowSize);
				if (includePolarityWordLists)
					addFeaturesForWordLists(wordListsPolarity, featureGen,
							windowSize);
			}
		}

		for (AnnotationProvider p : annotProv)
		{
			p.retrieveAnnotations();
			templ = new AnnotationFeatures(p);
			addFeatureTemplate(featureGen, templ, 1);
		}

		return featureGen;
	}

	private void addFeaturesForWordLists(String[][] wordLists,
			FeatureGenerator featureGen, int windowSize)
	{
		for (String[] listDesc : wordLists)
		{
			FeatureTemplate templ = new WordListFeatures(
				new File(listDesc[0]), listDesc[1],
				wordListNeighborhoodType, wordListNeighborhoodSize,
				true, false, false);
			addFeatureTemplate(featureGen, templ, windowSize);
		}		
	}

	private static void addFeatureTemplate(FeatureGenerator featureGen,
			FeatureTemplate templ, int windowSize)
	{
		featureGen.addFeatureTemplate(templ);
		for (int i = 0; i < windowSize; i++)
		{
			featureGen.addFeatureTemplate(new FeaturePositionShifter(templ,
					-(i + 1)));
			featureGen.addFeatureTemplate(new FeaturePositionShifter(templ, 
					(i + 1)));
		}
	}

}

package de.tum.in.SentimentAnalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


public class PoSAnnotationProvider extends AnnotationProvider {

	private static final String modelPath =
//		"./external/bidirectional-wsj-0-18.tagger";
		"./external/left3words-wsj-0-18.tagger";
	private static final String[] tags = {"\'\'", "CC", "CD", "DT", "EX", "FW",
	    "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP", "NNPS", "NNS",
	    "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO",
	    "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$",
	    "WRB"};
	private static final int[] tagMapping = {0, 1, 2, 3, 3, 4, 5, 6, 6, 6, 2, 7,
	    8, 4, 4, 8, 3, 0, 9, 9, 10, 10, 10, 11, 0, 11, 0, 12, 12, 12, 12,
	    12, 12,	3, 9, 9, 10};
	private static final int numSimpleTags = 13;

	private boolean useSimpleTags;

	public PoSAnnotationProvider(Corpus corpus, boolean useSimpleTags)
	{
		super(corpus);
		this.useSimpleTags = useSimpleTags;
	}

	@Override public String getKey()
	{
		return "PoS";
	}

	@Override public int getNumValues()
	{
		if (useSimpleTags)
			return numSimpleTags;
		return tags.length;
	}

	@Override public int getNumAnnot()
	{
		return 1;
	}

	@Override protected void performAnnotation()
	{
		try
		{
			@SuppressWarnings("unused") MaxentTagger tagger =
				new MaxentTagger(modelPath);
			int n = 0;
			Date t1 = new Date();
			for (de.tum.in.SentimentAnalysis.Sentence sentence : corpus)
			{
				ArrayList<String> words = new ArrayList<String>(
						sentence.length());
				for (de.tum.in.SentimentAnalysis.Word w : sentence)
					words.add(w.getWord());
				Sentence<Word> stanfordSentence = Sentence.toSentence(words);

				Sentence<TaggedWord> taggedSentence =
						MaxentTagger.tagSentence(stanfordSentence);

				int[] tagSequence = new int[taggedSentence.length()];
				for (int i = 0; i < taggedSentence.length(); i++)
				{
					TaggedWord w = taggedSentence.get(i);
					int tagIdx = Arrays.binarySearch(tags, w.tag());
					if (tagIdx < 0)
						tagIdx = 0;
					if (useSimpleTags)
						tagIdx = tagMapping[tagIdx];
					tagSequence[i] = tagIdx;
				}
				if (!annotateSentence(sentence, tagSequence, 0))
					System.err.printf("annotated string: \"%s\"\n",
							taggedSentence.toString(false));

				if (++n > 100)
				{
					Date t2 = new Date();
					System.err.printf("tagged 100 sentences in %.2f seconds\n",
						((double) (t2.getTime() - t1.getTime()) / 1000.0));
					t1 = t2;
					n = 0;
				}
			}
		}
		catch (Exception ex)
		{
			System.err.printf("Stanford PoS tagger error: \"%s\"\n",
					ex.getMessage());
		}
	}

	public static String getTagName(int index)
	{
		return tags[index];
	}

	@Override public String getConfigurationString()
	{
		if (useSimpleTags)
			return "simplified tagset";
		else
			return "Penn TreeBank tagset";
	}

}

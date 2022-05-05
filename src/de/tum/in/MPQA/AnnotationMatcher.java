package de.tum.in.MPQA;

import java.util.ArrayList;

import de.tum.in.SentimentAnalysis.Sentence;
import de.tum.in.SentimentAnalysis.Sentiment;
import de.tum.in.SentimentAnalysis.Sentiment.Polarity;
import de.tum.in.Linguistics.Tokenizer;


public class AnnotationMatcher {

/*
	private static int getAnnotationAtOffset(int offset, int length,
			ArrayList<SentimentAnnotation> annotations, Sentiment sentiment)
	{
		if (annotations.size() == 0)
			return -1;  // no annotations, sentiment remains unchanged

		// Perform binary search to find the first annotation with getStart() >
		// offset, then go backwards to find the smallest annotation that
		// contains the range (offset,offset+length). If there are two
		// annotations of the same length, it will return the annotation with
		// the highest sentiment intensity, because of the order induced by
		// CompareAnnotations.

		SentimentAnnotation a = null;

		int start = 0;
		int end = annotations.size() - 1;
		int pos = end;
		while (start <= end)
		{
			pos = (start + end) / 2;
			a = annotations.get(pos);
			if (a.getStart() == offset)
				break;
			else if (a.getStart() < offset)
				start = pos + 1;
			else
				end = pos - 1;
		}

		// Find the first annotation with getStart() > offset.
		while ((a.getStart() <= offset) && (++pos < annotations.size()))
			a = annotations.get(pos);

		// Find smallest matching annotation.
		int minAnnotLength = Integer.MAX_VALUE;
		SentimentAnnotation minAnnot = null;
		while (--pos >= 0)
		{
			a = annotations.get(pos);
			if ((offset - a.getStart()) >= minAnnotLength)
				break;
			if (a.getEnd() >= (offset + length))
			{
				int annotLength = a.getEnd() - a.getStart();
				if (annotLength < minAnnotLength)
				{
					minAnnotLength = annotLength;
					minAnnot = a;
				}
			}
		}

		if (minAnnot != null)
		{
			sentiment.clone(minAnnot.getSentiment());
			return minAnnot.getStart();
		}
		return -1;
	}
*/

	private static int getAllAnnotationsAtOffset(int offset, int length,
			ArrayList<SentimentAnnotation> annotations, Sentiment sentiment)
	{
		if (annotations.size() == 0)
			return -1;  // no annotations, sentiment remains unchanged

		// Find all matching annotations, return start of smallest annotation.
		// If there are two annotations with conflicting polarity classes, the
		// class "both" is chosen.
		int minAnnotLength = Integer.MAX_VALUE;
		SentimentAnnotation minAnnot = null;
		for (SentimentAnnotation a : annotations)
		{
			if ((a.getStart() <= offset) &&
				(a.getEnd() >= (offset + length)))
			{
				int annotLength = a.getEnd() - a.getStart();
				Sentiment annSentiment = a.getSentiment();
				if ((annSentiment.getPolarity() != sentiment.getPolarity()) &&
					(sentiment.getPolarity() != Polarity.NEUTRAL))
				{
					sentiment.setPolarity(Polarity.BOTH);
					sentiment.setIntensity((sentiment.getIntensity() +
							annSentiment.getIntensity()) / 2.0);
					sentiment.setConfidence((sentiment.getConfidence() +
							annSentiment.getConfidence()) / 2.0);
				}
				else
					sentiment.clone(annSentiment);

				if (annotLength < minAnnotLength)
				{
					minAnnotLength = annotLength;
					minAnnot = a;
				}
			}
		}

		if (minAnnot != null)
			return minAnnot.getStart();
		return -1;
	}

	public static Sentence processSentence(String rawSentence, int offset,
			ArrayList<SentimentAnnotation> sentimentAnnotations)
	{
		Sentence sentence = new Sentence();

		Sentiment.Polarity prevPolarity = Sentiment.Polarity.NEUTRAL;
		int prevWordEnd = 0;
		Tokenizer t = new Tokenizer(rawSentence, false);
		while (t.hasNext())
		{
			String word = t.next();
			int wordStart = t.getOffset();
			prevPolarity = processWord(word, sentence, sentimentAnnotations,
					offset + wordStart, offset + prevWordEnd, prevPolarity);
			prevWordEnd = wordStart + word.length();
		}

		return sentence;
	}

	private static Sentiment.Polarity processWord(String rawWord,
			Sentence sentence,
			ArrayList<SentimentAnnotation> sentimentAnnotations,
			int wordOffset, int prevWordEndOffset,
			Sentiment.Polarity prevPolarity)
	{
		Sentiment sentiment = new Sentiment();
//		int annotStart = getAnnotationAtOffset(wordOffset, rawWord.length(),
//				sentimentAnnotations, sentiment);
		int annotStart = getAllAnnotationsAtOffset(wordOffset, rawWord.length(),
				sentimentAnnotations, sentiment);

		boolean startsBlock = false;
		Sentiment.Polarity curPolarity = sentiment.getPolarity();
		if (curPolarity != Sentiment.Polarity.NEUTRAL)
		{
			if (annotStart >= prevWordEndOffset)
				startsBlock = true;
			else if (prevPolarity != curPolarity)
			{
				// This happens in the case of overlapping annotations; set the
				// startsBlock flag manually to restore consistency.
				startsBlock = true;
			}
		}

		sentence.append(rawWord, sentiment, startsBlock);
		return sentiment.getPolarity();
	}

}

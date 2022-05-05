package de.tum.in.MPQA;

import java.util.Comparator;

public class CompareAnnotations implements Comparator<Annotation> {

	public int compare(Annotation a1, Annotation a2)
	{
		if (a1 instanceof SentimentAnnotation)
			return compareSentiment((SentimentAnnotation) a1,
					(SentimentAnnotation) a2);
		return compareBasic(a1, a2);
	}

	public int compareBasic(Annotation a1, Annotation a2)
	{
		int d = a1.getStart() - a2.getStart();  // order ascending by position,
		if (d == 0)
			d = a2.getEnd() - a1.getEnd();  // then descending by length
		return d;
	}

	public int compareSentiment(SentimentAnnotation a1, SentimentAnnotation a2)
	{
		int d = a1.getStart() - a2.getStart();  // order ascending by position,
		if (d == 0)
			d = a2.getEnd() - a1.getEnd();  // then descending by length,
		if (d == 0)
		{
			// then ascending by intensity
			if (a1.getSentiment().getIntensity() <
				a2.getSentiment().getIntensity())
				d = -1;
			else
				d = 1;
		}
		return d;
	}

}

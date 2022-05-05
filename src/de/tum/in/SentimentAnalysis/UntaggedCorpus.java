package de.tum.in.SentimentAnalysis;

import de.tum.in.Linguistics.PersonalName;
import de.tum.in.Linguistics.Tokenizer;

public class UntaggedCorpus extends Corpus {

	public UntaggedCorpus(String text, int numClasses,
			boolean lineEndIsSentenceEnd)
	{
		super(numClasses);

		Tokenizer t = new Tokenizer(text, lineEndIsSentenceEnd);
		Sentence curSentence = null;
		boolean prevWordIsTitle = false;
		while (t.hasNext())
		{
			String word = t.next();
			boolean isLineEnd = word.equals(Tokenizer.lineEndToken);
			if (!isLineEnd)
			{
				if (curSentence == null)
					curSentence = new Sentence();
				curSentence.append(word, new Sentiment(), false);
			}
			if ((word.equals(".") && !prevWordIsTitle) || isLineEnd)
			{
				if (curSentence != null)
				{
					sentences.add(curSentence);
					curSentence = null;
				}
			}
			prevWordIsTitle = PersonalName.isShortTitle(word);
		}

		if (curSentence != null)
			sentences.add(curSentence);
	}

}

package de.tum.in.SentimentAnalysis;

public class LabelEncoding {

	public static enum EncodingScheme {
		ENC_NONE, ENC_BIO1, ENC_BIO2, ENC_BIOW, ENC_BMEO, ENC_BMEOW,
		ENC_BMEOW_PLUS
	};

	private static String[] encodingSchemeNames = {
		"no", "BIO1", "BIO2", "BIOW", "BMEO", "BMEOW", "BMEOWplus"
	};

	private int maxClass;
	private EncodingScheme scheme;
	
	public LabelEncoding(int numClasses, String scheme)
	{
		this.maxClass = numClasses - 1;
		for (int i = 0; i < encodingSchemeNames.length; i++)
			if (encodingSchemeNames[i].equals(scheme))
			{
				this.scheme = ordinalToScheme(i);
				break;
			}
		assert this.scheme != null;
	}

	public void encode(Corpus corpus)
	{
		for (Sentence s : corpus)
		{
			int[] newLabels = new int[s.length()];
			for (int i = 0; i < s.length(); i++)
			{
				Word prev = null;
				if (i > 0)
					prev = s.get(i - 1);
				Word cur = s.get(i);
				Word next = null;
				if ((i + 1) < s.length())
					next = s.get(i + 1);
				try
				{
					newLabels[i] = encode(prev, cur, next);
				}
				catch (AssertionError ex)
				{
					System.err.println(s.getSentenceAsString());
					throw ex;
				}
			}
			for (int i = 0; i < s.length(); i++)
				s.get(i).setTrueLabel(newLabels[i]);
		}
		corpus.setNumClasses(this.getNumClasses());
	}

	public int encode(Word prev, Word cur, Word next)
	{
		int prevLabel = -1;
		if (prev != null)
			prevLabel = Sentiment.mapPolarityClass(prev.getTrueLabel(),
					maxClass + 1);
		int curLabel = Sentiment.mapPolarityClass(cur.getTrueLabel(),
				maxClass + 1);
		int nextLabel = -1;
		if (next != null)
			nextLabel = Sentiment.mapPolarityClass(next.getTrueLabel(),
					maxClass + 1);

		// check chunk annotation for consistency
		if ((curLabel > 0) && !cur.getStartsBlock())
			if (prevLabel != curLabel)
				throw new AssertionError("missing chunk start tag");

		switch (scheme)
		{
			case ENC_NONE:
				return curLabel;
			case ENC_BIO1:
				if (cur.getStartsBlock() &&	(prevLabel == curLabel))
					return maxClass + curLabel;  // B
				else
					return curLabel;  // I, O
			case ENC_BIO2:
				if (cur.getStartsBlock())
					return maxClass + curLabel;  // B
				else
					return curLabel;  // I, O
			case ENC_BIOW:
				if (cur.getStartsBlock())
				{
					if ((nextLabel <= 0) || next.getStartsBlock())
						return (maxClass * 2) + curLabel;  // W
					else
						return maxClass + curLabel;  // B
				}
				else
					return curLabel;  // I, O
			case ENC_BMEO:
				if (cur.getStartsBlock())
					return maxClass + curLabel;  // B
				else if ((curLabel != 0) &&
						 ((nextLabel <= 0) || next.getStartsBlock()))
					return (maxClass * 2) + curLabel;  // E
				else
					return curLabel;  // M, O
			case ENC_BMEOW:
			{
				boolean nextIsNewBlock =
					((nextLabel <= 0) || next.getStartsBlock());
				if (cur.getStartsBlock())
				{
					if (nextIsNewBlock)
						return (maxClass * 3) + curLabel;  // W
					else
						return maxClass + curLabel;  // B
				}
				else if ((curLabel != 0) && nextIsNewBlock)
					return (maxClass * 2) + curLabel;  // E
				else
					return curLabel;  // M, O
			}
			case ENC_BMEOW_PLUS:
			{
				boolean nextIsNewBlock =
					((nextLabel <= 0) || next.getStartsBlock());
				if (cur.getStartsBlock())
				{
					if (nextIsNewBlock)
						return (maxClass * 3) + curLabel;  // W
					else
						return maxClass + curLabel;  // B
				}
				else if ((curLabel != 0) && nextIsNewBlock)
					return (maxClass * 2) + curLabel;  // E
				else if (curLabel == 0)
				{
					// E O O -> BB_O (4)
					// O O O -> MM_O
					// O O B -> EE_O (5)
					// E O B -> WW_O (6)
					if (prevLabel > 0)
					{
						if ((next != null) && next.getStartsBlock())
							return (maxClass * 6) + nextLabel;  // WW
						else
							return (maxClass * 4) + prevLabel;  // BB
					}
					else if ((next != null) && next.getStartsBlock())
						return (maxClass * 5) + nextLabel;  // EE
					else
						return 0;
				}
				else
					return curLabel;  // M		
			}
		}
		return 0;
	}

	public void decode(Corpus corpus)
	{
		for (Sentence s : corpus)
			for (Word w : s)
			{
				w.setTrueLabel(decode(w.getTrueLabel()));
				w.setLabel(decode(w.getLabel()));
			}
		corpus.setNumClasses(maxClass + 1);
	}

	public int decode(int label)
	{
		if (scheme == EncodingScheme.ENC_BMEOW_PLUS)
		{
			if (label > (maxClass * 4))
				label = 0;
		}
		while (label > maxClass)
			label -= maxClass;
		return label;
	}

	public boolean[][] getTransitionConstraints()
	{
		int numClasses = getNumClasses();
		boolean[][] constr = new boolean[numClasses][numClasses];

		// TODO: replace with a number of static 2D arrays, expand according to
		//   maxClass
		switch (scheme)
		{
			case ENC_NONE:
				// allowed: all transitions
				// disallowed: none
				return null;
			case ENC_BIO1:
				// allowed: B,I->B, B,I,O->I, B,I,O->O
				// disallowed: O->B
				for (int i = 1; i <= maxClass; i++)
					constr[0][maxClass + i] = true;
				break;
			case ENC_BIO2:
				// allowed: B,I,O->B, B,I->I, B,I,O->O
				// disallowed: O->I
				for (int i = 1; i <= maxClass; i++)
					constr[0][i] = true;
				break;
			case ENC_BIOW:
				// allowed: I,O,W->B, B,I->I, I,O,W->O, I,O,W->W
				// disallowed: B->B, O,W->I, B->O, B->W
				for (int i = 1; i <= maxClass; i++)
				{
					// O->I
					constr[0][i] = true;
					// B->O
					constr[maxClass + i][0] = true;

					for (int j = 1; j <= maxClass; j++)
					{
						// B->B
						constr[maxClass + i][maxClass + j] = true;
						// W->I
						constr[(maxClass * 2) + i][j] = true;
						// B->W
						constr[maxClass + i][(maxClass * 2) + j] = true;
					}
				}
				break;
			case ENC_BMEO:
				// allowed: B,E,O->B, B,M->M, B,M->E, B,E,O->O
				// disallowed: M->B, E,O->M, E,O->E, M->O
				for (int i = 1; i <= maxClass; i++)
				{
					// O->M
					constr[0][i] = true;
					// O->E
					constr[0][(maxClass * 2) + i] = true;
					// M->O
					constr[i][0] = true;

					for (int j = 1; j <= maxClass; j++)
					{
						// M->B
						constr[i][maxClass + j] = true;
						// E->M
						constr[(maxClass * 2) + i][j] = true;
						// E->E
						constr[(maxClass * 2) + i][(maxClass * 2) + j] = true;
					}
				}
				break;
			case ENC_BMEOW:
				// allowed: E,O,W->B, B,M->M, B,M->E, E,O,W->O, E,O,W->W
				// disallowed: B,M->B, E,O,W->M, E,O,W->E, B,M->O, B,M->W
				for (int i = 1; i <= maxClass; i++)
				{
					// O->M
					constr[0][i] = true;
					// O->E
					constr[0][(maxClass * 2) + i] = true;
					// B->O
					constr[maxClass + i][0] = true;
					// M->O
					constr[i][0] = true;

					for (int j = 1; j <= maxClass; j++)
					{
						// B->B
						constr[maxClass + i][maxClass + j] = true;
						// M->B
						constr[i][maxClass + j] = true;
						// E->M
						constr[(maxClass * 2) + i][j] = true;
						// W->M
						constr[(maxClass * 3) + i][j] = true;
						// E->E
						constr[(maxClass * 2) + i][(maxClass * 2) + j] = true;
						// W->E
						constr[(maxClass * 3) + i][(maxClass * 2) + j] = true;
						// B->W
						constr[maxClass + i][(maxClass * 3) + j] = true;
						// M->W
						constr[i][(maxClass * 3) + j] = true;
					}
				}
				break;
			case ENC_BMEOW_PLUS:
				// allowed: E,EE_O,WW_O,W->B, B,M->M, B,M->E, E,EE_O,WW_O,W->W,
				//   E,EE_O,WW_O,W->BB_O, BB_O,MM_O->MM_O, BB_O,MM_O->EE_O,
				//   E,EE_O,WW_O,W->WW_O
				// disallowed: B,M,BB_O,MM_O->B, E,BB_O,MM_O,EE_O,WW_O,W->M,
				//   E,BB_O,MM_O,EE_O,WW_O,W->E, B,M,BB_O,MM_O->W,
				//   B,M,BB_O,MM_O->BB_O, B,M,E,EE_O,WW_O,W->MM_O,
				//   B,M,E,EE_O,WW_O,W->EE_O, B,M,BB_O,MM_O->WW_O
				for (int i = 1; i <= maxClass; i++)
				{
					// MM_O->M
					constr[0][i] = true;
					// MM_O->B
					constr[0][maxClass + i] = true;
					// MM_O->E
					constr[0][(maxClass * 2) + i] = true;
					// MM_O->W
					constr[0][(maxClass * 3) + i] = true;
					// MM_O->BB_O
					constr[0][(maxClass * 4) + i] = true;
					// MM_O->WW_O
					constr[0][(maxClass * 6) + i] = true;
					
					// M->MM_O
					constr[i][0] = true;
					// B->MM_O
					constr[maxClass + i][0] = true;
					// E->MM_O
					constr[(maxClass * 2) + i][0] = true;
					// W->MM_O
					constr[(maxClass * 3) + i][0] = true;
					// EE_O->MM_O
					constr[(maxClass * 5) + i][0] = true;
					// WW_O->MM_O
					constr[(maxClass * 6) + i][0] = true;

					for (int j = 1; j <= maxClass; j++)
					{
						// B->B
						constr[maxClass + i][maxClass + j] = true;
						// M->B
						constr[i][maxClass + j] = true;
						// BB_O->B
						constr[(maxClass * 4) + i][maxClass + j] = true;
						// E->M
						constr[(maxClass * 2) + i][j] = true;
						// BB_O->M
						constr[(maxClass * 4) + i][j] = true;
						// EE_O->M
						constr[(maxClass * 5) + i][j] = true;
						// WW_O->M
						constr[(maxClass * 6) + i][j] = true;
						// W->M
						constr[(maxClass * 3) + i][j] = true;
						// E->E
						constr[(maxClass * 2) + i][(maxClass * 2) + j] = true;
						// BB_O->E
						constr[(maxClass * 4) + i][(maxClass * 2) + j] = true;
						// EE_O->E
						constr[(maxClass * 5) + i][(maxClass * 2) + j] = true;
						// WW_O->E
						constr[(maxClass * 6) + i][(maxClass * 2) + j] = true;
						// W->E
						constr[(maxClass * 3) + i][(maxClass * 2) + j] = true;
						// B->BB_O
						constr[maxClass + i][(maxClass * 4) + j] = true;
						// M->BB_O
						constr[i][(maxClass * 4) + j] = true;
						// BB_O->BB_O
						constr[(maxClass * 4) + i][(maxClass * 4) + j] = true;
						// B->EE_O
						constr[maxClass + i][(maxClass * 5) + j] = true;
						// M->EE_O
						constr[i][(maxClass * 5) + j] = true;
						// E->EE_O
						constr[(maxClass * 2) + i][(maxClass * 5) + j] = true;
						// EE_O->EE_O
						constr[(maxClass * 5) + i][(maxClass * 5) + j] = true;
						// WW_O->EE_O
						constr[(maxClass * 6) + i][(maxClass * 5) + j] = true;
						// W->EE_O
						constr[(maxClass * 3) + i][(maxClass * 5) + j] = true;
						// B->WW_O
						constr[maxClass + i][(maxClass * 6) + j] = true;
						// M->WW_O
						constr[i][(maxClass * 6) + j] = true;
						// BB_O->WW_O
						constr[(maxClass * 4) + i][(maxClass * 6) + j] = true;
					}
				}
				break;
		}

		return constr;
	}

	public int getNumClasses()
	{
		switch (scheme)
		{
			case ENC_NONE:  // IO
				return maxClass + 1;
			case ENC_BIO1:
			case ENC_BIO2:
				return (maxClass * 2) + 1;
			case ENC_BIOW:
			case ENC_BMEO:
				return (maxClass * 3) + 1;
			case ENC_BMEOW:
				return (maxClass * 4) + 1;
			case ENC_BMEOW_PLUS:
				return (maxClass * 7) + 1;
		}
		return 0;
	}

	public String getSchemeName()
	{
		return encodingSchemeNames[scheme.ordinal()];
	}

	private static EncodingScheme ordinalToScheme(int ordinal)
	{
		switch (ordinal)
		{
			case 0:
				return EncodingScheme.ENC_NONE;
			case 1:
				return EncodingScheme.ENC_BIO1;
			case 2:
				return EncodingScheme.ENC_BIO2;
			case 3:
				return EncodingScheme.ENC_BIOW;
			case 4:
				return EncodingScheme.ENC_BMEO;
			case 5:
				return EncodingScheme.ENC_BMEOW;
			case 6:
				return EncodingScheme.ENC_BMEOW_PLUS;
			default:
				return EncodingScheme.ENC_NONE;
		}
	}

	public static String getSchemeNames()
	{
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < encodingSchemeNames.length; i++)
		{
			if (i > 0)
				buf.append(", ");
			buf.append(encodingSchemeNames[i]);
		}
		return buf.toString();
	}

	public String getSchemeDescription()
	{
		StringBuffer buf = new StringBuffer();
		buf.append(maxClass + 1);
		buf.append(" classes, ");
		buf.append(getSchemeName());
		buf.append(" encoding -> ");
		buf.append(getNumClasses());
		buf.append(" distinct labels\n");
		return buf.toString();
	}

}

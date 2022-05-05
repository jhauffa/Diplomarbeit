package de.tum.in.Linguistics;

import java.util.Arrays;

public class PersonalName {

	private static final String[] shortTitles = {
		"dr", "mr", "mrs", "ms", "prof"
	};
	private static final String[] fullTitles = {
		"doctor", "miss", "mister", "professor", "sir"
	};

	public static boolean isName(String word)
	{
		// is anonymized name (== UUID)?
		return ((word.length() == 38) && (word.charAt(0) == '{'));
	}

	public static boolean isShortTitle(String word)
	{
		int pos = Arrays.binarySearch(shortTitles, word,
				String.CASE_INSENSITIVE_ORDER);
		return (pos >= 0);		
	}

	public static boolean isTitle(String word)
	{
		int pos = Arrays.binarySearch(shortTitles, word,
				String.CASE_INSENSITIVE_ORDER);
		if (pos < 0)
			pos = Arrays.binarySearch(fullTitles, word,
					String.CASE_INSENSITIVE_ORDER);
		return (pos >= 0);
	}

}

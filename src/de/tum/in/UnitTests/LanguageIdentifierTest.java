package de.tum.in.UnitTests;

import de.tum.in.Linguistics.LanguageIdentifierUnigram;
import de.tum.in.Linguistics.LanguageIdentifierBigram;
import junit.framework.TestCase;

public class LanguageIdentifierTest extends TestCase {

	private static final String msgGerman = "Sehr geehrter Herr Hauffa,"+
		"vielen Dank für Ihre Bestellung auf quelle.de, deren Eingang wir"+
		"hiermit bestätigen."+
		"Nachfolgend finden Sie zur Kontrolle alle Details zu Ihrem"+ 
		"Auftrag, den wir umgehend für Sie bearbeiten. "+
		"Bei Fragen kontaktieren Sie uns bitte per Telefon 0180/ 5 31 00"+ 
		"(€ 0,14 je Minute aus dem Festnetz der Dt. Telekom / Mobilfunknetze"+ 
		"ggf. höher)."+
		"Vielen Dank für Ihre Treue und Ihr Vertrauen,"+
		"Ihr QUELLE Online-Team";
	private static final String msgEnglish = "We are migrating the resource " +
		"packages from the /usr/ directory to the " + 
		"new /resources/ directory. The latter is a symlink to the former on " +
		"Syllable Desktop 0.6.6 and Syllable Server 0.3. Eventually, it will " + 
		"be a separate directory, leaving only legacy content for backwards " +
		"compatibility in /usr/. To get there, all resource packages will " +
		"need to be compiled into the /resources/ directory. I now switched " +
		"Builder over to do that, including many build recipes that are " +
		"referring to the path explicitly." +
		"If you are still using Builder on Syllable 0.6.5, either don't " +
		"update Builder or create the new /resources/ symlink yourself." +
		"Kaj";

	public void testIdentifier()
	{
		assertFalse(LanguageIdentifierUnigram.isLanguage(msgGerman,
				LanguageIdentifierUnigram.EN, false));
		assertTrue(LanguageIdentifierUnigram.isLanguage(msgEnglish,
				LanguageIdentifierUnigram.EN, false));

		assertFalse(LanguageIdentifierBigram.isLanguage(msgGerman,
				LanguageIdentifierBigram.EN));
		assertTrue(LanguageIdentifierBigram.isLanguage(msgEnglish,
				LanguageIdentifierBigram.EN));
		assertTrue(LanguageIdentifierBigram.isLanguage(msgGerman,
				LanguageIdentifierBigram.DE, LanguageIdentifierBigram.EN));
		assertTrue(LanguageIdentifierBigram.isLanguage(msgEnglish,
				LanguageIdentifierBigram.EN, LanguageIdentifierBigram.DE));
	}

}

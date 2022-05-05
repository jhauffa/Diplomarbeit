package de.tum.in.UnitTests;

import junit.framework.TestCase;

import de.tum.in.Util.ArgumentParser;
import de.tum.in.Util.ArgumentParser.ArgumentType;


public class ArgumentParserTest extends TestCase {

	private static final String[] args =
		{"-a", "-b", "12", "-c", "0.2", "-d", "blabla", "-e2", "10",
		 "anon1", "anon2"};

	public void testParser()
	{
		ArgumentParser parser = new ArgumentParser("testApp");
		parser.addArgument('a', "test 1", ArgumentType.BOOLEAN);
		parser.addArgument('b', "test 2", ArgumentType.INT);
		parser.addArgument('c', "test 3", ArgumentType.DOUBLE);
		parser.addArgument('d', "test 4", ArgumentType.STRING);
		parser.addArgument('e', "test 5", ArgumentType.INT, 4);
		parser.addAnonymousArgument("name", "test 6");
		parser.addAnonymousArgument("something", "test 7");

		parser.parse(args);
		assertEquals(true, parser.getBooleanValue('a', false));
		assertEquals(12, parser.getIntValue('b', 0));
		assertEquals(0.2, parser.getDoubleValue('c', 0.0));
		assertEquals("blabla", parser.getStringValue('d', ""));
		assertEquals(10, parser.getIntValue('e', 1, 0));
		assertEquals("anon1", parser.getAnonArgument(0));
		assertEquals("anon2", parser.getAnonArgument(1));
	}

}

package de.tum.in.Util;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ArgumentParser {

	public enum ArgumentType {INT, DOUBLE, BOOLEAN, STRING};

	private class ArgumentInfo
	{
		public String description;
		public ArgumentType type;
		public int numInstances;
	}

	private class AnonymousArgumentInfo
	{
		public String tag;
		public String description;
	}

	private String appName;
	private LinkedHashMap<Character, ArgumentInfo> validArguments;
	private HashMap<String, Integer> intValues;
	private HashMap<String, Double> doubleValues;
	private HashMap<String, Boolean> booleanValues;
	private HashMap<String, String> stringValues;
	private Vector<AnonymousArgumentInfo> validAnonArguments;
	private Vector<String> anonArgs;

	public ArgumentParser(String appName)
	{
		this.appName = appName;

		validArguments = new LinkedHashMap<Character, ArgumentInfo>();
		intValues = new HashMap<String, Integer>();
		doubleValues = new HashMap<String, Double>();
		booleanValues = new HashMap<String, Boolean>();
		stringValues = new HashMap<String, String>();
		validAnonArguments = new Vector<AnonymousArgumentInfo>();
		anonArgs = new Vector<String>();
	}

	public void addArgument(char tag, String description, ArgumentType type)
	{
		addArgument(tag, description, type, 1);
	}

	public void addArgument(char tag, String description, ArgumentType type,
			int numInstances)
	{
		ArgumentInfo info = new ArgumentInfo();
		info.description = description;
		info.type = type;
		info.numInstances = numInstances;
		validArguments.put(tag, info);
	}

	public void addAnonymousArgument(String tag, String description)
	{
		AnonymousArgumentInfo info = new AnonymousArgumentInfo();
		info.tag = tag;
		info.description = description;
		validAnonArguments.add(info);
	}

	private void printUsageAndExit(String message, int code)
	{
		if (message != null)
			System.err.printf("error: %s\n", message);
		System.err.printf("\nusage: %s ", appName);
		if (validArguments != null)
			System.err.print("[options] ");
		if (validAnonArguments != null)
			for (AnonymousArgumentInfo info : validAnonArguments)
				System.err.printf("%s ", info.tag);
		System.err.println();
		if (validAnonArguments != null)
			for (AnonymousArgumentInfo info : validAnonArguments)
				System.err.printf("\t%s\n", info.description);
		System.err.println("valid options are:");
		for (Map.Entry<Character, ArgumentInfo> e : validArguments.entrySet())
		{
			System.err.printf("\t-%c", e.getKey());
			if (e.getValue().numInstances > 1)
				System.err.printf("[1..%d]", e.getValue().numInstances);
			if (e.getValue().type != ArgumentType.BOOLEAN)
				System.err.print(" x");
			System.err.printf("\t%s\n", e.getValue().description);
		}
		System.err.println("\t-h\tshow this text\n");
		System.exit(code);
	}

	public void parse(String[] args)
	{
		int i = 0;
		while (i < args.length)
		{
			if ((args[i].length() > 1) && (args[i].charAt(0) == '-'))
			{
				char tag = args[i].charAt(1);
				int instIdx = 0;
				if (args[i].length() > 2)
					instIdx = (args[i].charAt(2) - '0') - 1;

				ArgumentInfo info = validArguments.get(tag);
				if (info != null)
				{
					if ((instIdx < 0) || (instIdx >= info.numInstances))
						printUsageAndExit("invalid instance index " +
								Integer.toString(instIdx), 1);
					String key = tag + Integer.toString(instIdx);

					if (info.type != ArgumentType.BOOLEAN)
					{
						if (++i >= args.length)
							printUsageAndExit("missing argument", 1);
						try
						{
							if (info.type == ArgumentType.INT)
								intValues.put(key, Integer.parseInt(args[i]));
							else if (info.type == ArgumentType.DOUBLE)
								doubleValues.put(key,
										Double.parseDouble(args[i]));
							else if (info.type == ArgumentType.STRING)
								stringValues.put(key, args[i]);
						}
						catch (NumberFormatException ex)
						{
							printUsageAndExit("invalid parameter "+ args[i], 1);
						}
					}
					else
						booleanValues.put(key, true);						
				}
				else if (tag == 'h')
					printUsageAndExit(null, 0);
				else
					printUsageAndExit("invalid option " + args[i], 1);
			}
			else
				anonArgs.add(args[i]);
			i++;
		}

		if (anonArgs.size() < validAnonArguments.size())
			printUsageAndExit("not enough arguments", 1);
	}

	public int getIntValue(char key, int defaultValue)
	{
		return getIntValue(key, 0, defaultValue);
	}

	public int getIntValue(char key, int instIdx, int defaultValue)
	{
		Integer value = intValues.get(key + Integer.toString(instIdx));
		if (value == null)
			return defaultValue;
		return value;
	}

	public double getDoubleValue(char key, double defaultValue)
	{
		return getDoubleValue(key, 0, defaultValue);
	}

	public double getDoubleValue(char key, int instIdx, double defaultValue)
	{
		Double value = doubleValues.get(key + Integer.toString(instIdx));
		if (value == null)
			return defaultValue;
		return value;
	}

	public boolean getBooleanValue(char key, boolean defaultValue)
	{
		return getBooleanValue(key, 0, defaultValue);
	}

	public boolean getBooleanValue(char key, int instIdx, boolean defaultValue)
	{
		Boolean value = booleanValues.get(key + Integer.toString(instIdx));
		if (value == null)
			return defaultValue;
		return value;
	}

	public String getStringValue(char key, String defaultValue)
	{
		return getStringValue(key, 0, defaultValue);
	}

	public String getStringValue(char key, int instIdx, String defaultValue)
	{
		String value = stringValues.get(key + Integer.toString(instIdx));
		if (value == null)
			return defaultValue;
		return value;
	}

	public String getAnonArgument(int idx)
	{
		return anonArgs.get(idx);
	}

}

package de.tum.in.MPQA;

import java.io.File;

public class SyntaxError extends Exception {

	private final String message;

	public SyntaxError(File f, int line)
	{
		message = "file '" + f.getPath() + "', line " + Integer.toString(line);
	}

	public String getMessage()
	{
		return message;
	}

}

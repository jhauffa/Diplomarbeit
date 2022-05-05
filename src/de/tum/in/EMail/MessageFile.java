package de.tum.in.EMail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.tum.in.Util.DynamicByteBuffer;
import de.tum.in.Util.ProgressNotificationSink;


public class MessageFile extends MessageDatabase {

	public MessageFile(File f) throws IOException
	{
		this(f, new PersonDatabase(), null);
	}

	public MessageFile(File f, ProgressNotificationSink p)
		throws IOException
	{
		this(f, new PersonDatabase(), p);
	}

	public MessageFile(File f, PersonDatabase db, ProgressNotificationSink p)
		throws IOException
	{
		super(db);

		try
		{
			DynamicByteBuffer msgBuffer = new DynamicByteBuffer();
			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(f));
			int curByte;
			while ((curByte = in.read()) >= 0)  // !EOF
				msgBuffer.append((byte) curByte);
			in.close();

			add(new Message(personDb, msgBuffer.getBuffer(), 0,
					msgBuffer.size()), false);
			if (p != null)
				p.step();
		}
		catch (IOException ex)
		{
			System.err.printf("error reading EML file: %s\n", ex.getMessage());
			ex.printStackTrace();
			throw ex;
		}
	}

}

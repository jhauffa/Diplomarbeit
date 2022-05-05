package de.tum.in.EMail;

import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import de.tum.in.Util.DynamicByteBuffer;
import de.tum.in.Util.ProgressNotificationSink;


public class MboxFile extends MessageDatabase {

	public MboxFile(File f) throws IOException
	{
		this(f, new PersonDatabase(), null);
	}

	public MboxFile(File f, ProgressNotificationSink p) throws IOException
	{
		this(f, new PersonDatabase(), p);
	}

	public MboxFile(File f, PersonDatabase db, ProgressNotificationSink p)
		throws IOException
	{
		super(db);

		try
		{
			BufferedInputStream in = new BufferedInputStream(
					new FileInputStream(f));

			DynamicByteBuffer msgBuffer = null;

			boolean precedingLineEmpty = true;
			DynamicByteBuffer lineBuffer = new DynamicByteBuffer(128);
			int curByte;
			int prevByte = -1;  // -1 => curByte is first byte of line
			while ((curByte = in.read()) >= 0)  // !EOF
			{
				lineBuffer.append((byte) curByte);
				if (curByte == MessageEncoding.LF)
				{
					if (precedingLineEmpty &&
						lineBuffer.startsWithAscii("From "))
					{
						parseMessage(msgBuffer, p);
						msgBuffer = new DynamicByteBuffer();
						precedingLineEmpty = false;
					}
					else if (msgBuffer != null)
					{
						precedingLineEmpty = (prevByte == -1) ||
								(prevByte == MessageEncoding.CR);
						msgBuffer.append(lineBuffer);
					}

					lineBuffer.clear();
					prevByte = -1;
				}
				else
					prevByte = curByte;
			}
			in.close();

			// add final message, if it exists
			parseMessage(msgBuffer, p);

			System.err.printf("read %d messages\n", messages.size());  // DBG
		}
		catch (IOException ex)
		{
			System.err.printf("error reading mbox file: %s\n", ex.getMessage());
			ex.printStackTrace();
			// return;
			throw ex;
		}
	}

	private void parseMessage(DynamicByteBuffer msgBuffer,
			ProgressNotificationSink p)
	{
		if (msgBuffer != null)
		{
			add(new Message(personDb, msgBuffer.getBuffer(), 0,
					msgBuffer.size()), false);
			if (p != null)
				p.step();
		}
	}

}

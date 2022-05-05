package de.tum.in.Util;

import java.util.Date;

public class Uninterrupted {

	public static void join(Thread t)
	{
		boolean finished = false;
		while (!finished)
		{
			try
			{
				t.join();
				finished = true;
			}
			catch (InterruptedException ex)
			{
			}
		}
	}

	public static void sleep(int ms)
	{
		while (ms > 0)
		{
			Date d1 = new Date();
			try
			{
				Thread.sleep(ms);
				ms = 0;
			}
			catch (InterruptedException ex)
			{
				Date d2 = new Date();
				ms -= d2.getTime() - d1.getTime();
			}
		}
	}

}

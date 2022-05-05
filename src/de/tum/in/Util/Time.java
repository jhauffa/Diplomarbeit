package de.tum.in.Util;

import java.util.Calendar;
import java.util.TimeZone;
import java.lang.StringBuffer;

public class Time {

	private static void padDateComponent(StringBuffer buf, int c)
	{
		if (c < 10)
			buf.append('0');
		buf.append(Integer.toString(c));
	}

	public static String getTimeStamp()
	{
		Calendar cal = Calendar.getInstance();
		StringBuffer buf = new StringBuffer(12); 
		buf.append(Integer.toString(cal.get(Calendar.YEAR)));
		padDateComponent(buf, cal.get(Calendar.MONTH) + 1);
		padDateComponent(buf, cal.get(Calendar.DAY_OF_MONTH));
		padDateComponent(buf, cal.get(Calendar.HOUR_OF_DAY));
		padDateComponent(buf, cal.get(Calendar.MINUTE));
		return buf.toString();
	}

	public static String getTimeStampGmt()
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		StringBuffer buf = new StringBuffer(14);
		buf.append(Integer.toString(cal.get(Calendar.YEAR)));
		padDateComponent(buf, cal.get(Calendar.MONTH) + 1);
		padDateComponent(buf, cal.get(Calendar.DAY_OF_MONTH));
		padDateComponent(buf, cal.get(Calendar.HOUR_OF_DAY));
		padDateComponent(buf, cal.get(Calendar.MINUTE));
		padDateComponent(buf, cal.get(Calendar.SECOND));
		return buf.toString();
	}

	public static String getTimeStampW3C()
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		StringBuffer buf = new StringBuffer(20);
		buf.append(Integer.toString(cal.get(Calendar.YEAR)));
		buf.append('-');
		padDateComponent(buf, cal.get(Calendar.MONTH) + 1);
		buf.append('-');
		padDateComponent(buf, cal.get(Calendar.DAY_OF_MONTH));
		buf.append('T');
		padDateComponent(buf, cal.get(Calendar.HOUR_OF_DAY));
		buf.append(':');
		padDateComponent(buf, cal.get(Calendar.MINUTE));
		buf.append(':');
		padDateComponent(buf, cal.get(Calendar.SECOND));
		buf.append('Z');
		return buf.toString();
	}

}

package de.tum.in.SocialWebCrawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.lang.StringBuffer;
import java.util.UUID;

import de.tum.in.Util.Time;


public class WarcWriter {

	private static final String crlf = "\r\n";
	private static final String recordDelimiter = crlf + crlf;

	private FileOutputStream out;
	private String fileName;

	public WarcWriter(String prefix, String userAgent) throws IOException
	{
		// Construct file name according to the pattern given in Annex B of the
		// WARC specification.
		String timeStamp = Time.getTimeStampGmt();
		String hostName = InetAddress.getLocalHost().getHostName();
		fileName = prefix + "-" + timeStamp + "-1-" + hostName + ".warc";

		// create new archive with specified prefix
		out = new FileOutputStream(fileName);
		writeInfoRecord(hostName, userAgent);
	}

	public WarcWriter(File file, String userAgent) throws IOException
	{
		// append to file, if already existing
		fileName = file.getName();
		boolean exists = file.exists();
		out = new FileOutputStream(file, true);
		if (!exists)
			writeInfoRecord(InetAddress.getLocalHost().getHostName(),
					userAgent);
	}

	private String generateUUID()
	{
		return "<urn:uuid:" + UUID.randomUUID().toString() + ">";
	}

	private void writeInfoRecord(String hostName, String userAgent)
		throws IOException
	{
		StringBuffer content = new StringBuffer();
		content.append("software: SocialWebCrawler" + crlf);
		content.append("hostname: " + hostName + crlf);
		content.append("http-header-user-agent: " + userAgent + crlf);
		content.append(
"robots: observing Crawl-delay, ignoring all other fields" + crlf +
"format: WARC file version 0.18" + crlf +
"conformsTo: http://www.archive.org/documents/WarcFileFormat-0.18.html" + crlf);

		StringBuffer record = new StringBuffer();
		record.append("WARC/0.18" + crlf);
		record.append("WARC-Type: warcinfo" + crlf);
		record.append("WARC-Date: " + Time.getTimeStampW3C() + crlf);
		record.append("WARC-Record-ID: " + generateUUID() + crlf);
		record.append("Content-Type: application/warc-fields" + crlf);
		record.append("Content-Length: " + Integer.toString(content.length()) +
				crlf + crlf);
		record.append(content);
		record.append(recordDelimiter);

		out.write(record.toString().getBytes("UTF-8"));
		out.flush();
	}

	public void write(WebResource res) throws IOException
	{
		StringBuffer header = new StringBuffer();
		header.append("WARC/0.18" + crlf);
		header.append("WARC-Type: response" + crlf);
		header.append("WARC-Target-URI: " + res.url + crlf);
		header.append("WARC-Date: " + Time.getTimeStampW3C() + crlf);
		header.append("WARC-Record-ID: " + generateUUID() + crlf);
		header.append("Content-Type: application/http;msgtype=response" + crlf);
		if (res.contentType != null)
			header.append("WARC-Identified-Payload-Type: " + res.contentType +
					crlf);
		int contentLength = res.header.length() + 1;
		if (res.content != null)
			contentLength += res.content.length;
		header.append("Content-Length: " + Integer.toString(contentLength) +
				crlf + crlf);
		header.append(res.header);
		header.append("\n");  // part of the HTTP server's response

		out.write(header.toString().getBytes("UTF-8"));
		if (res.content != null)
			out.write(res.content);
		out.write(recordDelimiter.getBytes("UTF-8"));
		out.flush();
	}

	public void close()
	{
		try
		{
			out.close();
		}
		catch (IOException ex)
		{
		}
	}

	public String getFileName()
	{
		return fileName;
	}

}

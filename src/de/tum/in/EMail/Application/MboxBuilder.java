package de.tum.in.EMail.Application;

import java.util.ArrayList;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JFileChooser;

import de.tum.in.EMail.Message;
import de.tum.in.EMail.MessageEncoding;
import de.tum.in.EMail.Person;


public class MboxBuilder extends JFrame {

	private MessageEntryPanel panel;

	private ArrayList<Message> messages;

	public MboxBuilder()
	{
		super("Mbox Builder");
		messages = new ArrayList<Message>();

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(500, 300));
		setLocationByPlatform(true);

		panel = new MessageEntryPanel();
		add(panel, BorderLayout.CENTER);
		add(new MboxNavigationPanel(this), BorderLayout.SOUTH);
		pack();
	}

	public void addMessage()
	{
		Message msg = new Message();

		Person sender = new Person();
		sender.addName(panel.getSender());
		msg.setSender(sender);

		Person recipient = new Person();
		recipient.addName(panel.getRecipient());
		msg.getRecipients().add(recipient);

		try
		{
			msg.setDate(DateFormat.getDateInstance().parse(panel.getDate()));
		}
		catch (ParseException ex)
		{
		}

		msg.setSubject(panel.getSubject());
		msg.setBody(panel.getBody());

		messages.add(msg);
		panel.clear();
	}

	public void finish()
	{
		JFileChooser fileChooser = new JFileChooser();
		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File destFile = fileChooser.getSelectedFile();
			try
			{
				writeMessages(destFile);
			}
			catch (IOException ex)
			{
				// TODO
				System.err.printf("error writing messages: %s\n",
						ex.getMessage());
			}
		}

		dispose();
	}

	private String encode(String text, boolean inHeader)
	{
		String enc = MessageEncoding.encodeQuotedPrintable(text, "UTF-8",
				inHeader);
		if (inHeader)
			return "=?utf-8?q?" + enc + "?=";
		return enc;
	}

	private String getFakeMail(Person p)
	{
		String id = p.getId();
		return id.substring(1, id.length() - 1) + "@foo.bar";
	}

	private void writeMessages(File file) throws IOException
	{
		DateFormat dateFmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z",
				Locale.US);
		PrintWriter writer = new PrintWriter(new FileOutputStream(file));
		int i = 0;
		for (Message msg : messages)
		{
			writer.println("From - ");
			Person sender = msg.getSender();
			String enc = encode(sender.getDisplayName(), true);
			writer.printf("From: %s <%s>\n", enc, getFakeMail(sender));
			Person recipient = msg.getRecipients().get(0);
			enc = encode(recipient.getDisplayName(), true);
			writer.printf("To: %s <%s>\n", enc, getFakeMail(recipient));
			writer.printf("Date: %s\n", dateFmt.format(msg.getDate()));
			enc = encode(msg.getSubject(), true);
			writer.printf("Subject: %s\n", enc);
			writer.println("Content-Type: text/plain; charset=utf-8");
			writer.println("Content-Transfer-Encoding: quoted-printable");
			writer.println();
			enc = encode(msg.getBody(), false);
			writer.println(enc);
			writer.println();
			i++;
		}
		writer.close();
	}

	public static void main(String[] args)
	{
		MboxBuilder mainWnd = new MboxBuilder();
		mainWnd.setVisible(true);
	}

}

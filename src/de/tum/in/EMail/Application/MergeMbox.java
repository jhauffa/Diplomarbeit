package de.tum.in.EMail.Application;

import java.io.IOException;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import de.tum.in.EMail.MessageEncoding;

public class MergeMbox extends JFrame {

	private static final byte[] emptyLine = {
		MessageEncoding.LF, MessageEncoding.LF
	};
	private static final byte[] messageDelimiter = {
		'F', 'r', 'o', 'm', ' ', '-', ' ', MessageEncoding.LF
	};
	private static final int readBufferSize = 128 * 1024;

	private MboxListPanel list;
	private File path;

	public MergeMbox()
	{
		super("Merge Mailboxes");
		path = new File(".");
		list = new MboxListPanel(this);
		add(list);
		pack();
	}

	public void merge()
	{
		JFileChooser fc = new JFileChooser(path);
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			path = fc.getCurrentDirectory();
			File outFile = fc.getSelectedFile();

			try
			{
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream(outFile));
				for (Object o : list.getFiles())
				{
					String inFileName = (String) o;
					copyToStream(inFileName, out);
				}
				out.close();
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				dispose();
			}

			JOptionPane.showMessageDialog(this, "Created new mailbox file: " +
					outFile.getPath(), "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void copyToStream(String fileName, OutputStream out)
			throws IOException
	{
		int idx = fileName.lastIndexOf('.');
		if (idx >= 0)
		{
			String extension = fileName.substring(idx);
			if (extension.equalsIgnoreCase(".eml"))
				out.write(messageDelimiter);
		}

		BufferedInputStream in = new BufferedInputStream(
				new FileInputStream(fileName), readBufferSize);
		byte[] buf = new byte[readBufferSize];
		int bytesRead = 0;
		while ((bytesRead = in.read(buf)) >= 0)
			out.write(buf, 0, bytesRead);
		in.close();

		out.write(emptyLine);
	}

	public void quit()
	{
		dispose();
	}

	public void setCurPath(File path)
	{
		this.path = path;
	}

	public File getCurPath()
	{
		return path;
	}

	public static void main(String[] args)
	{
		MergeMbox mainWnd = new MergeMbox();
		mainWnd.setVisible(true);
	}

}

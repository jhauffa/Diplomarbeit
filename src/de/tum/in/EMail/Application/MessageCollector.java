package de.tum.in.EMail.Application;

import de.tum.in.EMail.MessageDatabase;
import de.tum.in.EMail.Message;
import de.tum.in.EMail.MboxFile;
import de.tum.in.EMail.MessageFile;
import de.tum.in.EMail.Person;
import de.tum.in.EMail.PersonDatabase;
// import de.tum.in.Linguistics.LanguageIdentifierUnigram;
import de.tum.in.Linguistics.LanguageIdentifierBigram;
import de.tum.in.Util.ProgressNotificationSink;
import de.tum.in.Util.Time;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Iterator;


public class MessageCollector extends JFrame {

	private class Alias
	{
		public Person source;
		public Person dest;

		public Alias(Person source, Person dest)
		{
			this.source = source;
			this.dest = dest;
		}
	}

	private class MailboxLoaderThread extends Thread
	{
		private Object[] inboxes;
		private Object[] outboxes;
		private ProgressNotificationSink p;

		public MailboxLoaderThread(Object[] inboxes, Object[] outboxes,
				ProgressNotificationSink p)
		{
			this.inboxes = inboxes;
			this.outboxes = outboxes;
			this.p = p;
		}

		public void run()
		{
			for (Object o : inboxes)
				loadMailbox((String) o, true, p);
			for (Object o : outboxes)
				loadMailbox((String) o, false, p);

			candidatePersons.sort();
			log.println("\ncandidate persons:");
			candidatePersons.dump(log);

			candidateOwners.sort();
			log.println("\ncadidate owners:");
			candidateOwners.dump(log);

			if (candidatePersons.size() > 0)
			{
				SwingUtilities.invokeLater(
						new Runnable() {
							public void run()
							{
								navigationPanel.toggleAll();
								next();
							}
						});
			}
			else
			{
				SwingUtilities.invokeLater(
						new Runnable() {
							public void run()
							{
								navigationPanel.toggleAll();
								finish();
							}
						});
			}
		}
	}

	private int panelIdx;
	private ContentPanel curContentPanel;
	private NavigationPanel navigationPanel;
	private JPanel centerPanel;
	private MessageDatabase collectedMessages;
	private PersonDatabase collectedPersons;
	private SenderList candidatePersons;
	private SenderList candidateOwners;
	private Vector<Person> ratedPersons;
	private ArrayList<Alias> aliasList;
	private String destDir;
	private StringWriter logBuffer;
	private PrintWriter log;

	public MessageCollector()
	{
		super("E-Mail Collector");
		logBuffer = new StringWriter();
		log = new PrintWriter(logBuffer);
		candidatePersons = new SenderList();
		candidateOwners = new SenderList();
		collectedMessages = new MessageDatabase();
		collectedPersons = new PersonDatabase();
		ratedPersons = new Vector<Person>();
		aliasList = new ArrayList<Alias>();
		destDir = "." + File.separator;

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(500, 300));
		setLocationByPlatform(true);

		panelIdx = -3;
		curContentPanel = null;
		navigationPanel = new NavigationPanel(this);
		centerPanel = new JPanel();
		createUI(new SelectMailboxPanel(this));
	}

	private void createUI(ContentPanel panel)
	{
		if (curContentPanel != null)
			remove(curContentPanel);
		remove(navigationPanel);
		remove(centerPanel);

		curContentPanel = panel;

		add(curContentPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(navigationPanel, BorderLayout.SOUTH);
		pack();		
	}

	public boolean loadMailbox(String path, boolean isInbox,
			ProgressNotificationSink p)
	{
		log.printf("%s (%s)\n", path, isInbox ? "in" : "out");

		boolean isSingleMessage = false;
		int idx = path.lastIndexOf('.');
		if (idx >= 0)
		{
			String extension = path.substring(idx);
			if (extension.equalsIgnoreCase(".eml"))
				isSingleMessage = true;
		}

		MessageDatabase mbox = null;
		try
		{
			if (isSingleMessage)
				mbox = new MessageFile(new File(path), collectedPersons, p);
			else
				mbox = new MboxFile(new File(path), collectedPersons, p);
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(this, "Could not read mailbox file: "+
					ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		MessageDatabase messages = new MessageDatabase();
		for (Message m : mbox)
		{
/*
			if (LanguageIdentifierUnigram.isLanguage(m.getBody(),
					LanguageIdentifierUnigram.EN, true))
*/
			if (LanguageIdentifierBigram.isLanguage(m.getBody(),
					LanguageIdentifierBigram.EN, LanguageIdentifierBigram.DE))
				messages.add(m, true);
/*
			else
				System.err.printf("discarding message \"%s\" from \"%s\"\n",
						m.getSubject(), m.getSender().getDisplayName());
*/
		}

		if (isInbox)
		{
			candidatePersons.addIncomingMessages(messages);
			candidateOwners.addOutgoingMessages(messages);
		}
		else
		{
			candidatePersons.addOutgoingMessages(messages);
			candidateOwners.addIncomingMessages(messages);
		}
		return true;
	}

	public void next()
	{
		if (curContentPanel.evaluate())
		{
			panelIdx++;
			if (panelIdx == -2)
			{
				Object[] inboxes = ((SelectMailboxPanel)
						curContentPanel).getIncomingMailboxes();
				Object[] outboxes = ((SelectMailboxPanel)
						curContentPanel).getOutgoingMailboxes();

				ProgressPanel p = new ProgressPanel();
				navigationPanel.toggleAll();
				createUI(p);

				MailboxLoaderThread thr = new MailboxLoaderThread(inboxes,
						outboxes, p);
				thr.start();
			}
			else if (panelIdx == -1)
			{
				createUI(new SelectOwnerPanel(this, candidateOwners.get()));
			}
			else if (panelIdx < candidatePersons.size())
			{
				createUI(new AnnotationPanel(this,
						candidatePersons.get(panelIdx), ratedPersons,
						panelIdx + 1, candidatePersons.size()));
			}
			else
				finish();
		}
	}

	private void writeLogFile()
	{
		File f = new File(destDir + "log-" + Time.getTimeStamp() + ".txt");
		try
		{
			FileWriter writer = new FileWriter(f);
			writer.write(logBuffer.getBuffer().toString());
			writer.close();
		}
		catch (IOException ex)
		{
		}
	}

	private File writeMessageDatabase(String filePrefix, MessageDatabase db)
	{
		File f = null;
		boolean retry = true;
		while (retry)
		{
			f = new File(destDir + filePrefix + "-"+Time.getTimeStamp()+".dat");
			try
			{
				retry = false;
				MessageDatabase.writeToFile(f, db);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(this, "Could not write results: "+
						ex.getMessage() + "\nPlease select another location.",
						"Error", JOptionPane.ERROR_MESSAGE);

				// allow user to choose a new destination directory
				JFileChooser dirChooser = new JFileChooser();
				dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				dirChooser.setAcceptAllFileFilterUsed(false);
				if (dirChooser.showOpenDialog(this) ==
					JFileChooser.APPROVE_OPTION)
				{
					destDir = dirChooser.getSelectedFile().getPath() +
						File.separator;
					retry = true;
				}
				else
					destDir = null;
			}
		}
		return f;
	}

	public void finish()
	{
		if (collectedMessages.size() > 0)
		{
			writeMessageDatabase("backup", collectedMessages);
			resolveAliases();
			collectedMessages.anonymize(true);

			writeLogFile();

			File f = writeMessageDatabase("mail", collectedMessages);
			if (f == null)
			{
				dispose();
				return;
			}

			collectedMessages.clear();
			createUI(new ResultPanel(f.getPath()));
			navigationPanel.toggleFinishButtonText();
		}
		else
			dispose();
	}

	public void rankPerson(Person p, double valence, double intensity)
	{
		p.setRating("valence", valence);
		p.setRating("intensity", intensity);
		ratedPersons.add(p);
		candidatePersons.copyMessages(p, collectedMessages);
	}

	public void mergePersons(Person p, Person target)
	{
		aliasList.add(new Alias(p, target));
		candidatePersons.copyMessages(p, collectedMessages);
	}

	public void setMailboxOwner(Person p)
	{
		log.printf("\nowner set to %s\n", p.getId());
		collectedMessages.setOwner(p);
	}

	private void resolveAliases()
	{
		for (Message m : collectedMessages)
			for (Alias a : aliasList)
			{
				if (m.getSender() == a.source)
					m.setSender(a.dest);
				replaceInList(m.getRecipients(), a.source, a.dest);
				replaceInList(m.getRecipientsCc(), a.source, a.dest);
				replaceInList(m.getRecipientsBcc(), a.source, a.dest);				
			}
		for (Alias a : aliasList)
			a.dest.merge(a.source);
	}

	private void replaceInList(Vector<Person> list, Person source, Person dest)
	{
		Iterator<Person> it = list.iterator();
		boolean found = false;
		while (it.hasNext())
			if (it.next() == source)
			{
				it.remove();
				found = true;
			}
		if (found)
			list.add(dest);
	}

	public static void main(String[] args)
	{
		MessageCollector mainWnd = new MessageCollector();
		mainWnd.setVisible(true);
	}

}

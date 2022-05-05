package de.tum.in.EMail.Application;

import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class SelectMailboxPanel extends ContentPanel implements ActionListener {

	private class AddItemTask implements Runnable
	{
		private DefaultListModel model;

		public AddItemTask(DefaultListModel model)
		{
			this.model = model;
		}

		public void run()
		{
			JFileChooser fc = new JFileChooser(curPath);
			fc.setMultiSelectionEnabled(true);
			if (fc.showOpenDialog(parent) ==
				JFileChooser.APPROVE_OPTION)
			{
				for (File f : fc.getSelectedFiles())
					model.addElement(f.getPath());
				curPath = fc.getCurrentDirectory();
			}
		}
	}

	private class RemoveItemTask implements Runnable
	{
		private JList list;

		public RemoveItemTask(JList list)
		{
			this.list = list;
		}

		public void run()
		{
			int idx = list.getSelectedIndex();
			if (idx >= 0)
				((DefaultListModel) list.getModel()).remove(idx);
		}
	}

	private MessageCollector parent;
	private JList incomingList;
	private DefaultListModel incomingListData;
	private JList outgoingList;
	private DefaultListModel outgoingListData;
	private JButton[] buttons;
	private File curPath;

	public SelectMailboxPanel(MessageCollector parent)
	{
		super();
		this.parent = parent;
		curPath = new File(".");
		buttons = new JButton[4];

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(0, 10, 0, 10));

		// incoming mail
		JPanel topPanel = new JPanel();
		topPanel.add(new JLabel("Select the file(s) where your e-mail " +
				"application stores the incoming mail."));
		add(topPanel);

		incomingListData = new DefaultListModel();
		incomingList = new JList(incomingListData);
		add(new JScrollPane(incomingList));

		JPanel navigationPanel = new JPanel(
				new FlowLayout(FlowLayout.LEADING));
		buttons[0] = new JButton("Add");
		buttons[0].addActionListener(this);
		navigationPanel.add(buttons[0]);
		buttons[1] = new JButton("Remove");
		buttons[1].addActionListener(this);
		navigationPanel.add(buttons[1]);
		add(navigationPanel);

		// outgoing mail
		topPanel = new JPanel();
		topPanel.add(new JLabel("If possible, select the file(s) where your " +
				"e-mail application stores the outgoing mail."));
		add(topPanel);

		outgoingListData = new DefaultListModel();
		outgoingList = new JList(outgoingListData);
		add(new JScrollPane(outgoingList));

		navigationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		buttons[2] = new JButton("Add");
		buttons[2].addActionListener(this);
		navigationPanel.add(buttons[2]);
		buttons[3] = new JButton("Remove");
		buttons[3].addActionListener(this);
		navigationPanel.add(buttons[3]);
		add(navigationPanel);
	}

	@Override public boolean evaluate()
	{
		if (incomingListData.isEmpty())
		{
			incomingList.setBackground(Color.RED);
			JOptionPane.showMessageDialog(this.getParent(),
					"Please select a mailbox file.", "Missing data",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttons[0])
			SwingUtilities.invokeLater(new AddItemTask(incomingListData));
		else if (e.getSource() == buttons[1])
			SwingUtilities.invokeLater(new RemoveItemTask(incomingList));
		else if (e.getSource() == buttons[2])
			SwingUtilities.invokeLater(new AddItemTask(outgoingListData));
		else if (e.getSource() == buttons[3])
			SwingUtilities.invokeLater(new RemoveItemTask(outgoingList));
	}

	public Object[] getIncomingMailboxes()
	{
		return incomingListData.toArray();
	}

	public Object[] getOutgoingMailboxes()
	{
		return outgoingListData.toArray();
	}

}

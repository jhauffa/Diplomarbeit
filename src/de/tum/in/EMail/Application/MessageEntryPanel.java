package de.tum.in.EMail.Application;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JLabel;

public class MessageEntryPanel extends JPanel {

	private JTextField txtFrom;
	private JTextField txtTo;
	private JTextField txtDate;
	private JTextField txtSubject;
	private JTextPane txtBody;

	public MessageEntryPanel()
	{
		super(new GridBagLayout());

		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;

		constr.gridx = 0;
		constr.gridy = 0;
		constr.weightx = 0;
		add(new JLabel("From:"), constr);
		txtFrom = new JTextField();
		constr.weightx = 1;
		constr.gridx = GridBagConstraints.RELATIVE;
		constr.fill = GridBagConstraints.HORIZONTAL;
		add(txtFrom, constr);

		constr.gridx = 0;
		constr.gridy++;
		constr.weightx = 0;
		add(new JLabel("To:"), constr);
		txtTo = new JTextField();
		constr.weightx = 1;
		constr.gridx = GridBagConstraints.RELATIVE;
		constr.fill = GridBagConstraints.HORIZONTAL;
		add(txtTo, constr);

		constr.gridx = 0;
		constr.gridy++;
		constr.weightx = 0;
		add(new JLabel("Date:"), constr);
		txtDate = new JTextField();
		constr.weightx = 1;
		constr.gridx = GridBagConstraints.RELATIVE;
		constr.fill = GridBagConstraints.HORIZONTAL;
		add(txtDate, constr);

		constr.gridx = 0;
		constr.gridy++;
		constr.weightx = 0;
		add(new JLabel("Subject:"), constr);
		txtSubject = new JTextField();
		constr.weightx = 1;
		constr.gridx = GridBagConstraints.RELATIVE;
		constr.fill = GridBagConstraints.HORIZONTAL;
		add(txtSubject, constr);

		constr.gridx = 0;
		constr.gridy++;
		constr.weightx = 0;
		add(new JLabel("Body:"), constr);
		txtBody = new JTextPane();
		constr.weightx = 1;
		constr.weighty = 1;
		constr.gridx = GridBagConstraints.RELATIVE;
		constr.fill = GridBagConstraints.BOTH;
		add(txtBody, constr);
	}

	public void clear()
	{
		txtFrom.setText("");
		txtTo.setText("");
		txtDate.setText("");
		txtSubject.setText("");
		txtBody.setText("");
	}

	public String getSender()
	{
		return txtFrom.getText();
	}

	public String getRecipient()
	{
		return txtTo.getText();
	}

	public String getDate()
	{
		return txtDate.getText();
	}

	public String getSubject()
	{
		return txtSubject.getText();
	}

	public String getBody()
	{
		return txtBody.getText();
	}

}

package de.tum.in.EMail.Application;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MboxNavigationPanel extends JPanel implements ActionListener {

	private MboxBuilder parent;
	private JLabel statusLabel;
	private JButton nextButton;
	private JButton finishButton;

	private int numMessages;

	public MboxNavigationPanel(MboxBuilder parent)
	{
		super(new FlowLayout(FlowLayout.TRAILING));
		this.parent = parent;

		numMessages = 0;
		statusLabel = new JLabel("0 messages");
		add(statusLabel);
		nextButton = new JButton("Add Message");
		nextButton.addActionListener(this);
		add(nextButton);
		finishButton = new JButton("Save & Quit");
		finishButton.addActionListener(this);
		add(finishButton);
	}

	private synchronized void updateStatusLabel(int n)
	{
		numMessages += n;
		statusLabel.setText(numMessages + " messages");
		validate();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == nextButton)
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						parent.addMessage();
						updateStatusLabel(1);
					}
				});
		else if (e.getSource() == finishButton)
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						parent.finish();
					}
				});
	}

}

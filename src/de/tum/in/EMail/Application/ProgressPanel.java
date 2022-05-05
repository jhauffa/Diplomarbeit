package de.tum.in.EMail.Application;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import de.tum.in.Util.ProgressNotificationSink;


public class ProgressPanel extends ContentPanel
		implements ProgressNotificationSink {

	private static final String msgStart = "Processing message ";
	private static final String msgEnd = "...";

	private int n;
	private JLabel statusLabel;

	public ProgressPanel()
	{
		super();
		n = 0;
		statusLabel = new JLabel();
		JPanel panel = new JPanel();
		panel.add(statusLabel);
		add(panel);
	}

	public void step()
	{
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						n++;
						statusLabel.setText(msgStart + Integer.toString(n) +
								msgEnd);
						validate();
					}
				});
	}

	@Override public boolean evaluate()
	{
		return true;
	}

}

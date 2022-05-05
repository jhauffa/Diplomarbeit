package de.tum.in.EMail.Application;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class NavigationPanel extends JPanel implements ActionListener {

	private MessageCollector parent;
	private JButton nextButton;
	private JButton finishButton;
	private boolean hasFinishText;

	public NavigationPanel(MessageCollector parent)
	{
		super(new FlowLayout(FlowLayout.TRAILING));
		this.parent = parent;

		nextButton = new JButton("Next");
		nextButton.addActionListener(this);
		add(nextButton);
		finishButton = new JButton("Quit");
		finishButton.addActionListener(this);
		add(finishButton);
		hasFinishText = false;
	}

	public void toggleFinishButtonText()
	{
		if (!hasFinishText)
		{
			nextButton.setEnabled(false);
			finishButton.setText("Finish");
		}
		else
		{
			nextButton.setEnabled(true);
			finishButton.setText("Quit");
		}
		hasFinishText = !hasFinishText;
		validate();
	}

	public void toggleAll()
	{
		nextButton.setEnabled(!nextButton.isEnabled());
		finishButton.setEnabled(!finishButton.isEnabled());
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == nextButton)
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						parent.next();
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

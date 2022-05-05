package de.tum.in.EMail.Application;

import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ResultPanel extends ContentPanel {

	private static final String targetMailAddress = "hauffa@in.tum.de";

	public ResultPanel(String fileName)
	{
		super(new GridLayout(2, 1));
		JPanel[] panels = new JPanel[2];
		panels[0] = new JPanel();
		panels[0].add(new JLabel("Your input and the e-mail messages have " +
				"been written to the file \"" + fileName + "\"."));
		panels[1] = new JPanel();
		panels[1].add(new JLabel("Please send this file to " +
				targetMailAddress + "."));
		for (JPanel p : panels)
			add(p);
	}

	@Override public boolean evaluate()
	{
		return true;
	}

}

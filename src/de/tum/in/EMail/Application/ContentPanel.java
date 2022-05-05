package de.tum.in.EMail.Application;

import java.awt.LayoutManager;
import javax.swing.JPanel;

public abstract class ContentPanel extends JPanel {

	public ContentPanel()
	{
		super();
	}

	public ContentPanel(LayoutManager layout)
	{
		super(layout);
	}

	public abstract boolean evaluate();

}

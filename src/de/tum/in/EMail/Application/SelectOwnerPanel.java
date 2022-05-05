package de.tum.in.EMail.Application;

import java.awt.GridLayout;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.tum.in.EMail.Person;


public class SelectOwnerPanel extends ContentPanel {

	private MessageCollector parent;
	private List<Person> candidates;
	private JComboBox personSelector;

	public SelectOwnerPanel(MessageCollector parent, List<Person> candidates)
	{
		super(new GridLayout(2, 1));
		this.parent = parent;
		this.candidates = candidates;
		JPanel[] panels = new JPanel[2];

		panels[0] = new JPanel();
		panels[0].add(new JLabel("Please select the owner of the mailbox:"));

		panels[1] = new JPanel();
		personSelector = new JComboBox();
		for (Person p : candidates)
			personSelector.addItem(p.getDisplayName());
		personSelector.setSelectedIndex(0);
		panels[1].add(personSelector);

		for (JPanel p : panels)
			add(p);
	}

	@Override public boolean evaluate()
	{
		parent.setMailboxOwner(
				candidates.get(personSelector.getSelectedIndex()));
		return true;
	}

}

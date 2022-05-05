package de.tum.in.EMail.Application;

import java.util.Vector;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JComboBox;

import de.tum.in.EMail.Person;


public class AnnotationPanel extends ContentPanel implements ActionListener {

	private static final int numOptions = 3;
	private static final String[] optionStrings = {
		"I know this person, and rate my relationship with him/her as follows:",
		"This is the same person I rated earlier:",
		"Select this option if one of the following applies:"
	};
	private static final String[] noRatingReasons = {
		"I do not want to rate my relationship with this person.",
		"This is not a natural person.",
		"I do not remember this person."
	};

	private static final int numValenceSteps = 5;
	private static final int numIntensitySteps = 10;

	private MessageCollector parent;
	private Person curPerson;
	private Vector<Person> prevPersons;
	private ButtonGroup buttonGroup;
	private JRadioButton[] buttons;
	private JSlider valenceSlider;
	private JSlider intensitySlider;
	private JComboBox personSelector;
	private int selectedOption;

	public AnnotationPanel(MessageCollector parent, Person curPerson,
			Vector<Person> prevPersons, int stepIdx, int numSteps)
	{
		super();
		BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(layout);
		
		this.parent = parent;
		this.curPerson = curPerson;
		this.prevPersons = prevPersons;

		JPanel topPanel = new JPanel();
		topPanel.add(new JLabel("Please rate your relationship with " +
				curPerson.getDisplayName() + " (" + stepIdx + "/" + numSteps +
				")."));
		add(topPanel);

		buttons = new JRadioButton[numOptions];
		buttonGroup = new ButtonGroup();
		for (int i = 0; i < numOptions; i++)
		{
			buttons[i] = new JRadioButton(optionStrings[i]);
			buttons[i].addActionListener(this);
			buttonGroup.add(buttons[i]);
		}

		JPanel[] panels = new JPanel[numOptions];
		panels[0] = new JPanel(new GridBagLayout());
		GridBagConstraints constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.insets = new Insets(0, 10, 0, 0);
		constr.weightx = 0;
		constr.gridy = 0;
		constr.gridwidth = GridBagConstraints.REMAINDER;
		panels[0].add(buttons[0], constr);

		// valence slider and associated labels
		constr.insets = new Insets(5, 38, 0, 0);
		constr.gridy = 1;
		panels[0].add(new JLabel("Valence:"), constr);
		constr.insets = new Insets(0, 38, 0, 0);
		constr.gridwidth = 1;
		constr.gridy = 2;
		panels[0].add(new JLabel("negative"), constr);
		constr.insets = new Insets(0, 5, 0, 0);
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1;
		valenceSlider = new JSlider(-numValenceSteps, numValenceSteps, 0);
		valenceSlider.setPaintTicks(true);
		valenceSlider.setSnapToTicks(true);
		panels[0].add(valenceSlider, constr);
		constr.insets = new Insets(0, 5, 0, 20);
		constr.fill = GridBagConstraints.NONE;
		constr.weightx = 0;
		panels[0].add(new JLabel("positive"), constr);

		// intensity slider and associated labels
		constr.insets = new Insets(5, 38, 0, 0);
		constr.gridy = 3;
		panels[0].add(new JLabel("Intensity:"), constr);
		constr.insets = new Insets(0, 38, 0, 0);
		constr.gridwidth = 1;
		constr.gridy = 4;
		panels[0].add(new JLabel("low"), constr);
		constr.insets = new Insets(0, 5, 0, 0);
		constr.fill = GridBagConstraints.HORIZONTAL;
		constr.weightx = 1;
		intensitySlider = new JSlider(0, numIntensitySteps,
				numIntensitySteps / 2);
		intensitySlider.setPaintTicks(true);
		intensitySlider.setSnapToTicks(true);
		panels[0].add(intensitySlider, constr);
		constr.insets = new Insets(0, 5, 0, 20);
		constr.fill = GridBagConstraints.NONE;
		constr.weightx = 0;
		panels[0].add(new JLabel("high"), constr);

		panels[1] = new JPanel(new GridBagLayout());
		constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.weightx = 1;
		constr.insets = new Insets(10, 10, 0, 0);
		constr.gridy = 0;
		panels[1].add(buttons[1], constr);
		constr.insets = new Insets(5, 34, 0, 0);
		constr.gridy = 1;
		personSelector = new JComboBox();
		for (Person p : prevPersons)
			personSelector.addItem(p.getDisplayName());		
		panels[1].add(personSelector, constr);
		
		panels[2] = new JPanel(new GridBagLayout());
		constr = new GridBagConstraints();
		constr.anchor = GridBagConstraints.WEST;
		constr.weightx = 1;
		constr.gridy = 0;
		constr.insets = new Insets(10, 10, 0, 0);
		panels[2].add(buttons[2], constr);
		constr.insets = new Insets(5, 38, 0, 0);
		for (String s : noRatingReasons)
		{
			constr.gridy += 1;
			panels[2].add(new JLabel(s), constr);
		}

		for (JPanel p : panels)
			add(p);

		buttons[0].setSelected(true);
		selectedOption = 0;
		toggleEnabledComponents();
	}

	private void toggleEnabledComponents()
	{
		switch (selectedOption)
		{
		case 0:
			valenceSlider.setEnabled(true);
			intensitySlider.setEnabled(true);
			personSelector.setEnabled(false);
			break;
		case 1:
			valenceSlider.setEnabled(false);
			intensitySlider.setEnabled(false);
			personSelector.setEnabled(true);			
			break;
		default:
			valenceSlider.setEnabled(false);
			intensitySlider.setEnabled(false);
			personSelector.setEnabled(false);
			break;
		}
	}

	@Override public boolean evaluate()
	{
		switch (selectedOption)
		{
		case 0:
			parent.rankPerson(curPerson,
					(double) valenceSlider.getValue() / numValenceSteps,
					(double) intensitySlider.getValue() / numIntensitySteps);
			break;
		case 1:
			parent.mergePersons(curPerson,
					prevPersons.get(personSelector.getSelectedIndex()));
			break;
		}
		return true;
	}

	public void actionPerformed(ActionEvent e)
	{
		int i = 0;
		while (buttonGroup.getSelection() != buttons[i].getModel())
			i++;
		selectedOption = i;
		toggleEnabledComponents();
	}

}

package de.tum.in.EMail.Application;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

public class MboxListPanel extends JPanel implements ActionListener {

	private static final String[] buttonLabels = {
		"Add", "Remove", "Merge", "Quit"
	};

	private MergeMbox parent;
	private JList list;
	private DefaultListModel listData;
	private JButton[] buttons;

	public MboxListPanel(MergeMbox parent)
	{
		super(new BorderLayout());
		this.parent = parent;

		listData = new DefaultListModel();
		list = new JList(listData);
		add(new JScrollPane(list), BorderLayout.CENTER);

		JPanel navigationPanel = new JPanel(
				new FlowLayout(FlowLayout.TRAILING));
		buttons = new JButton[buttonLabels.length];
		for (int i = 0; i < buttonLabels.length; i++)
		{
			buttons[i] = new JButton(buttonLabels[i]);
			buttons[i].addActionListener(this);
			navigationPanel.add(buttons[i]);
		}
		add(navigationPanel, BorderLayout.SOUTH);
	}

	public Object[] getFiles()
	{
		return listData.toArray();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == buttons[0])
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						JFileChooser fc = new JFileChooser(parent.getCurPath());
						if (fc.showOpenDialog(parent) ==
							JFileChooser.APPROVE_OPTION)
						{
							listData.addElement(fc.getSelectedFile().getPath());
							parent.setCurPath(fc.getCurrentDirectory());
						}
					}
				});
		else if (e.getSource() == buttons[1])
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						int idx = list.getSelectedIndex();
						if (idx >= 0)
							listData.remove(idx);
					}
				});
		else if (e.getSource() == buttons[2])
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						parent.merge();
					}
				});
		else if (e.getSource() == buttons[3])
			SwingUtilities.invokeLater(
				new Runnable() {
					public void run()
					{
						parent.quit();
					}
				});
	}

}

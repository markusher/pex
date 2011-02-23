package se.pex.editors;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A popup dialog that shows the legend for the pex editor.
 */
public class LegendDialog extends PopupDialog {

	/**
	 * Creates a new legend dialog.
	 * @param parent The parent shell.
	 */
	public LegendDialog(Shell parent) {
		super(parent, PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, false, false, false, false, false, null, null);
	}

	@Override
	protected Control createContents(Composite parent) {
		return createDialogArea(parent);
	}

	@Override
	public int open() {
		int open = super.open();
		getShell().setFocus();
		return open;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		Label text = new Label(parent, SWT.NONE);
		text.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		text.setText("Exlusive is the time that is spent on the node exluding time in child nodes, where as\n" +
				"inclusive is the time including time in child nodes. The rowcount is the ratio between actual and\n" +
				"estimated row counts and an arrow indicating if the planner estimated too much or too little.\n\n" +
				"The rows are marked based on criteria, ranging from yellow to red.\n" +
				"The color is dependent on the mark mode.\n\nExclusive and inclusive:\n");
		text = new Label(parent, SWT.NONE);
		text.setBackground(PexEditor.red);
		text.setText("The time is > 90 % of the total query time.");
		text = new Label(parent, SWT.NONE);
		text.setBackground(PexEditor.brown);
		text.setText("The time is > 50 % of the total query time.");
		text = new Label(parent, SWT.NONE);
		text.setBackground(PexEditor.yellow);
		text.setText("The time is > 10 % of the total query time.");
		text = new Label(parent, SWT.NONE);
		text.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		text.setText("\nRowcount:\n");
		text = new Label(parent, SWT.NONE);
		text.setBackground(PexEditor.red);
		text.setText("The rowcount ratio > 1000");
		text = new Label(parent, SWT.NONE);
		text.setBackground(PexEditor.brown);
		text.setText("The rowcount ratio > 100");
		text = new Label(parent, SWT.NONE);
		text.setBackground(PexEditor.yellow);
		text.setText("The rowcount ratio > 10");

		return parent;
	}
}

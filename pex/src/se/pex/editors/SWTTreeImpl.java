package se.pex.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import se.pex.analyze.Node;

/**
 * A tree implementation using the default SWT tree widget.
 *
 * NOTE: Tree item height is not properly implemented in the default SWT tree widget and will look bad.
 */
public class SWTTreeImpl implements TreeImplementation {
	/**
	 * An extra container for the tree, reason for this is that when redrawing the tree and not using the extra container
	 * the variable row heights does not work any more, not sure why so created this workaround.
	 */
	private Composite treeContainer = null;

	/** The tree. */
	private Tree tree = null;

	/** The editor instance. */
	private PexEditor editor;

	/**
	 * Creates a new tree implementation.
	 * @param parent The composite where to add the tree.
	 * @param editor The editor instance.
	 */
	public SWTTreeImpl(Composite parent, PexEditor editor) {
		treeContainer = new Composite(parent, SWT.NONE);
		parent.setLayout(new FillLayout());
		treeContainer.setLayout(new FillLayout());
		this.editor = editor;
	}

	/**
	 * @see se.pex.editors.TreeImplementation#createTree()
	 */
	@Override
	public Composite createTree() {
		return treeContainer;
	}

	/**
	 * @see se.pex.editors.TreeImplementation#addNode(se.pex.analyze.Node, java.lang.Object, boolean, org.eclipse.swt.graphics.Color, java.lang.String, java.lang.String)
	 */
	@Override
	public Object addNode(Node node, Object parentNode, boolean disableMultiLine, Color c, String inclusiveTime, String exclusiveTime) {
		TreeItem item = null;
		if (parentNode != null) {
			item = new TreeItem((TreeItem) parentNode, SWT.NONE);
		}
		else {
			item = new TreeItem(tree, SWT.NONE);
		}
		String text = node.getMainLine();
		String extra = node.getExtraInformation();
		if (extra.length() > 0 && !disableMultiLine) {
			text = text + "\n" + extra; //$NON-NLS-1$
		}
		item.setText(new String[] {inclusiveTime, exclusiveTime, node.getRowCountInfo().toString(), text});
		item.setData("EXTRA", extra); //$NON-NLS-1$
		if (c != null) {
			item.setBackground(c);
		}
		return item;
	}

	/**
	 * @see se.pex.editors.TreeImplementation#clearTree()
	 */
	@Override
	public void clearTree() {
		if (tree != null) {
			tree.dispose();
		}
		buildTree();
	}

	/**
	 * Creates a tree in a composite.
	 */
	private void buildTree() {
		tree = new Tree(treeContainer, SWT.VIRTUAL | SWT.BORDER);
		tree.setHeaderVisible(true);
		TreeColumn column = new TreeColumn(tree, SWT.NONE);
		column.setWidth(300);
		column.setResizable(true);
		column.setText(Messages.Pex_Inclusive);
		column.setMoveable(true);
		column = new TreeColumn(tree, SWT.NONE);
		column.setWidth(100);
		column.setResizable(true);
		column.setText(Messages.Pex_Exclusive);
		column.setMoveable(true);
		column = new TreeColumn(tree, SWT.NONE);
		column.setWidth(100);
		column.setResizable(true);
		column.setText(Messages.Pex_Rowcount);
		column.setMoveable(true);
		column = new TreeColumn(tree, SWT.NONE);
		column.setText(Messages.Pex_Information);
		column.setWidth(1000);
		column.setResizable(true);
		column.setMoveable(true);

	    tree.setToolTipText(""); //$NON-NLS-1$

		createToolTip();
		treeContainer.layout(true);
		editor.createContextMenu(tree);
	}

	/**
	 * Expands a tree item and all its children.
	 * @param item Node to start with.
	 */
	private void expandTree(TreeItem item) {
		if (item != null) {
			item.setExpanded(true);
			TreeItem[] items = item.getItems();
			for (TreeItem child : items) {
				expandTree(child);
			}
		}
	}

	/**
	 * Adds tool tips to the tree items.
	 */
	private void createToolTip() {
		final Listener labelListener = new Listener() {
			public void handleEvent(Event event) {
				Label label = (Label) event.widget;
				Shell shell = label.getShell();
				switch (event.type) {
				case SWT.MouseDown:
					Event e = new Event();
					e.item = (TreeItem) label.getData("_TREEITEM"); //$NON-NLS-1$
					tree.setSelection(new TreeItem[] { (TreeItem) e.item });
					tree.notifyListeners(SWT.Selection, e);
					shell.dispose();
					tree.setFocus();
					break;
				case SWT.MouseExit:
					shell.dispose();
					break;
				}
			}
		};

		Listener tableListener = new Listener() {
			Shell tip = null;
			Label label = null;

			public void handleEvent(Event event) {
				switch (event.type) {
					case SWT.Dispose:
					case SWT.KeyDown:
					case SWT.MouseMove: {
						if (tip == null)
							break;
						tip.dispose();
						tip = null;
						label = null;
						break;
					}
					case SWT.MouseHover: {
						TreeItem item = tree.getItem(new Point(event.x, event.y));
						if (item != null && ((String) item.getData("EXTRA")).length() > 0) { //$NON-NLS-1$
							if (tip != null && !tip.isDisposed()) {
								tip.dispose();
							}
							tip = new Shell(tree.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
							tip.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							FillLayout layout = new FillLayout();
							layout.marginWidth = 2;
							tip.setLayout(layout);
							label = new Label(tip, SWT.NONE);
							label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
							label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							label.setData("_TREEITEM", item); //$NON-NLS-1$
							label.setText((String) item.getData("EXTRA")); //$NON-NLS-1$
							label.addListener(SWT.MouseExit, labelListener);
							label.addListener(SWT.MouseDown, labelListener);
							Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							Point pt = tree.toDisplay(event.x, event.y);
							tip.setBounds(pt.x, pt.y, size.x, size.y);
							tip.setVisible(true);
						}
					}
				}
			}
		};
		tree.addListener(SWT.Dispose, tableListener);
		tree.addListener(SWT.KeyDown, tableListener);
		tree.addListener(SWT.MouseMove, tableListener);
		tree.addListener(SWT.MouseHover, tableListener);
	}

	@Override
	public void expandTree() {
		expandTree(tree.getItem(0));
	}

	public void expandSelectedNode() {
		TreeItem[] items = tree.getSelection();
		for (TreeItem item : items) {
			expandTree(item);
		}
	}

}

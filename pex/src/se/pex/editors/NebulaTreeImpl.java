package se.pex.editors;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import se.pex.analyze.Node;

/**
 * A tree implementation using the nebula grid widget.
 */
public class NebulaTreeImpl implements TreeImplementation {

	/** The grid itself. */
	private Grid grid;

	/**
	 * Creates a new tree implementation.
	 * @param parent The parent container.
	 * @param editor The editor instance.
	 */
	public NebulaTreeImpl(Composite parent, PexEditor editor) {
		parent.setLayout(new FillLayout());
		grid = new Grid(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		grid.setHeaderVisible(true);
		grid.setAutoHeight(true);
		GridColumn column = new GridColumn(grid, SWT.NONE);
		column.setTree(true);
		column.setText(Messages.Pex_Inclusive);
		column.setResizeable(true);
		column.setMoveable(true);
		column.setWidth(200);
		column = new GridColumn(grid, SWT.NONE);
		column.setText(Messages.Pex_Exclusive);
		column.setResizeable(true);
		column.setMoveable(true);
		column.setWidth(80);
		column = new GridColumn(grid, SWT.NONE);
		column.setText(Messages.Pex_Rowcount);
		column.setResizeable(true);
		column.setMoveable(true);
		column.setWidth(80);
		column = new GridColumn(grid, SWT.NONE);
		column.setText(Messages.Pex_Information);
		column.setResizeable(true);
		column.setMoveable(true);
		column.setWidth(600);
		column.setWordWrap(true);
		editor.createContextMenu(grid);
	}

	/**
	 * @see se.pex.editors.TreeImplementation#createTree()
	 */
	@Override
	public Composite createTree() {
		return grid;
	}

	/**
	 * @see se.pex.editors.TreeImplementation#addNode(se.pex.analyze.Node, java.lang.Object, boolean, org.eclipse.swt.graphics.Color, java.lang.String, java.lang.String)
	 */
	@Override
	public Object addNode(Node node, Object parentNode, boolean disableMultiLine, Color c, String inclusiveTime, String exclusiveTime) {
		GridItem item = null;
		if (parentNode != null) {
			item = new GridItem((GridItem) parentNode, SWT.NONE);
		}
		else {
			item = new GridItem(grid, SWT.NONE);
		}
		item.setText(0, inclusiveTime);
		item.setText(1, exclusiveTime);
		item.setText(2, node.getRowCountInfo().toString());
		String text = node.getMainLine();
		String extra = node.getExtraInformation();
		if (extra.length() > 0 && !disableMultiLine) {
			text = text + "\n" + extra; //$NON-NLS-1$
		}
		if (c != null) {
			item.setBackground(0, c);
			item.setBackground(1, c);
			item.setBackground(2, c);
			item.setBackground(3, c);
		}
		item.setToolTipText(3, extra);
		item.setText(3, text);
		grid.getParent().layout();
		return item;
	}

	/**
	 * @see se.pex.editors.TreeImplementation#clearTree()
	 */
	@Override
	public void clearTree() {
		grid.removeAll();
	}

	/**
	 * Expands a node and all its childrens.
	 * @param item The grid item to expand.
	 */
	private void expand(GridItem item) {
		if (item != null) {
			item.setExpanded(true);
			for (GridItem child : item.getItems()) {
				expand(child);
			}
		}
	}

	/**
	 * @see se.pex.editors.TreeImplementation#expandTree()
	 */
	@Override
	public void expandTree() {
		expand(grid.getItem(0));
	}

	@Override
	public void expandSelectedNode() {
		expand(grid.getSelection()[0]);
	}

	@Override
	public boolean setRootNode(Node node) {
		return true;
	}

}

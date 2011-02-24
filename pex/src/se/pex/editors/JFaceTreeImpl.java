package se.pex.editors;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import se.pex.analyze.Node;

/**
 * Implementation of the tree using a TreeViewer.
 */
public class JFaceTreeImpl implements TreeImplementation, ITableLabelProvider, ITreeContentProvider, ITableColorProvider {
	/** The editor instance. */
	private PexEditor editor;
	/** The treeviewer. */
	private TreeViewer viewer;
	/** Total execution time of the explain tree. */
	private float totalTime;

	/**
	 * Creates a new tree implementation.
	 * @param parent The composite where to put the tree.
	 * @param editor The editor instance.
	 */
	public JFaceTreeImpl(Composite parent, PexEditor editor) {
		this.editor = editor;
		parent.setLayout(new FillLayout());
		viewer = new TreeViewer(parent);
		viewer.getTree().setHeaderVisible(true);
		Tree tree = viewer.getTree();
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
		viewer.setLabelProvider(this);
		viewer.setContentProvider(this);
		editor.createContextMenu(tree);
	}

	@Override
	public Composite createTree() {
		return viewer.getTree();
	}

	@Override
	public Object addNode(Node node, Object parentNode, boolean disableMultiLine, Color c, String inclusiveTime, String exclusiveTime) {
		return null;
	}

	@Override
	public void clearTree() {
	}

	@Override
	public void expandTree() {
		viewer.expandAll();
	}

	/**
	 * Expands a node and all children.
	 * @param item The node to expand.
	 */
	private void expandNode(TreeItem item) {
		if (item != null) {
			item.setExpanded(true);
			for (TreeItem child : item.getItems()) {
				expandNode(child);
			}
		}
	}

	@Override
	public void expandSelectedNode() {
		TreeItem[] items = viewer.getTree().getSelection();
		for (TreeItem item : items) {
			expandNode(item);
		}
	}

	@Override
	public void addListener(ILabelProviderListener arg0) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {
	}

	@Override
	public Image getColumnImage(Object arg0, int arg1) {
		return null;
	}

	@Override
	public String getColumnText(Object arg0, int arg1) {
		switch (arg1) {
			case 0:
				return editor.formatFloat(((Node) arg0).getTimeInclusive());
			case 1:
				return editor.formatFloat(((Node) arg0).getTimeExclusive());
			case 2:
				return ((Node) arg0).getRowCountInfo().toString();
			default:
				String text = ((Node) arg0).getMainLine();
				String extra = ((Node) arg0).getExtraInformation();
				if (extra.length() > 0) {
					text = text + "\n" + extra; //$NON-NLS-1$
				}
				return text;
		}
	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

	@Override
	public Object[] getChildren(Object arg0) {
		Node n = (Node) arg0;
		return n.getChildren().toArray();
	}


	@Override
	public Object getParent(Object arg0) {
		return ((Node) arg0).getParent();
	}

	@Override
	public boolean hasChildren(Object arg0) {
		return ((Node) arg0).getChildren().size() > 0;
	}

	@Override
	public Object[] getElements(Object arg0) {
		return getChildren(arg0);
	}

	@Override
	public boolean setRootNode(Node node) {
		totalTime = node.getTotalTime();
		viewer.setInput(node);
		return false;
	}

	@Override
	public Color getBackground(Object arg0, int arg1) {
		return editor.getColor((Node) arg0, totalTime);
	}

	@Override
	public Color getForeground(Object arg0, int arg1) {
		return null;
	}

}

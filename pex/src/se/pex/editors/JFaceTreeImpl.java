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
import org.eclipse.swt.widgets.Widget;

import se.pex.analyze.Node;

/**
 * Implementation of the tree using a TreeViewer.
 */
public class JFaceTreeImpl implements TreeImplementation, ITableLabelProvider, ITreeContentProvider, ITableColorProvider {
	/**
	 * A tree viewer that can ignore expanded paths that have never been executed.
	 */
	class ExtendedTreeViewer extends TreeViewer {

		@Override
		protected void internalExpandToLevel(Widget widget, int level) {
			if (editor.foldNeverExecuted() && widget instanceof TreeItem) {
				if (!((Node) widget.getData()).isExecuted()) {
					return;
				}
			}
			super.internalExpandToLevel(widget, level);
		}

		/**
		 * Creates a new tree viewer.
		 * @param parent The parent in which to place the tree viewer.
		 */
		public ExtendedTreeViewer(Composite parent) {
			super(parent);
		}

	}
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
		viewer = new ExtendedTreeViewer(parent);
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

	/**
	 * @see se.pex.editors.TreeImplementation#createTree()
	 */
	@Override
	public Composite createTree() {
		return viewer.getTree();
	}

	/**
	 * @see se.pex.editors.TreeImplementation#addNode(se.pex.analyze.Node, java.lang.Object, boolean, org.eclipse.swt.graphics.Color, java.lang.String, java.lang.String)
	 */
	@Override
	public Object addNode(Node node, Object parentNode, boolean disableMultiLine, Color c, String inclusiveTime, String exclusiveTime) {
		return null;
	}

	/**
	 * @see se.pex.editors.TreeImplementation#clearTree()
	 */
	@Override
	public void clearTree() {
	}

	/**
	 * @see se.pex.editors.TreeImplementation#expandTree()
	 */
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

	/**
	 * @see se.pex.editors.TreeImplementation#expandSelectedNode()
	 */
	@Override
	public void expandSelectedNode() {
		TreeItem[] items = viewer.getTree().getSelection();
		for (TreeItem item : items) {
			expandNode(item);
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener arg0) {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener arg0) {
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object arg0, int arg1) {
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getColumnText(Object node, int column) {
		switch (column) {
			case 0:
				return editor.formatFloat(((Node) node).getTimeInclusive());
			case 1:
				return editor.formatFloat(((Node) node).getTimeExclusive());
			case 2:
				return ((Node) node).getRowCountInfo().toString();
			default:
				String text = ((Node) node).getMainLine();
				String extra = ((Node) node).getExtraInformation();
				if (extra.length() > 0) {
					text = text + "\n" + extra; //$NON-NLS-1$
				}
				return text;
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object node) {
		if (node instanceof HiddenRoot) {
			return new Object[] {((HiddenRoot) node).realRoot};
		}
		Node n = (Node) node;
		return n.getChildren().toArray();
	}


	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object node) {
		if (node instanceof HiddenRoot) {
			return null;
		}
		return ((Node) node).getParent();
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object node) {
		if (node instanceof HiddenRoot) {
			return true;
		}
		return ((Node) node).getChildren().size() > 0;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object node) {
		return getChildren(node);
	}

	/**
	 * @see se.pex.editors.TreeImplementation#setRootNode(se.pex.analyze.Node)
	 */
	@Override
	public boolean setRootNode(Node node) {
		totalTime = node.getTotalTime();
		HiddenRoot root = new HiddenRoot(node);
		viewer.setInput(root);
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang.Object, int)
	 */
	@Override
	public Color getBackground(Object node, int column) {
		return editor.getColor((Node) node, totalTime);
	}

	/**
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang.Object, int)
	 */
	@Override
	public Color getForeground(Object node, int column) {
		return null;
	}

	/**
	 * A holder object for the root.
	 */
	class HiddenRoot {
		/** The real root. */
		public Node realRoot;

		/**
		 * Creates a new hidden root.
		 * @param realRoot The real root to use.
		 */
		public HiddenRoot(Node realRoot) {
			this.realRoot = realRoot;
		}
	}

}

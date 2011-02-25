package se.pex.editors;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import se.pex.analyze.Node;

/**
 * Implementation of the tree using a TreeViewer.
 */
public class JFaceTreeImpl implements TreeImplementation, ITreeContentProvider {
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
	public JFaceTreeImpl(Composite parent, final PexEditor editor) {
		this.editor = editor;
		parent.setLayout(new FillLayout());
		viewer = new ExtendedTreeViewer(parent);
		viewer.getTree().setHeaderVisible(true);
		ColumnViewerToolTipSupport.enableFor(viewer);
		TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(300);
		column.getColumn().setResizable(true);
		column.getColumn().setText(Messages.Pex_Inclusive);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getToolTipText(Object element) {
				return "Total execution: " + editor.formatFloat(((Node) element).getTotalTime());
			}

			@Override
			public String getText(Object node) {
				return editor.formatFloat(((Node) node).getTimeInclusive());
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime);
			}
		});
		column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(100);
		column.getColumn().setResizable(true);
		column.getColumn().setText(Messages.Pex_Exclusive);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getToolTipText(Object element) {
				return "Total execution: " + editor.formatFloat(((Node) element).getTotalTime());
			}

			@Override
			public String getText(Object node) {
				return editor.formatFloat(((Node) node).getTimeExclusive());
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime);
			}
		});
		column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(100);
		column.getColumn().setResizable(true);
		column.getColumn().setText(Messages.Pex_Rowcount);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object node) {
				return ((Node) node).getRowCountInfo().toString();
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime);
			}
		});
		column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(Messages.Pex_Information);
		column.getColumn().setWidth(1000);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getToolTipText(Object element) {
				String extraInformation = ((Node) element).getExtraInformation();
				return extraInformation.length() > 0 ? extraInformation : null;
			}

			@Override
			public String getText(Object node) {
				String text = ((Node) node).getMainLine();
				String extra = ((Node) node).getExtraInformation();
				if (extra.length() > 0) {
					text = text + "\n" + extra; //$NON-NLS-1$
				}
				return text;
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime);
			}
		});
		viewer.setContentProvider(this);
		editor.createContextMenu(viewer.getTree());
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
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
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

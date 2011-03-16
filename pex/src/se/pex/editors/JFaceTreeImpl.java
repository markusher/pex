package se.pex.editors;

import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import se.pex.Activator;
import se.pex.analyze.Node;
import se.pex.editors.PexEditor.MarkMode;
import se.pex.preferences.PreferenceConstants;

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

	/** Max line length for information. */
	private static final int MAX_LINE_LENGTH = 300;
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

		Menu menu = editor.createContextMenu(viewer.getTree());
		MenuItem hideMenuItem = new MenuItem(menu, SWT.CASCADE);
		hideMenuItem.setText(Messages.Pex_Show);
		Menu hideMenu = new Menu(menu);
		hideMenuItem.setMenu(hideMenu);

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
		setColumnWidth(PreferenceConstants.P_SHOW_INCLUSIVE, store, column, 300);
		column.getColumn().setText(Messages.Pex_Inclusive);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getToolTipText(Object element) {
				return "Total execution: " + editor.formatFloat(((Node) element).getTotalTime());
			}

			@Override
			public String getText(Object node) {
				return editor.formatFloat(((Node) node).getTimeInclusive(false));
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime, MarkMode.Inclusive);
			}
		});
		createMenuItem(hideMenu, column);
		column = new TreeViewerColumn(viewer, SWT.NONE);
		setColumnWidth(PreferenceConstants.P_SHOW_EXCLUSIVE, store, column, 100);
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
				return editor.getColor((Node) node, totalTime, MarkMode.Exclusive);
			}
		});
		createMenuItem(hideMenu, column);
		column = new TreeViewerColumn(viewer, SWT.NONE);
		setColumnWidth(PreferenceConstants.P_SHOW_ROWCOUNT, store, column, 100);
		column.getColumn().setText(Messages.Pex_Rowcount);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object node) {
				return ((Node) node).getRowCountInfo().toString();
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime, MarkMode.Count);
			}
		});
		createMenuItem(hideMenu, column);
		column = new TreeViewerColumn(viewer, SWT.NONE);
		setColumnWidth(PreferenceConstants.P_SHOW_LOOP, store, column, 100);
		column.getColumn().setText(Messages.Pex_Loops);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object node) {
				return "" + ((Node) node).getLoopCount();
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime, null);
			}
		});
		createMenuItem(hideMenu, column);
		column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(Messages.Pex_Information);
		column.getColumn().setWidth(1000);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(true);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getToolTipText(Object element) {
				String extraInformation = ((Node) element).getExtraInformation(MAX_LINE_LENGTH);
				return extraInformation.length() > 0 ? extraInformation : null;
			}

			@Override
			public String getText(Object node) {
				String text = ((Node) node).getMainLine();
				String extra = ((Node) node).getExtraInformation(MAX_LINE_LENGTH);
				if (extra.length() > 0) {
					text = text + "\n" + extra; //$NON-NLS-1$
				}
				return text;
			}

			@Override
			public Color getBackground(Object node) {
				return editor.getColor((Node) node, totalTime, null);
			}
		});
		viewer.setContentProvider(this);
	}

	/**
	 * Sets the column width and resizable flag on a column.
	 * @param prefConstant The preference to use in the store to get visiblity.
	 * @param store The preference store.
	 * @param column The column to adjust.
	 * @param width The size to set if visible.
	 */
	private void setColumnWidth(String prefConstant, IPreferenceStore store, TreeViewerColumn column, int width) {
		if (store.getBoolean(prefConstant)) {
			column.getColumn().setWidth(width);
			column.getColumn().setResizable(true);
		}
		else {
			column.getColumn().setWidth(0);
			column.getColumn().setResizable(false);
			column.getColumn().setData("_WIDTH", width);
		}
	}

	/**
	 * Creates a menu item to hide/show a column in the grid.
	 * @param parent The parent menu.
	 * @param column The column to add to the menu.
	 */
	private void createMenuItem(Menu parent, final TreeViewerColumn column) {
		final MenuItem item = new MenuItem(parent, SWT.CHECK);
		item.setText(column.getColumn().getText());
		item.setSelection(column.getColumn().getResizable());
		item.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event e) {
				if (item.getSelection()) {
					Integer width = (Integer) column.getColumn().getData("_WIDTH");
					if (width == null) {
						width = new Integer(100);
					}
					column.getColumn().setWidth(width);
					column.getColumn().setResizable(true);
				} else {
					column.getColumn().setData("_WIDTH", column.getColumn().getWidth());
					column.getColumn().setWidth(0);
					column.getColumn().setResizable(false);
				}
			}
		});
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

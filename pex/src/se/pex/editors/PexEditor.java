package se.pex.editors;


import java.text.DecimalFormat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

import se.pex.Activator;
import se.pex.analyze.Engine;
import se.pex.analyze.Node;
import se.pex.preferences.PreferenceConstants;


/**
 * An editor for analyzing postgresql explain analyze outputs.
 *
 * TODO: Inform depescz
 * TODO: Create update site
 * TODO: Support subplan when parsing (Fixed but only verified on one plan)
 */
public class PexEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** The different mark modes. */
	enum MarkMode {
		/** Based on exclusive times. */
		Exclusive,
		/** Based on inclusive times. */
		Inclusive,
		/** Based on row counts. */
		Count;

		/**
		 * Returns the mark mode from its name.
		 * @param name Name of the mark mode to look for.
		 * @return The mark mode enum.
		 */
		public static MarkMode getMarkMode(String name) {
			for (MarkMode mode : MarkMode.values()) {
				if (mode.name().equalsIgnoreCase(name)) {
					return mode;
				}
			}
			throw new IllegalArgumentException("Mark mode does not exist: " + name);
		}

		/**
		 * Creates a menu item for mode selection.
		 * @param childMenu The menu where to append the menu item.
		 * @param editor The editor.
		 * @return The new menu item.
		 */
		public MenuItem createMenuItem(Menu childMenu, final PexEditor editor) {
		    MenuItem mitem = new MenuItem (childMenu, SWT.RADIO);
		    mitem.setData("MODE", this);
		    mitem.setText (this.name());
		    if (editor.markMode == this) {
		    	mitem.setSelection(true);
		    }
	    	mitem.addSelectionListener(editor.adapter);
		    return mitem;
		}
	}

	/** Bad color. */
	private static final Color red = new Color(Display.getCurrent(), 255, 0, 0);
	/** Pretty bad color. */
	private static final Color brown = new Color(Display.getCurrent(), 255, 128, 51);
	/** A little bad color. */
	private static final Color yellow = new Color(Display.getCurrent(), 255, 255, 102);

	/** The text editor used in the text page. */
	private TextEditor editor;

	/** The explanation tree. */
	private Tree tree;

	/** Mode used for selecting colors. */
	private MarkMode markMode = MarkMode.Exclusive;

	/** Used to format floats. */
	DecimalFormat decimalFormat = new DecimalFormat("#.###");

	/** Disables multiline tree entries. */
	private boolean disableMultiLine;

	/**
	 * An extra container for the tree, reason for this is that when redrawing the tree and not using the extra container
	 * the variable row heights does not work any more, not sure why so created this workaround.
	 */
	private Composite treeContainer;

	/** The editor. */
	private final PexEditor instance = this;

	/** Selection adapter for setting markmode. */
	private SelectionAdapter adapter = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			MenuItem item = (MenuItem) e.getSource();
			if (item.getSelection()) {
				instance.setMarkMode((MarkMode) ((MenuItem) e.getSource()).getData("MODE"));
			}
		}
	};

	/**
	 * Creates a multi-page editor example.
	 */
	public PexEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		disableMultiLine = store.getBoolean(PreferenceConstants.P_DISABLEMULTILINE);
		markMode = MarkMode.getMarkMode(store.getString(PreferenceConstants.P_MARKMODE));
	}

	/**
	 * Creates page 0 of the multi-page editor,
	 * which contains a text editor.
	 */
	void createRawTextPage() {
		try {
			editor = new TextEditor();
			setPageText(addPage(editor, getEditorInput()), "Text");
		} catch (PartInitException e) {
			ErrorDialog.openError(
				getSite().getShell(),
				"Error creating nested text editor",
				null,
				e.getStatus());
		}
	}

	/**
	 * Create the explanation page.
	 */
	void createExplainPage() {
		treeContainer = new Composite(getContainer(), SWT.NONE);
		getContainer().setLayout(new FillLayout());
		treeContainer.setLayout(new FillLayout());
		setPageText(addPage(treeContainer), "Explain");
    }

	/**
	 * Creates a tree in a composite.
	 */
	private void createTree() {
		tree = new Tree(treeContainer, SWT.VIRTUAL | SWT.BORDER);
		tree.setHeaderVisible(true);
		TreeColumn column = new TreeColumn(tree, SWT.NONE);
		column.setWidth(300);
		column.setResizable(true);
		column.setText("Inclusive");
		column.setMoveable(true);
		column = new TreeColumn(tree, SWT.NONE);
		column.setWidth(100);
		column.setResizable(true);
		column.setText("Exclusive");
		column.setMoveable(true);
		column = new TreeColumn(tree, SWT.NONE);
		column.setWidth(100);
		column.setResizable(true);
		column.setText("Rowcount");
		column.setMoveable(true);
		column = new TreeColumn(tree, SWT.NONE);
		column.setText("Information");
		column.setWidth(1000);
		column.setResizable(true);
		column.setMoveable(true);

	    tree.setToolTipText("");

		createToolTip();
		treeContainer.layout(true);
		createContextMenu();
	}

	/**
	 * Create the context menu for the tree.
	 */
	private void createContextMenu() {
		Menu contextMenu = new Menu (tree);
	    MenuItem root = new MenuItem(contextMenu, SWT.CASCADE);
	    root.setText("Mode");
	    Menu childMenu = new Menu(root);
	    root.setMenu(childMenu);
	    MarkMode.Exclusive.createMenuItem(childMenu, this);
	    MarkMode.Inclusive.createMenuItem(childMenu, this);
	    MarkMode.Count.createMenuItem(childMenu, this);
	    MenuItem mitem = new MenuItem (contextMenu, SWT.PUSH);
	    mitem.setText ("Expand children");
	    mitem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] items = tree.getSelection();
				for (TreeItem item : items) {
					expandTree(item);
				}
			}
		});
		tree.setMenu(contextMenu);
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
					e.item = (TreeItem) label.getData("_TREEITEM");
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
						if (item != null && ((String) item.getData("EXTRA")).length() > 0) {
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
							label.setData("_TREEITEM", item);
							label.setText((String) item.getData("EXTRA"));
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

	/**
	 * Sets the mark mode.
	 * @param type New mode.
	 */
	private void setMarkMode(MarkMode type) {
		this.markMode = type;
		updateExplanation();
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	protected void createPages() {
		createRawTextPage();
		createExplainPage();
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/**
	 * Method declared on IEditorPart
	 * @param marker The marker to go to.
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput)
		throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput) && !(editorInput instanceof FileStoreEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput: " + editorInput.getClass().getName());
		super.init(site, editorInput);
		setPartName(editorInput.getName());
	}

	/**
	 * Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Return the color to be used on a certain row.
	 * @param n The node to check.
	 * @param totalTime The totaltime used for the query.
	 * @return Color to use or <code>null</code>
	 */
	private Color getColor(Node n, float totalTime) {
		switch (markMode) {
			case Exclusive:
				if (n.getTimeExclusive() > 0.9 * totalTime) {
					return red;
				}
				else if (n.getTimeExclusive() > 0.5 * totalTime) {
					return brown;
				}
				else if (n.getTimeExclusive() > 0.1 * totalTime) {
					return yellow;
				}
				break;
			case Inclusive:
				if (n.getTimeInclusive() > 0.9 * totalTime) {
					return red;
				}
				else if (n.getTimeInclusive() > 0.5 * totalTime) {
					return brown;
				}
				else if (n.getTimeInclusive() > 0.1 * totalTime) {
					return yellow;
				}
				break;
			case Count:
				if (n.getRowCountInfo().count > 1000) {
					return red;
				}
				else if (n.getRowCountInfo().count > 100) {
					return brown;
				}
				else if (n.getRowCountInfo().count > 10) {
					return yellow;
				}
				break;
		}
		return null;
	}

	/**
	 * Returns an empty string if the input contains 0, otherwise it returns the input.
	 * @param input Input string.
	 * @return EMpty string or input.
	 */
	private String setEmptyIfZero(String input) {
		if ("0".equals(input)) {
			return "";
		}
		return input;
	}

	/**
	 * Insert a node and its children.
	 * @param n The node to insert.
	 * @param node The parent node in the tree.
	 * @param totalTime Total execution time.
	 */
	private void insertNodes(Node n, TreeItem node, float totalTime) {
		TreeItem item = null;
		if (node != null) {
			item = new TreeItem(node, SWT.NONE);
		}
		else {
			item = new TreeItem(tree, SWT.NONE);
		}
		String text = n.getMainLine();
		String extra = n.getExtraInformation();
		if (extra.length() > 0 && !disableMultiLine) {
			text = text + "\n" + extra;
		}
		item.setText(new String[] {setEmptyIfZero(decimalFormat.format(n.getTimeInclusive())), setEmptyIfZero(decimalFormat.format(n.getTimeExclusive())), n.getRowCountInfo().toString(), text});
		item.setData("EXTRA", extra);
		Color c = getColor(n, totalTime);
		if (c != null) {
			item.setBackground(c);
		}
		for (Node child : n.getChildren()) {
			insertNodes(child, item, totalTime);
		}
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
	 * Updates the explanation tree.
	 */
	private void updateExplanation() {
		if (tree != null) {
			tree.dispose();
		}
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		disableMultiLine = store.getBoolean(PreferenceConstants.P_DISABLEMULTILINE);
		createTree();
		String editorText = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();
		Node n = Engine.analyze(editorText);
		float totalTime = n.getTotalTime();
		insertNodes(n, null, totalTime);
		expandTree(tree.getItem(0));
	}

	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 1) {
			updateExplanation();
		}
	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event){
		if(event.getType() == IResourceChangeEvent.PRE_CLOSE){
			Display.getDefault().asyncExec(new Runnable(){
				public void run(){
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i<pages.length; i++){
						if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart,true);
						}
					}
				}
			});
		}
	}
}

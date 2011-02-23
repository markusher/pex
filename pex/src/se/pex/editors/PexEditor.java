package se.pex.editors;


import java.text.DecimalFormat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
 * TODO: Support subplan when parsing (Fixed but only verified on one plan)
 * TODO: Publish to marketplace
 * TODO: Investigate how to sign, is that possible
 * TODO: Incorrect row height on items not visible from the start
 * TODO: Add JUnit test for the parser
 * TODO: Only update the tree if something has changed
 * TODO: Enable hiding cost and actual time data, maybe a separate column for loops
 */
public class PexEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** Used as a holder for data in the menu. */
	private static final String MODE_NAME = "MODE";

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
			throw new IllegalArgumentException(Messages.PexEditor_MarkModeNotExist + name);
		}

		/**
		 * Creates a menu item for mode selection.
		 * @param childMenu The menu where to append the menu item.
		 * @param editor The editor.
		 * @return The new menu item.
		 */
		public MenuItem createMenuItem(Menu childMenu, final PexEditor editor) {
		    MenuItem mitem = new MenuItem (childMenu, SWT.RADIO);
		    mitem.setData(MODE_NAME, this);
		    mitem.setText (this.name());
		    if (editor.markMode == this) {
		    	mitem.setSelection(true);
		    }
	    	mitem.addSelectionListener(editor.adapter);
		    return mitem;
		}
	}

	/** Bad color. */
	static final Color red = new Color(Display.getCurrent(), 255, 0, 0);
	/** Pretty bad color. */
	static final Color brown = new Color(Display.getCurrent(), 255, 128, 51);
	/** A little bad color. */
	static final Color yellow = new Color(Display.getCurrent(), 255, 255, 102);

	/** An implementation for the tree, made as an interface to easily be able to test different options as the SWT tree widget is pretty bad. */
	static TreeImplementation treeImpl;

	/** The text editor used in the text page. */
	private TextEditor editor;

	/** Mode used for selecting colors. */
	private MarkMode markMode = MarkMode.Exclusive;

	/** Used to format floats. */
	DecimalFormat decimalFormat = new DecimalFormat("#.###"); //$NON-NLS-1$

	/** Disables multiline tree entries. */
	private boolean disableMultiLine;

	/** The editor. */
	private final PexEditor instance = this;

	/** Selection adapter for setting markmode. */
	private SelectionAdapter adapter = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			MenuItem item = (MenuItem) e.getSource();
			if (item.getSelection()) {
				instance.setMarkMode((MarkMode) ((MenuItem) e.getSource()).getData(MODE_NAME));
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
			setPageText(addPage(editor, getEditorInput()), Messages.PexEditor_Text);
		} catch (PartInitException e) {
			ErrorDialog.openError(
				getSite().getShell(),
				"Error creating nested text editor", //$NON-NLS-1$
				null,
				e.getStatus());
		}
	}

	/**
	 * Create the explanation page.
	 */
	void createExplainPage() {
		treeImpl = new NebulaTreeImpl(getContainer(), this);
		setPageText(addPage(treeImpl.createTree()), Messages.PexEditor_Explain);
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
			throw new PartInitException("Input: Must be IFileEditorInput:" + editorInput.getClass().getName());
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
		if ("0".equals(input)) { //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		}
		return input;
	}

	/**
	 * Insert a node and its children.
	 * @param node The node to insert.
	 * @param parent The parent node in the tree.
	 * @param totalTime Total execution time.
	 */
	private void insertNodes(Node node, Object parent, float totalTime) {
		Object newParent = treeImpl.addNode(node, parent, disableMultiLine, getColor(node, totalTime), setEmptyIfZero(decimalFormat.format(node.getTimeInclusive())), setEmptyIfZero(decimalFormat.format(node.getTimeExclusive())));
		for (Node child : node.getChildren()) {
			insertNodes(child, newParent, totalTime);
		}
	}

	/**
	 * Updates the explanation tree.
	 */
	private void updateExplanation() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		disableMultiLine = store.getBoolean(PreferenceConstants.P_DISABLEMULTILINE);
		treeImpl.clearTree();
		String editorText = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();
		Node n = Engine.analyze(editorText);
		float totalTime = n.getTotalTime();
		insertNodes(n, null, totalTime);
		treeImpl.expandTree();
	}

	/**
	 * Create the context menu for the tree.
	 * @param tree The control which to attach the menu too.
	 */
	void createContextMenu(Control tree) {
		Menu contextMenu = new Menu(tree);
	    MenuItem root = new MenuItem(contextMenu, SWT.CASCADE);
	    root.setText(Messages.PexEditor_Mode);
	    Menu childMenu = new Menu(root);
	    root.setMenu(childMenu);
	    MarkMode.Exclusive.createMenuItem(childMenu, this);
	    MarkMode.Inclusive.createMenuItem(childMenu, this);
	    MarkMode.Count.createMenuItem(childMenu, this);
	    MenuItem mitem = new MenuItem (contextMenu, SWT.PUSH);
	    mitem.setText(Messages.PexEditor_ExpandChildren);
	    mitem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				treeImpl.expandSelectedNode();
			}
		});
	    mitem = new MenuItem(contextMenu, SWT.PUSH);
	    mitem.setText(Messages.PexEditor_ShowLegend);
	    mitem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PopupDialog dialog = new LegendDialog(instance.getContainer().getShell());
				dialog.open();
			}
		});
		tree.setMenu(contextMenu);
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

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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import se.pex.Activator;
import se.pex.analyze.Engine;
import se.pex.analyze.Node;
import se.pex.preferences.PreferenceConstants;


/**
 * An editor for analyzing postgresql explain analyze outputs.
 */
public class PexEditor extends MultiPageEditorPart implements IResourceChangeListener, IDocumentListener, IPropertyChangeListener {

	/** Used as a holder for data in the menu. */
	private static final String MODE_NAME = "MODE";

	/** The different mark modes. */
	enum MarkMode {
		/** Based on exclusive times. */
		Exclusive,
		/** Based on inclusive times. */
		Inclusive,
		/** Based on row counts. */
		Count,
		/** Colors the respective columns. */
		Mixed;

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
	private TreeImplementation treeImpl;

	/** The text editor used in the text page. */
	public PexTextEditor editor;

	/** Mode used for selecting colors. */
	private MarkMode markMode = MarkMode.Exclusive;

	/** Used to format floats. */
	DecimalFormat decimalFormat = new DecimalFormat("#.###"); //$NON-NLS-1$

	/** The editor. */
	private final PexEditor instance = this;

	/** Dirty flag for the explanation page. */
	private boolean documentChanged = true;

	/** Determines if never executed paths should be folded. */
	private boolean foldNe;

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
		foldNe = store.getBoolean(PreferenceConstants.P_FOLDNEVEREXECUTED);
		markMode = MarkMode.getMarkMode(store.getString(PreferenceConstants.P_MARKMODE));
		store.addPropertyChangeListener(this);
	}

	/**
	 * @return <code>true</code> if never executed paths should be folded.
	 */
	public boolean foldNeverExecuted() {
		return foldNe;
	}

	static public class PexTextEditor extends TextEditor {
		private final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";
		private final static String EDITOR_MATCHING_BRACKETS_COLOR= "matchingBracketsColor";

		@Override
		protected void configureSourceViewerDecorationSupport (SourceViewerDecorationSupport support) {
			super.configureSourceViewerDecorationSupport(support);

			support.setCharacterPairMatcher(matcher);
			support.setMatchingCharacterPainterPreferenceKeys(EDITOR_MATCHING_BRACKETS,EDITOR_MATCHING_BRACKETS_COLOR);

			//Enable bracket highlighting in the preference store
			IPreferenceStore store = getPreferenceStore();
			store.setDefault(EDITOR_MATCHING_BRACKETS, true);
			store.setDefault(EDITOR_MATCHING_BRACKETS_COLOR, "128,128,128");
		}

		protected final static char[] BRACKETS= { '{', '}', '(', ')', '[', ']', '<', '>' };

		private final ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(BRACKETS ,
				IDocumentExtension3.DEFAULT_PARTITIONING);

		private static boolean isBracket(char character) {
			for (int i= 0; i != BRACKETS.length; ++i)
				if (character == BRACKETS[i])
					return true;
			return false;
		}

		private static boolean isSurroundedByBrackets(IDocument document, int offset) {
			if (offset == 0 || offset == document.getLength())
				return false;

			try {
				return
					isBracket(document.getChar(offset - 1)) &&
					isBracket(document.getChar(offset));

			} catch (BadLocationException e) {
				return false;
			}
		}

		/**
		 * Returns the signed current selection.
		 * The length will be negative if the resulting selection
		 * is right-to-left (RtoL).
		 * <p>
		 * The selection offset is model based.
		 * </p>
		 *
		 * @param sourceViewer the source viewer
		 * @return a region denoting the current signed selection, for a resulting RtoL selections length is < 0
		 */
		protected IRegion getSignedSelection(ISourceViewer sourceViewer) {
			StyledText text= sourceViewer.getTextWidget();
			Point selection= text.getSelectionRange();

			if (text.getCaretOffset() == selection.x) {
				selection.x= selection.x + selection.y;
				selection.y= -selection.y;
			}

			selection.x= widgetOffset2ModelOffset(sourceViewer, selection.x);

			return new Region(selection.x, selection.y);
		}


		public void gotoMatchingBracket() {
				ISourceViewer sourceViewer= getSourceViewer();
				IDocument document= sourceViewer.getDocument();
				if (document == null) {
					return;
				}

				IRegion selection= getSignedSelection(sourceViewer);

				final int selectionLength = Math.abs(selection.getLength());
				if (selectionLength > 1) {
					sourceViewer.getTextWidget().getDisplay().beep();
					return;
				}

				int sourceCaretOffset= selection.getOffset() + selection.getLength();
				if (isSurroundedByBrackets(document, sourceCaretOffset)) {
					sourceCaretOffset -= selection.getLength();
				}

				final IRegion region= matcher.match(document, sourceCaretOffset);
				if (region == null) {
					sourceViewer.getTextWidget().getDisplay().beep();
					return;
				}

				int offset= region.getOffset();
				int length= region.getLength();

				if (length < 1)
					return;

				int anchor = matcher.getAnchor();
				int targetOffset= (ICharacterPairMatcher.RIGHT == anchor) ? offset + 1: offset + length;

				boolean visible= false;
				if (sourceViewer instanceof ITextViewerExtension5) {
					ITextViewerExtension5 extension= (ITextViewerExtension5) sourceViewer;
					visible= (extension.modelOffset2WidgetOffset(targetOffset) > -1);
				} else {
					IRegion visibleRegion= sourceViewer.getVisibleRegion();
					visible= (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength());
				}

				if (!visible) {
					sourceViewer.getTextWidget().getDisplay().beep();
					return;
				}

				if (selection.getLength() < 0)
					targetOffset -= selection.getLength();

				sourceViewer.setSelectedRange(targetOffset, selection.getLength());
				sourceViewer.revealRange(targetOffset, selection.getLength());
			}
	}

	/**
	 * Creates page 0 of the multi-page editor,
	 * which contains a text editor.
	 */
	void createRawTextPage() {
		try {
			editor = new PexTextEditor();
			setPageText(addPage(editor, getEditorInput()), Messages.PexEditor_Text);
			editor.getDocumentProvider().getDocument(getEditorInput()).addDocumentListener(this);
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
		treeImpl = new JFaceTreeImpl(getContainer(), this);
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
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(this);
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
		setPartName(editor.getEditorInput().getName());
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
		if (!(editorInput instanceof IEditorInput) && !(editorInput instanceof FileStoreEditorInput))
			throw new PartInitException("Input: Must be IEditorInput:" + editorInput.getClass().getName());
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
	 * @param column The column to get color for.
	 * @return Color to use or <code>null</code>
	 */
	public Color getColor(Node n, float totalTime, MarkMode column) {
		return getColor(markMode, n, totalTime, column);
	}

	/**
	 * Return the color to be used on a certain row.
	 * @param mode The markmode to use.
	 * @param n The node to check.
	 * @param totalTime The totaltime used for the query.
	 * @param column The column to get color for.
	 * @return Color to use or <code>null</code>
	 */
	private Color getColor(MarkMode mode, Node n, float totalTime, MarkMode column) {
		switch (mode) {
			case Mixed:
				if (column == MarkMode.Inclusive) {
					return getColor(MarkMode.Inclusive, n, totalTime, null);
				}
				else if (column == MarkMode.Exclusive) {
					return getColor(MarkMode.Exclusive, n, totalTime, null);
				}
				else if (column == MarkMode.Count) {
					return getColor(MarkMode.Count, n, totalTime, null);
				}
				break;
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
				if (n.getTimeInclusive(true) > 0.9 * totalTime) {
					return red;
				}
				else if (n.getTimeInclusive(true) > 0.5 * totalTime) {
					return brown;
				}
				else if (n.getTimeInclusive(true) > 0.1 * totalTime) {
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
	 * Formats a number as a string, 0 turns into an empty string.
	 * @param input The input number.
	 * @return Formatted output.
	 */
	public String formatFloat(float input) {
		return setEmptyIfZero(decimalFormat.format(input));
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
	 * Updates the explanation tree.
	 */
	private void updateExplanation() {
		treeImpl.clearTree();
		String editorText = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();
		Node n = Engine.analyze(editorText);
		treeImpl.setRootNode(n);
		treeImpl.expandTree();
	}

	/**
	 * Create the context menu for the tree.
	 * @param tree The control which to attach the menu too.
	 * @return The menu that was created.
	 */
	public Menu createContextMenu(Control tree) {
		Menu contextMenu = new Menu(tree);
	    MenuItem root = new MenuItem(contextMenu, SWT.CASCADE);
	    root.setText(Messages.PexEditor_Mode);
	    Menu childMenu = new Menu(root);
	    root.setMenu(childMenu);
	    MarkMode.Exclusive.createMenuItem(childMenu, this);
	    MarkMode.Inclusive.createMenuItem(childMenu, this);
	    MarkMode.Count.createMenuItem(childMenu, this);
	    MarkMode.Mixed.createMenuItem(childMenu, this);
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
		return contextMenu;
	}

	/**
	 * Calculates the contents of explanation page when the it is activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 1 && documentChanged) {
			updateExplanation();
			documentChanged = false;
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

	/**
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	@Override
	public void documentAboutToBeChanged(DocumentEvent arg0) {
	}

	/**
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	@Override
	public void documentChanged(DocumentEvent arg0) {
		documentChanged = true;
	}

	/**
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(PreferenceConstants.P_FOLDNEVEREXECUTED)) {
			instance.foldNe = (Boolean) event.getNewValue();
		}
	}
}

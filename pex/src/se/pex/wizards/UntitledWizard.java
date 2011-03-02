package se.pex.wizards;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;

import se.pex.Activator;

/**
 * Wizard that creates an unnamed file.
 */
public class UntitledWizard extends Wizard implements INewWizard {

	/** Used to get the workbench window. */
	private IWorkbenchWindow fWindow;

	/**
	 * Constructor for UntitledWizard.
	 */
	public UntitledWizard() {
		super();
	}

	/**
	 * @return The file store.
	 */
	private IFileStore queryFileStore() {
		IPath stateLocation = Activator.getDefaultStateLocation();
		IPath path = stateLocation.append("/_" + new Object().hashCode() + ".pex"); //$NON-NLS-1$
		return EFS.getLocalFileSystem().getStore(path);
	}

	/**
	 * An editor input.
	 * @param fileStore The file store.
	 * @return Editor input.
	 */
	private IEditorInput createEditorInput(IFileStore fileStore) {
		return new NonExistingFileEditorInput(fileStore, "Untitled");
	}

	/**
	 * Get the editor id, based on the filestore.
	 * @param fileStore The file store.
	 * @return The editor id.
	 */
	private String getEditorId(IFileStore fileStore) {
		IWorkbench workbench = fWindow.getWorkbench();
		IEditorRegistry editorRegistry = workbench.getEditorRegistry();
		IEditorDescriptor descriptor = editorRegistry.getDefaultEditor(fileStore.getName());
		if (descriptor != null)
			return descriptor.getId();
		return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		IFileStore fileStore = queryFileStore();
		IWorkbenchPage page = fWindow.getActivePage();
		try {
			IEditorInput input = createEditorInput(fileStore);
			String editorId = getEditorId(fileStore);
			IDE.openEditor(page, input, editorId);
		} catch (PartInitException e) {
			MessageDialog.openError(getShell(), "Error", e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize
	 * from it.
	 *
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fWindow = workbench.getActiveWorkbenchWindow();
	}
}
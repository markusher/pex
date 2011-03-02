package se.pex.wizards;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.editors.text.ILocationProvider;

/**
 * An editor input for creating unnamed files.
 */
public class NonExistingFileEditorInput implements IEditorInput, ILocationProvider {

	/*** A counter. */
	private static int fgNonExisting = 0;

	/** The file store for the editor input. */
	private IFileStore fFileStore;
	/** The name */
	private String fName;

	/**
	 * Creates a new editor input.
	 * @param fileStore The filestore.
	 * @param namePrefix The name prefix.
	 */
	public NonExistingFileEditorInput(IFileStore fileStore, String namePrefix) {
		fFileStore = fileStore;
		++fgNonExisting;
		fName = namePrefix + " " + fgNonExisting; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		return false;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return fName;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/*
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		return fName;
	}

	/*
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter) {
		if (ILocationProvider.class.equals(adapter))
			return this;
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/*
	 * @see
	 * org.eclipse.ui.editors.text.ILocationProvider#getPath(java.lang.Object)
	 */
	public IPath getPath(Object element) {
		if (element instanceof NonExistingFileEditorInput) {
			NonExistingFileEditorInput input = (NonExistingFileEditorInput) element;
			return new Path(input.fFileStore.toURI().getPath());
		}
		return null;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof NonExistingFileEditorInput) {
			NonExistingFileEditorInput input = (NonExistingFileEditorInput) o;
			return fFileStore.equals(input.fFileStore);
		}

		return false;
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return fFileStore.hashCode();
	}
}

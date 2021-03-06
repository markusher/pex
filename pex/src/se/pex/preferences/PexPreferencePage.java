package se.pex.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import se.pex.Activator;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PexPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	/**
	 * Creates a preference page.
	 */
	public PexPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Preferences for the Postgresql Explain Analyze Editor");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {

		addField(new RadioGroupFieldEditor(
				PreferenceConstants.P_MARKMODE,
			"Default mark mode",
			1,
			new String[][] { { "&Exclusive", "exclusive" }, {
				"&Inclusive", "inclusive" },
				{"&Count", "count"},
				{"&Mixed", "mixed"}
		}, getFieldEditorParent()));

		addField(new BooleanFieldEditor(PreferenceConstants.P_SHOW_INCLUSIVE, "Show inclusive column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_SHOW_EXCLUSIVE, "Show exclusive column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_SHOW_ROWCOUNT, "Show rowcount column", getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.P_SHOW_LOOP, "Show loops column", getFieldEditorParent()));

		addField(new BooleanFieldEditor(PreferenceConstants.P_FOLDNEVEREXECUTED, "Fold Never Executed paths", getFieldEditorParent()));
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}
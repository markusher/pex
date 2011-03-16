package se.pex.editors;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "se.pex.editors.messages"; //$NON-NLS-1$
	public static String Pex_Show;
	public static String Pex_Exclusive;
	public static String Pex_Inclusive;
	public static String Pex_Information;
	public static String Pex_Rowcount;
	public static String PexEditor_ExpandChildren;
	public static String PexEditor_Explain;
	public static String PexEditor_MarkModeNotExist;
	public static String PexEditor_Mode;
	public static String PexEditor_ShowLegend;
	public static String PexEditor_Text;
	public static String Pex_Loops;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}

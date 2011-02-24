package se.pex.editors;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import se.pex.analyze.Node;

/**
 * An implementation of the tree, can use different widgets for this purpose.
 */
public interface TreeImplementation {

	/**
	 * @return Creates a new tree and returns the main composite object.
	 */
	public Composite createTree();

	/**
	 * Inserts a new node in the tree.
	 * @param node The node to insert.
	 * @param parentNode The parent node, if <code>null</code> then it is a root node.
	 * @param disableMultiLine Disables multiline texts.
	 * @param c The color to set on this line, <code>null</code> if no color specified.
	 * @param inclusiveTime A string with the inclusive time.
	 * @param exclusiveTime A string with the exclusive time.
	 * @return The entry inserted, will be sent as parent in the next node.
	 */
	public Object addNode(Node node, Object parentNode, boolean disableMultiLine, Color c, String inclusiveTime, String exclusiveTime);

	/**
	 * Alternative to using the addNode this function is called when the root is changed.
	 * @param node The new root node.
	 * @return <code>false</code> to ignore calling addNode.
	 */
	public boolean setRootNode(Node node);

	/**
	 * Clears the tree.
	 */
	public void clearTree();

	/**
	 * Expands the entire tree.
	 */
	public void expandTree();

	/**
	 * Expands the currently selected node.
	 */
	public void expandSelectedNode();
}

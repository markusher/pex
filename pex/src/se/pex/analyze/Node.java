package se.pex.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A node in the analyze tree.
 */
public class Node {
	/** Regex for getting time. */
	static final Pattern timePattern = Pattern.compile(".*actual\\stime=(.*)\\.\\.(.*)\\srows=(.*)\\sloops=(\\d*).*");
	/** Regex for getting rowcount. */
	static final Pattern rowCountPattern = Pattern.compile(".*cost=([\\d\\.]*)\\srows=(\\d*).*");

	/** Main line for the node. */
	private StringBuilder line;

	/** Extra lines for the node. */
	private List<String> lines = new ArrayList<String>();

	/** Node children. */
	private List<Node> children = new ArrayList<Node>();

	/** Parent node. */
	private Node parent;

	/** Number of spaces on the line, used during parsing. */
	private int indentation;

	/**
	 * Creates a new node.
	 * @param parent The parent node.
	 * @param line The main line describing the node.
	 * @param indentation Spaces on the line.
	 */
	protected Node(Node parent, String line, int indentation) {
		this.line = new StringBuilder(line);
		this.indentation = indentation;
		if (parent != null) {
			this.parent = parent;
			parent.addChild(this);
		}
	}

	/**
	 * Adds a child node.
	 * @param n The child to add.
	 */
	private void addChild(Node n) {
		children.add(n);
	}

	/**
	 * Adds more text to the last line added to the node.
	 * @param string Text to append.
	 */
	public void appendToLastLine(String string) {
		if (lines.size() == 0) {
			line.append(string);
		}
		else {
			lines.add(lines.remove(lines.size() - 1) + string);
		}
	}

	/**
	 * Adds a new line to the node.
	 * @param trimmed The line.
	 */
	public void addLine(String trimmed) {
		lines.add(trimmed);
	}

	/**
	 * Adds indentation to a line.
	 * @param count Number of spaces to add.
	 * @param res The stringbuilder to add it to.
	 * @return The stringbuilder.
	 */
	private StringBuilder addIndentation(int count, StringBuilder res) {
		for (int i = 0; i < count; i++) {
			res.append(" ");
		}
		return res;
	}

	/**
	 * @return The main line of text describing the node.
	 */
	public String getMainLine() {
		return line.toString();
	}

	/**
	 * @return The execution time inclusive child nodes.
	 */
	public float getTimeInclusive() {
		Matcher m = timePattern.matcher(line.toString());
		if (m.matches()) {
			return Float.parseFloat(m.group(2)) * Integer.parseInt(m.group(4));
		}
		return 0;
	}

	/**
	 * @return The execution time exclusive child nodes.
	 */
	public float getTimeExclusive() {
		float result = getTimeInclusive();
		for (Node child : children ) {
			result -= child.getTimeInclusive();
		}
		return result;
	}

	/**
	 * @return The parent node.
	 */
	public Node getParent() {
		return parent;
	}

	/**
	 * A string describing the node and its children.
	 * @param indentation Number of spaces to indent lines with.
	 * @param res Stringbuilder to add output to.
	 * @return The resulting string.
	 */
	private String toString(int indentation, StringBuilder res) {
		StringBuilder internal = res != null ? res : new StringBuilder();
		addIndentation(indentation, internal).append(line).append("\n");
		for (String line : lines) {
			addIndentation(indentation, internal).append(line).append("\n");
		}
		for (Node child : children) {
			child.toString(indentation + 2, internal);
		}
		return internal.toString();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toString(0, null);
	}

	/**
	 * @return The child nodes.
	 */
	public List<Node> getChildren() {
		return children;
	}

	/**
	 * Goes up in the tree until reaching a node with a smaller indentation.
	 * @param newIndentation Indentation to look for.
	 * @return A node with smalled indentation.
	 */
	public Node walkToIndentation(int newIndentation) {
		Node result = this;
		while (result != null && result.indentation >= newIndentation) {
			result = result.getParent();
		}
		return result;
	}

	/**
	 * @return <code>true</code> if any of the children contains a node with the text SubPlan in the beginning.
	 */
	public boolean findSubPlanNode() {
		for (Node child : children) {
			if (child.line.indexOf("SubPlan") == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return Get the total execution time, can be called on any node in the tree.
	 */
	public float getTotalTime() {
		Node node = this;
		while (node.getParent() != null) {
			node = node.getParent();
		}
		return node.getTimeInclusive();
	}

	/**
	 * @return The extra information on the node.
	 */
	public String getExtraInformation() {
		StringBuilder res = new StringBuilder();
		for (String line : lines) {
			res.append(line).append("\n");
		}
		return res.toString().trim();
	}

	/**
	 * @return The row count information for a line.
	 */
	public RowCountInfo getRowCountInfo() {
		Matcher m = timePattern.matcher(line.toString());
		if (m.matches()) {
			int actual = Integer.parseInt(m.group(3));
			m = rowCountPattern.matcher(line.toString());
			if (m.matches()) {
				int estimated = Integer.parseInt(m.group(2));
				return new RowCountInfo(actual, estimated);
			}
		}
		return new RowCountInfo(-1, -1);
	}


	/**
	 * @return <code>true</code> if the path has been executed.
	 */
	public boolean isExecuted() {
		return line.indexOf("(never executed)") == -1;
	}

	/**
	 * Container class that contains information about the row count diff between estimated and actual.
	 */
	public class RowCountInfo {
		/** The ratio between estimated and actual. */
		public int count;

		/** Indicates in which direction the estimate differs. */
		public boolean toHigh;

		/**
		 * Creates a new row count info.
		 * @param actual Actual number of lines.
		 * @param estimated Estimated number of lines.
		 */
		public RowCountInfo(int actual, int estimated) {
			if (estimated < 0) {
				count = -1;
			}
			else if (estimated == 0 || actual == 0) {
				// TODO: Is this really correct
				count = 0;
			}
			else if (estimated > actual) {
				count = estimated / actual;
				toHigh = true;
			}
			else {
				count = actual / estimated;
				toHigh = false;
			}
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			if (count < 0) {
				return "";
			}
			if (count == 1) {
				return "1";
			}
			return count + " " + (toHigh ? "↑" : "↓");
		}
	}
}


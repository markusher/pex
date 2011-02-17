package se.pex.analyze;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * The part that analyzes the explain analyze text.
 */
public class Engine {
	/**
	 * Creates a new node.
	 * @param lines The lines to analyze.
	 * @param index The line index that is currently being analyzed.
	 * @param currentNode The current node.
	 * @param indentation The indentation of the line.
	 * @return A newly created node.
	 */
	private static Node createNode(String[] lines, int index, Node currentNode, int indentation) {
		Node newNode = new Node(currentNode,lines[index].trim(), indentation);
		analyze(lines, index + 1, newNode, indentation);
		return newNode;
	}

	/**
	 * Analyzes the input, line by line.
	 * @param lines The lines to analyze.
	 * @param index The index of the first line to analyze.
	 * @param currentNode The current node, which will become the parent of any new nodes under this.
	 * @param indentation The current space indentation.
	 * @return A new node.
	 */
	private static Node analyze(String[] lines, int index, Node currentNode, int indentation) {
		for (int i = index; i < lines.length; i++) {
			String trimmed = lines[i].trim();
			if (!trimmed.startsWith("---")) {
				if (trimmed.contains("Total runtime")) {
					return null;
				}
				if (trimmed.startsWith("->")) {
					// Count number of space, if same as indentation, then use parent node instead
					int newIndentation = getIndentation(lines, i);
					currentNode = currentNode.walkToIndentation(newIndentation);
					return createNode(lines, i, currentNode, newIndentation);
				}
				else if (currentNode == null) {
					return createNode(lines, i, currentNode, indentation);
				}
				else {
					if (lines[i].length() > 0 && lines[i].charAt(0) != ' ') {
						currentNode.appendToLastLine(lines[i]);
					}
					else if (trimmed.startsWith("SubPlan")) {
						trimmed = lines[i + 1].trim();
						int subplanIndentation = getIndentation(lines, i);
						currentNode = currentNode.walkToIndentation(subplanIndentation);
						if (!currentNode.findSubPlanNode()) {
							new Node(currentNode, "SubPlan", subplanIndentation);
						}
						return createNode(lines, i + 1, currentNode, subplanIndentation);
					}
					else {
						currentNode.addLine(trimmed);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get the number of spaces of the current line, spaces are used to determine whether a node is a subnode or not.
	 * @param lines The lines to analyze.
	 * @param i The line index to get indentation for.
	 * @return The number of spaces of the line.
	 */
	private static int getIndentation(String[] lines, int i) {
		int newIndentation = 0;
		while (lines[i].length() > newIndentation && lines[i].charAt(newIndentation) == ' ') {
			newIndentation++;
		}
		return newIndentation;
	}

	/**
	 * Analyzes and creates a node tree of all the data from the explain.
	 * @param text The explain as given by psql.
	 * @return The root node in the resulting tree.
	 */
	public static Node analyze(String text) {
		String input = text;
		if (text.lastIndexOf("----") > -1) {
			input = text.substring(text.lastIndexOf("----") + 5);
		}
		return analyze(input.split("\n"), 0, null, 0);
	}

	/**
	 * Analyzes and creates a node tree of all the data from the explain.
	 * @param stream The explain as given by psql.
	 * @return The root node in the resulting tree.
	 * @throws IOException On error reading from the stream.
	 */
	public static Node analyze(BufferedReader stream) throws IOException {
		StringBuilder text = new StringBuilder();
		String line = stream.readLine();
		while (line != null) {
			text.append(line).append("\n");
			line = stream.readLine();
		}
		return analyze(text.toString());
	}

}

package se.pex.analyze;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.Test;

/**
 * A test for the parser engine. Will read files that will look like
 *
 * output from node.tostring on the root node
 * -----
 * text
 *
 * and make sure that there is a match.
 */
public class EngineTest {

	/**
	 * Reads files, parses the query and make sure that the output looks like expected.
	 * @throws IOException On error reading the input stream.
	 */
	@Test
	public void testAnalyzeString() throws IOException {
		int index = 1;
		do {
			StringBuilder expected = new StringBuilder();
			StringBuilder explain = new StringBuilder();
			InputStream stream = Engine.class.getResourceAsStream("test" + index + ".pex");
			if (stream == null) {
				if (index == 1) {
					Assert.fail("Error loading first test file");
				}
				return;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			String line = br.readLine();
			boolean onExplain = false;
			while (line != null) {
				if (onExplain) {
					explain.append(line).append('\n');
				}
				else if (line.equals("-----")) {
					onExplain = true;
				}
				else {
					expected.append(line).append('\n');
				}
				line = br.readLine();
			}
			checkMatch(expected, explain, index);
			index++;
		} while (true);
	}

	/**
	 * Checks so that the expected output looks like the actual output, otherwise causes a JUnit failure.
	 * @param expected The expected string.
	 * @param explain The explain string that is sent to the parser engine.
	 * @param index The index of the test file.
	 */
	private void checkMatch(StringBuilder expected, StringBuilder explain, int index) {
		Node n = Engine.analyze(explain.toString());
		if (!expected.toString().trim().equalsIgnoreCase(n.toString().trim())) {
			System.out.println(n.toString());
			Assert.fail("Error in file: test" + index + ".pex");
		}
	}

}

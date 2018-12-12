package au.com.sensis.whereis.fake.bridge;

public class LineAssembler {
	
	private StringBuffer candidateLineBuffer = new StringBuffer();
	
	/**
	 * Adds the fragment to the currently assembling line, and
	 * checks for the line terminator, returning true if found.
	 */
	public boolean isCompleteLineAfterAppending(String fragment) {
		candidateLineBuffer.append(fragment);
		return (indexOfLineTerminator() >= 0);
	}
	
	public int indexOfLineTerminator() {
		return candidateLineBuffer.indexOf("\n");
	}
	
	/**
	 * If a complete line is in the buffer, remove and return it (leaving
	 * any remaining text in the buffer as the start of the next command).
	 * Else returns null.
	 */
	public String nextLine() {
		int i = indexOfLineTerminator();
		if (i < 0) {
			return null;
		}
		String allReceivedText = candidateLineBuffer.toString();
		String line = allReceivedText.substring(0, i);			// Strip line terminator.
		String remaining = allReceivedText.substring(i + 1);
		candidateLineBuffer = new StringBuffer(remaining);
		return line;
	}

}

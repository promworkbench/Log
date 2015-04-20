package org.processmining.log.csvimport;

public enum CSVQuoteCharacter {
	
	QUOTE("QUOTE (\")", '"');

	private final String description;
	private final char quoteChar;

	private CSVQuoteCharacter(String description, char quoteCharacter) {
		this.description = description;
		this.quoteChar = quoteCharacter;
	}

	public String toString() {
		return description;
	}

	public char getQuoteChar() {
		return quoteChar;
	}

}

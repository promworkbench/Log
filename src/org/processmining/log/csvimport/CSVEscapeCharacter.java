package org.processmining.log.csvimport;

public enum CSVEscapeCharacter {
	
	QUOTE("QUOTE (\")", '"');
	
	private final String description;
	private final char escapeChar;

	private CSVEscapeCharacter(String description, char escapeCharacter) {
		this.description = description;
		this.escapeChar = escapeCharacter;
	}

	public String toString() {
		return description;
	}

	public char getEscapeChar() {
		return escapeChar;
	}

}

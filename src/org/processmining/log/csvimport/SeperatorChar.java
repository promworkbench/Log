package org.processmining.log.csvimport;

public enum SeperatorChar {
	COMMA("Comma (,)", ','), 
	SEMICOLON("Semicolon (;)",';'),
	TAB("Tab", '\t'),
	WHITESPACE("Whitespace",' ');

	private final String description;
	private final char seperatorChar;

	private SeperatorChar(String description, char seperatorChar) {
		this.description = description;
		this.seperatorChar = seperatorChar;
	}

	public String toString() {
		return description;
	}

	public char getSeperatorChar() {
		return seperatorChar;
	}

}
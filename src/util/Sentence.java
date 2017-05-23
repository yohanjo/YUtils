package util;

import java.util.Vector;

public class Sentence {

	private Vector<Word> words;
	
	public Sentence() {
		words = new Vector<Word>();
	}
	
	public void addWord(Word word) {
		words.add(word);
	}
	
	public Vector<Word> words() {
		return words;
	}
	
	public int length() {
		return words.size();
	}
	
}

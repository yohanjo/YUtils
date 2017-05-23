package util;

import java.util.Vector;


public class Document {

	private int docNo;
	private int author;
	private Vector<Sentence> sentences = new Vector<Sentence>();
	
	public Document(){
	}
	
	public Document(int docNo) {
		this.docNo = docNo;
	}
	
	public void setDocNo(int docNo) {
		this.docNo = docNo;
	}
	
	public void setAuthor(int author) {
		this.author = author;
	}
	
	public int docNo() {
		return docNo;
	}
	
	public int author() {
		return author;
	}
	
	public Vector<Sentence> sentences() {
		return sentences;
	}
	
	public int addSentence() {
		sentences.add(new Sentence());
		return sentences.size();
	}
	
	public void addWord(Word word){
		sentences.lastElement().addWord(word);
	}

	public void addWord(int wordNo) {
		addWord(new Word(wordNo));
	}
	
	public int length() {
		int length = 0;
		for (Sentence sentence : sentences)
			length += sentence.length();
		return length;
	}
	
	public int numSentences() {
		return sentences.size();
	}

//	public Vector<Word> getWords() {
//		return words;
//	}
	
//	public void setWords(Vector<Word> words){
//		this.words = words;
//	}


//	public TreeMap<Integer,Integer> getWordCount() {
//		TreeMap<Integer,Integer> wordCntTable = new TreeMap<Integer,Integer>();
//		for (Word word : this.words) {
//			Integer cnt = wordCntTable.get(word.wordNo);
//			if (cnt == null) wordCntTable.put(word.wordNo, 1);
//			else wordCntTable.put(word.wordNo, cnt+1);
//		}
//		return wordCntTable;
//	}
	

	
}


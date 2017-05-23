package util;

public class Word implements Comparable<Word> {

	public Integer wordNo = null;
//	public Vocabulary voca = null;
	public String string = null;
	
//	public Word(int wordNo, Vocabulary voca){
//		this.wordNo = wordNo;
//		this.voca = voca;
//	}
	
	public Word(int wordNo){
		this.wordNo = wordNo;
	}
	
	public Word(String wordString) {
		this.string = wordString;
	}
	
	public Integer wordNo() {
		return wordNo;
	}
	
	public void setWordNo(int wordNo) {
		this.wordNo = wordNo;
	}

	public String string() {
		if (string != null) return string;
//		else if (voca != null) return voca.getWordString(wordNo);
		else return null;
	}
	
	public void setString(String string) {
		this.string = string;
	}

	public boolean equals(Word word) {
		if (wordNo != null && word.wordNo() != null) return (this.wordNo == word.wordNo());
		else if (string != null && word.string() != null) return (this.string.equals(word.string()));
		else return false;
	}

	@Override
	public int compareTo(Word word)  {
		assert (wordNo != null || string != null);
		if (wordNo != null && word.wordNo() != null) return (this.wordNo - word.wordNo());
		else return (this.string.compareTo(word.string()));
	}
	
}
package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class Vocabulary {

	public String [] wordList;
	
	public Vocabulary(String filePath) throws Exception {
		loadFromFile(filePath);
	}
	
	public String getWordString(int wordNo) {
		return wordList[wordNo];
	}
	
	public Integer getWordNo(String wordString) {
		for (int wordNo = 0; wordNo < wordList.length; wordNo++)
			if (wordList[wordNo].equals(wordString)) return wordNo;
		return null;
	}
	
	public int size() {
		return wordList.length;
	}
	
	public void loadFromFile(String filePath) throws Exception {
		Vector<String> voca = new Vector<String>();
		
		BufferedReader file = new BufferedReader(new FileReader(filePath));
		String line;
		while ((line = file.readLine()) != null) {
			voca.add(line);
		}
		file.close();
		
		this.wordList = new String[voca.size()];
		voca.toArray(this.wordList);
	}
}

package util.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import util.datastructure.Counter;


public class TermFrequency {
	
	static public Counter<String> countFilesUpToNGrams(String inDir, int n) throws Exception {
		Counter<String> counter = new Counter<String>();
		String [] fileNames = new File(inDir).list();
		for (String fileName : fileNames) {
			System.err.println(fileName);
			countFileUpToNGrams(inDir+"/"+fileName, n, counter);
		}
		return counter;
	}
	
	static public void countFileUpToNGrams(String inFilePath, int n, Counter<String> counter) throws Exception {
		BufferedReader inFile = new BufferedReader(new FileReader(inFilePath));
		String line;
		while ((line = inFile.readLine()) != null) {
			addUpToNGrams(line, n, counter);
		}
		inFile.close();
	}

	static public Counter<String> countFileUpToNGrams(String inFilePath, int n) throws Exception {
		Counter<String> counter = new Counter<String>();
		BufferedReader inFile = new BufferedReader(new FileReader(inFilePath));
		String line;
		while ((line = inFile.readLine()) != null) {
			addUpToNGrams(line, n, counter);
		}
		inFile.close();
		return counter;
	}
	
	static public void addUpToNGrams(String text, int n, Counter<String> counter) {
		String [] tokens = text.split(" ");
		for (int i = 1; i <= n; i++)
			addNGrams(tokens, i, counter);
	}
	
	static public void addNGrams(String [] tokens, int n, Counter<String> counter) {
		if (tokens.length < n) return; 
		for (int i = 0; i < tokens.length-n+1; i++) {
			String phrase = tokens[i];
			for (int j = 1; j < n; j++)
				phrase += " " + tokens[i+j];
			counter.increase(phrase);
		}
	}


	
}

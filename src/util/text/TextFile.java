package util.text;

import java.util.Collection;
import java.util.TreeSet;

import util.io.BufferedFileReader;
import util.io.PrintFileWriter;

public class TextFile {

	public static void main(String [] args) throws Exception {
		RemoveDuplicateLines();
	}
	
	public static void RemoveDuplicateLines() throws Exception {
		String inFilePath = "d:/data/twitter/experiments/2012.08.01 training set 재정비/감성 단어.txt";
		String outFilePath = "d:/data/twitter/experiments/2012.08.01 training set 재정비/감성 단어-.txt";
		
		BufferedFileReader inFile = new BufferedFileReader(inFilePath);
		
		
		Collection<String> bag = new TreeSet<String>();
		while (inFile.nextLine()) {
			String line = inFile.readLine();
			if (!bag.contains(line)) bag.add(line);
		}
		inFile.close();
		
		// Print
		PrintFileWriter outFile = new PrintFileWriter(outFilePath);
		for (String line : bag) {
			outFile.println(line);
		}
		outFile.close();
	}
}

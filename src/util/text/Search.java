package util.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.datastructure.Tuple;


public class Search {

	public static void main(String [] args) throws Exception {
		Search.filesAndPrint("맥OS.*윈도우|윈도우.*맥OS", "g:/data/twitter/raw data", "g:/data/twitter/topics/전북대/맥OS윈도우.json");
//		Search.fileAndPrint("무상급식", "C:/Research/Data/Twitter/Topics/무상급식, 반값등록금, 아이패드, 갤스2/All-Space/Sampled_twitter_무상급식.txt", "C:/Research/Data/Twitter/Topics/무상급식, 반값등록금, 아이패드, 갤스2/All-Space/Sampled_twitter_무상급식__.txt");
	}
	
	public static void filesAndPrint(String pattern, String inDir, String outFilePath) throws Exception {
		if (outFilePath != null) new File(outFilePath).delete();
		String [] fileNames = new File(inDir).list();
		for (String fileName : fileNames) {
			System.err.println(fileName);
			if (outFilePath != null) Search.fileAndPrint(pattern, inDir+"/"+fileName, outFilePath, true);
			else Search.fileAndPrint(pattern, inDir+"/"+fileName, null);
		}
	}
	
	public static void fileAndPrint(String pattern, String inFilePath) throws Exception {
		Search.fileAndPrint(pattern, inFilePath, null);
	}
	
	public static void fileAndPrint(String pattern, String inFilePath, boolean append) throws Exception {
		Search.fileAndPrint(pattern, inFilePath, null, append);
	}
	
	public static void fileAndPrint(String pattern, String inFilePath, String outFilePath) throws Exception {
		Search.fileAndPrint(pattern, null, inFilePath, outFilePath);
	}
	
	public static void fileAndPrint(String pattern, String inFilePath, String outFilePath, boolean append) throws Exception {
		Search.fileAndPrint(pattern, null, inFilePath, outFilePath, append);
	}
	
	public static void fileAndPrint(String pattern, Integer groupNo, String inFilePath, String outFilePath) throws Exception {
		Vector<Tuple<Pattern,Integer>> patternList = new Vector<Tuple<Pattern,Integer>>(1);
		patternList.add(new Tuple<Pattern,Integer>(Pattern.compile(pattern), groupNo));
		Search.fileAndPrint(patternList, inFilePath, outFilePath, false);
	}
	
	public static void fileAndPrint(String pattern, Integer groupNo, String inFilePath, String outFilePath, boolean append) throws Exception {
		Vector<Tuple<Pattern,Integer>> patternList = new Vector<Tuple<Pattern,Integer>>(1);
		patternList.add(new Tuple<Pattern,Integer>(Pattern.compile(pattern), groupNo));
		Search.fileAndPrint(patternList, inFilePath, outFilePath, append);
	}
	
	public static void fileAndPrint(List<Tuple<Pattern,Integer>> patternList, String inFilePath, String outFilePath, boolean append) throws Exception {
		BufferedReader inFile = new BufferedReader(new FileReader(inFilePath));
		PrintStream outStream;
		if (outFilePath != null) outStream = new PrintStream(new FileOutputStream(outFilePath, append));
		else outStream = System.out;
		
		String line;
		while ((line = inFile.readLine()) != null) {
			for (Tuple<Pattern,Integer> patternEntry : patternList) {
				Pattern pattern = patternEntry.first();
				Integer groupNo = patternEntry.second();
				Matcher m = pattern.matcher(line);
				if (m.find()) {
//					if (groupNo == null) System.out.println(line);
//					else System.out.println(m.group(groupNo));
					if (groupNo == null) outStream.println(line);
					else outStream.println(m.group(groupNo));
				}
			}
		}
		inFile.close();
	}
}

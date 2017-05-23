package util.text;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.io.YFileReader;

public class Dependency {
	public static String processDependencyFeatures(String sentence) {
		TreeMap<Integer,Set<String>> dependency = new TreeMap<Integer,Set<String>>();
		TreeMap<Integer,String> words = new TreeMap<Integer,String>();
		words.put(-1, "ROOT");
		
		Matcher m = Pattern.compile("\\{([\\d]+),([\\S]*),(-?[\\d]+)\\} ").matcher(sentence);
		while (m.find()) {
			int wordNo = Integer.valueOf(m.group(1));
			String word = m.group(2);
			int parentNo = Integer.valueOf(m.group(3));
			
			Set<String> children = dependency.get(parentNo);
			if (children == null) {
				children = new TreeSet<String>();
				children.add(word);
				dependency.put(parentNo, children);
			}
			else {
				children.add(word);
				dependency.put(parentNo, children);
			}
			
			words.put(wordNo, word);
		}
		
		String result = "";
		for (int parentNo : dependency.keySet()) {
			String feature = "{" + words.get(parentNo);
			for (String word : dependency.get(parentNo)) {
				feature += "," + word;
			}
			feature += "} ";
			
			result += feature;
		}
		
		return result;
	}

	public static void doProcessDependencyFeatures() throws Exception {
		String [] inDirs = { 
				"c:/research/data/twitter/all/all-space-1-dparse",
				"c:/research/data/twitter/all/all-space-2-dparse",
				"c:/research/data/twitter/all/all-space-3-dparse",
				"c:/research/data/twitter/all/all-space-4-dparse"
		};
		String [] outDirs = {
				"c:/research/data/twitter/all/all-space-1-dependency",
				"c:/research/data/twitter/all/all-space-2-dependency",
				"c:/research/data/twitter/all/all-space-3-dependency",
				"c:/research/data/twitter/all/all-space-4-dependency"
		};
		
		for (int i = 0; i < inDirs.length; i++) {
			String [] fileNames = new File(inDirs[i]).list();
			for (String fileName : fileNames) {
				System.err.println(inDirs[i] + "/" + fileName);
				YFileReader inFile = new YFileReader(inDirs[i] + "/" + fileName);
				PrintWriter outFile = new PrintWriter(new FileWriter(outDirs[i] + "/" + fileName));
				while (inFile.readLine()) {
					String line = inFile.getLine();
					outFile.println(Dependency.processDependencyFeatures(line));
				}
				inFile.close();
				outFile.close();
			}
		}
	}
	
	public static void main(String [] args) throws Exception {
		doProcessDependencyFeatures();
	}
}

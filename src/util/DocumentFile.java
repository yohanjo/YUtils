package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import util.datastructure.Tuple;

public class DocumentFile {
	public Vector<Tuple<Pattern,String>> replacePatternList = new Vector<Tuple<Pattern,String>>();
	public Vector<Tuple<Pattern,String>> wordReplacePatternList = new Vector<Tuple<Pattern,String>>();
	public Vector<Tuple<Pattern,String>> collapsePatternList = new Vector<Tuple<Pattern,String>>();
	
	public TreeSet<Word> stopWords = new TreeSet<Word>();
	
	public Pattern sentenceDelimiter = null; //Pattern.compile("[.!?\\n]");
	public Pattern wordDelimiter = Pattern.compile("[\\s]+");
	public Integer minWordLength = 1;
	public Integer maxWordLength = 10000;
	public Integer minSentenceLength = 1;
	public Integer maxSentenceLength = 10000;
	public Integer minWordOccur = 1;
	public Integer minDocLength = 1;
	public boolean usePOSTagger = false;
	public boolean useStemmer = false;
	public boolean caseSensitive = false;
	
	public Vector<Vector<Vector<Integer>>> bag;
	private TreeMap<Word,Integer> wordIndex;
//	public Vector<String> docList;
//	private TreeMap<String,Integer> authorIndex;
//	public Vector<Integer> authorIndexForDoc;
	
//	StanfordPOSTagger tagger;
	
	public DocumentFile() throws Exception {
		this(false, false);
	}
	
	public DocumentFile(boolean POSTagging, boolean stemming) throws Exception {
		this.usePOSTagger = POSTagging;
		this.useStemmer = stemming;
		initialize();
	}
	
	public void initialize() throws Exception {
//		if (this.usePOSTagger)tagger = new StanfordPOSTagger();  // Should be generalized
		bag = new Vector<Vector<Vector<Integer>>>();
		wordIndex = new TreeMap<Word,Integer>();
//		docList = new Vector<String>();
//		authorIndexForDoc = new Vector<Integer>();
//		authorIndex = new TreeMap<String,Integer>();
	}
	
	public void addReplacePattern(String regex, String replace) {
		Pattern pattern;
		if (!this.caseSensitive) pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		else pattern = Pattern.compile(regex);
		replacePatternList.add(new Tuple<Pattern,String>(pattern, replace));
	}
	
	public void addWordReplacePattern(String regex, String replace) {
		Pattern pattern;
		if (!this.caseSensitive) pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		else pattern = Pattern.compile(regex);
		wordReplacePatternList.add(new Tuple<Pattern,String>(pattern, replace));
	}
	
	public void addCollapsePattern(String regex, String collapse) {
		Pattern pattern;
		if (!this.caseSensitive) pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		else pattern = Pattern.compile(regex);
		collapsePatternList.add(new Tuple<Pattern,String>(pattern,collapse));
	}
	
	public void setStopWords(String filePath) throws Exception {
		stopWords.clear();
		BufferedReader stopWordsFile = new BufferedReader(new FileReader(filePath));
		String line;
		while ((line = stopWordsFile.readLine()) != null) {
			stopWords.add(new Word(line));
		}
		stopWordsFile.close();
	}
	
	public int [] getWordCount() {
		int [] count = new int[wordIndex.size()];
		for (Vector<Vector<Integer>> sentenceList : this.bag){
			for (Vector<Integer> sentence : sentenceList) {
				for (int wordIndex : sentence) {
					count[wordIndex]++;
				}
			}
		}
		return count;
	}
	
	public Vector<Vector<Integer>> buildDocument(String document) throws Exception {
		Vector<Vector<Word>> sentenceList = new Vector<Vector<Word>>();
		Vector<Vector<Word>> tmpSentenceList = new Vector<Vector<Word>>();
		String newDocument = document;
		int numTotalWords = 0;
		
		// Replace the patterns in replacePatternList
		for (Tuple<Pattern,String> rp : replacePatternList) {
			newDocument = rp.first().matcher(document).replaceAll(rp.second());
		}
		
		// Tokenize
		String [] sentences;
		if (sentenceDelimiter == null) {
			sentences = new String[1];
			sentences[0] = newDocument;
		}
		else {
			sentences = sentenceDelimiter.split(newDocument);
		}
		for (String sentence : sentences) {
			Vector<Word> wordList = new Vector<Word>();
			String [] words = wordDelimiter.split(sentence);
			for (String word : words)
				wordList.add(new Word(word));
			tmpSentenceList.add(wordList);
		}
		
		// Filter
		PorterStemmer stemmer = new PorterStemmer();
		for (Vector<Word> tmpSentence : tmpSentenceList) {
			Vector<Word> sentence = new Vector<Word>();
			for (Word word : tmpSentence) {
				if (word.string().length() < minWordLength || word.string().length() > this.maxWordLength) continue;
				if (!this.caseSensitive) word.setString(word.string().toLowerCase());
				for (Tuple<Pattern,String> rp : wordReplacePatternList) {
					rp.first().matcher(word.string()).replaceAll(rp.second());
				}
				
				// Stemming
				if (this.useStemmer)
					word.setString(stemmer.stemming(word.string()));
				
				// Stop words
				if (stopWords.contains(word.string())) continue;
				
				sentence.add(word);
			}
			
			if (sentence.size() >= minSentenceLength && sentence.size() <= maxSentenceLength) {
				sentenceList.add(sentence);
				numTotalWords += sentence.size();
			}
		}
		
		if (numTotalWords < this.minDocLength) return null;
		
		// Store into the bag
		Vector<Vector<Integer>> indexSentenceList = new Vector<Vector<Integer>>();
		for (Vector<Word> wordList : sentenceList) {
			Vector<Integer> sentence = new Vector<Integer>();
			for (Word word : wordList) {
				Integer index = wordIndex.get(word);
				if (index == null) {
					index = wordIndex.size();
					wordIndex.put(word, index);
				}
				sentence.add(index);
			}
			indexSentenceList.add(sentence);
		}
		this.bag.add(indexSentenceList);
	
		return indexSentenceList;
	}
	
	public void filterWords() {
		System.out.println("Filtering Words...");
		
		TreeSet<Word> removeWords = new TreeSet<Word>();
		TreeSet<Integer> removeWordIndices = new TreeSet<Integer>();
		while (true) {
			int [] wordCount = getWordCount();
			int cntRemoveWords = 0;
			for (Map.Entry<Word,Integer> entry : this.wordIndex.entrySet()) {
				Word word = entry.getKey();
				Integer index = entry.getValue();
				if (wordCount[index] < minWordOccur) {
					if (!removeWords.contains(word)) {
						removeWords.add(word);
						removeWordIndices.add(index);
						cntRemoveWords++;
					}
				}
			}
			
			if (cntRemoveWords == 0) break;
			
			Vector<Vector<Vector<Integer>>> newBag = new Vector<Vector<Vector<Integer>>>();
			for (Vector<Vector<Integer>> document : this.bag) {
				Vector<Vector<Integer>> newDocument = new Vector<Vector<Integer>>();
				for (Vector<Integer> sentence : document) {
					Vector<Integer> newSentence = new Vector<Integer>();
					for (int wordIndex : sentence) {
						if (!removeWordIndices.contains(wordIndex)) newSentence.add(wordIndex);
					}
					if (newSentence.size() >= minSentenceLength && newSentence.size() <= maxSentenceLength)
						newDocument.add(newSentence);
				}
				if (newDocument.size() > 0) newBag.add(newDocument);
			}
			this.bag = newBag;
		}
		
		for (Word word : removeWords) {
			this.wordIndex.remove(word);
		}
		
		sort();
	}
	
	public void sort() {
		System.out.println("Sorting Words...");
		HashMap<Integer,Integer> indexToIndex = new HashMap<Integer,Integer>();
		Integer newWordIndex = 0;
		for (Word word : this.wordIndex.keySet()) {
			indexToIndex.put(this.wordIndex.get(word), newWordIndex);
			this.wordIndex.put(word, newWordIndex++);
		}
		for (Vector<Vector<Integer>> document : this.bag) {
			for (int s = 0; s < document.size(); s++) {
				Vector<Integer> newSentence = new Vector<Integer>();
				for (int oldWordIndex : document.get(s)) {
					newWordIndex = indexToIndex.get(oldWordIndex);
					if (newWordIndex != null) newSentence.add(newWordIndex);
				}
				document.set(s, newSentence);
			}
		}
	}
	
	public String [] getWordList() {
		String [] list = new String[wordIndex.size()];
		for (Map.Entry<Word,Integer> entry : wordIndex.entrySet()) {
			list[entry.getValue()] = entry.getKey().string();
		}
		return list;
	}
	
	public void write(String outDir) throws Exception {
		String [] wordList = getWordList();
		
		int [] wordCount = getWordCount();
		PrintWriter out = new PrintWriter(new FileWriter(outDir+"/WordCount.csv"));
		for (int i = 0; i < wordList.length; i++) out.println("\""+wordList[i].replaceAll("\"", "\"\"")+"\","+wordCount[i]);
		out.close();
	
		out = new PrintWriter(new FileWriter(outDir+"/WordList.txt"));
		for (String word:wordList) out.println(word);
		out.close();
		
//		Vector<String> docList = this.docList;
//		out = new PrintWriter(new FileWriter(new File(outDir+"/DocumentList.txt")));
//		for (String doc : docList) out.println(doc);
//		out.close();
		
//		if (!this.authorIndex.isEmpty()) {
//			String [] authorList = this.getAuthorList();
//			out = new PrintWriter(new FileWriter(new File(outDir+"/AuthorList.txt")));
//			for (String author : authorList) out.println(author);
//			out.close();
//			
//			Vector<Integer> authors = this.authorIndexForDoc;
//			out = new PrintWriter(new FileWriter(new File(outDir+"/Authors.txt")));
//			for (Integer author : authors) out.println(author);
//			out.close();
//		}
		
		out = new PrintWriter(new FileWriter(new File(outDir+"/BagOfSentences.txt")));
		for (Vector<Vector<Integer>> sentenceList : this.bag) {
			out.println(sentenceList.size());
			for (Vector<Integer> sentence : sentenceList) {
				for (int wordIndex : sentence)
					out.print(wordIndex + " ");
				out.println();
			}
		}
		out.close();
	}
	
	public static <DocumentType extends Document> Vector<DocumentType> instantiate(Class<DocumentType> documentTypeClass, String filePath) throws Exception {
		Vector<DocumentType> documents = new Vector<DocumentType>();
		BufferedReader docFile = new BufferedReader(new FileReader(filePath));
		
		int docCount = 0;
		String line;
		while ((line = docFile.readLine()) != null) {
			DocumentType document = documentTypeClass.newInstance();
			document.setDocNo(docCount++);
			
			int numSentences = Integer.valueOf(line);
			assert(numSentences == 1);
			for (int i = 0; i < numSentences; i++) {
				document.addSentence();
				line = docFile.readLine();
				String [] tokens = line.split(" ");
				for (int j = 0; j < tokens.length; j++) {
					document.addWord(Integer.valueOf(tokens[j]));
				}
			}
			documents.add(document);
		}
		docFile.close();
		
		return documents;
	}
	
	public static <DocumentType extends Document> Vector<DocumentType> instantiate(Class<DocumentType> documentTypeClass, String filePath, List<Integer> authors) throws Exception {
		Vector<DocumentType> documents = instantiate(documentTypeClass, filePath);
		for (int i = 0; i < authors.size(); i++) {
			documents.get(i).setAuthor(authors.get(i));
		}
		return documents;
	}

	
	public static void main(String [] args) throws Exception {
		String filePath = "G:/Data/Twitter/test/twitter_20101101";
		BufferedReader file = new BufferedReader(new FileReader(filePath));
		
		DocumentFile df = new DocumentFile();
		df.minWordOccur = 5;
		
		String line;
		while ((line = file.readLine()) != null) {
			df.buildDocument(line);
		}
		file.close();
		
		df.filterWords();
		df.write("G:/data/twitter/test");
		
	}
}

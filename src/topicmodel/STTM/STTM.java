package topicmodel.STTM;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import util.container.Counter;
import util.io.BufferedFileReader;
import util.io.PrintFileWriter;
import util.json.JSONArray;
import util.json.JSONObject;
import util.matrix.DoubleMatrix;
import util.matrix.IntegerMatrix;

public class STTM {
	
	enum EntryType { LDA, MM };
	
	private static class EntryParams {  // For each entry
		EntryType type;  // 0: LDA, 1: MM
	}
	
	private static class LDAEntryParams extends EntryParams {
		int numTopics=-1;
		double alpha=-1, sumAlpha=-1;
		DoubleMatrix N_ST;

		int numWords=-1, minNumWords=1, numProbWords=100;
		double beta=-1, sumBeta=-1;
		TreeMap<String,Integer> wordIndex = null;
		Vector<String> words = null;
		Counter<String> wordCnt = new Counter<String>(); 
		TreeSet<String>  stopwords = new TreeSet<String>(); 
		DoubleMatrix N_TW;
	}
	
	private static class MMEntryParams extends EntryParams {
		int numTokens=-1;
		TreeMap<String,Integer> tokenIndex = null;
		Vector<String> tokens = null;
		double alpha=-1, sumAlpha=-1;
		DoubleMatrix N_ST;
	}
	
	private static class StatusParams {
		String key = null;
		static double gamma, sumGamma;
		DoubleMatrix N_SS;
		DoubleMatrix N_0S;
		public StatusParams(String key) {
			this.key = key;
		}
	}

	static int numStates=-1;
	static int numIters=-1, numTmpIters=-1;
	static String inDir=null, outDir=null;

	static String dataFileName = null;
	static String confFileName = null;

	static TreeMap<String,Integer> statusIndex = null;
	static TreeMap<String,EntryParams> entryParams = new TreeMap<String,EntryParams>(); 
	static Vector<StatusParams> statusParams = new Vector<StatusParams>();
	static String outPrefix = null;
	
	static TreeMap<String, TreeMap<Integer,Instance>> seqs = new TreeMap<String, TreeMap<Integer,Instance>>();


	
	public static class Parameters {
		  @Parameter(names = "-i", description = "Number of iterations", required=true)
		  public int numIters = -1;
		 
		  @Parameter(names = "-to", description = "Number of iterations for temporary output files")
		  public int numTmpIters = -1;
		 
		  @Parameter(names = "-d", description = "Input directory", required=true)
		  public String inDir = null;
		  
		  @Parameter(names = "-o", description = "Output directory")
		  public String outDir = null;
		  
		  @Parameter(names = "-data", description = "Data file name", required=true)
		  public String dataFileName = null;
		  
		  @Parameter(names = "-conf", description = "Config file name", required=true)
		  public String confFileName = null;
		  
		  @Parameter(names = "-help", description = "Command description", help=true)
		  public boolean help;
	}

	public static void main(String [] args) throws Exception {
		
		Parameters inParams = new Parameters();
		JCommander cmd = new JCommander(inParams, args);
		if (inParams.help) {
			cmd.usage();
			System.exit(0);
		}
		  
		numIters = inParams.numIters;
		numTmpIters = inParams.numTmpIters;
		inDir = inParams.inDir;
		outDir = inParams.outDir;
		dataFileName = inParams.dataFileName;
		confFileName = inParams.confFileName;
		if (outDir == null) outDir = new String(inDir);

		
		// Load config file
		loadConfig(inDir+"/"+confFileName);
		outPrefix = "STTM-"+dataFileName.replace(".csv","")+"-"+confFileName.replace(".json","")+"-S"+numStates;

		
		// Read instances
		System.out.println("Loading data...");
		TreeMap<String, TreeMap<Integer,RawInstance>> rawSeqs = new TreeMap<String, TreeMap<Integer,RawInstance>>();
		TreeSet<String> statuses = new TreeSet<String>();
		TreeMap<String, TreeSet<String>> tokens = new TreeMap<String, TreeSet<String>>();
		MaxentTagger tagger = new MaxentTagger("rsc/english-left3words-distsim.tagger");
		CSVParser inData = new CSVParser(new FileReader(inDir+"/"+dataFileName), CSVFormat.EXCEL.withHeader());
		Map<String,Integer> header = inData.getHeaderMap();
		for (CSVRecord record : inData) {
			RawInstance inst = new RawInstance();
			for (String key : header.keySet()) {
				if (key.equals("SeqId")) {
					if (!rawSeqs.containsKey(record.get("SeqId"))) 
						rawSeqs.put(record.get("SeqId"), new TreeMap<Integer,RawInstance>());
				}
				else if (key.equals("InstNo")) {
					rawSeqs.get(record.get("SeqId")).put(Integer.valueOf(record.get("InstNo")), inst);
				}
				else if (key.equals("FromStatus")) {
					String status = record.get("FromStatus");
					inst.fromStatus = status;
					if (statusIndex==null) statuses.add(status);
				}
				else if (key.equals("ToStatus")) {
					String status = record.get("ToStatus");
					inst.toStatus = status;
					if (statusIndex==null) statuses.add(status);
				}
				else {  // Entry
					if (!entryParams.containsKey(key)) continue;
					if (entryParams.get(key).type == EntryType.LDA) {
						LDAEntryParams thisEntryParams = (LDAEntryParams)entryParams.get(key);
						inst.entries.put(key, new RawEntry());
						String text = record.get(key);
//						text = text.replaceAll("https?://[\\S]*", "URL");
						String taggedString = tagger.tagString(text);
						String [] words = taggedString.split("_[^_ ]* ");
						for (String word : words) {
							if (word.length()==0) continue;
							String w = word.toLowerCase();
							if (thisEntryParams.stopwords.contains(w)) continue;
//							if (w.matches("^[^a-zA-Z]+$")) continue;
							inst.entries.get(key).words.add(w);
							thisEntryParams.wordCnt.increase(w);
						}
					}
					else {
						MMEntryParams thisEntryParams = (MMEntryParams)entryParams.get(key);
						inst.entries.put(key, new RawEntry());
						for (String token : record.get(key).split(",")) {
							inst.entries.get(key).tokens.add(token);
							if (thisEntryParams.tokenIndex==null) {
								if (!tokens.containsKey(key)) tokens.put(key, new TreeSet<String>());
								tokens.get(key).add(token);
							}
						}
					}
				}
			}
		}
		inData.close();
		
		
		// Statuses
		if (!header.containsKey("FromStatus") && !header.containsKey("ToStatus")) {
			statusIndex = new TreeMap<String,Integer>();  // This may override config
			statusIndex.put("DefaultStatus", 0);
		}
		if (statusIndex == null) {
			statusIndex = new TreeMap<String,Integer>();
			for (String status : statuses) statusIndex.put(status, statusIndex.size());
		}
		String [] statusKeys = new String[statusIndex.size()];
		for (String key : statusIndex.keySet()) statusKeys[statusIndex.get(key)] = key; 
		for (String key : statusKeys) statusParams.add(new StatusParams(key));

		
		// Make indices
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type == EntryType.LDA) {
				LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
				if (params.wordIndex != null) continue;
				params.wordIndex = new TreeMap<String,Integer>();
				params.words = new Vector<String>();
				for (String word : params.wordCnt.keySet()) {
					if (params.minNumWords > 0 && params.wordCnt.get(word) < params.minNumWords) continue;
					params.wordIndex.put(word, params.wordIndex.size());
					params.words.add(word);
				}
				params.numWords = params.wordIndex.size();
			}
			else {
				MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
				if (params.tokenIndex != null) continue;
				params.tokenIndex = new TreeMap<String,Integer>();
				params.tokens = new Vector<String>();
				for (String token : tokens.get(entryKey)) {
					params.tokenIndex.put(token, params.tokenIndex.size());
					params.tokens.add(token);
				}
				params.numTokens = params.tokenIndex.size();
			}
		}
		
		// Print WordCount
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type != EntryType.LDA) continue;
			LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
			
			Counter<String> wordCnt = params.wordCnt;
			CSVPrinter outWordCount = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-"+entryKey+"-WordCount.csv"), CSVFormat.EXCEL);
			outWordCount.printRecord(Arrays.asList("Word","Count"));
			List<String> sortedKeys = wordCnt.valueSortedKeys();
			Collections.reverse(sortedKeys);
			for (String word : sortedKeys) {
				outWordCount.printRecord(word, wordCnt.get(word));
			}
			outWordCount.flush();
			outWordCount.close();
		}
		
		
		// Indexize
		for (String seqId : rawSeqs.keySet()) {
			TreeMap<Integer,RawInstance> rawSeq = rawSeqs.get(seqId);
			TreeMap<Integer,Instance> seq = new TreeMap<Integer,Instance>();
			for (int instNo : rawSeq.keySet()) {
				RawInstance rawInst = rawSeq.get(instNo);
				Instance inst = new Instance();
				
				inst.fromStatus = statusIndex.get(rawInst.fromStatus);
				inst.toStatus = statusIndex.get(rawInst.toStatus);
				for (String entryKey : rawInst.entries.keySet()) {
					RawEntry rawEntry = rawInst.entries.get(entryKey);
					if (entryParams.get(entryKey).type == EntryType.LDA) {
						LDAEntry thisEntry = new LDAEntry();
						TreeMap<String,Integer> wordIndex = ((LDAEntryParams)entryParams.get(entryKey)).wordIndex;
						for (String word : rawEntry.words) {
							Integer index = wordIndex.get(word);
							if (index != null) thisEntry.words.add((int)index);
						}
						inst.entries.put(entryKey, thisEntry);
					}
					else {
						MMEntry thisEntry = new MMEntry();
						TreeMap<String,Integer> tokenIndex = ((MMEntryParams)entryParams.get(entryKey)).tokenIndex;
						for (String token : rawEntry.tokens)
							thisEntry.tokens.add(tokenIndex.get(token));
						inst.entries.put(entryKey, thisEntry);
					}
				}
				seq.put(instNo, inst);
			}
			seqs.put(seqId, seq);
		}
		
		
		// Initialize
		initialize();

		
		// Sample
		for (int iter = 0; iter < numIters; iter++) {
			System.out.print("Iteration "+iter);
			long startTime = new Date().getTime();
			
			for (TreeMap<Integer,Instance> instMap : seqs.values()) {
				Vector<Instance> seq = new Vector<Instance>(instMap.values());
				for (int i = 0; i < seq.size(); i++) {
					Instance inst = seq.get(i);
					Instance prevInst=null, nextInst=null;
					if (i > 0) prevInst = seq.get(i-1);
					if (i+1 < seq.size()) nextInst = seq.get(i+1);
						
					// Sample a state
					delInstFromState(inst);
					delTrans(inst, prevInst, nextInst);
					
					Double [] probs = new Double[numStates];
					for (int s = 0; s < numStates; s++) {
						probs[s] = calLogProbOfInst(inst, s);
						probs[s] += calLogProbOfTrans(s, inst, prevInst, nextInst);
					}
					double maxLogProb = Collections.max(Arrays.asList(probs));
					double sumProb = 0;
					for (int s = 0; s < numStates; s++) {
						probs[s] = Math.exp(probs[s] - maxLogProb);
						sumProb += probs[s];
					}
					
					double randNo = Math.random() * sumProb;
					double tmpSum = 0;
					int newState = -1;
					for (int s = 0; s < numStates; s++) {
						tmpSum += probs[s];
						if (randNo <= tmpSum) {
							newState = s;
							break;
						}
					}
		
					inst.state = newState;
					addInstToState(inst, newState);
					addTrans(inst, prevInst, nextInst);
					
					// Sample a topic
					sampleTopics(inst);
				}
			}
			
			long endTime = new Date().getTime();
			double seconds = (int)(endTime - startTime)/1000.0;
			int minutes = (int)(seconds * (numIters - iter - 1) / 60);
			System.out.println(" took "+String.format("%.2f", seconds)+"s. (EST: "+(minutes/60)+"h "+String.format("%02d",minutes%60)+"m)");
			
			// Generate output files
			if (iter+1 == numIters || (numTmpIters > 0 && (iter+1) % numTmpIters == 0)){
				System.out.println(" - Generating output files...");
				genOutFiles(iter+1);
			}
		}
	}
	
	public static void initialize() throws Exception {
		StatusParams.sumGamma = StatusParams.gamma * numStates;
		
		for (StatusParams params : statusParams) {
			params.N_0S = new DoubleMatrix(1,numStates);
			params.N_SS = new DoubleMatrix(numStates, numStates);
		}
		
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type == EntryType.LDA) {
				LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
				if (params.alpha < 0) throw new Exception("alpha is not specified for "+entryKey);
				if (params.numTopics < 0) throw new Exception("numTopics is not specified for "+entryKey);
				params.sumAlpha = params.alpha * params.numTopics;
				params.N_ST = new DoubleMatrix(numStates, params.numTopics);

				if (params.beta < 0) throw new Exception("beta is not specified for "+entryKey);
				if (params.numWords < 0) throw new Exception("numWords is not specified for "+entryKey);
				params.sumBeta = params.beta * params.numWords;
				params.N_TW = new DoubleMatrix(params.numTopics, params.numWords);
			}
			else if (entryParams.get(entryKey).type == EntryType.MM) {
				MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
				if (params.alpha < 0) throw new Exception("alpha is not specified for "+entryKey);
				if (params.numTokens < 0) throw new Exception("numTokens is not specified for "+entryKey);
				params.sumAlpha = params.alpha * params.numTokens;
				params.N_ST = new DoubleMatrix(numStates, params.numTokens);
			}
			else {
				throw new Exception();
			}
		}
		
		Random rand = new Random();
		for (TreeMap<Integer,Instance> seq : seqs.values()) {
			for (Instance inst : seq.values()) {
				int state = rand.nextInt(numStates);
				inst.state = state;
				for (String entryKey : inst.entries.keySet()) {
					if (entryParams.get(entryKey).type == EntryType.LDA) {
						LDAEntry entry = (LDAEntry)inst.entries.get(entryKey);
						LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
						for (int i = 0; i < entry.words.size(); i++) {
							int topic = rand.nextInt(params.numTopics);
							entry.topics.add(topic);
							params.N_ST.incValue(state,topic);
							params.N_TW.incValue(topic, entry.words.get(i));
						}
					}
					else {
						MMEntry entry = (MMEntry)inst.entries.get(entryKey);
						MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
						for (int t : entry.tokens) {
							params.N_ST.incValue(state, t);
						}	
					}
				}
			}
			// State transition
			ArrayList<Instance> seqArray = new ArrayList<Instance>(seq.values());
			for (int i = 0; i < seqArray.size(); i++) {
				Instance inst = seqArray.get(i);
				int state = inst.state;
				if (i==0) {
					statusParams.get(inst.fromStatus).N_0S.incValue(0, state);
				}
				else {
					statusParams.get(inst.fromStatus).N_SS.incValue(seqArray.get(i-1).state, state);
				}
				
				if (i < seqArray.size()-1) {
					statusParams.get(inst.toStatus).N_SS.incValue(state, seqArray.get(i+1).state);
				}
			}
		}
	}
	

	public static double calLogProbOfInst(Instance inst, int state) {
		double prob = 0.0;
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type == EntryType.LDA) {
				LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
				for (int t : ((LDAEntry)inst.entries.get(entryKey)).topics) {
					prob += Math.log(params.N_ST.getValue(state, t) + params.alpha) - Math.log(params.N_ST.getRowSum(state) + params.sumAlpha);
					params.N_ST.incValue(state,t);
				}
				for (int t : ((LDAEntry)inst.entries.get(entryKey)).topics) {
					params.N_ST.decValue(state,t);
				}
			}
			else {
				MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
				for (int t : ((MMEntry)inst.entries.get(entryKey)).tokens) {
					prob += Math.log(params.N_ST.getValue(state, t) + params.alpha) - Math.log(params.N_ST.getRowSum(state) + params.sumAlpha);
					params.N_ST.incValue(state,t);
				}
				for (int t : ((MMEntry)inst.entries.get(entryKey)).tokens) {
					params.N_ST.decValue(state,t);
				}
			}
		}
		return prob;
	}
	
	public static double calLogProbOfTrans(int state, Instance inst, Instance prevInst, Instance nextInst) {
		StatusParams thisStatusParams = statusParams.get(inst.toStatus);
		StatusParams prevStatusParams = statusParams.get(inst.fromStatus);

		double prob = 0.0;
		
		if (prevInst == null) {
			prob += Math.log(prevStatusParams.N_0S.getValue(0, state) + StatusParams.gamma) - Math.log(prevStatusParams.N_0S.getRowSum(0) + StatusParams.sumGamma);
		}
		else {
			int prevState = prevInst.state;
			prob += Math.log(prevStatusParams.N_SS.getValue(prevState, state) + StatusParams.gamma) - Math.log(prevStatusParams.N_SS.getRowSum(prevState) + StatusParams.sumGamma);
			prevStatusParams.N_SS.incValue(prevState, state);
		}
		
		if (nextInst != null) {
			prob += Math.log(thisStatusParams.N_SS.getValue(state, nextInst.state) + StatusParams.gamma) - Math.log(thisStatusParams.N_SS.getRowSum(state) + StatusParams.sumGamma);
		}
		
		if (prevInst != null) {
			int prevState = prevInst.state;
			prevStatusParams.N_SS.decValue(prevState, state);
		}
		
		return prob;
	}
	
	/* Sample the topics of the LDA-type entries in the instance */
	public static void sampleTopics(Instance inst) {
		int state = inst.state;
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type != EntryType.LDA) continue;
			LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
			
			LDAEntry entry = (LDAEntry)inst.entries.get(entryKey);
			for (int i = 0; i < entry.words.size(); i++) {
				int word = entry.words.get(i);
				int oldTopic = entry.topics.get(i);
				
				params.N_ST.decValue(state, oldTopic);
				params.N_TW.decValue(oldTopic, word);
				
				Double [] probs = new Double[params.numTopics];
				for (int t = 0; t < params.numTopics; t++) {
					probs[t] = Math.log(params.N_ST.getValue(state,t) + params.alpha) + Math.log(params.N_TW.getValue(t,word) + params.beta) - Math.log(params.N_TW.getRowSum(t) + params.sumBeta);
				}
				double maxLogProb = Collections.max(Arrays.asList(probs));
				double sumProb = 0;
				for (int t = 0; t < params.numTopics; t++) {
					probs[t] = Math.exp(probs[t] - maxLogProb);
					sumProb += probs[t];
				}
				
				double randNo = Math.random() * sumProb;
				double tmpSum = 0;
				int newTopic = -1;
				for (int t = 0; t < params.numTopics; t++) {
					tmpSum += probs[t];
					if (randNo <= tmpSum) {
						newTopic = t;
						break;
					}
				}
				
				entry.topics.set(i, newTopic);
				
				params.N_ST.incValue(state, newTopic);
				params.N_TW.incValue(newTopic, word);
			}
		}

	}
	
	
	public static void delInstFromState(Instance inst) {
		int state = inst.state;
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type == EntryType.LDA) {
				LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
				LDAEntry entry = (LDAEntry)inst.entries.get(entryKey);
				for (int topic : entry.topics) {
					params.N_ST.decValue(state,topic);
				}
			}
			else if (entryParams.get(entryKey).type == EntryType.MM) {
				MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
				MMEntry entry = (MMEntry)inst.entries.get(entryKey);
				for (int token : entry.tokens) {
					params.N_ST.decValue(state,token);
				}
			}
		}
	}
	
	public static void addInstToState(Instance inst, int state) {
		for (String entryKey : entryParams.keySet()) {
			if (entryParams.get(entryKey).type == EntryType.LDA) {
				LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
				LDAEntry entry = (LDAEntry)inst.entries.get(entryKey);
				for (int topic : entry.topics) {
					params.N_ST.incValue(state,topic);
				}
			}
			else if (entryParams.get(entryKey).type == EntryType.MM) {
				MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
				MMEntry entry = (MMEntry)inst.entries.get(entryKey);
				for (int token : entry.tokens) {
					params.N_ST.incValue(state,token);
				}
			}
		}
	}

	public static void delTrans(Instance inst, Instance prevInst, Instance nextInst) {
		StatusParams thisStatusParams = statusParams.get(inst.toStatus);
		StatusParams prevStatusParams = statusParams.get(inst.fromStatus);
		int thisState = inst.state;
		
		if (prevInst == null) {
			prevStatusParams.N_0S.decValue(0, thisState);
		}
		else {
			prevStatusParams.N_SS.decValue(prevInst.state, thisState);
		}
		
		if (nextInst != null) {
			thisStatusParams.N_SS.decValue(thisState, nextInst.state);
		}
	}
	
	public static void addTrans(Instance inst, Instance prevInst, Instance nextInst) {
		StatusParams thisStatusParams = statusParams.get(inst.toStatus);
		StatusParams prevStatusParams = statusParams.get(inst.fromStatus);
		int thisState = inst.state;
		
		if (prevInst == null) {
			prevStatusParams.N_0S.incValue(0, thisState);
		}
		else {
			prevStatusParams.N_SS.incValue(prevInst.state, thisState);
		}
		
		if (nextInst != null) {
			thisStatusParams.N_SS.incValue(thisState, nextInst.state);
		}
	}
	
	public static void genOutFiles(int iter) throws Exception {
		// Pi
		CSVPrinter outPi = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-Pi.csv"), CSVFormat.EXCEL);
		outPi.print("");
		for (int s1 = 0; s1 < numStates; s1++) outPi.print("S"+s1);
		outPi.println();
		
		for (int st = 0; st < statusParams.size(); st++) {
			StatusParams params = statusParams.get(st);
			// Initial
			outPi.print(statusParams.get(st).key+"-Init");
			for (int s1 = 0; s1 < numStates; s1++) {
				outPi.print((params.N_0S.getValue(0, s1) + StatusParams.gamma) / (params.N_0S.getRowSum(0) + StatusParams.sumGamma));
			}
			outPi.println();

			for (int s1 = 0; s1 < numStates; s1++) {
				outPi.print(statusParams.get(st).key+"-S"+s1);
				for (int s2 = 0; s2 < numStates; s2++) {
					outPi.print((params.N_SS.getValue(s1, s2) + StatusParams.gamma) / (params.N_SS.getRowSum(s1) + StatusParams.sumGamma));
				}
				outPi.println();
			}
		}
		outPi.flush();
		outPi.close();
		
		
		// Inst-state mapping
		CSVPrinter outInstState = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-InstState.csv"), CSVFormat.EXCEL);
		outInstState.print("SeqId");
		outInstState.print("InstNo");
		outInstState.print("State");
		outInstState.println();
		
		for (String seqKey : seqs.keySet()) {
			TreeMap<Integer, Instance> instMap = seqs.get(seqKey);
			for (int instNo : instMap.keySet()) {
				Instance inst = instMap.get(instNo);
				outInstState.print(seqKey);
				outInstState.print(instNo);
				outInstState.print(inst.state);
				outInstState.println();
			}
		}
		outInstState.flush();
		outInstState.close();

		
		for (String entryKey : entryParams.keySet()) {
			// Theta
			CSVPrinter outTheta = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-"+entryKey+"-Theta.csv"), CSVFormat.EXCEL);
			outTheta.print("");
			if (entryParams.get(entryKey).type == EntryType.LDA) {
				LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);
				for (int t = 0; t < params.numTopics; t++) outTheta.print("T"+t);
				outTheta.println();
				
				for (int s = 0; s < numStates; s++) {
					outTheta.print("S"+s);
					for (int t = 0; t < params.numTopics; t++) {
						outTheta.print((params.N_ST.getValue(s,t) + params.alpha) / (params.N_ST.getRowSum(s) + params.sumAlpha));
					}
					outTheta.println();
				}
				outTheta.flush();
				outTheta.close();
			}
			else if (entryParams.get(entryKey).type == EntryType.MM) {
				MMEntryParams params = (MMEntryParams)entryParams.get(entryKey);
				for (int t = 0; t < params.numTokens; t++) outTheta.print(params.tokens.get(t));
				outTheta.println();
				
				for (int s = 0; s < numStates; s++) {
					outTheta.print("S"+s);
					for (int t = 0; t < params.numTokens; t++) {
						outTheta.print((params.N_ST.getValue(s,t) + params.alpha) / (params.N_ST.getRowSum(s) + params.sumAlpha));
					}
					outTheta.println();
				}
				outTheta.flush();
				outTheta.close();
			}
			
			if (entryParams.get(entryKey).type != EntryType.LDA) continue;
			
			LDAEntryParams params = (LDAEntryParams)entryParams.get(entryKey);

			
			// DTTheta
			CSVPrinter outDTheta = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-"+entryKey+"-DTheta.csv"), CSVFormat.EXCEL);
			outDTheta.print("SeqId");
			outDTheta.print("InstNo");
			for (int t = 0; t < params.numTopics; t++) outDTheta.print("T"+t);
			outDTheta.println();
			
			for (String seqKey : seqs.keySet()) {
				TreeMap<Integer, Instance> instMap = seqs.get(seqKey);
				for (int instNo : instMap.keySet()) {
					Instance inst = instMap.get(instNo);
					outDTheta.print(seqKey);
					outDTheta.print(instNo);
					
					double [] N_DT = new double[params.numTopics];
					for (int t : ((LDAEntry)inst.entries.get(entryKey)).topics) N_DT[t]++;
					double S_DT = ((LDAEntry)inst.entries.get(entryKey)).topics.size();
					
					for (int t = 0; t < params.numTopics; t++)
						outDTheta.print((N_DT[t] + params.alpha) / (S_DT + params.sumAlpha));
					outDTheta.println();
				}
			}
			outDTheta.flush();
			outDTheta.close();
			
			
			// Phi
			CSVPrinter outPhi = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-"+entryKey+"-Phi.csv"), CSVFormat.EXCEL);
			outPhi.print("");
			for (int t = 0; t < params.numTopics; t++) outPhi.print("T"+t);
			outPhi.println();
			
			for (int w = 0; w < params.numWords; w++) {
				outPhi.print(params.words.get(w));
				for (int t = 0; t < params.numTopics; t++) {
					outPhi.print((params.N_TW.getValue(t,w) + params.beta) / (params.N_TW.getRowSum(t) + params.sumBeta));
				}
				outPhi.println();
			}
			outPhi.flush();
			outPhi.close();
				
			
			// ProbWords
			CSVPrinter outProbWords = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-"+entryKey+"-ProbWords.csv"), CSVFormat.EXCEL);
			for (int t = 0; t < params.numTopics; t++) outProbWords.print("T"+t);
			outProbWords.println();

			IntegerMatrix P_TW = params.N_TW.getSortedIndexMatrix(1, Math.min(params.numProbWords, params.numWords));
			for (int p = 0; p < Math.min(params.numProbWords, params.numWords); p++) {
				for (int t = 0; t < params.numTopics; t++) {
					int w = P_TW.getValue(t,p);
					outProbWords.print(params.words.get(w)+" ("+String.format("%.3f", (params.N_TW.getValue(t,w) + params.beta) / (params.N_TW.getRowSum(t) + params.sumBeta))+")");
				}
				outProbWords.println();
			}
			outProbWords.flush();
			outProbWords.close();
		}
		
	}
	
	public static void loadConfig(String configPath) throws Exception {
		BufferedFileReader configFile = new BufferedFileReader(configPath);
		String rString = configFile.readAll();
		JSONObject json = new JSONObject(rString);
		configFile.close();
		
		StatusParams.gamma = json.getDouble("gamma");
		numStates = json.getInt("num_states");
		if (json.has("statuses")) {
			JSONArray statuses = json.getJSONArray("statuses");
			statusIndex = new TreeMap<String,Integer>();
			for (int i = 0; i < statuses.length(); i++) {
				statusIndex.put(statuses.getString(i), statusIndex.size());
			}
		}

		JSONArray entries = json.getJSONArray("entries");
		for (int e = 0; e < entries.length(); e++) {
			JSONObject entry = entries.getJSONObject(e);
			if (entry.getString("type").equals("LDA")) {
				LDAEntryParams params = new LDAEntryParams();
				// Required
				params.type = EntryType.LDA;
				params.alpha = entry.getDouble("alpha");
				params.beta = entry.getDouble("beta");
				params.numTopics = entry.getInt("num_topics");
				// Optional
				if (entry.has("min_num_words")) params.minNumWords = entry.getInt("min_num_words");
				if (entry.has("num_prob_words")) params.numProbWords = entry.getInt("num_prob_words");
				if (entry.has("stopwords")) {
					JSONArray stopwords = entry.getJSONArray("stopwords");
					for (int i = 0; i < stopwords.length(); i++)
						params.stopwords.add(stopwords.getString(i));
				}
				if (entry.has("words")) {
					JSONArray words = entry.getJSONArray("words");
					params.wordIndex = new TreeMap<String,Integer>();
					params.words = new Vector<String>();
					for (int i = 0; i < words.length(); i++) {
						params.wordIndex.put(words.getString(i), params.wordIndex.size());
						params.words.add(words.getString(i));
					}
					params.numWords = params.words.size();
				}
				entryParams.put(entry.getString("key"), params);
			}
			else if (entry.getString("type").equals("MM")) {
				MMEntryParams params = new MMEntryParams();
				// Required
				params.type = EntryType.MM;
				params.alpha = entry.getDouble("alpha");
				// Optional
				if (entry.has("tokens")) {
					JSONArray tokens = entry.getJSONArray("tokens");
					params.tokenIndex = new TreeMap<String,Integer>();
					params.tokens = new Vector<String>();
					for (int i = 0; i < tokens.length(); i++) {
						params.tokenIndex.put(tokens.getString(i), params.tokenIndex.size());
						params.tokens.add(tokens.getString(i));
					}
					params.numTokens = params.tokens.size();
				}
				entryParams.put(entry.getString("key"), params);
			}
			else {
				throw new Exception();
			}
		}
	}
	

	
	private static class Instance {
		int state = -1;
		int fromStatus = 0, toStatus = 0;
		TreeMap<String,Entry> entries = new TreeMap<String,Entry>();
	}
	
	private static class Entry {
	}
	
	private static class LDAEntry extends Entry {
		Vector<Integer> words = new Vector<Integer>();
		Vector<Integer> topics = new Vector<Integer>();
	}
	
	private static class MMEntry extends Entry {
		Vector<Integer> tokens = new Vector<Integer>();
	}
	
	private static class RawInstance {
		String fromStatus="DefaultStatus", toStatus="DefaultStatus";
		TreeMap<String,RawEntry> entries = new TreeMap<String,RawEntry>();
	}
	
	private static class RawEntry {
		Vector<String> words = new Vector<String>();
		Vector<String> tokens = new Vector<String>();
	}
	
	
}

/**
 * Sentence LDA in the WSDM11 paper.
 * 
 * @author Yohan Jo
 * @version June 22, 2017
 */

package topicmodel.SLDA;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import util.container.Counter;
import util.io.BufferedFileReader;
import util.io.PrintFileWriter;
import util.matrix.DoubleMatrix;
import util.matrix.IntegerMatrix;
import util.matrix.MatrixView;


public class SLDA implements Callable<Void> {
	
    static boolean updateParams;  // Update parameters?
    static boolean tokenization;
    
	static double alpha, sumAlpha;
	static double beta, sumBeta;
	static double gamma;  // p(word=FG) (Bernoulli parameter)
	static int numProbWords = 100;  // Number of top words to be displayed in the ProbWords file
	
	static int numFTopics;
	static final int numBTopics = 1;
	static int numWords;
	static int numDocs;
	static int numThreads;
	static int numIters;
	static int numLogIters;  // Num of iterations for computing log-likelihood
	static int numTmpIters;  // Period of generating temporary output files
	
	static DoubleMatrix N_TW;  // Foreground topic x word count
	static DoubleMatrix N_BW;  // Background topic x word count  (only one background topic)
	static DoubleMatrix N_DT;  // Doc x topic count
	
    static String inDir = null;  // Input data directory
    static String outDir = null;  // Output directory
    static String dataFileName = null;  // Data file name (e.g., data.csv)
    static String modelPathPrefix = null;  // Trained model
    static String outPrefix = null;

    static Counter<String> wordCnt = new Counter<>();  // Vocab counts
    static TreeMap<String,Integer> wordIndex = new TreeMap<>();  // Vocab index
    static Vector<String> wordList = new Vector<>();  // Vocab list

    static TreeMap<Integer,Double> logL = new TreeMap<>();  // Log-likelihood of the data
    
    static TreeMap<String, RawDocument> rawDocs = new TreeMap<>();
    static TreeMap<String,Document> docs = new TreeMap<>();  // Data
    static TreeSet<String> stopwords = new TreeSet<String>();
    static int minNumWords = 1;  // Minimum number of occurrences for a word to be included

    
    // Thread-specific variables
    int threadId;
    List<String> docIds;
  
    // Matrix views used by each thread. Keep track of within-thread changes.
    static MatrixView V_TW;
    static MatrixView V_BW;


	/**
	 * Command arguments
	 */
	public static class Parameters {
		@Parameter(names = "-t", description = "Number of topics", required=true)
		public int numTopics = -1;

		@Parameter(names = "-i", description = "Number of iterations", required=true)
		public int numIters = -1;

		@Parameter(names = "-to", description = "Number of iterations for temporary output files.")
		public int numTmpIters = -1;
		
        @Parameter(names = "-log", description = "Number of iterations for calculating "
                                                    + " log-likelihood.")
        public int numLogIters = -1;

		@Parameter(names = "-th", description = "Number of threads.")
		public int numThreads = 1;

		@Parameter(names = "-a", description = "Alpha.", required=true)
		public double alpha = -1;

		@Parameter(names = "-b", description = "Beta.", required=true)
		public double beta = -1;

		@Parameter(names = "-g", description = "Gamma. 1 means no background topic.")
		public double gamma = 1;

		@Parameter(names = "-d", description = "Input directory.", required=true)
		public String inDir = null;

		@Parameter(names = "-o", description = "Output directory.")
		public String outDir = null;
		
        @Parameter(names = "-data", description = "Data file name.", required=true)
        public String dataFileName = null;

        @Parameter(names = "-model", description = "Trained model to fit (no parameter update).")
        public String model = null;

        @Parameter(names = "-tok", description = "Do tokenization/pattern replacement.")
        public boolean tokenize;

        @Parameter(names = "-sw", description = "Stopwords file name.")
        public String stopwordsFileName = null;

		@Parameter(names = "-help", description = "Command description.", help=true)
		public boolean help;
	}
	
	public static void main (String [] args) throws Exception {
		// Load command arguments
		Parameters inParams = new Parameters();
		JCommander cmd = new JCommander(inParams, args);
		if (inParams.help) {
			cmd.usage();
			System.exit(0);
		}
		
        if (inParams.model != null) {
            updateParams = false;
            modelPathPrefix = inParams.model;
        } else {
            updateParams = true;
        }
        tokenization = inParams.tokenize;
		numFTopics = inParams.numTopics;
		numIters = inParams.numIters;
		numThreads = inParams.numThreads;
		alpha = inParams.alpha;
		beta = inParams.beta;
		gamma = inParams.gamma;
		inDir = inParams.inDir;
		outDir = inParams.outDir;
		dataFileName = inParams.dataFileName;
		numTmpIters = inParams.numTmpIters;
		numLogIters = inParams.numLogIters;
		

		// Validity
		if (!new File(inDir).exists()) 
		    throw new Exception("There's no such an input directory as " + inDir);
		if (alpha <= 0) 
		    throw new Exception("Alpha should be specified as a positive real number.");
		if (beta <= 0) 
		    throw new Exception("Beta should be specified as a positive real number.");
		if (gamma < 0) 
		    throw new Exception("Gamma should be specified as a nonnegative real number.");

		
        // Training
        if (updateParams) {
            loadStopwords(inParams.stopwordsFileName);
            loadInstances();
            indexizeWords();
            indexizeInstances();
            
            setUpOutputEnvironment();
            saveWords();
            printConfiguration();
            
            runSampling();
            
        // Fitting (no parameter update)
        } else {
            loadWords();
            loadInstances();
            indexizeInstances();
            
            setUpOutputEnvironment();
            saveWords();
            printConfiguration();
            
            runSampling();
        }
        
	}
	
    /**
     * Prints the model configuration.
     */
    public static void printConfiguration() {
        System.out.println("Data: "+dataFileName);
        System.out.println("Documents: "+docs.size());
        System.out.println("Unique Words: "+wordList.size());
        System.out.println("Topics: "+numFTopics);
        System.out.println("Alpha: "+alpha);
        System.out.println("Beta: "+beta);
        System.out.println("Gamma: "+gamma);
        System.out.println("Iterations: "+numIters);
        System.out.println("Tmp Output Iterations: "+numTmpIters);
        System.out.println("Threads: "+numThreads);
        System.out.println("Input Dir: "+inDir);
        System.out.println("Output Dir: "+outDir);
    }
    
    
    /**
     * Runs Gibbs sampling. For each iteration, this method shuffles the order 
     * of the sequences, splits and assigns the sequences into threads,
     * and invokes and joins the threads.
     */
    public static void runSampling() throws Exception {
        initialize();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        Vector<String> docIds = new Vector<String>(docs.keySet());
        
        for (int iter = 0; iter < numIters; iter++) {
            System.out.print("Iteration "+iter);
            LocalDateTime startTime = LocalDateTime.now();

            // Randomize the order of sequences.
            // Make sure previous threads have been finished before calling this.
            Collections.shuffle(docIds);
            
            List<Callable<Void>> tasks = new Vector<Callable<Void>>();
            for (int tid = 0; tid < numThreads; tid++) {
                tasks.add(new SLDA(tid, docIds));
            }
            executor.invokeAll(tasks);
            
            Duration runTime = Duration.between(startTime, LocalDateTime.now());
            Duration remainTime = runTime.multipliedBy(numIters - iter - 1);
            System.out.println(String.format(" took %.3fs. (Remain: %dH %2dM %2dS)",
                                runTime.toMillis()/1000.0, remainTime.toHours(), 
                                remainTime.toMinutes()%60, remainTime.getSeconds()%60));

            if (numLogIters > 0 && (iter+1) % numLogIters == 0) {
                double ll = logLikelihood();
                System.out.println("  - logP: "+ll);
                logL.put(iter+1, ll);
            }
            if (iter+1 == numIters || (numTmpIters > 0 && (iter+1) % numTmpIters == 0)) {
                System.out.println("  - Generating output files...");
                genOutFiles(iter+1);
            }
        }
        executor.shutdownNow();
    }
    
    
    /**
     * Constructs an instance of this class for running a thread.
     */
    public SLDA(int threadId, List<String> docIds) {
        this.threadId = threadId;
        this.docIds = docIds;
        
        V_TW = new MatrixView(N_TW, true);
        V_BW = new MatrixView(N_BW, true);
    }
    
    
    /**
     * Samples the parameters for one iteration and 
     * updates the within-thread counters to the global counters.
     */
    @Override
    public Void call() throws Exception {
        int beginThread = threadId * docIds.size() / numThreads;
        int endThread = (threadId+1) * docIds.size() / numThreads;
        List<String> subDocIds = docIds.subList(beginThread, endThread); 

        try {
            sampleTopic(subDocIds);
            sampleLevel(subDocIds);

            // Commit within-thread changes and update the global matrix.
            V_TW.commit();
            V_BW.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }
    
    
    /**
     * Generates the output file name prefix and necessary output directories.
     */
    public static void setUpOutputEnvironment() throws Exception {
        outPrefix = "SLDA-" 
                    + dataFileName.replace(".csv","")
                    + "-T" + numFTopics
                    + "-A" + alpha
                    + "-B" + beta
                    + "-G" + gamma;
        if (outDir == null) outDir = inDir+"/"+outPrefix;
        Files.createDirectories(Paths.get(outDir));
    }

    /**
     * Loads stop words from the given file.
     * 
     * @param stopwordsFileName the name of the stop words file.
     */
    public static void loadStopwords(String stopwordsFileName) throws Exception {
        if (stopwordsFileName != null) {
            BufferedFileReader stopwordsFile = new BufferedFileReader(inDir+"/"+stopwordsFileName);
            while (stopwordsFile.nextLine()) {
                stopwords.add(stopwordsFile.readLine().trim());
            }
            stopwordsFile.close();
        }
    }

    /**
     * Loads instances from the data file. The loaded data is stored in
     * temporary data structure {@link #rawDocs}, which is later indexized 
     * in {@link #indexizeInstances()}.
     */
    public static void loadInstances() throws Exception {
        System.out.println("Loading data...");

        CSVParser inData = new CSVParser(
                new FileReader(inDir+"/"+dataFileName), CSVFormat.DEFAULT.withHeader());

        for (CSVRecord record : inData) {
            String docId = record.get("DocId");

            RawDocument doc = new RawDocument();

            doc.text = record.get("Text");
            if (tokenization) {
                DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(doc.text));
                for (List<HasWord> sentence : dp) {
                    Vector<String> words = new Vector<String>();
                    for (HasWord word : sentence) {
                        if (word.word().length()==0) continue;
                        String w = word.word().toLowerCase();
                        if (w.matches("^[\\d]+$")) w = "##NUMBER##";
                        if (stopwords.contains(w)) continue;

                        words.add(w);
                        if (updateParams) wordCnt.increase(w);
                    }
                    if (!words.isEmpty()) doc.sentences.add(words);
                }
            } else {
                Vector<String> words = new Vector<String>();
                for (String w : doc.text.split(" ")) {
                    if (w.length()==0) continue;
                    if (stopwords.contains(w)) continue;

                    words.add(w);
                    if (updateParams) wordCnt.increase(w);
                }
                if (!words.isEmpty()) doc.sentences.add(words);
            }

            if (!doc.sentences.isEmpty()) {
                rawDocs.put(docId, doc);
            }
        }
        inData.close();
    }

    /**
     * Indexizes the words loaded from the data file and generates 
     * {@link #wordIndex} and {@link #wordList}.
     */
    public static void indexizeWords() {
        wordIndex = new TreeMap<String,Integer>();
        wordList = new Vector<String>();
        for (String word : wordCnt.keySet()) {
            if (minNumWords > 0 && wordCnt.get(word) < minNumWords) continue;
            wordIndex.put(word, wordIndex.size());
            wordList.add(word);
        }
        numWords = wordIndex.size();
    }

    /**
     * Indexizes the loaded instances by changing the words and authors with
     * the corresponding indices. This methods generates {@link #seqs}.
     */
    public static void indexizeInstances() {
        for (String docId : rawDocs.keySet()) {
            RawDocument rawDoc= rawDocs.get(docId);
            Document doc = new Document();
            for (Vector<String> rawSentence : rawDoc.sentences) {
                Sentence sentence = new Sentence();
                for (String word : rawSentence) {
                    Integer index = wordIndex.get(word);
                    if (index != null) sentence.words.add(new Word(index));
                }
                if (!sentence.words.isEmpty()) doc.sentences.add(sentence);
            }
            if (!doc.sentences.isEmpty()) {
                doc.idx = docs.size();
                docs.put(docId, doc);
            }
        }
        numDocs = docs.size();
    }

    /**
     * Initializes all parameters and variables.
     */
	public static void initialize() throws Exception {
        N_DT = new DoubleMatrix(numDocs, numFTopics);
	    if (updateParams) {
    		N_TW = new DoubleMatrix(numFTopics, numWords);
    		N_BW = new DoubleMatrix(numBTopics, numWords);
	    } else {
	        restoreModel();
	    }
		
		sumAlpha = alpha * numFTopics;
		sumBeta = beta * numWords;
		
		// Randomly assign topics to sentences
		Random random = new Random();
		for (String docId : docs.keySet()) {
		    Document doc = docs.get(docId);
			for (Sentence sentence : doc.sentences) {
				int newTopic = random.nextInt(numFTopics);
				sentence.fTopic = newTopic;
				N_DT.incValue(doc.idx, newTopic);
				for (Word word : sentence.words) {
					// Choose between foreground & background
					int level = (random.nextDouble() <= gamma ? 1 : 0);
					word.level = level;
					if (updateParams) {
    					if (level==1) {  // Foreground
    						N_TW.incValue(newTopic, word.idx);
    					} else {  // Background
    						N_BW.incValue(0, word.idx);
    					}
					}
				}
			}
		}
	}

    /**
     * Saves {@link #wordCnt} to a file. 
     */  
    public static void saveWords() throws Exception {
        CSVPrinter outWordCount = new CSVPrinter(
                new PrintFileWriter(outDir+"/"+outPrefix+"-WordCount.csv"), CSVFormat.EXCEL);
        outWordCount.printRecord("Word","Count");
        for (String word : wordList)
            outWordCount.printRecord(word, wordCnt.get(word));
        outWordCount.flush();
        outWordCount.close();
    }

    
    /**
     * Saves the current counter matrices to files.
     */  
    public static void saveModel(String dir, String prefix) throws Exception {
        String outPrefix = dir+"/"+prefix;
        N_TW.saveToCsvFile(outPrefix+"-N_FW.csv");
        N_BW.saveToCsvFile(outPrefix+"-N_BW.csv");
    }

	
    /**
     * Samples the foreground topics of the given documents. 
     *
     * @param docIds the documents whose foreground topics are sampled.
     */
    private static void sampleTopic(List<String> docIds) throws Exception {
	    for (String docId : docIds) {
	        Document doc = docs.get(docId);
    		for (Sentence sentence : doc.sentences) {
    			int oldTopic = sentence.fTopic;
    			
    			// Remove old topic assignment for this sentence
                N_DT.decValue(doc.idx, oldTopic);
    			if (updateParams) {
        			for (Word word : sentence.words) {
        				if (word.level == 0) continue;
        				V_TW.decValue(oldTopic, word.idx);
        			}
    			}
    			
    			// Calculate the conditional probability of topic assignment
                double [] probs = new double[numFTopics];
    			for (int t = 0; t < numFTopics; t++) {
    			    probs[t] += Math.log(N_DT.getValue(doc.idx, t) + alpha);
    			    
    			    for (Word word : sentence.words) {
    					if (word.level == 0) continue;  // Ignore a background word
    					probs[t] += Math.log((V_TW.getValue(t, word.idx) + beta) / (V_TW.getRowSum(t) + sumBeta));
    					if (updateParams) V_TW.incValue(t, word.idx);
    				}
    			    
    			    // Revert the counter
    			    if (updateParams) {
        			    for (Word word : sentence.words) {
                            if (word.level == 0) continue;  // Ignore a background word
                            V_TW.decValue(t, word.idx);
                        }
    			    }
    			}
    			
    			// Randomly choose a new topic for the sentence
    			double maxLogP = max(probs);
                for (int t = 0; t < numFTopics; t++)
                    probs[t] = Math.exp(probs[t] - maxLogP);

                int newTopic = sampleIndex(probs);
                sentence.fTopic = newTopic;
    			
    			// Update the new topic assignment
                N_DT.incValue(doc.idx, newTopic);
                if (updateParams) {
        			for (Word word : sentence.words) {
        				if (word.level == 0) continue;
        				V_TW.incValue(newTopic, word.idx);
        			}
                }
    		}
	    }
	}
	
	
    /**
     * Samples the level ("foreground" or "background") of each word 
     * in the given documents. 
     *
     * @param docIds the documents for which the level of each word is sampled.
     */
	private static void sampleLevel(List<String> docIds) throws Exception {
	    for (String docId : docIds) {
	        Document doc = docs.get(docId);
    		for (Sentence sentence : doc.sentences) {
    			int fTopic = sentence.fTopic;
    			
    			for (Word word : sentence.words) {
    			    int wordIdx = word.idx;
    				int oldLevel = word.level;
    				
    				// Remove the old level assignment for the words
    				if (updateParams) {
        				if (oldLevel == 1) {
                            V_TW.decValue(fTopic, wordIdx);
        				} else {
                            V_BW.decValue(0, wordIdx);
        				}
    				}
    
    				// Calculate the probability of FG & BG for this word
    				double [] probs = new double[2];
                    probs[0] = (V_BW.getValue(0, wordIdx) + beta) / (V_BW.getRowSum(0) + sumBeta) * (1-gamma);
                    probs[1] = (V_TW.getValue(fTopic, wordIdx) + beta) / (V_TW.getRowSum(fTopic) + sumBeta) * gamma;
                    
                    int newLevel = sampleIndex(probs);
                    word.level = newLevel;
                    
                    if (updateParams) {
        				if (newLevel == 1) {
                            word.level = 1;
                            V_TW.incValue(fTopic, wordIdx);
        				} else {
                            word.level = 0;
                            V_BW.incValue(0, wordIdx);
        				}
                    }
    			}
    		}
	    }
	}
	
    /**
     * Returns the max value of the given array. 
     *
     * @param v the double array from which the max value is returned.
     * @return the max value of the given array.
     * @throws Exception if the array is empty.
     */
    public static double max(double[] v) throws Exception {
        return Arrays.stream(v).max().orElseThrow(
                () -> new RuntimeException("Array is empty"));
    }

    /**
     * Returns a value drawn from the categorical distribution with
     * the given array as its parameter. 
     *
     * @param probs the parameter of the categorical distribution.
     * @return the sampled index.
     */
    public static int sampleIndex(double[] probs) throws Exception {
        double[] cumulProbs = probs.clone();
        Arrays.parallelPrefix(cumulProbs, (x,y) -> x+y);
        double rand = Math.random() * cumulProbs[cumulProbs.length-1];
        return IntStream.range(0, cumulProbs.length)
                        .filter(i -> cumulProbs[i] >= rand)
                        .findFirst()
                        .orElseThrow(() -> new Exception("Sampling failed"));
    }
    
    /**
     * Restores pre-trained counter matrices from files.
     */
    public static void restoreModel() throws Exception {
        N_TW = DoubleMatrix.loadFromCsvFile(modelPathPrefix+"-N_FW.csv");
        N_BW = DoubleMatrix.loadFromCsvFile(modelPathPrefix+"-N_BW.csv");
    }
    
    /**
     * Loads indexed words {@link #wordCnt}, {@link #wordIndex}, 
     * and {@link #wordList} from files.
     */
    public static void loadWords() throws Exception {
        // Load wordCount, wordIndex, and wordList
        CSVParser csvWordCount = new CSVParser(
                new BufferedFileReader(modelPathPrefix.replaceAll("-I\\d+$","")+"-WordCount.csv"),
                CSVFormat.EXCEL.withHeader());
        for (CSVRecord record : csvWordCount) {
            String word = record.get("Word");
            double cnt = Double.valueOf(record.get("Count"));
            wordCnt.increase(word, cnt);
            wordIndex.put(word, wordIndex.size());
            wordList.add(word);
        }
        csvWordCount.close();
        numWords = wordIndex.size();
    }
    
    /**
     * Calculates and returns the log-likelihood of the data. 
     *
     * @return the log-likelihood of the data.
     */
    public static double logLikelihood() {
        DoubleMatrix P_TW = N_TW.copy();
        for (int r = 0; r < P_TW.getNumRows(); r++) {
            double denom = P_TW.getRowSum(r) + sumBeta;
            for (int c = 0; c < P_TW.getNumColumns(); c++)
                P_TW.setValue(r,c, (P_TW.getValue(r,c)+beta) / denom);
        }
        DoubleMatrix P_BW = N_BW.copy();
        for (int r = 0; r < P_BW.getNumRows(); r++) {
            double denom = P_BW.getRowSum(r) + sumBeta;
            for (int c = 0; c < P_BW.getNumColumns(); c++)
                P_BW.setValue(r,c, (P_BW.getValue(r,c)+beta) / denom);
        }
        DoubleMatrix P_DT = N_DT.copy();
        for (int r = 0; r < P_DT.getNumRows(); r++) {
            double denom = P_DT.getRowSum(r) + sumAlpha;
            for (int c = 0; c < P_DT.getNumColumns(); c++)
                P_DT.setValue(r,c, (P_DT.getValue(r,c)+alpha) / denom);
        }

        double logP = 0;
        for (String docId : docs.keySet()) {
            Document doc = docs.get(docId);
            
            DoubleMatrix N_T = new DoubleMatrix(1, numFTopics);
            for (Sentence sentence : doc.sentences) {
                int fTopic = sentence.fTopic;
                
                // p(fTopic | doc)
                logP += Math.log((N_T.getValue(0, fTopic) + alpha) / (N_T.getRowSum(0) + sumAlpha));
                N_T.incValue(0, fTopic);
                
                for (Word word : sentence.words) {
                    if (word.level == 1) {
                        logP += Math.log(gamma * P_TW.getValue(fTopic, word.idx));
                    } else {
                        logP += Math.log((1-gamma) * P_BW.getValue(0, word.idx));
                    }
                }
            }
        }

        return logP;
    }
    
    /**
     * Generates output files. 
     *
     * @param iter the iteration number of the results to be output.
     */
    public static void genOutFiles(int iter) throws Exception {
        saveModel(outDir, outPrefix+"-I"+iter);
        
		// Theta
		CSVPrinter outTheta = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-Theta.csv"), CSVFormat.EXCEL);
		outTheta.print("");
		for (int t = 0; t < numFTopics; t++) outTheta.print("T"+t);
		outTheta.println();
		
		for (String docId : docs.keySet()) {
		    Document doc = docs.get(docId);
			outTheta.print(docId);
			for (int t = 0; t < numFTopics; t++)
			    outTheta.print((N_DT.getValue(doc.idx, t) + alpha) / (N_DT.getRowSum(doc.idx) + sumAlpha));
			outTheta.println();
		}
		outTheta.flush();
		outTheta.close();
		
		
		// PhiF
		CSVPrinter outPhiF = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-PhiF.csv"), CSVFormat.EXCEL);
		outPhiF.print("");
		for (int t = 0; t < numFTopics; t++) outPhiF.print("T"+t);
		outPhiF.println();
		
		for (int w = 0; w < numWords; w++) {
			outPhiF.print(wordList.get(w));
			for (int t = 0; t < numFTopics; t++)
			    outPhiF.print((N_TW.getValue(t,w) + beta) / (N_TW.getRowSum(t) + sumBeta));
			outPhiF.println();
		}
		outPhiF.flush();
		outPhiF.close();
		
		
		// PhiB
		if (gamma > 0) {
			CSVPrinter outPhiB = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-PhiB.csv"), CSVFormat.EXCEL);
			outPhiB.print("");
			for (int t = 0; t < numBTopics; t++) outPhiB.print("BT"+t);
			outPhiB.println();
			
			for (int w = 0; w < numWords; w++) {
				outPhiB.print(wordList.get(w));
				for (int t = 0; t < numBTopics; t++)
                    outPhiB.print((N_BW.getValue(t, w) + beta) / (N_BW.getRowSum(t) + sumBeta));
				outPhiB.println();
			}
			outPhiB.flush();
			outPhiB.close();
		}

		
		// Top words for each FG topic
		CSVPrinter outProbWordsF = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-ProbWordsF.csv"), CSVFormat.EXCEL);
		for (int t = 0; t < numFTopics; t++) outProbWordsF.print("T"+t);
		outProbWordsF.println();

		IntegerMatrix P_TW = N_TW.getSortedIndexMatrix(1, Math.min(numProbWords, numWords));
		for (int p = 0; p < Math.min(numProbWords, numWords); p++) {
			for (int t = 0; t < numFTopics; t++) {
				int w = P_TW.getValue(t,p);
                outProbWordsF.print(String.format("%s (%.3f)", wordList.get(w), (N_TW.getValue(t, w) + beta) / (N_TW.getRowSum(t) + sumBeta)));
			}
			outProbWordsF.println();
		}
		outProbWordsF.flush();
		outProbWordsF.close();
	
		
		// Top words for each BG topic
		if (gamma > 0) {
			CSVPrinter outProbWordsB = new CSVPrinter(new PrintFileWriter(outDir+"/"+outPrefix+"-I"+iter+"-ProbWordsB.csv"), CSVFormat.EXCEL);
			for (int t = 0; t < numBTopics; t++) outProbWordsB.print("BT"+t);
			outProbWordsB.println();

			IntegerMatrix P_BW = N_BW.getSortedIndexMatrix(1, Math.min(numProbWords, numWords));
			for (int p = 0; p < Math.min(numProbWords, numWords); p++) {
				for (int t = 0; t < numBTopics; t++) {
					int w = P_BW.getValue(t,p);
                    outProbWordsB.print(String.format("%s (%.3f)", wordList.get(w), (N_BW.getValue(t,w) + beta) / (N_BW.getRowSum(t) + sumBeta)));
				}
				outProbWordsB.println();
			}
			outProbWordsB.flush();
			outProbWordsB.close();	
		}
		
		// Sentence-topic assignment
		CSVPrinter outInstAssign = new CSVPrinter(new PrintFileWriter(
                outDir+"/"+outPrefix+"-I"+iter+"-InstAssign.csv"), CSVFormat.EXCEL);
        outInstAssign.printRecord("DocId", "Sentence", "Text", "TaggedText", "Topic");

        for (String docId : rawDocs.keySet()) {
            Document doc = docs.get(docId);
            if (doc == null) continue;
            for (int s = 0; s < doc.sentences.size(); s++) {
                Sentence sentence = doc.sentences.get(s);
                String taggedText = 
                        sentence.words.stream().map(w -> (
                                w.level == 1 ? "F"+sentence.fTopic+":"+wordList.get(w.idx)
                                             : "B:"+wordList.get(w.idx)))
                                               .collect(Collectors.joining(" "));
                String rawSentence = 
                        rawDocs.get(docId).sentences.get(s).stream()
                                                           .collect(Collectors.joining(" "));
                outInstAssign.printRecord(docId, s, rawSentence, taggedText, sentence.fTopic);
            }
        }
        outInstAssign.flush();
        outInstAssign.close();
        
        
        // Log-likelihood
        if (logL.size() > 0) {
            CSVPrinter outLogL = new CSVPrinter(new PrintFileWriter(
                    outDir+"/"+outPrefix+"-I"+iter+"-LogL.csv"), CSVFormat.EXCEL);
            outLogL.printRecord("Iter","LogL");
            for (Map.Entry<Integer,Double> entry : logL.entrySet()) {
                outLogL.printRecord(entry.getKey(), entry.getValue());
            }
            outLogL.flush();
            outLogL.close();
        }
	}
	
	
    /**
     * A raw document before being indexed.
     */
    private static class RawDocument {
        String text = null;
        Vector<Vector<String>> sentences = new Vector<>();
    }

    /**
     * A document.
     */
    private static class Document {
        int idx = -1;
        Vector<Sentence> sentences = new Vector<>();
    }

    /**
     * A sentence.
     */
    private static class Sentence {
        int fTopic = -1;
        Vector<Word> words = new Vector<>();
    }

    /**
     * A word.
     */
    private static class Word {
        int idx = -1;
        int level = -1;
        public Word(int id) {
            this.idx = id;
        }
    }
}
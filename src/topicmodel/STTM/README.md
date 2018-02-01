# STTM

State Transition Topic Model (STTM) that supports multiple observation sets, each with/without latent topic distribution.
Cite: Y. Jo, G. Tomar, O. Ferschke, C. P. Rose, D. Gasevic. Expediting Support for Social Learning with Behavior Modeling. In Proceedings of the 9th International Conference on Educational Data Mining (EDM), 2016


## Input Files

* data.csv (required)
* config.json (reqiured)

### data.csv
This file must contain five required columns and additional optional columns with corresponding headers. Each row represents an instance at a certain time point.
* SeqId: Sequence ID.
* InstNo: Instance number in the sequence. The model automatically sorts the instances of a sequence in the ascending order of InstNo.
* FromStatus: Status right before the current time point.
* ToStatus: Status of the current time point.
* [EntryKey]: Content of the instance. You should define your own entry key (e.g., "Discussion", "Dtype"). If the entry type is LDA, this field is plain text. If the entry type is MM, this field is tokens separated by commas. The type of each entry is set in the configuration file described below.

### config.json
This JSON file requires the following structure:
* statuses (string array, optional): Statuses to be considered. If not set, all statuses existing in the data file are included. If you don't want to condition state transitions on statuses, this should be reflected in the data file; set the values of the "FromStatus" and "ToStatus" columns in the data file to one value.
* gamma (float, required): Hyperparameter that determines the sparsity of state transition probabilities.
* num_states (int, required): Number of states to be learned.
* entries (array of JSON objects, required): Parameters for the entries to be considered.

An entry's type is either "LDA" or "MM". An LDA-type entry assumes a latent topic distribution, where each topic is a probability distribution over observations. An MM-type entry assumes a direct multinomial distribution over observations without a latent topic distribution. 
An LDA-type entry object requires the following structure:
* key (string, required): Name of the entry. The data file must contain a column with the same key.
* type (string, required): Type of the entry. The value must be "LDA".
* num_topics (int, required): Number of topics to be considered.
* alpha (float, required): Hyperparameter that determines the sparsity of the topic distribution.
* beta (float, required): Hyperparameter that determines the sparsity of the word distribution of each topic.
* num_prob_words (int, optional): Number of top words to be displayed in the output "ProbWords" file. If not set, 100 is the default value.
* min_words (int, optional): Minimum frequency of words to be included in the vocabulary. If not set, 1 is the default value, i.e., all words existing in the data file are considered.
* stopwords (string array, optional): Stop words to be excluded from the vocabulary. If not set, no stop word is used.
* words (string array, optional): Words to be included in the vocabulary. If not set, all words existing in the data file are considered.

An MM-type entry object requires the following structure:
* key (string, required): Name of the entry. The data file must contain a column with the same key.
* type (string, required): Type of the entry. The value must be "MM".
* alpha (float, required): Hyperparameter that determines the sparsity of the observation distribution.
* tokens (string array, optional): Observation tokens to be included in the vocabulary. If not set, all tokens existing in the data file are considered.



## Dependencies

The following files in the "rsc" folder should be in your build path.
* commons-csv-1.4.jar
* slf4j-api.jar
* slf4j-simple.jar
* stanford-postagger-3.6.0.jar

You also need the following model file for POS tagging. The default location is "{PROJECT_DIR}/rsc/". You can change the path or model in the source code.
* english-left3words-distsim.tagger



## Command Options

Command example: `java STTM -i 200 -to 100 -d /input/dir -data data.csv -conf config.json`
```
  * -conf
      Config file name
  * -d
      Input directory
  * -data
      Data file name
    -help
      Command description
      Default: false
  * -i
      Number of iterations
      Default: -1
    -o
      Output directory
    -to
      Number of iterations for temporary output files
      Default: -1
```

## Output Files

All output file names have the form of either
* `STTM-[DATA_File]-[CONFIG_FILE]-S[NUM_STATES]-I[NUM_ITERATIONS]-[VARIABLE].csv`
* `STTM-[DATA_File]-[CONFIG_FILE]-S[NUM_STATES]-I[NUM_ITERATIONS]-[ENTRY_KEY]-[VARIABLE].csv`

### Entry-Independent Variables
	Pi
		State transition probabilities. Each row is a pair of conditional status and source state, and each column is a target state. "Init" indicates the probability of being an initial state.

	InstState
		State assigned to each instance.


### Entry-Specific Variables
	Theta
		Topic (LDA-type) or token (MM-type) distribution for each state. Rows are states and columns are topics or tokens.

	DTTheta
		Only for LDA-type entries. Topic distribution for each instance. Rows are instances and columns are topics.

	Phi
		Only for LDA-type entries. Word distribution of each topic. Rows are words and columns are topics.

	ProbWords
		Only for LDA-type entries. Top words of each topic. Rows are top words and columns are topics.



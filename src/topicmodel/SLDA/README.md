# SLDA
## Input data
A csv file that has two columns with the headings:
 * DocId: Unique document ID
 * Text: Text of the document

## Input Arguments
`Usage: <main class> [options]`

`  Options:`

`  * -a`

`      Alpha.`

`      Default: -1.0`

`  * -b`

`      Beta.`

`      Default: -1.0`

`  * -d`

`      Input directory.`

`  * -data`

`      Data file name.`

`    -g`

`      Gamma. 1 means no background topic.`

`      Default: 1.0`

`    -help`

`      Command description.`

`      Default: false`

`  * -i`

`      Number of iterations`

`      Default: -1`

`    -log`

`      Number of iterations for calculating  log-likelihood.`

`      Default: -1`

`    -model`

`      Trained model to fit (no parameter update).`

`    -o`

`      Output directory.`

`    -sw`

`      Stopwords file name.`

`  * -t`

`      Number of topics`

`      Default: -1`

`    -th`

`      Number of threads.`

`      Default: 1`

`    -to`

`      Number of iterations for temporary output files.`

`      Default: -1`

`    -tok`

`      Do tokenization/pattern replacement.`

`      Default: false`



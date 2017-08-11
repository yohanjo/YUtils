# CSM (Content Word Filtering and Speaker Preference Model)

## Command Examples

Training:
-s 5 -ft 10 -bt 10 -fa 0.1 -ba 1 -b 0.001 -k 0.1 -g 1 -e 0.8 -n 0.9 -mw 1 -ms 1 -seq -d data_dir -data data_train.csv -i 1000 -to 100 -log 100 -th 2

Fitting:
-s 5 -ft 10 -bt 10 -fa 0.1 -ba 1 -b 0.001 -k 0.1 -g 1 -e 0.8 -n 0.9 -mw 1 -ms 1 -seq -d data_dir -data data_test.csv -i 1000 -to 100 -log 100 -th 2 -model models/HSTTM8C-data_train-S5-FT10-BT10-FA0.1-BA1.0-B0.001-G1.0-K0.1-E0.8-N0.9-SEQ-I1000


## Tips
 * We recommend NOT using stop words, because especially exhaustive lists of stop words may remove important words from the vocabulary, such as "should", "what", etc.


# HSTTM8W

## Command Examples

Training:
-s 5 -ft 10 -fa 0.1 -b 0.001 -k 0.1 -g 1 -e 0.75 -n 0.5 -mw 1 -ms 1 -d data_dir -data data_train.csv -background background/MalletLDA-content.csv-T100-A0.1-B0.001-I5000 -i 1000 -to 100 -log 100 -th 2

Fitting:
-s 5 -ft 10 -fa 0.1 -b 0.001 -k 0.1 -g 1 -e 0.75 -n 0.5 -mw 1 -ms 1 -d data_dir -data data_test.csv -background background/MalletLDA-content.csv-T100-A0.1-B0.001-I5000 -i 1000 -to 100 -log 100 -th 2 -model models/HSTTM8W-data_train-S5-FT10-BT100-FA0.1-B0.001-G1.0-K0.1-E0.75-N0.5-I1000


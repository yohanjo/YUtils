# Command Examples

## HSTTM8W

Training:
-s 5 -ft 10 -fa 0.1 -b 0.001 -k 0.1 -g 1 -e 0.75 -n 0.5 -mw 1 -ms 1 -d data_dir -data data_train.csv -background background/MalletLDA-content.csv-T100-A0.1-B0.001-I5000 -i 1000 -to 100 -log 100 -th 2

Fitting:
-s 5 -ft 10 -fa 0.1 -b 0.001 -k 0.1 -g 1 -e 0.75 -n 0.5 -mw 1 -ms 1 -d data_dir -data data_test.csv -background background/MalletLDA-content.csv-T100-A0.1-B0.001-I5000 -i 1000 -to 100 -log 100 -th 2 -model models/HSTTM8W-data_train-S5-FT10-BT100-FA0.1-B0.001-G1.0-K0.1-E0.75-N0.5-I1000

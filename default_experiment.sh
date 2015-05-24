#!/bin/bash
DATASET=$1
METHOD=$2
SEED=$3
#echo $1 $2 $3
java -Xmx18000m -cp /home/msalvador/autoweka/autoweka.jar weka.classifiers.functions.$METHOD -t /home/msalvador/autoweka/datasets/$DATASET-train70perc.arff -x 8 -s $SEED -o | grep "Root mean squared error" | sed 1d | awk '{ print $5 }' > /home/msalvador/autoweka/experiments/defaultParameters/$DATASET.$METHOD.CV.$SEED.csv
java -Xmx18000m -cp /home/msalvador/autoweka/autoweka.jar weka.classifiers.functions.$METHOD -t /home/msalvador/autoweka/datasets/$DATASET-train70perc.arff -T /home/msalvador/autoweka/datasets/$DATASET-test30perc.arff -s $SEED -o | grep "Root mean squared error" | sed 1d | awk '{ print $5 }' > /home/msalvador/autoweka/experiments/defaultParameters/$DATASET.$METHOD.Test.$SEED.csv 

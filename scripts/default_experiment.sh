#!/bin/bash
DATASET=$1
METHOD=$2
SEED=$3
#echo $1 $2 $3
$MY_JAVA_PATH/java -Xmx18000m -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.functions.$METHOD -t $AUTOWEKA_PATH/datasets/$DATASET-train70perc.arff -x 8 -s $SEED -o | grep "Root mean squared error" | sed 1d | awk '{ print $5 }' > $AUTOWEKA_PATH/experiments/defaultParameters/$DATASET.$METHOD.CV.$SEED.csv
$MY_JAVA_PATH/java -Xmx18000m -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.functions.$METHOD -t $AUTOWEKA_PATH/datasets/$DATASET-train70perc.arff -T $AUTOWEKA_PATH/datasets/$DATASET-test30perc.arff -s $SEED -o | grep "Root mean squared error" | sed 1d | awk '{ print $5 }' > $AUTOWEKA_PATH/experiments/defaultParameters/$DATASET.$METHOD.Test.$SEED.csv 

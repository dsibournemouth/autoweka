#!/bin/bash
# For some reason is not getting the env variables, so I have to pass them manually here
export AUTOWEKA_PATH=$HOME/autoweka
export MY_JAVA_PATH=/usr/lib/jvm/j2sdk1.8-oracle/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/openblas/lib:/opt/software/matlab/2014/runtime/glnxa64
export PATH=$PATH:$HOME/openblas/bin
EXPERIMENT=$1
DATASET=$2
SEED=$3
#echo $1 $2 $3
$MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.misc.SerializedClassifier -l $AUTOWEKA_PATH/experiments/$EXPERIMENT/trained.$SEED.model -T $AUTOWEKA_PATH/datasets/$DATASET/test.arff -classifications "weka.classifiers.evaluation.output.prediction.CSV -file $AUTOWEKA_PATH/experiments/$EXPERIMENT/predictions.$SEED.csv"

#!/bin/bash
# For some reason is not getting the env variables, so I have to pass them manually here
export AUTOWEKA_PATH=$HOME/autoweka
export MY_JAVA_PATH=/usr/lib/jvm/j2sdk1.8-oracle/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/openblas/lib:/opt/software/matlab/2014/runtime/glnxa64
export PATH=$PATH:$HOME/openblas/bin
EXPERIMENTS_FOLDER=$1
EXPERIMENT=$2
DATASET=$3
SEED=$4
#echo $1 $2 $3 $4
$MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.misc.SerializedClassifier -l $AUTOWEKA_PATH/$EXPERIMENTS_FOLDER/$EXPERIMENT/trained.$SEED.model -T $AUTOWEKA_PATH/datasets/$DATASET/train.arff -classifications "weka.classifiers.evaluation.output.prediction.CSV -file $AUTOWEKA_PATH/$EXPERIMENTS_FOLDER/$EXPERIMENT/training.predictions.$SEED.csv"

#!/bin/bash
# For some reason is not getting the env variables, so I have to pass them manually here
export AUTOWEKA_PATH=$HOME/autoweka
export MY_JAVA_PATH=/usr/lib/jvm/j2sdk1.8-oracle/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/openblas/lib:/opt/software/matlab/2014/runtime/glnxa64
export PATH=$PATH:$HOME/openblas/bin
EXPERIMENTS_FOLDER=experiments
DATASET=$1
METHOD=$2
SEED=0
FOLDS=10
#echo $1 $2 $3
$MY_JAVA_PATH/java -Xmx18000m -cp $AUTOWEKA_PATH/autoweka.jar $METHOD -t $AUTOWEKA_PATH/datasets/$DATASET/train.arff -x $FOLDS -s $SEED -o | grep "Correctly Classified Instances" | sed 1d | awk '{ print $5 }' > $AUTOWEKA_PATH/$EXPERIMENTS_FOLDER/defaultParameters/$DATASET.$METHOD.CV.$SEED.csv
$MY_JAVA_PATH/java -Xmx18000m -cp $AUTOWEKA_PATH/autoweka.jar $METHOD -t $AUTOWEKA_PATH/datasets/$DATASET/train.arff -T $AUTOWEKA_PATH/datasets/$DATASET/test.arff -s $SEED -o | grep "Correctly Classified Instances" | sed 1d | awk '{ print $5 }' > $AUTOWEKA_PATH/$EXPERIMENTS_FOLDER/defaultParameters/$DATASET.$METHOD.Test.$SEED.csv
#$MY_JAVA_PATH/java -Xmx18000m -cp $AUTOWEKA_PATH/autoweka.jar $METHOD -t $AUTOWEKA_PATH/datasets/$DATASET/train.arff -x $FOLDS -s $SEED -o | grep "Root mean squared error\|Correctly Classified Instances" | sed 1d | awk '{ print $5 }' > $AUTOWEKA_PATH/experiments/defaultParameters/$DATASET.$METHOD.CV.$SEED.csv
#$MY_JAVA_PATH/java -Xmx18000m -cp $AUTOWEKA_PATH/autoweka.jar $METHOD -t $AUTOWEKA_PATH/datasets/$DATASET/train.arff -T $AUTOWEKA_PATH/datasets/$DATASET/test.arff -s $SEED -o | grep "Root mean squared error\|Correctly Classified Instances" | sed 1d | awk '{ print $5 }' > $AUTOWEKA_PATH/experiments/defaultParameters/$DATASET.$METHOD.Test.$SEED.csv


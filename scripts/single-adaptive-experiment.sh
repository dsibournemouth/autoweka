#!/bin/sh
# For some reason is not getting the env variables, so I have to pass them manually here
export AUTOWEKA_PATH=$HOME/autoweka
export MY_JAVA_PATH=/usr/lib/jvm/j2sdk1.8-oracle/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/openblas/lib:/opt/software/matlab/2014/runtime/glnxa64
export PATH=$PATH:$HOME/openblas/bin
$MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar autoweka.tools.AdaptiveExperimentRunner $1 $2 $3 $4

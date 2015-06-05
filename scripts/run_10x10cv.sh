#!/bin/bash
export AUTOWEKA_PATH=$HOME/autoweka
export MY_JAVA_PATH=/usr/lib/jvm/j2sdk1.8-oracle/bin
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$HOME/openblas/lib:/opt/software/matlab/2014/runtime/glnxa64
export PATH=$PATH:$HOME/openblas/bin
source $HOME/python/autoweka-env/bin/activate && python $AUTOWEKA_PATH/scripts/run_10x10cv.py $1 $2 $3 $4

#!/bin/sh
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/home/msalvador/openblas/lib
export PATH=$PATH:/home/msalvador/openblas/bin
/usr/lib/jvm/j2sdk1.8-oracle/bin/java -cp /home/msalvador/autoweka/autoweka.jar autoweka.tools.ExperimentRunner $1 $2

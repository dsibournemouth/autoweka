import os, sys
from datasets import datasets

if len(sys.argv) < 3:
    print 'Syntax python launch_experiments.py <STRATEGY> <GENERATION>'
    print '<STRATEGY> can be SMAC or TPE or ROAR'
    print '<GENERATION> can be CV or DPS'
    exit(1)

num_seeds = 25

strategy = sys.argv[1]
generation = sys.argv[2]

for d in datasets:
    folder = '%s/experiments/%s.%s.%s-%s' % (os.environ['AUTOWEKA_PATH'], d, strategy, generation, d)
    for seed in range(0, num_seeds):
        command = 'qsub -l q=compute ./single-experiment.sh %s %d' % (folder, seed)
        print command
        os.system(command)

from os import system
from datasets import datasets

# strategies = ['DEFAULT', 'RAND', 'SMAC', 'ROAR', 'TPE']
strategies = ['RAND']
generations = ['CV']
seeds = range(0, 25)

for dataset in datasets:
    for strategy in strategies:
        for generation in generations:
            for seed in seeds:
                name = "%s_%s_%s_%s" % (dataset, strategy, generation, seed)
                system("qsub -N %s -l q=compute ./run_10x10cv.sh %s %s %s %d" % (
                       name, dataset, strategy, generation, seed))

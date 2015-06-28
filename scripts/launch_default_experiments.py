import argparse
import os
from config import *

def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--seed', choices=seeds, required=False)

    args = parser.parse_args()

    # override default values
    if args.dataset:
        selected_datasets = [args.dataset]
    else:
        selected_datasets = datasets
    if args.seed:
        selected_seeds = [args.seed]
    else:
        selected_seeds = seeds

    for d in selected_datasets:
        for m in methods:
            for s in selected_seeds:
                experiment_name = '%s.%s.%s' % (d, m, s)
                command = "qsub -N %s -l q=compute %s/scripts/default_experiment.sh %s %s %s" % (
                    experiment_name, os.environ['AUTOWEKA_PATH'], d, m, s)
                print(command)
                os.system(command)

if __name__ == "__main__":
    main()

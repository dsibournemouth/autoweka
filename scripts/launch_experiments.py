import argparse
import os
from config import *


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--strategy', choices=strategies, required=False)
    parser.add_argument('--generation', choices=generations, required=False)
    parser.add_argument('--seed', choices=seeds, required=False)

    args = parser.parse_args()

    # override default values
    if args.dataset:
        selected_datasets = [args.dataset]
    else:
        selected_datasets = datasets
    if args.strategy:
        selected_strategies = [args.strategy]
    else:
        selected_strategies = strategies
    if args.generation:
        selected_generations = [args.generation]
    else:
        selected_generations = generations
    if args.seed:
        selected_seeds = [args.seed]
    else:
        selected_seeds = seeds

    for dataset in selected_datasets:
        for strategy in selected_strategies:
            for generation in selected_generations:
                folder = '%s/experiments/%s.%s.%s-%s' % (
                    os.environ['AUTOWEKA_PATH'], dataset, strategy, generation, dataset)
                for seed in selected_seeds:
                    experiment_name = '%s.%s.%s.%s' % (dataset, strategy, generation, seed)
                    command = 'qsub  -N %s -l q=compute ./single-experiment.sh %s %s' % (experiment_name, folder, seed)
                    print command
                    os.system(command)


if __name__ == "__main__":
    main()

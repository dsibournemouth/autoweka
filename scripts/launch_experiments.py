import argparse
import os

from config import *


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--strategy', choices=strategies, required=False)
    parser.add_argument('--generation', choices=generations, required=False)
    parser.add_argument('--seed', choices=seeds, required=False)
    parser.add_argument('--batches', type=int, required=False)
    parser.add_argument('--initial-batch', type=int, required=False)

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
    if args.initial_batch:
        initial_batch = args.initial_batch
    else:
        initial_batch = 1

    for dataset in selected_datasets:
        for strategy in selected_strategies:
            for generation in selected_generations:
                folder = '%s/%s/%s.%s.%s-%s' % (
                    os.environ['AUTOWEKA_PATH'], experiments_folder, dataset, strategy, generation, dataset)
                for seed in selected_seeds:
                    experiment_name = '%s.%s.%s.%s' % (dataset, strategy, generation, seed)
                    if args.batches:
                        command = 'qsub  -N %s -l q=compute ./single-adaptive-experiment.sh %s %s %s %s' % (experiment_name, folder, args.batches, seed, initial_batch)
                    else:
                        command = 'qsub  -N %s -l q=compute ./single-experiment.sh %s %s' % (experiment_name, folder, seed)
                    print command
                    os.system(command)


if __name__ == "__main__":
    main()

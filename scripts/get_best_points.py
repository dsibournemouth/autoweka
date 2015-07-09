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
            if strategy not in ['DEFAULT', 'RAND']:
                for generation in selected_generations:
                    for seed in selected_seeds:
                        d = {"dataset": dataset, "strategy": strategy, "generation": generation}
                        experiment = "{dataset}.{strategy}.{generation}-{dataset}".format(**d)
                        trajectory_file = "%s.trajectories.%s" % (experiment, seed)

                        os.system(
                            "cd %s && %s/java -cp autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV experiments/%s/%s" % (
                                os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], experiment, trajectory_file))


if __name__ == "__main__":
    main()

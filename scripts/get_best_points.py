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
            if strategy not in ['DEFAULT']:
                for generation in selected_generations:
                    for seed in selected_seeds:
                        d = {"dataset": dataset, "strategy": strategy, "generation": generation}
                        experiment = "{dataset}.{strategy}.{generation}-{dataset}".format(**d)
                        trajectory_file = "%s.trajectories.%s" % (experiment, seed)
                        command = "cd %s && %s/java -cp autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV" % (
                            os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'])

                        if args.batches:
                            for batch in range(0, args.batches):
                                full_path = '%s/%s/batch%d/%s' % (
                                experiments_folder, experiment, batch, trajectory_file)
                                os.system('%s %s %d' % (command, full_path, batch))
                        else:
                            full_path = '%s/%s/%s' % (experiments_folder, experiment, trajectory_file)
                            os.system('%s %s %s', command, full_path)


if __name__ == "__main__":
    main()

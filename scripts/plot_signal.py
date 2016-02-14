import argparse
import numpy as np
import os
import traceback

import matplotlib.pyplot as plt

from config import *


def plot_target_vs_prediction(targets, predictions, limit, title):
    plt.close()

    plt.plot(targets)
    plt.plot(predictions)

    plt.axvline(limit, color='r', linestyle='--')
    plt.xlim(0, len(targets))

    plt.title(title)
    plt.savefig("%s/plots%s/signal.%s.png" % (os.environ['AUTOWEKA_PATH'], suffix, title), bbox_inches='tight')


def get_target_and_predictions(dataset, strategy, generation, seed):
    experiment_name = '%s.%s.%s-%s' % (dataset, strategy, generation, dataset)
    training_predictions_filename = '%s/%s/%s/training.predictions.%s.csv' % (
        os.environ['AUTOWEKA_PATH'], experiments_folder, experiment_name, seed)
    predictions_filename = '%s/%s/%s/predictions.%s.csv' % (
        os.environ['AUTOWEKA_PATH'], experiments_folder, experiment_name, seed)

    training_results = np.genfromtxt(training_predictions_filename, skip_header=1, delimiter=",")
    testing_results = np.genfromtxt(predictions_filename, skip_header=1, delimiter=",")

    training_size = training_results.shape[0]
    data = np.concatenate((training_results, testing_results), axis=0)

    # data row = id,actual,predicted,error

    return data[:, 1], data[:, 2], training_size


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--strategy', choices=strategies, required=False)
    parser.add_argument('--generation', choices=generations, required=False)
    parser.add_argument('--seed', choices=seeds, required=False)

    args = parser.parse_args()

    # override default values
    selected_datasets = [args.dataset] if args.dataset else datasets
    selected_strategies = [args.strategy] if args.strategy else strategies
    selected_generations = [args.generation] if args.generation else generations
    selected_seeds = [args.seed] if args.seed else seeds

    for dataset in selected_datasets:
        for strategy in selected_strategies:
            for generation in selected_generations:
                for seed in selected_seeds:
                    try:
                        targets, predictions, limit = get_target_and_predictions(dataset, strategy, generation, seed)
                        title = '%s.%s.%s.%s' % (dataset, strategy, generation, seed)
                        plot_target_vs_prediction(targets, predictions, limit, title)
                    except Exception as e:
                        print e
                        traceback.print_exc()


if __name__ == "__main__":
    main()

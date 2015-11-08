import os
import argparse
import numpy as np
import matplotlib.pyplot as plt
from config import *


def plot_target_vs_prediction(targets, predictions, limit, title):
    plt.close()

    plt.plot(targets)
    plt.plot(predictions)

    plt.axvline(limit, color='r', linestyle='--')

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
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--strategy', choices=strategies, required=True)
    parser.add_argument('--generation', choices=generations, required=True)
    parser.add_argument('--seed', choices=seeds, required=True)

    args = parser.parse_args()

    dataset = args.dataset
    strategy = args.strategy
    generation = args.generation
    seed = args.seed

    targets, predictions, limit = get_target_and_predictions(dataset, strategy, generation, seed)

    title = '%s.%s.%s.%s' % (dataset, strategy, generation, seed)
    plot_target_vs_prediction(targets, predictions, limit, title)


if __name__ == "__main__":
    main()

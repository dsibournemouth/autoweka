import os
import argparse
import numpy as np
from config import *
from table_configurations import parse_configuration, get_results
from plot_dendograms import plot_dendogram


def create_distance_matrix(configurations):
    rows = len(configurations)
    if rows == 0:
        return 0

    columns = len(configurations[0])
    weights = [1, 1, 1, 1, 1, 2, 1.5]
    total_weight = sum(weights)

    distance = np.zeros((rows, rows))
    # Each row is configuration
    for i in range(0, rows):
        for j in range(0, rows):
            row_similarity = 0
            # each column is a component
            for k in range(0, columns):
                row_similarity += weights[k] if configurations[i][k] == configurations[j][k] else 0

            # distance == 0 if they are the same
            distance[i, j] = 1 - float(row_similarity) / total_weight

    return distance


def sub_main(dataset, strategy, generation):
    results, best_error_seed, best_test_error_seed = get_results(dataset, strategy, generation)
    configurations = []
    labels = []
    for result in results:
        seed = result[3]
        params = parse_configuration(result[4], True)
        error = result[5]
        configurations.append([
            params['missing_values']['method'],
            params['outliers']['method'],
            params['transformation']['method'],
            params['dimensionality_reduction']['method'],
            params['sampling']['method'],
            params['predictor']['method'],
            params['meta']['method']
        ])
        labels.append("%s (%.2f)" % (seed, error))

    matrix = create_distance_matrix(configurations)
    np.savetxt("../distances/%s.%s.%s.csv" % (dataset, strategy, generation), matrix, fmt="%.6f", delimiter=",")

    plot_dendogram(matrix, labels, "%s.%s.%s" % (dataset, strategy, generation))


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--strategy', choices=strategies, required=False)
    parser.add_argument('--generation', choices=generations, required=False)

    args = parser.parse_args()

    # override default values
    selected_datasets = [args.dataset] if args.dataset else datasets
    selected_strategies = [args.strategy] if args.strategy else strategies
    selected_generations = [args.generation] if args.generation else generations

    for dataset in selected_datasets:
        for strategy in selected_strategies:
            for generation in selected_generations:
                try:
                    sub_main(dataset, strategy, generation)
                except Exception as e:
                    print e
                    continue


if __name__ == "__main__":
    main()

import argparse
import numpy as np
import os
from operator import itemgetter

import matplotlib.cm as cm
import matplotlib.pyplot as plt

from config import *


def scatter_all_seeds(results, title):
    plt.close()
    results = np.array(results)

    for seed in seeds:
        mask = results[:, 0] == seed
        time = results[mask, 1] / 60 / 60
        error = results[mask, 2]
        if is_regression:
            error = np.log10(error)

        plt.plot(time, error, linestyle='solid', linewidth=.5, marker='o', markersize=5, color=cm.hsv(float(seed) / number_seeds, 1), alpha=.75)

    plt.xlabel("Time (h)")
    if is_regression:
        plt.ylabel("log(RMSE)")
    else:
        plt.ylabel("% class. error")
        plt.ylim(0, 100)

    plt.margins(0.1, 0.1)

    plt.title(title)
    plt.savefig("%s/plots%s/trajectories-%s.scatter.png" % (os.environ['AUTOWEKA_PATH'], suffix, title))
    # plt.show()


def aggregated_line_seeds(results, title):
    plt.close()
    sorted_points = np.array(sorted(results, key=itemgetter(1)))
    sorted_time = sorted_points[:, 1] / 60 / 60
    sorted_errors = sorted_points[:, 2]
    if is_regression:
        sorted_errors = np.log10(sorted_errors)

    #y_mean = stats.rolling_mean(sorted_errors, 5)
    # y_std = stats.rolling_std(sorted_errors, 5)
    #y_upper = stats.rolling_max(sorted_errors, 5)
    #y_lower = stats.rolling_min(sorted_errors, 5)
    #y_mean = np.cumsum(sorted_errors)/np.array(range(1, len(sorted_errors)+1))
    #y_upper = np.maximum.accumulate(sorted_errors)
    y_lower = np.minimum.accumulate(sorted_errors)
    #np.savetxt("%s/plots%s/trajectories-%s-accumulate.csv" % (os.environ['AUTOWEKA_PATH'], suffix, title), y_lower, delimiter=",")

    #plt.plot(sorted_time, y_mean, color="red", label="Accumulated mean")
    plt.plot(sorted_time, y_lower, color="red", label="Accumulated min")

    # plt.legend()
    #plt.fill_between(sorted_time, y_mean, y_upper, facecolor='gray', interpolate=True, alpha=0.5)
    #plt.fill_between(sorted_time, y_lower, y_mean, facecolor='gray', interpolate=True, alpha=0.5)

    plt.xlabel("Time (h)")
    if is_regression:
        plt.ylabel("log(RMSE)")
    else:
        plt.ylabel("% class. error")
        plt.ylim(0, 100)

    plt.margins(0.05, 0.05)

    plt.title(title)
    plt.savefig("%s/plots%s/trajectories-%s.aggregated.png" % (os.environ['AUTOWEKA_PATH'], suffix, title), bbox_inches='tight')
    # plt.show()


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--strategy', choices=strategies, required=False)
    parser.add_argument('--generation', choices=generations, required=False)

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

    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    for dataset in selected_datasets:
        for strategy in selected_strategies:
            for generation in selected_generations:

                query = "SELECT seed, time, error FROM trajectories WHERE dataset='%s' AND strategy='%s' " \
                        "AND (generation='%s' or generation='%s-%s') AND error<1000000000 " \
                        "AND time<=%d" % (
                            dataset, strategy, generation, generation, dataset, TIME_LIMIT)

                results = c.execute(query).fetchall()

                if not results:
                    print "No results for dataset='%s' strategy='%s' generation='%s'" % (dataset, strategy, generation)
                    continue

                title = '%s.%s.%s' % (dataset, strategy, generation)

                scatter_all_seeds(results, title)
                aggregated_line_seeds(results, title)

    conn.close()


if __name__ == "__main__":
    main()

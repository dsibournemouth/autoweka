import os
import argparse
import sqlite3
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import pandas.stats.moments as stats
from operator import itemgetter
from config import *

TIME_LIMIT = 30 * 60 * 60  # 30 hours


def scatter_all_seeds(results, title):
    plt.close()
    results = np.array(results)

    for seed in range(0, 25):
        mask = results[:, 0] == seed
        time = results[mask, 1] / 60 / 60
        error = results[mask, 2]
        if is_regression:
            error = np.log10(error)

        plt.plot(time, error, linestyle='solid', linewidth=.5, marker='o', markersize=5, color=cm.hsv(seed / 25., 1), alpha=.75)

    plt.xlabel("Time (h)")
    if is_regression:
        plt.ylabel("log(RMSE)")
    else:
        plt.ylabel("% class. error")
        plt.ylim(0, 100)

    plt.margins(0.1, 0.1)

    plt.title(title)
    plt.savefig("%s/plots/trajectories-%s.scatter.png" % (os.environ['AUTOWEKA_PATH'], title))
    # plt.show()


def aggregated_line_seeds(results, title):
    plt.close()
    sorted_points = np.array(sorted(results, key=itemgetter(1)))
    sorted_time = sorted_points[:, 1] / 60 / 60
    sorted_errors = sorted_points[:, 2]
    if is_regression:
        sorted_errors = np.log10(sorted_errors)

    y_mean = stats.rolling_mean(sorted_errors, 5)
    # y_std = stats.rolling_std(sorted_errors, 5)
    y_upper = stats.rolling_max(sorted_errors, 5)
    y_lower = stats.rolling_min(sorted_errors, 5)

    plt.plot(sorted_time, y_mean, color="red", label="Rolling mean")

    # plt.legend()
    plt.fill_between(sorted_time, y_mean, y_upper, facecolor='gray', interpolate=True, alpha=0.5)
    plt.fill_between(sorted_time, y_lower, y_mean, facecolor='gray', interpolate=True, alpha=0.5)

    plt.xlabel("Time (h)")
    if is_regression:
        plt.ylabel("log(RMSE)")
    else:
        plt.ylabel("% class. error")
        plt.ylim(0, 100)

    plt.margins(0.05, 0.05)

    plt.title(title)
    plt.savefig("%s/plots/trajectories-%s.aggregated.png" % (os.environ['AUTOWEKA_PATH'], title), bbox_inches='tight')
    # plt.show()


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
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

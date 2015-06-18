import os
import sys
import argparse
import sqlite3
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.cm as cm
import pandas.stats.moments as stats
from operator import itemgetter
from datasets import datasets


def scatter_all_seeds(results, title):
    plt.close()
    results = np.array(results)

    for seed in range(0, 25):
        mask = results[:, 0] == seed
        time = results[mask, 1] / 60 / 60
        error = np.log10(results[mask, 2])
        plt.scatter(time, error, c=cm.hsv(seed / 25., 1))

    plt.margins(0.1, 0)
    plt.xlabel("Time (h)")
    plt.ylabel("log(RMSE)")

    plt.title(title)
    plt.savefig("%s/plots/trajectories-%s.scatter.png" % (os.environ['AUTOWEKA_PATH'], title))
    #plt.show()


def aggregated_line_seeds(results, title):
    plt.close()
    sorted_points = np.array(sorted(results, key=itemgetter(1)))
    sorted_time = sorted_points[:, 1] / 60 / 60
    sorted_errors = np.log10(sorted_points[:, 2])

    y_mean = stats.rolling_mean(sorted_errors, 5)
    # y_std = stats.rolling_std(sorted_errors, 5)
    y_upper = stats.rolling_max(sorted_errors, 5)
    y_lower = stats.rolling_min(sorted_errors, 5)

    plt.plot(sorted_time, y_mean, color="red", label="Rolling mean")

    # plt.legend()
    plt.fill_between(sorted_time, y_mean, y_upper, facecolor='gray', interpolate=True, alpha=0.5)
    plt.fill_between(sorted_time, y_lower, y_mean, facecolor='gray', interpolate=True, alpha=0.5)

    plt.margins(0.05, 0.05)
    plt.xlabel("Time (h)")
    plt.ylabel("log(RMSE)")

    plt.title(title)
    plt.savefig("%s/plots/trajectories-%s.aggregated.png" % (os.environ['AUTOWEKA_PATH'], title), bbox_inches='tight')
    #plt.show()


def main():
    parser = argparse.ArgumentParser(prog='plot_trajectories.vs.time.py')
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--strategy', required=True)
    parser.add_argument('--generation', required=True)

    args = parser.parse_args()

    dataset = args.dataset
    strategy = args.strategy
    generation = args.generation

    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    query = "SELECT seed, time, error FROM trajectories WHERE dataset='%s' AND strategy='%s' AND (generation='%s' or generation='%s-%s') AND error<1000000000" % (
        dataset, strategy, generation, generation, dataset)

    results = c.execute(query).fetchall()

    conn.close()

    if not results:
        raise Exception("No results for dataset='%s' strategy='%s' generation='%s'" % (dataset, strategy, generation))

    title = '%s.%s.%s' % (dataset, strategy, generation)

    scatter_all_seeds(results, title)
    aggregated_line_seeds(results, title)


if __name__ == "__main__":
    main()

import os
import csv
import argparse
import glob
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

    for seed in seeds:
        mask = results[:, 0] == seed
        time = results[mask, 1].astype(float) 
        time = np.divide(time, 3600.)
        error = results[mask, 2].astype(float)
        if is_regression:
            error = np.log10(error)

        plt.plot(time, error, linestyle='solid', linewidth=.5, color=cm.hsv(float(seed) / 25., 1), alpha=.75)

    plt.xlabel("Time (h)")
    if is_regression:
        plt.ylabel("log(RMSE)")
    else:
        plt.ylabel("% class. error")
        plt.ylim(0, 100)

    plt.margins(0.1, 0.1)

    plt.title(title)
    plt.savefig("%s/plots/smac-runs-%s.individual.png" % (os.environ['AUTOWEKA_PATH'], title))
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
    parser.add_argument('--skip-crashes', action='store_true')

    args = parser.parse_args()

    # override default values
    if args.dataset:
        selected_datasets = [args.dataset]
    else:
        selected_datasets = datasets

    for dataset in selected_datasets:
        results = []
        for seed in seeds:
            path = "%s/experiments/%s.SMAC.CV-%s/out/autoweka/state-run%s" % (os.environ['AUTOWEKA_PATH'], dataset, dataset, seed)
            try:
                os.chdir(path)
                found_files = glob.glob("runs_and_results-it*.csv")
                latest_file = found_files[-1]
            except:
                print "No results for dataset='%s' strategy='SMAC' generation='CV' seed=%s" % (dataset,seed)
                continue
                
            
            print "Reading %s..." % latest_file
            f = open(latest_file, 'r')
            try:
                reader = csv.reader(f)
                reader.next() # skip header
                
                for row in reader:
                    # time, error
                    time = float(row[12])
                    error = float(row[3])
                    if args.skip_crashes and error>=100:
                        continue
                        
                    results.append([seed, time, error])

            finally:
                f.close()
        
        if results:
            title = '%s.SMAC.CV' % dataset
            scatter_all_seeds(results, title)
            #aggregated_line_seeds(results, title)

if __name__ == "__main__":
    main()

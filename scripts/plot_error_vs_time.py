import argparse
import os
import subprocess

from config import *


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--strategy', choices=strategies, required=True)
    parser.add_argument('--generation', choices=generations, required=True)
    parser.add_argument('--seed', choices=seeds, required=True)

    args = parser.parse_args()

    dataset = args.dataset
    strategy = args.strategy
    generation = args.generation
    seed = args.seed

    experiment = "%s.%s.%s-%s" % (dataset, strategy, generation, dataset)
    logfile = '%s/%s/%s/out/logs/%s' % (os.environ['AUTOWEKA_PATH'], experiments_folder, experiment, seed)

    command = "grep SubProcessWrapper: %s.log | sed -e 's/SubProcessWrapper: Time(\([0-9.]*\)) Score(\([0-9.]*\))/\\1,\\2/g'" % logfile

    results = subprocess.check_output(command, shell=True).rstrip()

    if not results:
        raise Exception("No results for dataset='%s' strategy='%s' generation='%s' seed='%s'" % (
        dataset, strategy, generation, seed))

    counter = 0
    for line in results:
        tmp = line.split(',')
        time = float(tmp[0])
        error = tmp[1]
        counter += 1
        cum_time += time

    print results


if __name__ == "__main__":
    main()

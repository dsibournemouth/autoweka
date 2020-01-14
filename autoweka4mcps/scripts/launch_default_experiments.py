import argparse
import os

from config import *


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=False)

    args = parser.parse_args()

    # override default values
    if args.dataset:
        selected_datasets = [args.dataset]
    else:
        selected_datasets = datasets

    for d in selected_datasets:
        for m in methods:
            experiment_name = '%s.%s' % (d, m)
            command = "qsub -N %s -l q=compute %s/scripts/default_experiment.sh %s %s" % (
                experiment_name, os.environ['AUTOWEKA_PATH'], d, m)
            print(command)
            os.system(command)


if __name__ == "__main__":
    main()

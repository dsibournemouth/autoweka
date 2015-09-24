import os
import argparse
import sqlite3
from config import *
from table_configurations import parse_configuration

def get_results(dataset):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, configuration, error
                 FROM results WHERE dataset='%s' AND error IS NOT NULL ORDER BY error LIMIT 1''' % dataset)
    results = c.fetchone()

    if not results:
        conn.close()
        raise Exception('No results for dataset=%s' % dataset)

    conn.close()

    return results

def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=False)

    args = parser.parse_args()

    # override default values
    if args.dataset:
        selected_datasets = [args.dataset]
    else:
        selected_datasets = datasets

    # --- Pre-processing table ---
    print '\\begin{table*}'
    print '\\centering'
    print '\\caption{Best pre-processing configuration for each dataset}'
    print '\\begin{tabular}{l l l l l l}'
    print '\\hline'

    for dataset in selected_datasets:

        result = get_results(dataset)

        params = parse_configuration(result[1], True)

        custom_keys = ['missing_values', 'outliers', 'transformation',
                       'dimensionality_reduction', 'sampling']

        line = '%s' % dataset
        for i in range(0, len(custom_keys)):
            # getting only the Weka class name
            tag = params[custom_keys[i]]['method'].split('.')[-1]
            line = '%s & %s' % (line, tag)
        print line, '\\\\'

    print '\\hline'
    print '\\end{tabular}'
    print '\\label{tab:test-error}'
    print '\\end{table*}'

    # --- Model table ---
    print '\\begin{table*}'
    print '\\centering'
    print '\\caption{Best model configuration for each dataset}'
    print '\\begin{tabular}{l l l l l l}'
    print '\\hline'

    for dataset in selected_datasets:

        result = get_results(dataset)

        params = parse_configuration(result[1], True)

        custom_keys = ['predictor', 'meta']

        line = '%s' % dataset
        for i in range(0, len(custom_keys)):
            # getting only the Weka class name
            tag = params[custom_keys[i]]['method'].split('.')[-1]
            line = '%s & %s' % (line, tag)

        print line, '\\\\'

    print '\\hline'
    print '\\end{tabular}'
    print '\\label{tab:test-error}'
    print '\\end{table*}'


if __name__ == "__main__":
    main()

import os
import argparse
import sqlite3
from table_configurations import parse_configuration
from config import *

def get_results(dataset):
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    c.execute('''SELECT dataset, configuration, cast(error as REAL) as error
                 FROM results WHERE dataset='%s' AND error IS NOT NULL ORDER BY error LIMIT 1''' % dataset)
    results = c.fetchone()

    if not results:
        conn.close()
        raise Exception('No results for dataset=%s' % dataset)

    conn.close()

    return results
    
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

    print '\\begin{table*}'
    print '\\centering'
    print '\\caption{Best MCPS found for each dataset}'
    print '\\begin{tabular}{l l l l l l l l r}'
    print '\\hline'

    for dataset in selected_datasets:

        result = get_results(dataset)

        try:
            params = parse_configuration(result[1], True)
        except:
            print result[1]
            print result[2]
            continue

        custom_keys = ['missing_values', 'outliers', 'transformation',
                      'dimensionality_reduction', 'sampling', 'predictor', 'meta']
        
#        custom_keys = ['predictor', 'meta']               

        line = '%s & EXT' % dataset
        for i in range(0, len(custom_keys)):
            # getting only the Weka class name
            tag = params[custom_keys[i]]['method'].split('.')[-1]
            line = '%s & %s' % (line, tag)
        
        line = '%s & %.2f' % (line, float(result[2]))
        
        print line, '\\\\'

    print '\\hline'
    print '\\end{tabular}'
    print '\\label{tab:test-error}'
    print '\\end{table*}'


if __name__ == "__main__":
    main()

import os
import sqlite3
import argparse
import traceback
from config import *
from table_configurations import create_table


def get_results(dataset, strategy, generation):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration, error, test_error, num_evaluations
                 FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s' AND error IS NOT NULL ORDER BY error LIMIT 5
                 ''' % (dataset, strategy, generation))
    results = c.fetchall()

    if not results:
        conn.close()
        raise Exception('No results!')

    c.execute('''SELECT seed, error FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 AND error IS NOT NULL ORDER BY error LIMIT 1''' % (dataset, strategy, generation))
    try:
        best_error_seed = c.fetchone()[0]
    except:
        best_error_seed = -1

    c.execute('''SELECT seed, test_error FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 AND test_error IS NOT NULL ORDER BY test_error LIMIT 1''' % (dataset, strategy, generation))
    try:
        best_test_error_seed = c.fetchone()[0]
    except:
        best_test_error_seed = -1

    conn.close()

    return results, best_error_seed, best_test_error_seed


def sub_main(dataset, strategy, generation):
    print "Creating table for %s %s %s" % (dataset, strategy, generation)

    results, best_error_seed, best_test_error_seed = get_results(dataset, strategy, generation)

    if not results:
        raise Exception("No results for %s.%s.%s" % (dataset, strategy, generation))

    table = create_table(results, best_error_seed, best_test_error_seed, strategy != 'DEFAULT')
    return table


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

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'
    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    header = '<html><head>%s%s</head><body>\n' % (css, javascript)
    footer = '</body></html>'

    for dataset in selected_datasets:
        tables = ''
        for strategy in selected_strategies:
            for generation in selected_generations:
                try:
                    title = '<h2>Dataset: %s - Strategy: %s - Generation: %s</h2>\n' % (dataset, strategy, generation)
                    table = sub_main(dataset, strategy, generation)
                    tables += title + table
                except Exception as e:
                    print e
                    traceback.print_exc()
                    continue

        f = open('../tables/top_%s.html' % dataset, 'w')
        f.write(header + tables + footer)
        f.close()


if __name__ == "__main__":
    main()

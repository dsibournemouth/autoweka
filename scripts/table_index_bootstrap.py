import argparse
import os
import numpy as np
import sqlite3
from config import *


def table_header():
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    table += '<thead>' \
             '<tr><th rowspan="3">dataset</th><th colspan="4">10-fold CV performance (\%)</th><th colspan="4">Test performance (\%)</th></tr>' \
             '<tr><th>WEKA-DEF</th><th>RAND</th><th>SMAC</th><th>TPE</th>' \
             '<th>WEKA-DEF</th><th>RAND</th><th>SMAC</th><th>TPE</th></tr>' \
             '</thead>'
    return table


def format_number(n):
    return "%.2f" % n if n > 1 else "%.4f" % n


def table_row(dataset, results, format='html'):
    d = {'results_link': '%s.html' % dataset,
         'dataset': dataset,
         'cv_def': format_number(results['DEFAULT']['CV']['error']) if 'DEFAULT' in results else "-",
         'cv_rand': format_number(results['RAND']['CV']['error']) if 'RAND' in results else "-",
         'cv_smac': format_number(results['SMAC']['CV']['error']) if 'SMAC' in results else "-",
         'cv_tpe': format_number(results['TPE']['CV']['error']) if 'TPE' in results else "-",
         'cv_rand_ci': format_number(results['RAND']['CV']['error_ci']) if 'RAND' in results else "-",
         'cv_smac_ci': format_number(results['SMAC']['CV']['error_ci']) if 'SMAC' in results else "-",
         'cv_tpe_ci': format_number(results['TPE']['CV']['error_ci']) if 'TPE' in results else "-",
         'test_def': format_number(results['DEFAULT']['CV']['test_error']) if 'DEFAULT' in results else "-",
         'test_rand': format_number(results['RAND']['CV']['test_error']) if 'RAND' in results else "-",
         'test_smac': format_number(results['SMAC']['CV']['test_error']) if 'SMAC' in results else "-",
         'test_tpe': format_number(results['TPE']['CV']['test_error']) if 'TPE' in results else "-",
         'test_rand_ci': format_number(results['RAND']['CV']['test_error_ci']) if 'RAND' in results else "-",
         'test_smac_ci': format_number(results['SMAC']['CV']['test_error_ci']) if 'SMAC' in results else "-",
         'test_tpe_ci': format_number(results['TPE']['CV']['test_error_ci']) if 'TPE' in results else "-",
         }

    min_cv = {'cv_def': float(d['cv_def']),
              'cv_rand': float(d['cv_rand']),
              'cv_smac': float(d['cv_smac']),
              'cv_tpe': float(d['cv_tpe'])}
    min_key_cv = min(min_cv, key=min_cv.get)

    min_test = {'test_def': float(d['test_def']),
                'test_rand': float(d['test_rand']),
                'test_smac': float(d['test_smac']),
                'test_tpe': float(d['test_tpe'])}
    min_key_test = min(min_test, key=min_test.get)

    if format is 'latex_cv':
        d[min_key_cv] = '\\textbf{%s}' % d[min_key_cv]
        return '{dataset} & {cv_def} & {cv_rand} & {cv_smac} & {cv_tpe} \\\\'.format(**d)

    if format is 'latex_test':
        d[min_key_test] = '\\textbf{%s}' % d[min_key_test]
        return '{dataset} &{test_def} & {test_rand} & {test_smac} & {test_tpe} \\\\'.format(**d)

    # ---- HTML ----
    d[min_key_cv] = '<b>%s</b>' % d[min_key_cv]
    d[min_key_test] = '<b>%s</b>' % d[min_key_test]
    return '<tr>' \
           '<td><a href="{results_link}">{dataset}</a></td>' \
           '<td>{cv_def}</td><td>{cv_rand} &#177; {cv_rand_ci}</td>' \
           '<td>{cv_smac} &#177; {cv_smac_ci}</td><td>{cv_tpe} &#177; {cv_tpe_ci}</td>' \
           '<td>{test_def}</td><td>{test_rand} &#177; {test_rand_ci}</td>' \
           '<td>{test_smac} &#177; {test_smac_ci}</td><td>{test_tpe} &#177; {test_tpe_ci}</td>' \
           '</tr>'.format(**d)


def get_results(dataset):
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed,
             cast(error as REAL) as error, cast(test_error as REAL) as test_error
             FROM results WHERE dataset='%s' ''' % dataset)
    results = c.fetchall()

    conn.close()

    if not results:
        raise Exception('No results for dataset = %s' % dataset)

    d = dict()
    for row in results:
        if row[1] not in d:
            d[row[1]] = dict()
        if row[2] not in d[row[1]]:
            d[row[1]][row[2]] = dict()
        if 'error' not in d[row[1]][row[2]]:
            d[row[1]][row[2]]['error'] = []
        if 'test_error' not in d[row[1]][row[2]]:
            d[row[1]][row[2]]['test_error'] = []

        try:
            d[row[1]][row[2]]['error'].append(float(row[4]))
        except:
            # d[row[1]][row[2]]['error'] = None
            pass
        try:
            d[row[1]][row[2]]['test_error'].append(float(row[5]))
        except:
            pass

    for strategy in d.keys():
        for generation in d[strategy].keys():
            if 'DEFAULT' in strategy:
                d[strategy][generation]['error'] = min(d[strategy][generation]['error'])
                d[strategy][generation]['test_error'] = min(d[strategy][generation]['test_error'])
            else:
                print 'Bootstrap for strategy = %s (no.seeds = %s | %s)' % (strategy,
                                                                            len(d[strategy][generation]['error']),
                                                                            len(d[strategy][generation]['test_error']))
                errors = []
                test_errors = []
                for i in range(0, 100000):  # bootstrap 100k
                    # lowest error from 4 elements randomly selected
                    best_error = min(np.random.choice(d[strategy][generation]['error'], size=4, replace=True))
                    errors.append(best_error)

                    best_test_error = min(np.random.choice(d[strategy][generation]['test_error'], size=4, replace=True))
                    test_errors.append(best_test_error)

                d[strategy][generation]['error'] = np.mean(errors)
                d[strategy][generation]['error_ci'] = np.percentile(errors, 97.5) - np.percentile(errors, 2.5)
                d[strategy][generation]['test_error'] = np.mean(test_errors)
                d[strategy][generation]['test_error_ci'] = np.percentile(test_errors, 97.5) - np.percentile(test_errors, 2.5)

    return d


def create_table():
    table = table_header()
    table += '<tbody>'

    for dataset in datasets:
        results = get_results(dataset)
        table += table_row(dataset, results)

    table += '</tbody></table>'

    return table


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--latex-cv', action='store_true')
    parser.add_argument('--latex-test', action='store_true')

    args = parser.parse_args()

    print "Creating index"

    if args.latex_cv:
        table = ''
        for dataset in datasets:
            results = get_results(dataset)
            table += table_row(dataset, results, format='latex_cv') + "\n"

        print table
        return

    if args.latex_test:
        table = ''
        for dataset in datasets:
            results = get_results(dataset)
            table += table_row(dataset, results, format='latex_test') + "\n"

        print table
        return

    # ---- HTML ----

    table = create_table()

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'

    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    html = '<html><head>%s%s</head>' % (css, javascript)
    html += '<body><h1>Datasets</h1>'
    html += table
    html += '</body></html>'

    f = open('../tables/index_bootstrap.html', 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

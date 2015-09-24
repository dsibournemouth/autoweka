import argparse
import os
import sqlite3
from config import *


def table_header():
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    table += '<thead>' \
             '<tr><th rowspan="3">dataset</th><th colspan="8">10-fold CV performance (\%)</th><th colspan="8">Test performance (\%)</th></tr>' \
             '<tr><th colspan="2">WEKA-DEF</th><th colspan="2">RAND</th><th colspan="2">SMAC</th><th colspan="2">TPE</th>' \
             '<th colspan="2">WEKA-DEF</th><th colspan="2">RAND</th><th colspan="2">SMAC</th><th colspan="2">TPE</th></tr>' \
             '<tr><th>best</th><th>worst</th><th>best</th><th>worst</th>' \
             '<th>best</th><th>worst</th><th>best</th><th>worst</th>' \
             '<th>best</th><th>worst</th><th>best</th><th>worst</th>' \
             '<th>best</th><th>worst</th><th>best</th><th>worst</th></tr>' \
             '</thead>'
    return table


def table_row(dataset, results, format='html'):
    d = {'results_link': '%s.html' % dataset,
         'dataset': dataset,
         'min_cv_def': results['DEFAULT']['CV']['min_error'] if 'DEFAULT' in results else "-",
         'min_cv_rand': results['RAND']['CV']['min_error'] if 'RAND' in results else "-",
         'min_cv_smac': results['SMAC']['CV']['min_error'] if 'SMAC' in results else "-",
         'min_cv_tpe': results['TPE']['CV']['min_error'] if 'TPE' in results else "-",
         'max_cv_def': results['DEFAULT']['CV']['max_error'] if 'DEFAULT' in results else "-",
         'max_cv_rand': results['RAND']['CV']['max_error'] if 'RAND' in results else "-",
         'max_cv_smac': results['SMAC']['CV']['max_error'] if 'SMAC' in results else "-",
         'max_cv_tpe': results['TPE']['CV']['max_error'] if 'TPE' in results else "-",
         'min_test_def': results['DEFAULT']['CV']['min_test_error'] if 'DEFAULT' in results else "-",
         'min_test_rand': results['RAND']['CV']['min_test_error'] if 'RAND' in results else "-",
         'min_test_smac': results['SMAC']['CV']['min_test_error'] if 'SMAC' in results else "-",
         'min_test_tpe': results['TPE']['CV']['min_test_error'] if 'TPE' in results else "-",
         'max_test_def': results['DEFAULT']['CV']['max_test_error'] if 'DEFAULT' in results else "-",
         'max_test_rand': results['RAND']['CV']['max_test_error'] if 'RAND' in results else "-",
         'max_test_smac': results['SMAC']['CV']['max_test_error'] if 'SMAC' in results else "-",
         'max_test_tpe': results['TPE']['CV']['max_test_error'] if 'TPE' in results else "-"
         }

    min_cv = {'min_cv_def': d['min_cv_def'],
              'min_cv_rand': d['min_cv_rand'],
              'min_cv_smac': d['min_cv_smac'],
              'min_cv_tpe': d['min_cv_tpe']}
    min_key_cv = min(min_cv, key=min_cv.get)

    min_test = {'min_test_def': d['min_test_def'],
                'min_test_rand': d['min_test_rand'],
                'min_test_smac': d['min_test_smac'],
                'min_test_tpe': d['min_test_tpe']}
    min_key_test = min(min_test, key=min_test.get)

    if format is 'latex_cv':
        d[min_key_cv] = '\\textbf{%s}' % d[min_key_cv]
        return '{dataset} & {min_cv_def} & {max_cv_def} & {min_cv_rand} & {max_cv_rand} & ' \
               '{min_cv_smac} & {max_cv_smac} & {min_cv_tpe} & {max_cv_tpe} \\\\'.format(**d)

    if format is 'latex_test':
        d[min_key_test] = '\\textbf{%s}' % d[min_key_test]
        return '{dataset} & {min_test_def} & {max_test_def} & {min_test_rand} & {max_test_rand} & ' \
               '{min_test_smac} & {max_test_smac} & {min_test_tpe} & {max_test_tpe} \\\\'.format(**d)

    # ---- HTML ----
    d[min_key_cv] = '<b>%s</b>' % d[min_key_cv]
    d[min_key_test] = '<b>%s</b>' % d[min_key_test]
    return '<tr>' \
           '<td><a href="{results_link}">{dataset}</a></td>' \
           '<td>{min_cv_def}</td><td>{max_cv_def}</td><td>{min_cv_rand}</td><td>{max_cv_rand}</td>' \
           '<td>{min_cv_smac}</td><td>{max_cv_smac}</td><td>{min_cv_tpe}</td><td>{max_cv_tpe}</td>' \
           '<td>{min_test_def}</td><td>{max_test_def}</td><td>{min_test_rand}</td><td>{max_test_rand}</td>' \
           '<td>{min_test_smac}</td><td>{max_test_smac}</td><td>{min_test_tpe}</td><td>{max_test_tpe}</td>' \
           '</tr>'.format(**d)


def get_results(dataset):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation,
                 avg(cast(error as REAL)) as avg_error, avg(cast(test_error as REAL)) as avg_test_error,
                 min(cast(error as REAL)) as min_error, min(cast(test_error as REAL)) as min_test_error,
                 max(cast(error as REAL)) as max_error, max(cast(test_error as REAL)) as max_test_error
                 FROM results WHERE dataset='%s' GROUP BY dataset, strategy, generation''' % dataset)
    results = c.fetchall()

    conn.close()

    d = dict()
    for row in results:
        if row[1] not in d:
            d[row[1]] = dict()
        if row[2] not in d[row[1]]:
            d[row[1]][row[2]] = dict()

        try:
            d[row[1]][row[2]]['error'] = "%.2f" % row[3] if row[3] > 1 else "%.4f" % row[3]
        except:
            d[row[1]][row[2]]['error'] = None
        try:
            d[row[1]][row[2]]['test_error'] = "%.2f" % row[4] if row[4] > 1 else "%.4f" % row[4]
        except:
            d[row[1]][row[2]]['test_error'] = None
        try:
            d[row[1]][row[2]]['min_error'] = "%.2f" % row[5] if row[5] > 1 else "%.4f" % row[5]
        except:
            d[row[1]][row[2]]['min_error'] = None
        try:
            d[row[1]][row[2]]['min_test_error'] = "%.2f" % row[6] if row[6] > 1 else "%.4f" % row[6]
        except:
            d[row[1]][row[2]]['min_test_error'] = None
        try:
            d[row[1]][row[2]]['max_error'] = "%.2f" % float(row[7]) if float(row[7]) > 1 else "%.4f" % float(row[7])
        except:
            d[row[1]][row[2]]['max_error'] = None
        try:
            d[row[1]][row[2]]['max_test_error'] = "%.2f" % float(row[8]) if float(row[8]) > 1 else "%.4f" % float(
                row[8])
        except:
            d[row[1]][row[2]]['max_test_error'] = None

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

    f = open('../tables/index_latex.html', 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

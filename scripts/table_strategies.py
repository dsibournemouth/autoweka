import os
import sqlite3
import argparse
from config import *


def table_header():
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    table += '<thead><tr><th>dataset</th><th>strategy</th><th>generation</th> \
              <th>number of evaluations</th><th>min(error)</th><th>min(test error)</th><th>Results</th></tr></thead>'
    return table


def table_row(tr_class, dataset, strategy, generation, num_evaluations, error, test_error):
    results_link = '%s.%s.%s.html' % (dataset, strategy, generation)
    return '<tr style="%s"><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td><a href="%s">Results</a></td></tr>' % (
        tr_class, dataset, strategy, generation, num_evaluations, error, test_error, results_link)


def create_table(results):
    table = table_header()
    table += '<tbody>'

    for result in results:
        dataset = result[0]
        strategy = result[1]
        generation = result[2]
        num_evaluations = result[3]
        if strategy == 'DEFAULT':
            num_evaluations = int(num_evaluations) * 5  # 5 methods!
        error = result[4]
        test_error = result[5]
        tr_class = ''

        table += table_row(tr_class, dataset, strategy, generation, num_evaluations, error, test_error)

    table += '</tbody></table>'

    return table


def get_results(dataset):
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, sum(num_evaluations), min(error), min(test_error)
                 FROM results WHERE dataset='%s' GROUP BY dataset, strategy, generation
                 ''' % dataset)
    results = c.fetchall()

    conn.close()

    if not results:
        raise Exception('No results!')

    return results


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=True)

    args = parser.parse_args()

    dataset = args.dataset

    print "Creating table for %s" % dataset

    results = get_results(dataset)

    table = create_table(results)

    plots = '<table id="plots"><thead>' \
            '<tr><th>Estimated error (70% dataset)</th><th>Test error (30% dataset)</th></tr>' \
            '</thead><tbody>'
    plots += '<tr><td><img src="../plots/boxplot.error.%s.png" width="100%%"/></td>' % dataset
    plots += '<td><img src="../plots/boxplot.test_error.%s.png" width="100%%"/></td></tr>' % dataset
    plots += '</tbody></table>'

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'

    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    html = '<html><head>%s%s</head>' % (css, javascript)
    html += '<body><h1>Results of dataset: %s</h1>' % dataset
    html += table
    html += plots
    html += '</body></html>'

    f = open('../tables/%s.html' % dataset, 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

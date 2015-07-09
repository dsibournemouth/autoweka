import sqlite3
from config import *


def table_header():
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    table += '<thead>' \
             '<tr><th rowspan="2">dataset</th><th colspan="4">10-fold CV performance</th><th colspan="4">Test performance</th></tr>' \
             '<tr><th>DEF</th><th>RAND</th><th>SMAC</th><th>TPE</th><th>DEF</th><th>RAND</th><th>SMAC</th><th>TPE</th></tr>' \
             '</thead>'
    return table


def table_row(dataset, results):
    d = {'results_link': '%s.html' % dataset,
         'dataset': dataset,
         'cv_def': results['DEFAULT']['CV']['error'] if 'DEFAULT' in results else "-",
         'cv_rand': results['RAND']['CV']['error'] if 'RAND' in results else "-",
         'cv_smac': results['SMAC']['CV']['error'] if 'SMAC' in results else "-",
         'cv_tpe': results['TPE']['CV']['error'] if 'TPE' in results else "-",
         'test_def': results['DEFAULT']['CV']['test_error'] if 'DEFAULT' in results else "-",
         'test_rand': results['RAND']['CV']['test_error'] if 'RAND' in results else "-",
         'test_smac': results['SMAC']['CV']['test_error'] if 'SMAC' in results else "-",
         'test_tpe': results['TPE']['CV']['test_error'] if 'TPE' in results else "-"
    }

    return '<tr>' \
           '<td><a href="{results_link}">{dataset}</a></td>' \
           '<td>{cv_def}</td><td>{cv_rand}</td><td>{cv_smac}</td><td>{cv_tpe}</td>' \
           '<td>{test_def}</td><td>{test_rand}</td><td>{test_smac}</td><td>{test_tpe}</td>' \
           '</tr>'.format(**d)

def get_results(dataset):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, avg(error), avg(test_error)
                 FROM results WHERE dataset='%s' GROUP BY dataset, strategy, generation''' % dataset)
    results = c.fetchall()

    conn.close()

    d = dict()
    for row in results:
        if row[1] not in d:
            d[row[1]] = dict()
        if row[2] not in d[row[1]]:
            d[row[1]][row[2]] = dict()

        d[row[1]][row[2]]['error'] = row[3]
        d[row[1]][row[2]]['test_error'] = row[4]

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
    print "Creating index"

    table = create_table()

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'

    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    html = '<html><head>%s%s</head>' % (css, javascript)
    html += '<body><h1>Datasets</h1>'
    html += table
    html += '</body></html>'

    f = open('../tables/index.html', 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

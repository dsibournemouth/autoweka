import argparse
import os

from config import *


def table_header():
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    table += '<thead>' \
             '<tr><th rowspan="2">dataset</th></tr>' \
             '</thead>'
    return table


def table_row(dataset):
    d = {'results_link': 'top_%s.html' % dataset,
         'dataset': dataset
         }

    return '<tr>' \
           '<td><a href="{results_link}">{dataset}</a></td>' \
           '</tr>'.format(**d)


def create_table():
    table = table_header()
    table += '<tbody>'

    for dataset in datasets:
        table += table_row(dataset)

    table += '</tbody></table>'

    return table


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))

    print "Creating index_top"

    table = create_table()

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'

    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    html = '<html><head>%s%s</head>' % (css, javascript)
    html += '<body><h1>Datasets</h1>'
    html += table
    html += '</body></html>'

    f = open('../tables%s/index_top.html' % suffix, 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

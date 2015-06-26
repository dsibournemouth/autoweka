from datasets import datasets


def table_header():
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    table += '<thead><tr><th>dataset</th></tr></thead>'
    return table


def table_row(dataset):
    results_link = '%s.html' % dataset
    return '<tr><td><a href="%s">%s</a></td></tr>' % (results_link, dataset)


def create_table():
    table = table_header()
    table += '<tbody>'

    for dataset in datasets:
        table += table_row(dataset)

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

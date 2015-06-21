import os
import sqlite3
import subprocess
import argparse
import operator
from datasets import datasets
from strategies import strategies
from generations import generations


def parse_configuration(configuration, complete):
    if not complete:
        return {'predictor': {'method': configuration, 'params': ''}}

    params = {'missing_values': {'method': '', 'params': ''},
              'outliers': {'method': '', 'params': ''},
              'transformation': {'method': '', 'params': ''},
              'dimensionality_reduction': {'method': '', 'params': ''},
              'sampling': {'method': '', 'params': ''},
              'predictor': {'method': '', 'params': ''}}

    command = "$MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.core.Utils parseOptions %s" % configuration
    output = subprocess.check_output(command, shell=True)
    output = output.split("\n")
    for line in output:
        option = line.rstrip().split('=')
        if len(option) == 2:
            tmp = option[1].split(" ")
            this_method = tmp[0].replace("weka.filters.AllFilter", "Nothing")
            this_params = tmp[1:-1] if len(tmp) > 1 else ''
            if option[0] != 'predictor' and len(this_params) > 0:
                this_params = " ".join(this_params)
            params[option[0]]['method'] = this_method
            params[option[0]]['params'] = this_params

    return params


def create_frequency_table(frequency):
    table = '<div><h3>Frequencies for each component</h3>'

    # iterating over a predefined array for custom formatting
    if len(frequency.keys()) > 1:
        custom_keys = ['missing_values', 'outliers', 'transformation',
                       'dimensionality_reduction', 'sampling', 'predictor']
    else:
        custom_keys = ['predictor']

    for key in custom_keys:
        table += '<strong>%s</strong>' % key
        try:
            table += '<ul>'
            frequency[key] = sorted(frequency[key].items(), key=operator.itemgetter(1), reverse=True)
            for element in frequency[key]:
                table += '<li>%s: %d</li>' % (element[0], element[1])
            table += '</ul>'
        except:
            table += 'Component not available!'
            pass

    table += '</div>'

    return table


def table_header(complete):
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">'
    if complete:
        table += '<thead><tr><th>dataset</th><th>strategy</th><th>generation</th><th>seed</th> \
              <th>missing values</th><th>outliers</th><th>transformation</th><th>dimensionality reduction</th> \
              <th>sampling</th><th>predictor</th><th>CV RMSE</th><th>Test RMSE</th></tr></thead>'
    else:
        table += '<thead><tr><th>dataset</th><th>strategy</th><th>generation</th><th>seed</th> \
              <th>predictor</th><th>CV RMSE</th><th>Test RMSE</th></tr></thead>'

    return table


def table_row(tr_class, dataset, strategy, generation, seed, params, error, test_error, complete):
    if complete:
        return '<tr style="%s"><td>%s</td><td>%s</td><td>%s</td><td>%s</td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s</td><td>%s</td></tr>' % (
                   tr_class, dataset, strategy, generation, seed,
                   params['missing_values']['method'], params['missing_values']['params'],
                   params['outliers']['method'], params['outliers']['params'],
                   params['transformation']['method'], params['transformation']['params'],
                   params['dimensionality_reduction']['method'], params['dimensionality_reduction']['params'],
                   params['sampling']['method'], params['sampling']['params'],
                   params['predictor']['method'], params['predictor']['params'],
                   error, test_error)
    else:
        return '<tr style="%s"><td>%s</td><td>%s</td><td>%s</td><td>%s</td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s</td><td>%s</td></tr>' % (
                   tr_class, dataset, strategy, generation, seed,
                   params['predictor']['method'], params['predictor']['params'],
                   error, test_error)


def create_table(results, best_error_seed, best_test_error_seed, complete):
    table = table_header(complete)
    table += '<tbody>'

    frequency = dict()

    for result in results:
        dataset = result[0]
        strategy = result[1]
        generation = result[2]
        seed = result[3]
        params = parse_configuration(result[4], complete)
        error = result[5]
        test_error = result[6]
        tr_class = ''
        if seed is best_error_seed:
            tr_class = 'border: 2px solid lightgreen;'
        elif seed is best_test_error_seed:
            tr_class = 'border: 2px solid lightblue;'

        table += table_row(tr_class, dataset, strategy, generation, seed, params, error, test_error, complete)

        # update frequencies
        for key in params.keys():
            if key not in frequency:
                frequency[key] = dict()
            if params[key]['method'] not in frequency[key]:
                frequency[key][params[key]['method']] = 0
            frequency[key][params[key]['method']] += 1

    table += '</tbody></table>'

    frequency_table = create_frequency_table(frequency)

    table += frequency_table

    return table


def get_results(dataset, strategy, generation):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration, error, test_error
                 FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
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


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--strategy', choices=strategies, required=True)
    parser.add_argument('--generation', choices=generations, required=True)

    args = parser.parse_args()

    dataset = args.dataset
    strategy = args.strategy
    generation = args.generation

    print "Creating table for %s %s %s" % (dataset, strategy, generation)

    results, best_error_seed, best_test_error_seed = get_results(dataset, strategy, generation)

    table = create_table(results, best_error_seed, best_test_error_seed, strategy != 'DEFAULT')

    plots = ''
    if strategy not in ['DEFAULT', 'RAND']:
        plots = '<img src="../plots/trajectories-%s.%s.%s.scatter.png" />' % (dataset, strategy, generation)

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'
    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    html = '<html><head>%s%s</head>' % (css, javascript)
    html += '<body><h1>Components of best configurations</h1>' \
            '<h2>Dataset: %s - Strategy: %s - Generation: %s</h2>' % (dataset, strategy, generation)
    html += table
    html += plots
    html += '</body></html>'

    f = open('../tables/%s.%s.%s.html' % (dataset, strategy, generation), 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

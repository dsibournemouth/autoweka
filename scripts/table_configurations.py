import os
import sqlite3
import subprocess
import argparse
import operator
import numpy as np
from config import *


def parse_configuration(configuration, complete):
    if not complete:
        return {'predictor': {'method': configuration, 'params': ''}}

    params = {'missing_values': {'method': '', 'params': ''},
              'outliers': {'method': '', 'params': ''},
              'transformation': {'method': '', 'params': ''},
              'dimensionality_reduction': {'method': '', 'params': ''},
              'sampling': {'method': '', 'params': ''},
              'predictor': {'method': '', 'params': ''},
              'meta': {'method': '', 'params': ''}}

    command = "$MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.core.Utils parseOptions %s" % configuration
    output = subprocess.check_output(command, shell=True)
    output = output.split("\n")
    for line in output:
        option = line.rstrip().split('=')
        if len(option) == 2:
            tmp = option[1].split(" ")
            this_method = tmp[0].replace("weka.filters.AllFilter", "Nothing")
            if this_method == "": this_method = "Nothing"
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
                       'dimensionality_reduction', 'sampling', 'predictor', 'meta']
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


def create_percentage_used_table(frequency):
    # iterating over a predefined array for custom formatting
    if len(frequency.keys()) > 1:
        custom_keys = ['missing_values', 'outliers', 'transformation',
                       'dimensionality_reduction', 'sampling', 'predictor', 'meta']
    else:
        return ""

    percentage_used = dict()
    line = ""

    for key in custom_keys:
        # try:
        total = 0
        total_nothing = 0
        for element in frequency[key]:
            total += element[1]
            if element[0] == "Nothing":
                total_nothing += 1

        percentage_used[key] = (1 - float(total_nothing) / total) * 100
        line += "%0.2f," % percentage_used[key]

        # except:
        #   line += "0,"
        #  pass

    return line[:-1]


def table_header(complete):
    table = '<table id="myTable" class="tablesorter" style="border-collapse: collapse;">\n'
    if complete:
        table += '<thead><tr><th>dataset</th><th>strategy</th><th>generation</th><th>seed</th> \
              <th>missing values</th><th>outliers</th><th>transformation</th><th>dimensionality reduction</th> \
              <th>sampling</th><th>predictor</th><th>meta</th><th>CV RMSE</th><th>Test RMSE</th><th>Evaluations</th></tr></thead>\n'
    else:
        table += '<thead><tr><th>dataset</th><th>strategy</th><th>generation</th><th>seed</th> \
              <th>predictor</th><th>CV RMSE</th><th>Test RMSE</th></tr></thead>\n'

    return table


def table_row(tr_class, dataset, strategy, generation, seed, params, error, test_error, num_evaluations, complete):
    if complete:
        signal_plot = '../plots/signal.%s.%s.%s.%s.png' % (dataset, strategy, generation, seed)
        return '<tr style="%s"><td>%s</td><td>%s</td><td>%s</td><td>%s</td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s</td><td><a href="%s">%s</a></td><td>%s</td></tr>\n' % (
                   tr_class, dataset, strategy, generation, seed,
                   params['missing_values']['method'], params['missing_values']['params'],
                   params['outliers']['method'], params['outliers']['params'],
                   params['transformation']['method'], params['transformation']['params'],
                   params['dimensionality_reduction']['method'], params['dimensionality_reduction']['params'],
                   params['sampling']['method'], params['sampling']['params'],
                   params['predictor']['method'], params['predictor']['params'],
                   params['meta']['method'], params['meta']['params'],
                   error, signal_plot, test_error, num_evaluations)
    else:
        return '<tr style="%s"><td>%s</td><td>%s</td><td>%s</td><td>%s</td>' \
               '<td>%s<br/><small>%s</small></td>' \
               '<td>%s</td><td>%s</td></tr>\n' % (
                   tr_class, dataset, strategy, generation, seed,
                   params['predictor']['method'], params['predictor']['params'],
                   error, test_error)


def average_similarity(matrix):
    rows = len(matrix)
    if rows == 0:
        return 0

    columns = len(matrix[0])

    similarities = []
    for i in range(0, rows):
        for j in range(i + 1, rows):
            row_similarity = 0
            for k in range(0, columns):
                row_similarity += 1 if matrix[i][k] == matrix[j][k] else 0
            similarities.append(float(row_similarity) / columns)

    return np.mean(similarities)


def create_table(results, best_error_seed, best_test_error_seed, complete):
    table = table_header(complete)
    table += '<tbody>\n'

    frequency = dict()
    matrix = []
    matrix_preprocessing = []
    matrix_predictor = []

    cv_errors = []

    for result in results:
        dataset = result[0]
        strategy = result[1]
        generation = result[2]
        seed = result[3]
        params = parse_configuration(result[4], complete)
        error = result[5]
        test_error = result[6]
        num_evaluations = result[7]
        tr_class = ''
        if seed is best_error_seed:
            tr_class = 'border: 2px solid lightgreen;'
        elif seed is best_test_error_seed:
            tr_class = 'border: 2px solid lightblue;'

        table += table_row(tr_class, dataset, strategy, generation, seed, params, error, test_error, num_evaluations,
                           complete)
        if complete:
            matrix.append([
                params['missing_values']['method'],
                params['outliers']['method'],
                params['transformation']['method'],
                params['dimensionality_reduction']['method'],
                params['sampling']['method'],
                params['predictor']['method'],
                params['meta']['method']
            ])
            matrix_preprocessing.append([
                params['missing_values']['method'],
                params['outliers']['method'],
                params['transformation']['method'],
                params['dimensionality_reduction']['method'],
                params['sampling']['method']
            ])
            matrix_predictor.append([params['predictor']['method']])
        else:
            matrix.append([
                params['predictor']['method']
            ])

        try:
            cv_errors.append(float(error))
        except:
            pass

        # update frequencies
        for key in params.keys():
            if key not in frequency:
                frequency[key] = dict()
            if params[key]['method'] not in frequency[key]:
                frequency[key][params[key]['method']] = 0
            frequency[key][params[key]['method']] += 1

    table += '</tbody></table>\n'

    sim = average_similarity(matrix)
    table += '<strong>Average flow similarity: %0.2f &#37;</strong><br/>\n' % (sim * 100)

    sim_predictor = average_similarity(matrix_predictor)
    table += '<strong>Average predictor similarity: %0.2f &#37;</strong><br/>\n' % (sim_predictor * 100)

    sim_preprocessing = average_similarity(matrix_preprocessing)
    table += '<strong>Average preprocessing similarity: %0.2f &#37;</strong><br/>\n' % (sim_preprocessing * 100)

    cv_variance = np.var(cv_errors)
    table += '<strong>CV error variance: %0.4f</strong><br/>\n' % cv_variance

    frequency_table = create_frequency_table(frequency)
    table += frequency_table

    table += "\n<div>Percentage used: " + create_percentage_used_table(frequency) + "</div>\n"

    return table


def get_results(dataset, strategy, generation):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration, error, test_error, num_evaluations
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

    if not results:
        raise Exception("No results for %s.%s.%s" % (dataset, strategy, generation))

    table = create_table(results, best_error_seed, best_test_error_seed, strategy != 'DEFAULT')

    plots = ''
    if strategy not in ['DEFAULT', 'RAND']:
        plots = '<img src="../plots/trajectories-%s.%s.%s.scatter.png" />\n' % (dataset, strategy, generation)

    css = '<link rel="stylesheet" href="js/themes/blue/style.css" type="text/css" media="print, projection, screen" />'
    javascript = '<script type="text/javascript" src="js/jquery-latest.js"></script>' \
                 '<script type="text/javascript" src="js/jquery.tablesorter.min.js"></script>' \
                 '<script>$(document).ready(function(){$("#myTable").tablesorter();});</script>'

    html = '<html><head>%s%s</head>\n' % (css, javascript)
    html += '<body><h1>Components of best configurations</h1>\n' \
            '<h2>Dataset: %s - Strategy: %s - Generation: %s</h2>\n' % (dataset, strategy, generation)
    html += table
    html += plots
    html += '</body></html>'

    f = open('../tables/%s.%s.%s.html' % (dataset, strategy, generation), 'w')
    f.write(html)
    f.close()


if __name__ == "__main__":
    main()

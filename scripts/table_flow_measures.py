import os
import argparse
import sqlite3
import subprocess
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


def create_row(results):
    frequency = dict()
    flow_lengths = []
    matrix = []
    matrix_preprocessing = []
    matrix_predictor = []

    cv_errors = []

    flow_frequency = dict()
    flow_frequency['missing_values'] = number_seeds
    flow_frequency['outliers'] = number_seeds
    flow_frequency['transformation'] = number_seeds
    flow_frequency['dimensionality_reduction'] = number_seeds
    flow_frequency['sampling'] = number_seeds

    for result in results:
        dataset = result[0]
        strategy = result[1]
        generation = result[2]
        seed = result[3]
        params = parse_configuration(result[4], True)

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

        no_missing_values = 1 if params['missing_values']['method'] == 'Nothing' else 0
        no_outliers = 1 if params['outliers']['method'] == 'Nothing' else 0
        no_transformation = 1 if params['transformation']['method'] == 'Nothing' else 0
        no_dimensionality_reduction = 1 if params['dimensionality_reduction']['method'] == 'Nothing' else 0
        no_sampling = 1 if params['sampling']['method'] == 'Nothing' else 0

        flow_length = 5 - no_missing_values - no_outliers - no_transformation - no_dimensionality_reduction - no_sampling

        flow_frequency['missing_values'] -= no_missing_values
        flow_frequency['outliers'] -= no_outliers
        flow_frequency['transformation'] -= no_transformation
        flow_frequency['dimensionality_reduction'] -= no_dimensionality_reduction
        flow_frequency['sampling'] -= no_sampling

        flow_lengths.append(flow_length)

        # update frequencies
        for key in params.keys():
            if key not in frequency:
                frequency[key] = dict()
            if params[key]['method'] not in frequency[key]:
                frequency[key][params[key]['method']] = 0
            frequency[key][params[key]['method']] += 1

    # sim = average_similarity(matrix) * 100
    # sim_predictor = average_similarity(matrix_predictor) * 100
    # sim_preprocessing = average_similarity(matrix_preprocessing) * 100

    return [np.min(flow_lengths), np.max(flow_lengths), np.mean(flow_lengths), np.var(flow_lengths), flow_frequency]


def get_results(dataset, strategy, generation):
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration
                 FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 ''' % (dataset, strategy, generation))
    results = c.fetchall()

    conn.close()

    return results


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    print "dataset, RAND mean, RAND var, SMAC mean, SMAC var, TPE mean, TPE var"

    for dataset in datasets:
        length_mean = dict()
        length_variance = dict()
        components_frequency = dict()
        for strategy in strategies:
            if strategy == 'DEFAULT':
                continue

            if strategy not in length_mean:
                length_mean[strategy] = dict()
                length_variance[strategy] = dict()
                components_frequency[strategy] = dict()

            for generation in generations:
                results = get_results(dataset, strategy, generation)

                if not results:
                    raise Exception("No results for %s.%s.%s" % (dataset, strategy, generation))

                row = create_row(results)
                length_mean[strategy][generation] = row[2]
                length_variance[strategy][generation] = row[3]
                components_frequency = row[4]

                print "%s, %s, %s, %d, %d, %d, %d, %d" % (
                    dataset, strategy, generation,
                    components_frequency['missing_values'],
                    components_frequency['outliers'],
                    components_frequency['transformation'],
                    components_frequency['dimensionality_reduction'],
                    components_frequency['sampling']
                )
                # print "%s, %s, %s, %0.2f, %0.2f, %0.2f, %0.2f" % (
                #    dataset, strategy, generation, row[0], row[1], row[2], row[3])

        '''
        print "%s, %0.2f, %0.2f, %0.2f, %0.2f, %0.2f, %0.2f" % (
            dataset,
            length_mean['RAND']['CV'],
            length_variance['RAND']['CV'],
            length_mean['SMAC']['CV'],
            length_variance['SMAC']['CV'],
            length_mean['TPE']['CV'],
            length_variance['TPE']['CV'])
        '''


if __name__ == "__main__":
    main()

#!/usr/bin/python
# -*- coding: utf-8 -*-

import argparse
import numpy as np
import os
import traceback

import matplotlib.cm as cm
import matplotlib.pyplot as plt
from matplotlib import rcParams

from config import *
from plot_dendograms import plot_dendogram
from table_configurations import parse_configuration

rcParams['font.family'] = 'serif'
rcParams['font.sans-serif'] = ['Times New Roman']


def create_distance_matrix_by_configuration(configurations):
    rows = len(configurations)
    if rows == 0:
        return 0

    columns = len(configurations[0])
    weights = [1, 1, 1, 1, 1, 2, 1.5]
    total_weight = sum(weights)

    distance = np.zeros((rows, rows))
    # Each row is configuration
    for i in range(0, rows):
        for j in range(0, rows):
            row_similarity = 0
            # each column is a component
            for k in range(0, columns):
                row_similarity += weights[k] if configurations[i][k] == configurations[j][k] else 0

            # distance == 0 if they are the same
            distance[i, j] = float(row_similarity) / total_weight

    return distance


def create_distance_matrix_by_error(errors):
    rows = len(errors)
    if rows == 0:
        return 0

    distance = np.zeros((rows, rows))
    # Each row is an error value
    for i in range(0, rows):
        for j in range(0, rows):
            # distance == 0 if they have the same error
            distance[i, j] = abs(errors[i] - errors[j])

    return distance


def plot_distances(x, y, labels):
    print "----"
    print labels
    print x
    print y
    print "----"

    fig, ax = plt.subplots()
    colors = np.random.rand(21)  # 21 datasets
    dataset_dict = dict()
    markers = dict()
    markers['RAND'] = '>'
    markers['SMAC'] = 'o'
    markers['TPE'] = (5, 1)

    # shape by strategy 
    # color by dataset
    color_index = 0
    for p in range(0, len(x)):
        tmp = labels[p].split('.')
        dataset = tmp[0]

        dataset_index = datasets.index(dataset)
        color = cm.hsv(dataset_index / 25., 1)

        strategy = tmp[1]
        plt.plot(x[p], y[p], marker=markers[strategy], c=color, alpha=.75, label=labels[p])

    # plt.tight_layout()
    plt.title('error variance vs configuration dissimilarity')
    plt.xlabel('MCPS dissimilarity')
    plt.ylabel('Error variance')
    # TODO fix legend
    # handles, labels = ax.get_legend_handles_labels()
    # lgd = ax.legend(handles, labels, loc='center right', bbox_to_anchor=(0.5,-0.1), numpoints=1)
    plt.savefig('../distances%s/_all.svg' % suffix, dpi=200, bbox_inches='tight')
    plt.close("all")


def plot_similarity_matrix(matrix, this_title):
    mask =  np.tri(matrix.shape[0], k=-1)
    A = np.ma.array(matrix, mask=mask) # mask out the lower triangle
    fig = plt.figure()
    ax1 = fig.add_subplot(111)
    cmap = cm.get_cmap('jet', 10) # jet doesn't have white color
    cmap.set_bad('w') # default value is 'k'
    img = ax1.imshow(A, interpolation='nearest', cmap=cmap, vmin=0, vmax=1.0)
    ax1.grid(True)
    plt.title(this_title)
    ax1.set_xlabel('Batch')
    ax1.set_ylabel('Batch')
    cb = fig.colorbar(img)
    cb.set_clim(0, 1.0)
    cb.set_label('Similarity')
    plt.show()
    plt.savefig("../distances%s/by_configuration/%s.png" % (suffix, this_title), dpi=200, bbox_inches='tight')
    plt.close("all")

def sub_main(dataset, strategy, generation, plot_flags):
    results, best_error_seed, best_test_error_seed = get_results(dataset, strategy, generation)
    configurations = dict()
    errors = []
    labels = []
    for result in results:
        seed = result[3]
        batch = result[8]
        try:
            params = parse_configuration(result[4], True)
        except:
            continue
        error = float(result[5])
        flow = [
            params['missing_values']['method'] if 'missing_values' in params.keys() else "-",
            params['outliers']['method'] if 'outliers' in params.keys() else "-",
            params['transformation']['method'] if 'transformation' in params.keys() else "-",
            params['dimensionality_reduction']['method'] if 'dimensionality_reduction' in params.keys() else "-",
            params['sampling']['method'] if 'sampling' in params.keys() else "-"
        ]

        if 'predictors' in params.keys():
            for predictor in params['predictors']:
                predictor_string = predictor['predictor']['method'].split(' ')[0]
                flow.append(predictor_string)
        else:
            flow.append(params['predictor']['method'])

        flow.append(params['meta']['method'])

        if seed not in configurations.keys():
            configurations[seed] = []

        configurations[seed].append(flow)
        flow_string = ""
        for i in reversed(range(0, len(flow))):
            this_component = flow[i].split(".")[-1]
            if this_component != '-':
                flow_string += this_component + u" ← "
        flow_string = flow_string[:-3]
        labels.append("#%s (%.2f) %s" % (seed, error, flow_string))

    mean_distance_configuration = [0 for i in range(0, len(configurations))]
    for seed in range(0, len(configurations)):
        matrix = create_distance_matrix_by_configuration(configurations[seed])
        mean_distance_configuration[seed] = (np.sum(matrix) / 2) / (matrix.shape[0] * matrix.shape[1] / 2)
        np.savetxt("../distances%s/by_configuration/%s.%s.%s.%s.csv" % (suffix, dataset, strategy, generation, seed),
                   matrix, fmt="%.6f", delimiter=",")

        if plot_flags['by_configuration']:
            title = "%s.%s.%s.%s" % (dataset, strategy, generation, seed)
            plot_similarity_matrix(matrix, title)
            #plot_dendogram(matrix, labels, title,
            #               "../distances%s/by_configuration" % suffix)

    return np.mean(mean_distance_configuration)


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--strategy', choices=strategies, required=False)
    parser.add_argument('--generation', choices=generations, required=False)
    parser.add_argument('--plot-by-configuration', action="store_true")
    parser.add_argument('--plot-by-error', action="store_true")

    args = parser.parse_args()

    # override default values
    selected_datasets = [args.dataset] if args.dataset else datasets
    selected_strategies = [args.strategy] if args.strategy else strategies
    selected_generations = [args.generation] if args.generation else generations

    plot_flags = {'by_configuration': args.plot_by_configuration,
                  'by_error': args.plot_by_error}

    means_label = []
    means_config = []
    means_error = []
    for dataset in selected_datasets:
        for strategy in selected_strategies:
            for generation in selected_generations:
                try:
                    mean_distance_configuration = sub_main(dataset, strategy, generation,
                                                                                plot_flags)
                    means_label.append("%s.%s.%s" % (dataset, strategy, generation))
                    means_config.append(mean_distance_configuration)
                except Exception as e:
                    print e
                    traceback.print_exc()
                    continue

    #plot_distances(means_config, means_error, means_label)


if __name__ == "__main__":
    main()

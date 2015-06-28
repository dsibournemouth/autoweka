import os
import argparse
import pygraphviz as pgv
import sqlite3
from config import *
from table_configurations import parse_configuration


def get_results(dataset, strategy, generation):
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration, error
                 FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 AND error IS NOT NULL ORDER BY error LIMIT 1
                 ''' % (dataset, strategy, generation))
    results = c.fetchone()

    if not results:
        conn.close()
        raise Exception('No results!')

    conn.close()

    return results


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--strategy', choices=strategies, required=True)
    parser.add_argument('--generation', choices=generations, required=True)

    args = parser.parse_args()

    dataset = args.dataset
    strategy = args.strategy
    generation = args.generation

    result = get_results(dataset, strategy, generation)

    dataset = result[0]
    strategy = result[1]
    generation = result[2]
    seed = result[3]
    params = parse_configuration(result[4], True)

    custom_keys = ['missing_values', 'outliers', 'transformation',
                   'dimensionality_reduction', 'sampling', 'predictor']

    flowchart = pgv.AGraph(directed=True)
    flowchart.node_attr['shape'] = 'box'
    flowchart.node_attr['fontsize'] = 10
    flowchart.node_attr['mindist'] = 0.1

    for i in range(0, len(custom_keys) - 1):
        # getting only the Weka class name
        tag1 = params[custom_keys[i]]['method'].split('.')[-1]
        tag2 = params[custom_keys[i + 1]]['method'].split('.')[-1]

        node1 = '%s:\n%s' % (custom_keys[i], tag1)
        node2 = '%s:\n%s' % (custom_keys[i + 1], tag2)

        flowchart.add_edge(node1, node2)

    # print flowchart.string()

    # available layouts: https://pygraphviz.github.io/documentation/latest/reference/agraph.html
    flowchart.layout(prog='circo')
    flowchart.draw('../flowcharts/flow.%s.%s.%s.%s.circo.svg' % (dataset, strategy, generation, seed))


if __name__ == "__main__":
    main()

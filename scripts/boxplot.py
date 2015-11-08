#!/usr/bin/python
import sqlite3
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.font_manager import FontProperties

mpl.use('Agg')
from pylab import *
import sys
from collections import OrderedDict
from config import *

def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--error', choices=['error', 'test_error', 'full_cv_error'], required=True)
    
    args = parser.parse_args()

    dataset = args.dataset
    type_error = args.error

    skip_keys = ['TPE-DPS']

    dataset = sys.argv[1]

    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    query = "SELECT strategy,generation,%s FROM results WHERE dataset='%s' AND %s<100000" % (
        type_error, dataset, type_error)

    results = c.execute(query).fetchall()

    conn.close()

    if not results:
        raise Exception('No results')

    data = dict()
    for row in results:
        key = "%s-%s" % (row[0], row[1])
        if key in skip_keys:
            continue

        if key == 'DEFAULT-CV':
            key = 'WEKA-DEF'
        if key == 'RAND-CV':
            key = 'RAND'
        if key == 'SMAC-CV':  # TO REMOVE
            key = 'SMAC'
        if key == 'TPE-CV':  # TO REMOVE
            key = 'TPE'

        if key not in data:
            data[key] = []
        try:
            data[key].append(float(row[2]))
        except Exception, e:
            print "[ERROR] ", e, " -- ", row[2]

    # data = OrderedDict(sorted(data.items(), key=lambda t: t[0]))
    labels = ['WEKA-DEF', 'RAND', 'SMAC', 'TPE']
    data = [data['WEKA-DEF'], data['RAND'], data['SMAC'], data['TPE']]

    fig, ax = plt.subplots(figsize=(6, 2))
    ax.set_aspect(6)
    fig.canvas.draw()
    #bp = plt.boxplot(data.values(), vert=False, whis='range')  # , labels=data.keys())
    #ytickNames = plt.setp(ax, yticklabels=data.keys())
    bp = plt.boxplot(data[::-1], vert=False, whis='range', widths=0.8, fontname='Times New Roman')  # , labels=data.keys())
    ytickNames = plt.setp(ax, yticklabels=labels[::-1])
    plt.setp(ytickNames, fontsize=10, fontname='Times New Roman')
    xlim(0, 100)
    plt.margins(0.05, 0.05)
    xlabel('% misclassification')
    #ylabel('Strategy')
    title(dataset)
    tight_layout()
    savefig('../plots%s/boxplot.%s.%s.png' % (suffix, type_error, dataset))
    # show()
    
if __name__ == "__main__":
    main()

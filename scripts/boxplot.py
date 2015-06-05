#!/usr/bin/python
import sqlite3
from pylab import *
import sys

if len(sys.argv) < 2:
    print 'Syntax: python boxplot.py <dataset_name>'
    exit(1)

dataset = sys.argv[1]

conn = sqlite3.connect('results.db')
c = conn.cursor()

query = "SELECT strategy,generation,coalesce(full_cv_error,error) FROM results WHERE dataset='%s'" % dataset
results = c.execute(query).fetchall()

conn.close()

if not results:
    raise Exception('No results')

data = dict()
for row in results:
    key = "%s-%s" % (row[0], row[1])
    if key not in data:
        data[key] = []
    try:
        data[key].append(float(row[2]))
    except:
	print row[2]

figure()
boxplot(data.values(), labels=data.keys())
ylabel('Error')
xlabel('Strategy')
title(dataset)
savefig('../plots/boxplot.%s.png' % dataset)
# show()

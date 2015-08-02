import os
import sys
from config import *

seeds = [0]

if len(sys.argv) < 2:
    print 'Syntax: python get_best_points_default.py <CV|Test>'
    exit(1)

validation = sys.argv[1]

path = "%s/experiments/defaultParameters" % os.environ['AUTOWEKA_PATH']

best = dict()
best_seed = dict()
f_validation = open("%s/%s.csv" % (path, validation), 'w')
validation_error = float("inf")

f_best = open("%s/scripts/results_default.csv" % os.environ['AUTOWEKA_PATH'], 'w')

for d in datasets:
    best[d] = {'error': float("inf"), 'conf': "", 'seed': -1, 'test_error': float("inf")}
    best_seed[d] = dict()
    for m in methods:
        for s in seeds:
            # initialize
            if s not in best_seed[d]:
                best_seed[d][s] = {'error': float("inf"), 'conf': "", 'test_error': float("inf")}

            filename = "%s/%s.%s.%s.%s.csv" % (path, d, m, validation, s)
            f = open(filename, 'r')
            try:
                error = float(f.read().strip(' \t\n\r'))
            except:
                error = float("inf")
            f.close()

            if error < best[d]['error']:
                best[d]['error'] = error
                best[d]['conf'] = m
                best[d]['seed'] = s

            if error < best_seed[d][s]['error']:
                best_seed[d][s]['error'] = error
                best_seed[d][s]['conf'] = m

            line = "%s,%s,%s,%.5f\n" % (d, m, s, error)
            f_validation.write(line)

    f_test = open("%s/%s.%s.Test.%s.csv" % (path, d, best[d]['conf'], best[d]['seed']), 'r')
    test_error = f_test.read().strip(' \t\n\r')
    try:
        best[d]['test_error'] = float(test_error)
    except:
        best[d]['test_error'] = float("inf")

    f_test.close()

    for s in seeds:
        f_test = open("%s/%s.%s.Test.%s.csv" % (path, d, best_seed[d][s]['conf'], s), 'r')
        test_error = f_test.read().strip(' \t\n\r')
        try:
            best_seed[d][s]['test_error'] = float(test_error)
        except:
            best_seed[d][s]['test_error'] = float("inf")
        f_test.close()

        # dataset.strategy.generation-dataset, seed, num_trajectories, num_evaluations, total_evaluations,
        # memout_evaluations, timeout_evaluations, error, test_error, configuration
        this_error = "NULL" if best_seed[d][s]['error'] >= float("inf") else "%.5f" % best_seed[d][s]['error']
        this_test_error = "NULL" if best_seed[d][s]['test_error'] >= float("inf") else "%.5f" % best_seed[d][s]['test_error']

        line = "%s.%s.%s-%s,%s,%d,%d,%d,%d,%d,%s,%s,%s\n" % (
            d, 'DEFAULT', 'CV', d, s, 1, 1, 1, 0, 0,
            this_error, this_test_error, best_seed[d][s]['conf'])
        f_best.write(line)

f_validation.close()
f_best.close()

print "DONE!"

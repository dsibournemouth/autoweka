import os
import sys
import argparse
from config import *

if len(sys.argv) < 2:
    print 'Syntax: python get_best_points_default.py <CV|Test>'
    exit(1)

is_classification = True

validation = sys.argv[1]

path = "%s/%s/defaultParameters" % (os.environ['AUTOWEKA_PATH'], experiments_folder)

f_validation = open("%s/%s.csv" % (path, validation), 'w')
f_best = open("%s/scripts/results_default.csv" % os.environ['AUTOWEKA_PATH'], 'w')

for d in datasets:
    fake_seed = 0
    for m in methods:
        filename = "%s/%s.%s.%s.0.csv" % (path, d, m, validation)
        f = open(filename, 'r')
        try:
            error = float(f.read().strip(' \t\n\r'))
            if is_classification:
                error = 100 - error
        except:
            error = "NULL"
        f.close()

        line = "%s,%s,0,%s\n" % (d, m, error)
        f_validation.write(line)

        f_test = open("%s/%s.%s.Test.0.csv" % (path, d, m), 'r')
        try:
            test_error = float(f_test.read().strip(' \t\n\r'))
            if is_classification:
                test_error = 100 - test_error
        except:
            test_error = "NULL"
        f_test.close()

        # dataset.strategy.generation-dataset, seed, num_trajectories, num_evaluations, total_evaluations,
        # memout_evaluations, timeout_evaluations, error, test_error, configuration
        line = "%s.%s.%s-%s,%s,%d,%d,%d,%d,%d,%s,%s,%s\n" % (
            d, 'DEFAULT', 'CV', d, fake_seed, 1, 1, 1, 0, 0,
            error, test_error, m)
        f_best.write(line)

        fake_seed += 1

f_validation.close()
f_best.close()

print "DONE!"

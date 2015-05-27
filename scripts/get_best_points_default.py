import os
from os import system
import sys

if len(sys.argv)<2:
	print 'Syntax: python get_best_points_defaul.py <CV|Test>'
	exit(1)

validation = sys.argv[1]

datasets = ['absorber', 'catalyst_activation', 'debutanizer', 'oxeno-hourly', 'sulfur', 'IndustrialDrier', 'ThermalOxidizerConv']
methods = ['LinearRegression', 'MultilayerPerceptron', 'PLSClassifier', 'RBFRegressor', 'SMOreg']
path = "%s/experiments/defaultParameters" % os.environ['AUTOWEKA_PATH']

best = dict()
f_validation = open("%s/%s.csv" % (path, validation), 'w')
validation_error = float("inf")

for d in datasets:
	best[d] = {'error': float("inf"), 'conf':"", 'seed':-1, 'test_error': float("inf")}
	for m in methods:
		for s in range(0,25):
			filename = "%s/%s.%s.%s.%d.csv" % (path, d, m, validation, s)
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
			line = "%s,%s,%d,%.5f\n" % (d, m, s, error)
			f_validation.write(line)
	f_test = open("%s/%s.%s.Test.%d.csv" % (path, d, best[d]['conf'], best[d]['seed']), 'r')
	test_error = f_test.read().strip(' \t\n\r')
	best[d]['test_error'] = test_error
	f_test.close()
f_validation.close()

print best

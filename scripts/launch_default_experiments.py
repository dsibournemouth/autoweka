import os
from os import system
from datasets import datasets

methods = ['LinearRegression', 'MultilayerPerceptron', 'PLSClassifier', 'RBFRegressor', 'SMOreg']

for d in datasets:
    for m in methods:
        for s in range(0, 25):
            command = "qsub -l q=compute %s/default_experiment.sh %s %s %d" % (os.environ['AUTOWEKA_PATH'], d, m, s)
            system(command)

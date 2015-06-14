import os
from os import listdir, system
from os.path import isfile, isdir, join
from datasets import datasets

mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
experiments = [f for f in listdir(mypath) if isdir(join(mypath, f))]
for e in experiments:
    folder = "%s/%s" % (mypath, e)
    if isfile(join(folder, "%s.trajectories.0" % e)):
        system("python extract_points.py %s" % folder)

for d in datasets:
    system("python boxplot.py %s error" % d)
    system("python boxplot.py %s test_error" % d)
    system("python boxplot.py %s full_cv_error" % d)

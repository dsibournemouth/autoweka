from os import system
from datasets import datasets
from strategies import strategies
from generations import generations

for d in datasets:
    system("python boxplot.py %s error" % d)
    system("python boxplot.py %s test_error" % d)
    system("python boxplot.py %s full_cv_error" % d)
    for s in strategies:
        if s not in ['DEFAULT', 'RAND']:
            system("python plot_cv.vs.dps.py --dataset=%s --strategy=%s" % (d, s))
            for g in generations:
                system("python plot_trajectories.vs.time.py --dataset=%s --strategy=%s --generation=%s" % (d, s, g))

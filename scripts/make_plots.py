import argparse
from os import system
import os
from config import *

parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
parser.add_argument('--only-boxplots', action='store_true')
parser.add_argument('--only-cv-dps', action='store_true')
parser.add_argument('--only-trajectories', action='store_true')
parser.add_argument('--only-signals', action='store_true')
parser.add_argument('--only-flows', action='store_true')

args = parser.parse_args()

boxplots = args.only_boxplots or (not args.only_cv_dps and not args.only_trajectories and not args.only_signals and not args.only_flows)
cv_dps = args.only_cv_dps or (not args.only_boxplots and not args.only_trajectories and not args.only_signals and not args.only_flows)
trajectories = args.only_trajectories or (not args.only_boxplots and not args.only_cv_dps and not args.only_signals and not args.only_flows)
signals = args.only_signals or (not args.only_boxplots and not args.only_cv_dps and not args.only_trajectories and not args.only_flows)
flows = args.only_flows or (not args.only_boxplots and not args.only_cv_dps and not args.only_trajectories and not args.only_signals)

for d in datasets:
    if boxplots:
        system("python boxplot.py %s error" % d)
        system("python boxplot.py %s test_error" % d)
        system("python boxplot.py %s full_cv_error" % d)

    for s in strategies:
        if s not in ['DEFAULT', 'RAND']:
            if trajectories:
                system("python plot_cv.vs.dps.py --dataset=%s --strategy=%s" % (d, s))

            for g in generations:
                if cv_dps:
                    system("python plot_trajectories.vs.time.py --dataset=%s --strategy=%s --generation=%s" % (
                        d, s, g))

                if flows:
                    system("python plot_flow.py --dataset=%s --strategy=%s --generation=%s" % (
                        d, s, g))

                for seed in seeds:
                    if signals:
                        system("python plot_signal.py --dataset=%s --strategy=%s --generation=%s --seed=%s" % (
                            d, s, g, seed))

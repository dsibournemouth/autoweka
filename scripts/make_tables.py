from os import system
from datasets import datasets
from strategies import strategies
from generations import generations

system("python table_index.py")
for d in datasets:
    system("python table_strategies.py --dataset=%s" % d)
    for s in strategies:
        for g in generations:
            if s in ['DEFAULT', 'RAND'] and g is 'DPS':
                continue

            system("python table_configurations.py --dataset=%s --strategy=%s --generation=%s" % (d, s, g))

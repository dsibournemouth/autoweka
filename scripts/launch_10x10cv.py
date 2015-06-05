from os import system
datasets = ['absorber',
            'catalyst_activation',
            'debutanizer',
            'oxeno-hourly',
            'sulfur',
            'IndustrialDrier',
            'ThermalOxidizerConv']


#strategies = ['DEFAULT', 'RANDOM', 'SMAC', 'ROAR', 'TPE']
strategies = ['SMAC']
generations = ['CV', 'DPS']
seeds = range(0,25)

for dataset in datasets:
    for strategy in strategies:
        for generation in generations:
            for seed in seeds:
                 name = "%s_%s_%s_%s" % (dataset, strategy, generation, seed)
                 system("qsub -N %s -l q=compute ./run_10x10cv.sh %s %s %s %d" % (name, dataset, strategy, generation, seed))

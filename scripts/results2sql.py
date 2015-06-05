import sqlite3

conn = sqlite3.connect('results.db')
c = conn.cursor()

create_tables = False
insert_datasets = False
insert_experiments = False


def insert_results(f):
    for line in f:
        tmp = line.split('.')
        dataset = tmp[0]
        strategy = tmp[1]
        generation = tmp[2].split('-')[0]
        fields = line.replace('NaN', 'NULL').split(',')
        seed = fields[1]
        select_full_cv_error = '''SELECT full_cv_error FROM results
                                  WHERE dataset='%s' AND strategy='%s' AND generation='%s' AND seed=%s''' % (
            dataset, strategy, generation, seed)
        newline = "'%s','%s','%s',%s" % (dataset, strategy, generation, ','.join(fields[1:-1]))
        newline = "%s,(%s),'%s'" % (
            newline, select_full_cv_error, fields[-1].rstrip('\n'))  # weka quotes can be a problem here
        # print newline
        c.execute('''INSERT OR REPLACE INTO results(
                     dataset, strategy, generation, seed, num_trajectories, num_evaluations, total_evaluations,
                     memout_evaluations, timeout_evaluations, error, test_error, full_cv_error, configuration)
                     VALUES (%s)''' % newline)
    conn.commit()


if create_tables:
    c.execute('''CREATE TABLE datasets (name PRIMARY KEY, train, test)''')

    c.execute('''CREATE TABLE experiments
                (dataset, strategy, generation,
                 FOREIGN KEY(dataset) REFERENCES datasets(name),
                 PRIMARY KEY(dataset, strategy, generation)
                 )''')

    c.execute('''CREATE TABLE results
                 (dataset, strategy, generation, seed, num_trajectories,
                  num_evaluations, total_evaluations, memout_evaluations, timeout_evaluations,
                  error, test_error, full_cv_error, configuration,
                  FOREIGN KEY(dataset) REFERENCES experiments(dataset),
                  FOREIGN KEY(strategy) REFERENCES experiments(strategy),
                  FOREIGN KEY(generation) REFERENCES experiments(generation),
                  UNIQUE(dataset, strategy, generation, seed) ON CONFLICT REPLACE
                  )''')

# --------- Insert datasets ---------

if insert_datasets:
    datasets = ['absorber',
                'catalyst_activation',
                'debutanizer',
                'oxeno-hourly',
                'sulfur',
                'IndustrialDrier',
                'ThermalOxidizerConv']

    for dataset in datasets:
        trainfile = "%s-train70perc.arff" % dataset
        testfile = "%s-test30perc.arff" % dataset
        c.execute("INSERT INTO datasets VALUES(?,?,?)", (dataset, trainfile, testfile))

    conn.commit()

# --------- Insert experiments ---------

if insert_experiments:
    strategies = ['DEFAULT', 'RANDOM', 'SMAC', 'ROAR', 'TPE']
    generations = ['CV', 'DPS']

    for dataset in datasets:
        for strategy in strategies:
            for generation in generations:
                c.execute("INSERT INTO experiments(dataset, strategy, generation) VALUES(?,?,?)",
                          (dataset, strategy, generation))

    conn.commit()

# --------- Insert SMAC, ROAR and TPE results ---------

f = open('results.csv', 'r')
insert_results(f)
f.close()

# --------- Insert RANDOM results ---------

f = open('results_random.csv', 'r')
insert_results(f)
f.close()

# --------- Close DB ---------

conn.close()

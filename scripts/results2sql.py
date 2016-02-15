import argparse
import os
import subprocess

from config import *


def insert_results(conn, file, convert_configuration=False, pretend=False):
    print "Inserting results from %s (convert=%s, pretend=%s)" % (file, convert_configuration, pretend)

    c = conn.cursor()
    f = open(file, 'r')
    for line in f:
        batch = None
        tmp = line.split('.')
        dataset = tmp[0]

        if ',' in dataset:
            tmp2 = dataset.split(',')
            batch = tmp2[0]
            dataset = tmp2[1]

        strategy = tmp[1]
        generation = tmp[2].split('-')[0]
        fields = line.replace('NaN', 'NULL').split(',')

        if batch:
            del fields[0]

        seed = fields[1]
        configuration = fields[-1].rstrip('\n')
        if convert_configuration:
            # transform autoweka config to weka config
            command = 'cd $AUTOWEKA_PATH && $MY_JAVA_PATH/java -Xmx2000M -cp autoweka.jar autoweka.WekaArgumentConverter "%s"' % (
                configuration.lstrip().rstrip())
            output = subprocess.check_output(command, shell=True)
            configuration = output.rstrip()

        select_full_cv_error = '''SELECT full_cv_error FROM results
                                  WHERE dataset='%s' AND strategy='%s' AND generation='%s' AND seed=%s''' % (
            dataset, strategy, generation, seed)
        newline = "'%s','%s','%s',%s" % (dataset, strategy, generation, ','.join(fields[1:-1]))
        newline = "%s,(%s),'%s'" % (
            newline, select_full_cv_error, configuration)

        if batch:
            newline = "%s,%s" % (newline, batch)

        attributes = '''dataset, strategy, generation, seed, num_trajectories, num_evaluations, total_evaluations,
                         memout_evaluations, timeout_evaluations, error, test_error, full_cv_error, configuration'''

        if batch:
            attributes = '%s, batch' % attributes

        insert_sql = '''INSERT OR REPLACE INTO results(%s) VALUES (%s)''' % (attributes, newline)
        if not pretend:
            print "Inserting batch %s of %s" % (batch, dataset)
            c.execute(insert_sql)
        else:
            print insert_sql
    conn.commit()
    f.close()


def create_tables(conn, pretend=False, adaptive=False):
    c = conn.cursor()

    create_datasets = "CREATE TABLE datasets (name PRIMARY KEY, train, test)"
    create_experiments = '''CREATE TABLE experiments
                            (dataset, strategy, generation,
                             FOREIGN KEY(dataset) REFERENCES datasets(name),
                             PRIMARY KEY(dataset, strategy, generation)
                             )'''
    attributes = '''dataset, strategy, generation, seed, num_trajectories,
                      num_evaluations, total_evaluations, memout_evaluations, timeout_evaluations,
                      error, test_error, full_cv_error, configuration'''

    unique = 'dataset, strategy, generation, seed'

    if adaptive:
        attributes = '%s, batch' % attributes
        unique = '%s, batch' % unique

    create_results = '''CREATE TABLE results
                     (%s,
                      FOREIGN KEY(dataset) REFERENCES experiments(dataset),
                      FOREIGN KEY(strategy) REFERENCES experiments(strategy),
                      FOREIGN KEY(generation) REFERENCES experiments(generation),
                      UNIQUE(%s) ON CONFLICT REPLACE
                      )''' % (attributes, unique)

    if not pretend:
        c.execute(create_datasets)
        c.execute(create_experiments)
        c.execute(create_results)
    else:
        print create_datasets
        print create_experiments
        print create_results


# --------- Insert datasets ---------

def insert_datasets(conn, pretend=False):
    c = conn.cursor()

    for dataset in datasets:
        trainfile = "%s/train.arff" % dataset
        testfile = "%s/test.arff" % dataset
        insert_sql = "INSERT INTO datasets VALUES('%s','%s','%s')" % (dataset, trainfile, testfile)
        if not pretend:
            c.execute(insert_sql)
        else:
            print insert_sql

    conn.commit()


# --------- Insert experiments ---------

def insert_experiments(conn, pretend=False):
    c = conn.cursor()
    for dataset in datasets:
        for strategy in strategies:
            for generation in generations:
                insert_sql = "INSERT INTO experiments(dataset, strategy, generation) VALUES('%s','%s','%s')" % (
                    dataset, strategy, generation)
                if not pretend:
                    c.execute(insert_sql)
                else:
                    print insert_sql

    conn.commit()


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--create-tables', action='store_true')
    parser.add_argument('--insert-datasets', action='store_true')
    parser.add_argument('--insert-experiments', action='store_true')
    parser.add_argument('--insert')
    parser.add_argument('--convert-configuration', action='store_true')
    parser.add_argument('--pretend', action='store_true')
    parser.add_argument('--adaptive', action='store_true')

    args = parser.parse_args()

    conn = sqlite3.connect(database_file)

    if args.create_tables:
        create_tables(conn, pretend=args.pretend, adaptive=args.adaptive)
    if args.insert_datasets:
        insert_datasets(conn, pretend=args.pretend)
    if args.insert_experiments:
        insert_experiments(conn, pretend=args.pretend)
    if args.insert:
        insert_results(conn, args.insert, convert_configuration=args.convert_configuration, pretend=args.pretend)

    conn.close()


if __name__ == "__main__":
    main()

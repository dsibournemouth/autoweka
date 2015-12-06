import argparse
import os

from config import *


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=False)
    parser.add_argument('--generation', choices=generations, required=False)
    parser.add_argument('--seed', choices=seeds, required=False)

    args = parser.parse_args()

    # override default values
    if args.dataset:
        selected_datasets = [args.dataset]
    else:
        selected_datasets = datasets
    if args.generation:
        selected_generations = [args.generation]
    else:
        selected_generations = generations
    if args.seed:
        selected_seeds = [args.seed]
    else:
        selected_seeds = seeds

    strategy = 'SMAC'

    conn = sqlite3.connect(database_file)

    c = conn.cursor()

    for dataset in selected_datasets:
        for generation in selected_generations:
            d = {"dataset": dataset, "strategy": strategy, "generation": generation}
            experiment = "{dataset}.{strategy}.{generation}-{dataset}".format(**d)
            for seed in selected_seeds:
                cv_logfile = 'rawValidationExecutionResults-tunertime-run%s.csv' % seed

                cv_error = 0
                failure = False
                try:
                    f = open('%s/%s/%s/out/autoweka/%s' % (
                    os.environ['AUTOWEKA_PATH'], experiment, experiments_folder, cv_logfile))
                    for line in f:
                        if "TIMEOUT" in line or "CRASH" in line:
                            failure = True
                            break

                        if "SAT" in line:
                            line = line.split(",")
                            cv_error += float(line[7])

                    f.close()
                except:
                    failure = True
                    break

                if not failure:
                    cv_error /= NUM_FOLDS_CONFIG
                    cv_error = '%.03f' % cv_error
                else:
                    cv_error = None

                c.execute('''SELECT error FROM results
                             WHERE dataset="%s" AND strategy="%s" AND generation="%s" AND seed=%s''' % (
                    dataset, strategy, generation, seed))
                result = c.fetchone()
                try:
                    old_cv_error = '%.03f' % result[0]
                except:
                    old_cv_error = None

                if cv_error != old_cv_error:
                    print "%s,%s,%s,%s,%s,%s" % (dataset, strategy, generation, seed, old_cv_error, cv_error)

                    # print 'SELECT * FROM RESULTS WHERE dataset="%s" AND strategy="%s" AND generation="%s" AND seed=%s AND error!=%s;' % (dataset, strategy, generation, seed, cv_error)

    conn.close()


if __name__ == "__main__":
    main()

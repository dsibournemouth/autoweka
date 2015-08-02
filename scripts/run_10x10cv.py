import sqlite3
from os.path import join
import subprocess
import sys
import os
from numpy import median

if len(sys.argv) < 5:
    # print "Syntax: python run_10x10cv.py <dataset_name> <strategy> <generation> <seed>"
    # exit()
    individual = False
else:
    dataset = sys.argv[1]
    strategy = sys.argv[2]
    generation = sys.argv[3]
    seed = sys.argv[4]
    individual = True

datasets_path = "datasets"
repetitions = 10

conn = sqlite3.connect('%s/scripts/results.db' % os.environ['AUTOWEKA_PATH'])
c = conn.cursor()

if individual:
    sql_call = "SELECT * FROM results WHERE dataset='%s' \
               AND strategy='%s' AND generation='%s' AND seed=%s \
               AND (full_cv_error=0.0 or full_cv_error is NULL)" % (
        dataset, strategy, generation, seed)
else:
    sql_call = "SELECT * FROM results WHERE full_cv_error=0.0"

c.execute(sql_call)

results = c.fetchall()
if len(results) < 1:
    print "No results"

for result in results:
    print result[0], result[1], result[2], result[3]
    config = result[-1]
    if strategy == 'DEFAULT':
        config = '-F weka.filters.AllFilter -W weka.classifiers.functions.%s' % config

    # run 10x10 CV in weka.classifiers.meta.MyFilteredClassifier using that config
    trainfile = c.execute("SELECT train FROM datasets WHERE name = '%s'" % result[0]).fetchone()[0]
    trainfile = join(datasets_path, trainfile)
    rmse = []
    for seed in range(0, repetitions):
        weka_call = "$MY_JAVA_PATH/java -Xmx2000M -cp autoweka.jar weka.classifiers.meta.MyFilteredClassifier -s %d -o -t %s %s" % (
            seed, trainfile, config)
        pipes_call = "| grep 'Root mean squared error' | tail -1 | awk '{print $5}'"
        command = "cd $AUTOWEKA_PATH && %s %s" % (weka_call, pipes_call)
        # print command
        output = subprocess.check_output(command, shell=True)
        try:
            local_rmse = float(output.rstrip())
            print seed, " ", local_rmse
            rmse.append(local_rmse)
        except:
            print "[ERROR]: %s" % output

    if rmse:
        try:
            rmse = median(rmse)
        except:
            rmse = 'NULL'
    else:
        rmse = 'NULL'

    print "Total = ", rmse
    # update 10x10cv-error field in database
    c.execute(
        "UPDATE results SET full_cv_error=%s WHERE dataset='%s' AND strategy='%s' AND generation='%s' AND seed=%s" % (
            rmse, result[0], result[1], result[2], result[3]))

conn.commit()
# print c.execute("SELECT * FROM results WHERE full_cv_error>0").fetchall()
conn.close()
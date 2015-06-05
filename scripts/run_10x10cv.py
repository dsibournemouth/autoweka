import sqlite3
from os.path import join
import subprocess
import sys
from numpy import median


if len(sys.argv)<5:
	print "Syntax: python run_10x10cv.py <dataset_name> <strategy> <generation> <seed>"
	exit()

dataset = sys.argv[1]
strategy = sys.argv[2]
generation = sys.argv[3]
seed = sys.argv[4]

datasets_path = "$AUTOWEKA_PATH/datasets"
repetitions = 10

conn = sqlite3.connect('/home/msalvador/autoweka/scripts/results.db')
c = conn.cursor()

c.execute("SELECT * FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s' AND seed=%s" % (dataset, strategy, generation, seed))

results = c.fetchall()
if len(results)<1:
	print "No results"

for result in results:
    print result[0], result[1], result[2], result[3]
    config = result[-1]
    # run 10x10 CV in weka.classifiers.meta.MyFilteredClassifier using that config
    trainfile = c.execute("SELECT train FROM datasets WHERE name = '%s'" % result[0]).fetchone()[0]
    trainfile = join(datasets_path, trainfile)
    rmse = [] 
    for seed in range(0,repetitions):
        weka_call = "$MY_JAVA_PATH/java -Xmx5000M -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.meta.MyFilteredClassifier -s %d -o -t %s %s" % (seed, trainfile, config)
	pipes_call = "| grep 'Root mean squared error' | tail -1 | awk '{print $5}'"
        command = "%s %s" % (weka_call, pipes_call)
	output = subprocess.check_output(command, shell=True)
	try:
	    local_rmse = float(output.rstrip())
	    print seed, " ", local_rmse
	    rmse.append(local_rmse)
	except:
	    print "[ERROR]: %s" % output
    rmse = median(rmse)
    print "Total = ", rmse
    # update 10x10cv-error field in database
    c.execute("UPDATE results SET full_cv_error=%f WHERE dataset='%s' AND strategy='%s' AND generation='%s' AND seed=%s" % (rmse, result[0], result[1], result[2], result[3]))

conn.commit()
#print c.execute("SELECT * FROM results WHERE full_cv_error>0").fetchall()
conn.close()

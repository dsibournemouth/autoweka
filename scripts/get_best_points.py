import os
import sys
from os import listdir, system
from os.path import isfile, isdir, join

if len(sys.argv) < 2:
    mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
    experiments = [f for f in listdir(mypath) if isdir(join(mypath, f))]
else:
    experiments = [sys.argv[1]]

individual = True

if individual:
    for e in experiments:
        for s in range(0, 25):
            filepath = "%s/experiments/%s/%s.trajectories.%d" % (os.environ['AUTOWEKA_PATH'], e, e, s)
            if isfile(filepath) and "RAND" not in filepath:
                system("cd %s && %s/java -cp autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV %s" % (
                    os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], filepath))
else:
    for e in experiments:
        system("cd %s && %s/java -cp autoweka.jar autoweka.TrajectoryMerger %s/experiments/%s > /dev/null" % (
            os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], os.environ['AUTOWEKA_PATH'], e))
        filepath = "%s/experiments/%s/%s.trajectories" % (os.environ['AUTOWEKA_PATH'], e, e)
        if isfile(filepath) and "RAND" not in filepath:
            system("cd %s && %s/java -cp autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV %s" % (
                os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], filepath))

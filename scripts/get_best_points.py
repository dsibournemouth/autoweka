import os
from os import listdir,system
from os.path import isdir, join
mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
experiments = [ f for f in listdir(mypath) if isdir(join(mypath,f)) ]

individual = True

if individual:
	for e in experiments:
		for s in range(0,25):
			system("cd %s && %s/java -cp autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV %s/experiments/%s/%s.trajectories.%d" % (os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], os.environ['AUTOWEKA_PATH'], e,e,s))
else:
	for e in experiments:
		system("cd %s && %s/java -cp %s/autoweka.jar autoweka.TrajectoryMerger %s/experiments/%s > /dev/null" % (os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], os.environ['AUTOWEKA_PATH'],e))
		system("cd %s && %s/java -cp %s/autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV %s/experiments/%s/%s.trajectories" % (os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], os.environ['AUTOWEKA_PATH'],e,e))

from os import listdir,system
from os.path import isdir, join
mypath = './experiments'
experiments = [ f for f in listdir(mypath) if isdir(join(mypath,f)) ]
for e in experiments:
	system("java -cp autoweka.jar autoweka.TrajectoryMerger experiments/%s > /dev/null" % e)
	system("java -cp autoweka.jar autoweka.tools.GetBestFromTrajectoryGroupCSV experiments/%s/%s.trajectories" % (e,e))

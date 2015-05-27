import os
from os import listdir,system
from os.path import isdir, join
mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
experiments = [ f for f in listdir(mypath) if isdir(join(mypath,f)) ]
for e in experiments:
	system("python extract_points.py %s/experiments/%s" % (os.environ['AUTOWEKA_PATH'], e))

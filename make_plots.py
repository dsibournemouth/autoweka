from os import listdir,system
from os.path import isdir, join
mypath = './experiments'
experiments = [ f for f in listdir(mypath) if isdir(join(mypath,f)) ]
for e in experiments:
	system("python extract_points.py experiments/%s" % e)

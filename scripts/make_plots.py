import os
from os import listdir,system
from os.path import isfile, isdir, join

#mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
#experiments = [ f for f in listdir(mypath) if isdir(join(mypath,f)) ]
#for e in experiments:
#	folder = "%s/experiments/%s" % (os.environ['AUTOWEKA_PATH'], e)
#	if isfile(join(mypath,"%s.trajectories.0" % e)):
#		system("python extract_points.py %s" % folder)

datasets = ['absorber', 'catalyst_activation', 'debutanizer', 'oxeno-hourly', 'sulfur', 'IndustrialDrier', 'ThermalOxidizerConv']
for d in datasets:
	system("python boxplot.py %s" % d)

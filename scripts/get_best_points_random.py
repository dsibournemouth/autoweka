import os
from os import listdir,system
from os.path import isfile, isdir, join
mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
experiments = [ f for f in listdir(mypath) if isdir(join(mypath,f)) ]

"""
for e in experiments:
	if "RAND" in e:
		for seed in range(0,25):
			filepath = "%s/experiments/%s/out/logs/%d.log" % (os.environ['AUTOWEKA_PATH'], e, seed)
			if isfile(filepath):
				cmd = "grep -E 'SubProcessWrapper.*Score.*' %s | tr '(' ' ' | tr ')' ' ' | awk '{ print $5 }' | sed s/\.0E/00000000000/g | sed s/Infinity/10000000000010/g | sort -n | head -1 | xargs echo '%s,%d,'" % (filepath, e, seed)
				os.system(cmd)
"""
import xml.etree.ElementTree as ET
def parse_random_point(filename):
	try:
		e = ET.parse(filename).getroot()
		config = e.find('argstring').text
		num_folds = 0
		rmse = 0
		rmse_test = 0
		for instance_result in e.findall('instanceResult'):
			fold = instance_result.find('instance').text
			if fold!='default':
				rmse += float(instance_result.find('error').text)
				num_folds += 1
			else:
				rmse_test = float(instance_result.find('error').text)
		rmse /= num_folds
	except:
		rmse = 1000000
		rmse_test = 1000000
		config = ''
	#print rmse
	#return {'error': rmse, 'configuration': config}
	return (rmse, rmse_test, config)

from heapq import heappush, nsmallest, heappop
for e in experiments:
	if "RAND" in e:
		points_path = "%s/experiments/%s/points" % (os.environ['AUTOWEKA_PATH'], e)
		best_results = []
		if isdir(points_path):
			points_folders = [ f for f in listdir(points_path) if isdir(join(points_path,f)) ]
			for folder in points_folders:
				tmp_folder = join(points_path, folder)
				if isdir(tmp_folder):
					points = [f for f in listdir(tmp_folder) if isfile(join(tmp_folder,f))]
					for point in points:
						result = parse_random_point(join(tmp_folder, point))
						heappush(best_results, result)
						best_results = nsmallest(25, best_results)

		count = 0
		for i in range(0,len(best_results)):
			result = heappop(best_results)
			#print "%s, %d, %f, %s" % (e, i, result[0], result[1])
			print "%s, %d, 1, 0, 0, 0, 0, %f, %f, %s" % (e, i, result[0], result[1], result[2])

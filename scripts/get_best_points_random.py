from __future__ import print_function
import os
import sys
from os import listdir, system
from os.path import isfile, isdir, join
import subprocess
import xml.etree.ElementTree as ET
from heapq import heappush, nsmallest, heappop

"""
for e in experiments:
	if "RAND" in e:
		for seed in range(0,25):
			filepath = "%s/experiments/%s/out/logs/%d.log" % (os.environ['AUTOWEKA_PATH'], e, seed)
			if isfile(filepath):
				cmd = "grep -E 'SubProcessWrapper.*Score.*' %s | tr '(' ' ' | tr ')' ' ' | awk '{ print $5 }' | sed s/\.0E/00000000000/g | sed s/Infinity/10000000000010/g | sort -n | head -1 | xargs echo '%s,%d,'" % (filepath, e, seed)
				os.system(cmd)
"""


def info(*objs):
    print("INFO: ", *objs, file=sys.stderr)


def parse_random_point(filename):
    try:
        e = ET.parse(filename).getroot()
        config = e.find('argstring').text
        num_folds = 0
        rmse = 0
        rmse_test = 0
        for instance_result in e.findall('instanceResult'):
            fold = instance_result.find('instance').text
            if fold != 'default':
                rmse += float(instance_result.find('error').text)
                num_folds += 1
            else:
                rmse_test = float(instance_result.find('error').text)
        rmse /= num_folds
    except:
        rmse = 1000000
        rmse_test = 1000000
        config = ''

    return (rmse, rmse_test, config)


def get_seed(tmp_folder, point):
    point_hash = point.split(".")[0]
    command = "cd %s && grep '%s' * | tr '.' ' ' | awk '{print $1}'" % (tmp_folder, point_hash)
    output = subprocess.check_output(command, shell=True)
    return int(output.rstrip())


def main():
    mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
    experiments = [f for f in listdir(mypath) if isdir(join(mypath, f))]

    for e in experiments:
        if "RAND" in e:
            info(e)
            points_path = "%s/experiments/%s/points" % (os.environ['AUTOWEKA_PATH'], e)
            logs_path = "%s/experiments/%s/out/hashes" % (os.environ['AUTOWEKA_PATH'], e)
            if not isdir(points_path):
                info("!!!! No points for this experiment: %s" % e)
                continue

            total_points = dict()
            best_results = dict()

            points_folders = [f for f in listdir(points_path) if isdir(join(points_path, f))]
            for folder in points_folders:
                tmp_folder = join(points_path, folder)
                if isdir(tmp_folder):
                    points = [f for f in listdir(tmp_folder) if isfile(join(tmp_folder, f))]

                    for point in points:
                        result = parse_random_point(join(tmp_folder, point))
                        seed = get_seed(logs_path, point)

                        if seed not in best_results.keys():
                            best_results[seed] = result
                        elif result[0] < best_results[seed][0]:
                            best_results[seed] = result

                        if seed not in total_points.keys():
                            total_points[seed] = 1
                        else:
                            total_points[seed] += 1

                            # heappush(best_results, result)
                            # best_results = nsmallest(25, best_results)  # keep only the 25 best results

            # for i in range(0, len(best_results)):
            # result = heappop(best_results)
            for seed in best_results.keys():
                print("%s, %d, 1, %d, 0, 0, 0, %f, %f, %s" % (
                    e, seed, total_points[seed], best_results[seed][0], best_results[seed][1], best_results[seed][2]))


if __name__ == "__main__":
    main()

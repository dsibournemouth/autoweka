from __future__ import print_function
import argparse
import os
import sys
from os import listdir
from os.path import isfile, isdir, join
import subprocess
import xml.etree.ElementTree as ET
from config import *


def info(*objs):
    print("INFO: ", *objs, file=sys.stderr)


def parse_random_point(filename):
    num_folds = 0

    try:
        e = ET.parse(filename).getroot()
        config = e.find('argstring').text
        rmse = 0
        rmse_test = 0
        for instance_result in e.findall('instanceResult'):
            fold = instance_result.find('instance').text
            if fold != 'default':
                rmse += float(instance_result.find('error').text)
                num_folds += 1
            else:
                rmse_test = float(instance_result.find('error').text)

        if num_folds != NUM_FOLDS_CONFIG:
            raise Exception("Not enough folds: skipping configuration '%s'" % filename)

        rmse /= num_folds
    except:
        rmse = 1000000
        rmse_test = 1000000
        config = ''

    return {'error': rmse, 'test_error': rmse_test, 'config': config, 'num_evaluations': num_folds}


def get_seed(tmp_folder, point):
    point_hash = point.split(".")[0]
    command = "cd %s && grep '%s' * | tr '.' ' ' | awk '{print $1}'" % (tmp_folder, point_hash)
    output = subprocess.check_output(command, shell=True)
    try:
        seed = int(output.rstrip())
    except:
        info("!!!! Seed not found for point: %s" % point_hash)
        seed = -1
    return seed


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    parser.add_argument('--skip-hashes', action='store_true')

    args = parser.parse_args()

    if not args.skip_hashes:
        info("Extracting hashes...")
        subprocess.call("./extract_hashes.sh", shell=True)

    mypath = '%s/%s' % (os.environ['AUTOWEKA_PATH'], experiments_folder)
    experiments = [f for f in listdir(mypath) if isdir(join(mypath, f))]

    for e in experiments:
        if "RAND" in e:
            info(e)
            dataset = e.split('.')[0]
            if dataset not in datasets:
                continue

            points_path = "%s/%s/%s/points" % (os.environ['AUTOWEKA_PATH'], experiments_folder, e)
            logs_path = "%s/%s/%s/out/hashes" % (os.environ['AUTOWEKA_PATH'], experiments_folder, e)
            if not isdir(points_path):
                info("!!!! No points for this experiment: %s" % e)
                continue

            total_evaluations = dict()
            best_results = dict()

            points_folders = [f for f in listdir(points_path) if isdir(join(points_path, f))]
            for folder in points_folders:
                tmp_folder = join(points_path, folder)
                if isdir(tmp_folder):
                    points = [f for f in listdir(tmp_folder) if isfile(join(tmp_folder, f))]

                    for point in points:
                        result = parse_random_point(join(tmp_folder, point))
                        seed = get_seed(logs_path, point)

                        if (seed not in best_results.keys()) or (result['error'] < best_results[seed]['error']):
                            best_results[seed] = result

                        if seed not in total_evaluations.keys():
                            total_evaluations[seed] = result['num_evaluations']
                        else:
                            total_evaluations[seed] += result['num_evaluations']

            for seed in best_results.keys():
                print("%s, %d, 1, %d, 0, 0, 0, %f, %f, %s" % (
                    e, seed, total_evaluations[seed], best_results[seed]['error'], best_results[seed]['test_error'],
                    best_results[seed]['config']))


if __name__ == "__main__":
    main()

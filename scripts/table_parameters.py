import os
import argparse
from os import listdir
from os.path import isfile, isdir, join
from config import *

def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    params_folder = '%s/params' % os.environ['AUTOWEKA_PATH']

    individual_folders = [f for f in listdir(params_folder) if isdir(join(params_folder, f))]

    params = dict()

    for folder in individual_folders:
        param_files = [f for f in listdir(join(params_folder, folder)) if isfile(join(params_folder, folder, f))]

        for method in param_files:
            params[method] = {"numerical": 0, "categorical": {"simple": 0, "complex": 0}}
            filename = join(params_folder, folder, method)
            f = open(filename, 'r')
            for line in f:
                tokens = line.rstrip().split(" ")
                if len(tokens) < 2:
                    break

                if tokens[1].startswith('['):
                    params[method]["numerical"] += 1
                elif tokens[1].startswith('{'):
                    if 'weka' in tokens[1]:
                        params[method]["categorical"]["complex"] += 1
                    else:
                        params[method]["categorical"]["simple"] += 1
            f.close()

    print "method & numerical & categorical (simple:complex)"
    for method in params.keys():
        print "%s & %d & %d:%d" % (
        method.replace('.params', ''), params[method]["numerical"], params[method]["categorical"]["simple"],
        params[method]["categorical"]["complex"])
        
if __name__ == "__main__":
    main()

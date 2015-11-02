import argparse
import os
from config import *

parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
parser.add_argument('--template', required=True)
parser.add_argument('--dataset', choices=datasets, required=False)

args = parser.parse_args()

template_file = args.template
# override default values
selected_datasets = [args.dataset] if args.dataset else datasets

f_input = open(template_file, 'r')
template = f_input.read()

for d in selected_datasets:
    print d
    f_output_name = '%s/%s/%s.batch' % (os.environ['AUTOWEKA_PATH'], experiments_folder, d)
    f_output = open(f_output_name, 'w')
    clean = template.replace("{DATASET}", d)
    f_output.write(clean)
    f_output.close()
    os.system("cd $AUTOWEKA_PATH && $MY_JAVA_PATH/java -cp autoweka.jar autoweka.ExperimentConstructor %s" %
              f_output_name)

f_input.close()

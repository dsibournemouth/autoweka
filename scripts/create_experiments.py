import os
import sys
from datasets import datasets

if len(sys.argv) < 2:
    print 'Syntax: python create_experiments.py experiments/template.xml'
    exit(1)

f_input = open(sys.argv[1], 'r')
template = f_input.read()

for d in datasets:
    print d
    f_output_name = '%s/experiments/%s.batch' % (os.environ['AUTOWEKA_PATH'], d)
    f_output = open(f_output_name, 'w')
    clean = template.replace("{DATASET}", d)
    f_output.write(clean)
    f_output.close()
    os.system("cd $AUTOWEKA_PATH && $MY_JAVA_PATH/java -cp autoweka.jar autoweka.ExperimentConstructor %s" %
              f_output_name)

f_input.close()

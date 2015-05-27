import os, sys
if len(sys.argv)<2:
  print 'Syntax: python create_experiments.py experiments/template.xml'
  exit(1)
datasets = ['absorber', 'catalyst_activation', 'debutanizer', 'oxeno-hourly', 'sulfur', 'IndustrialDrier', 'ThermalOxidizerConv']
for d in datasets:
   print d
   f_input  = open(sys.argv[1], 'r')
   f_output_name = '%s/experiments/%s.batch' % (os.environ['AUTOWEKA_PATH'], d)
   f_output = open(f_output_name, 'w')
   clean  = f_input.read().replace("{DATASET}", d)
   f_output.write(clean)
   f_output.close()
   os.system("cd %s && %s/java -cp autoweka.jar autoweka.ExperimentConstructor %s" % (os.environ['AUTOWEKA_PATH'], os.environ['MY_JAVA_PATH'], f_output_name))

f_input.close()

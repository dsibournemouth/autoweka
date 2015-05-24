import os, sys
if len(sys.argv)<2:
  print 'Syntax: python create_experiments.py experiments/template.xml'
  exit(1)
datasets = ['absorber', 'catalyst_activation', 'debutanizer', 'oxeno-hourly', 'sulfur', 'IndustrialDrier', 'ThermalOxidizerConv']
for d in datasets:
   print d
   f_input  = open(sys.argv[1], 'r')
   f_output_name = 'experiments/%s.batch' % d
   f_output = open(f_output_name, 'w')
   clean  = f_input.read().replace("{DATASET}", d)
   f_output.write(clean)
   f_output.close()
   os.system("/usr/lib/jvm/j2sdk1.8-oracle/bin/java -cp autoweka.jar autoweka.ExperimentConstructor %s" % f_output_name)

f_input.close()

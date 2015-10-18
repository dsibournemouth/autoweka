from os import system
from config import *

system("python table_index.py")
system("python table_configurations.py")
for d in datasets:
    system("python table_strategies.py --dataset=%s" % d)          
    

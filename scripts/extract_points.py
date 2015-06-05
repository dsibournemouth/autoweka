import sys
import os
from os import path
import xml.etree.ElementTree as ET
import glob

if len(sys.argv)<2:
  print 'Syntax: python extract_points.py <experiment_folder>'

all_points = []
all_time = []
all_error = []
all_seed = []
time_by_seed = [[] for i in range(25)]
error_by_seed = [[] for i in range(25)]
folder = sys.argv[1]
os.chdir(folder)
all_trajectories = glob.glob("*trajectories*")
if len(all_trajectories)==0:
   print "No trajectories found"
   exit(0)
print 'Parsing: ', folder
for file in all_trajectories:
   #print 'Parsing: ', file
   e = ET.parse(file).getroot()
   traj = e.find('trajectories')
   seed = int(traj.find('seed').text) # should be only 1 element
   time_by_seed[seed] = []
   error_by_seed[seed] = []

   for point in traj.findall('point'):
      time = float(point.find('time').text)/60/60 # change to hours
      binned_time = int(float(point.find('time').text)/60/60)
      error = float(point.find('errorEstimate').text)
      if error<100:
        all_points.append([time,binned_time,error,seed])
        all_time.append(time)
        all_error.append(error)
        all_seed.append(seed)
        time_by_seed[seed].append(time)
        error_by_seed[seed].append(error)


import numpy as np
from operator import itemgetter, attrgetter, methodcaller
sorted_points = np.array(sorted(all_points, key=itemgetter(0)))
sorted_time = sorted_points[:,0]
sorted_binned_time = sorted_points[:,1]
sorted_errors = sorted_points[:,2]

def movingaverage(interval, window_size):
    window = np.ones(int(window_size))/float(window_size)
    return np.convolve(interval, window, 'same')

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import matplotlib.cm as cm
#import seaborn as sns
from pandas import DataFrame
import pandas.stats.moments as stats

#sns.set_style("whitegrid")

#colors = ['r', 'g', 'b']
#fig = plt.figure()
fig, ax1 = plt.subplots(1, 1, sharex=True)
#title = folder.split("/")[1].split("-")[0]
title = folder.split("/")[-1]
print title
ax1.set_title(title)
#ax.set_xlabel('Time (h)')
#ax.set_ylabel('RMSE')
#ax.set_yscale('log')
#ax.set_xlim(0,30)
#colors = sns.color_palette("husl", 25)
#for i in range(0,25):
   #ax.scatter(time_by_seed[i], error_by_seed[i], c=cm.hsv(i/25.,1), s=[30]*len(time_by_seed[i]))
   #ax.scatter(time_by_seed[i], error_by_seed[i], c=[colors[i]]*len(time_by_seed[i]), s=[30]*len(time_by_seed[i]))

ax1.set_xlabel('Time (h)')
ax1.set_ylabel('RMSE')
ax1.set_xlim(-1,30)
y_mean = stats.rolling_mean(sorted_errors, 5)
y_std = stats.rolling_std(sorted_errors, 5)
#y_upper = y_mean + 2*y_std
y_upper = stats.rolling_max(sorted_errors, 5)
#y_lower = y_mean - 2*y_std
y_lower = stats.rolling_min(sorted_errors, 5)
sorted_data = DataFrame(data=sorted_points, columns=['time', 'binned_time', 'error', 'seed'])
#sns.jointplot("binned_time", "error", sorted_data)
#ax1.scatter(sorted_binned_time, sorted_errors)
ax1.plot(sorted_time, y_mean, color="red", label="Rolling mean")
#ax1.errorbar(sorted_binned_time, sorted_errors, marker='o', ms=8, yerr=3*y_std, ls='dotted', label="Rolling mean")
ax1.legend()
ax1.fill_between(sorted_time, y_mean, y_upper, facecolor='gray', interpolate=True, alpha=0.5)
ax1.fill_between(sorted_time, y_lower, y_mean, facecolor='gray', interpolate=True, alpha=0.5)
if not os.path.isdir("plots"):
   os.mkdir("plots")
fig.savefig("plots/points.png", bbox_inches='tight')
fig.savefig("%s/plots/points-%s.png" % (os.environ['AUTOWEKA_PATH'], title), bbox_inches='tight')
#plt.show()



# from pylab import *
# fig2 = plt.figure()
# ax2 = fig2.add_subplot(1, 1, 1)
# colors = sns.color_palette("husl", 25)
# sorted_data = DataFrame(data=sorted_points, columns=['time', 'error', 'seed'])
# errors_av = movingaverage(sorted_errors, 10)
# plot(sort(all_time), errors_av)
# scatter(all_time, all_error)
# ax2.set_title(folder)
# ax2.set_xlabel('Time (h)')
# ax2.set_ylabel('RMSE')
# ax2.set_xlim(0,30)
# #if not os.path.isdir("plots"):
# #   os.mkdir("plots")
# fig2.savefig("plots/logistic.png", bbox_inches='tight')

# import numpy as np
# sns.set(style="darkgrid", palette="Set2")
# sns.axlabel("Time (h)", "RMSE")
# 
# sorted_data = DataFrame(data=sorted_points, columns=['time', 'error', 'seed'])
# sorted_data.error.plot()
# plt.show()

#data = DataFrame(data=all_points, columns=['time', 'error', 'seed'])
#print data["time"]

#f = sns.pointplot("time", "error", "seed", data=data, join=False)
#f = sns.pointplot(all_time, all_error, all_seed,  join=False)
#sns.regplot("time", "error", data=data, logistic=True)
#ax = f.axes
#x_ticks = ax.xaxis.get_majorticklocs()
#ax.xaxis.set_ticks(np.arange(min(x_ticks), max(x_ticks), 10))
#ax.xaxis.set_major_formatter(ticker.FormatStrFormatter('%0.0f'))
#sns.tsplot(sorted_points, err_style="boot_traces", n_boot=500, ci=95)
#x = np.dstack([error_by_seed[s] for s in range(0,25)])
#sns.tsplot(data, time="time", value="error")
#savefig("plots/points.png")
#sns.plt.show()

import os
import sys
import glob
import sqlite3
import xml.etree.ElementTree as ET
from os import listdir
from os.path import isdir, join
import subprocess


def create_table():
    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    c.execute('DROP TABLE trajectories')

    c.execute('''CREATE TABLE trajectories
                 (dataset, strategy, generation, seed, time, error, configuration,
                  FOREIGN KEY(dataset) REFERENCES experiments(dataset),
                  FOREIGN KEY(strategy) REFERENCES experiments(strategy),
                  FOREIGN KEY(generation) REFERENCES experiments(generation),
                  UNIQUE(dataset, strategy, generation, seed, time) ON CONFLICT REPLACE
                  )''')

    conn.commit()
    conn.close()


def parse_trajectories(folder, c):
    os.chdir(folder)

    all_trajectories = glob.glob("*trajectories*")
    if len(all_trajectories) == 0:
        print "No trajectories found"
        return

    print 'Parsing: ', folder

    for file in all_trajectories:
        # file = absorber.TPE.CV-absorber.trajectories.0
        tmp = file.split('.')
        dataset = tmp[0]
        strategy = tmp[1]
        generation = tmp[2].split('-')[0]

        e = ET.parse(file).getroot()
        traj = e.find('trajectories')
        if traj is not None:
            seed = int(traj.find('seed').text)  # should be only 1 element
            # numEvaluatedEvaluations = traj.find('numEvaluatedEvaluations').text
            # numMemOutEvaluations = traj.find('numMemOutEvaluations').text
            # numTimeOutEvaluations = traj.find('numTimeOutEvaluations').text

            for point in traj.findall('point'):
                time = float(point.find('time').text)
                error = float(point.find('errorEstimate').text)
                configuration = point.find('args').text

                # transform autoweka config to weka config
                command = '$MY_JAVA_PATH/java -Xmx2000M -cp $AUTOWEKA_PATH/autoweka.jar autoweka.WekaArgumentConverter "%s"' % (
                    configuration.lstrip().rstrip())
                output = subprocess.check_output(command, shell=True)
                configuration = output.rstrip()

                sql_query = '''INSERT OR REPLACE INTO trajectories(
                                 dataset, strategy, generation, seed, time, error, configuration)
                                 VALUES ('%s', '%s', '%s', %d, %f, %f, '%s')''' % (
                    dataset, strategy, generation, seed, time, error, configuration)
                c.execute(sql_query)


def main():
    if len(sys.argv) > 1:
        create_table()

    conn = sqlite3.connect('results.db')
    c = conn.cursor()

    mypath = '%s/experiments' % os.environ['AUTOWEKA_PATH']
    experiments = [f for f in listdir(mypath) if isdir(join(mypath, f))]
    for e in experiments:
        folder = "%s/%s" % (mypath, e)
        parse_trajectories(folder, c)

    conn.commit()
    conn.close()


if __name__ == "__main__":
    main()

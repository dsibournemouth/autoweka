import sqlite3
import argparse
from pylab import *
from config import *


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    strategy = 'SMAC'

    for dataset in datasets:
        try:
            sql_CV = "SELECT seed, full_cv_error FROM results WHERE dataset='%s' AND strategy='%s' AND generation='CV' ORDER BY seed" % (
                dataset, strategy)
            sql_DPS = "SELECT seed, full_cv_error FROM results WHERE dataset='%s' AND strategy='%s' AND generation='DPS' ORDER BY seed" % (
                dataset, strategy)

            results_CV = c.execute(sql_CV).fetchall()
            results_DPS = c.execute(sql_DPS).fetchall()

            print "Results CV: ", len(results_CV)
            print "Results DPS: ", len(results_DPS)

            if len(results_CV) != len(results_DPS):
                raise Exception("[ERROR] Different number of seeds!!!")

            points = []
            for seed in range(0, 25):
                if results_CV[seed][0] == results_DPS[seed][0]:
                    points.append((results_CV[seed][1], results_DPS[seed][1]))
                else:
                    print "[ERROR] Different seeds"

            x = [p[0] for p in points]
            y = [p[1] for p in points]

            plot(x, y, 'o')
            title('%s / %s ' % (dataset, strategy))
            xlabel('RMSE on CV')
            ylabel('RMSE on DPS')

            max_value = max(xlim()[1], ylim()[1])
            min_value = min(xlim()[0], ylim()[0])

            xlim(min_value, max_value)
            ylim(min_value, max_value)

            # Plot your initial diagonal line based on the starting
            # xlims and ylims.
            diag_line, = plot(xlim(), ylim(), ls="--", c=".3")
            print "Saving %s / %s" % (dataset, strategy)
            savefig('../plots%s/comparison.%s.%s.png' % (suffix, dataset, strategy))
            clf()
            # plt.show()
        except Exception, e:
            print e

    conn.close()
    
if __name__ == "__main__":
    main()

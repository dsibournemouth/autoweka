import os
import argparse
import sqlite3
from pylab import *  # includes np and plt
from config import *
import matplotlib.cm as cm


def comparison_plot(results_CV, title):
    #results_CV = np.array(results_CV)
    # dataset, strategy, seed, error, test_error
    points = []
    strategies = []
    for r in results_CV:

        dataset = r[0]
        strategy = r[1]
        seed = r[2]
        try:
            cv_error = float(r[3])
            test_error = float(r[4])
            if cv_error is not None and test_error is not None:
                points.append([cv_error, test_error])
                strategies.append(strategy)
        except:
            pass

    if not points:
        raise Exception("[ERROR] No points for %s" % title)

    points = np.array(points)
    #points = np.log10(points)  # logscale to better visualization
    strategies = np.array(strategies, dtype=object)
    color_mask_def = np.equal(strategies, 'DEFAULT')
    color_mask_rand = np.equal(strategies, 'RAND')
    color_mask_smac = np.equal(strategies, 'SMAC')
    color_mask_tpe = np.equal(strategies, 'TPE')

    plt.plot(points[color_mask_def, 0], points[color_mask_def, 1], 'o', c='r', alpha=.5, label='WEKA-DEF')
    plt.plot(points[color_mask_rand, 0], points[color_mask_rand, 1], '>', c='b', alpha=.5, label='RAND')
    plt.plot(points[color_mask_smac, 0], points[color_mask_smac, 1], '*', c='y', alpha=.5, label='SMAC')
    plt.plot(points[color_mask_tpe, 0], points[color_mask_tpe, 1], 'D', c='g', alpha=.5, label='TPE')
    plt.title(title)
    plt.xlabel('% CV error')
    plt.ylabel('% Test error')
    plt.legend(loc=4)

    max_value = 100
    min_value = 0

    plt.xlim(min_value, max_value)
    plt.ylim(min_value, max_value)

    # Plot your initial diagonal line based on the starting xlims and ylims.
    diag_line, = plt.plot(plt.xlim(), plt.ylim(), ls="--", c=".3")

    # plt.show()
    print "Saving %s" % title
    plt.savefig('../plots%s/comparison-cv-test.png' % suffix)
    plt.clf()


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    sql_CV = "SELECT dataset, strategy, seed, error, test_error FROM results WHERE generation='CV'"

    results_CV = c.execute(sql_CV).fetchall()

    conn.close()

    if not results_CV:
        print "Results CV: ", len(results_CV)
        raise Exception("[ERROR] No results")

    this_title = "CV error vs Test error"
    comparison_plot(results_CV, this_title)



if __name__ == "__main__":
    main()

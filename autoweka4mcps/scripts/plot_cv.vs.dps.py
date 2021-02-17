import argparse
import os

from pylab import *  # includes np and plt

from config import *


def comparison_plot(results_CV, results_DPS, title):
    results_CV = np.array(results_CV)
    results_DPS = np.array(results_DPS)

    points = []
    for r in results_CV:
        mask = results_DPS[:, 0] == r[0]
        if np.sum(mask) > 1:
            print mask
            raise Exception("[ERROR] Duplicated seed")

        r_cv = r[1]
        r_dps = results_DPS[mask, 1][0]
        if r_cv is not None and r_dps is not None:
            points.append([r_cv, r_dps])

    if not points:
        raise Exception("[ERROR] No points for %s" % title)

    points = np.array(points)
    points = np.log10(points)  # logscale to better visualization

    color_mask = points[:, 0] > points[:, 1]
    not_color_mask = np.logical_not(color_mask)

    plt.plot(points[color_mask, 0], points[color_mask, 1], 'o', c='r')
    plt.plot(points[not_color_mask, 0], points[not_color_mask, 1], 'o', c='y')
    plt.title(title)
    plt.xlabel('log(RMSE) on CV')
    plt.ylabel('log(RMSE) on DPS')

    max_value = max(plt.xlim()[1], plt.ylim()[1])
    min_value = min(plt.xlim()[0], plt.ylim()[0])

    plt.xlim(min_value, max_value)
    plt.ylim(min_value, max_value)

    # Plot your initial diagonal line based on the starting xlims and ylims.
    diag_line, = plt.plot(plt.xlim(), plt.ylim(), ls="--", c=".3")

    # plt.show()
    print "Saving %s" % title
    plt.savefig('../plots%s/comparison-%s.png' % (suffix, title))
    plt.clf()


def num_evaluations_plot(results_CV, results_DPS, title):
    results_CV = np.array(results_CV)
    results_DPS = np.array(results_DPS)

    points = []
    for r in results_CV:
        mask = results_DPS[:, 0] == r[0]
        if np.sum(mask) > 1:
            print mask
            raise Exception("[ERROR] Duplicated seed")

        r_cv = r[2]
        r_dps = results_DPS[mask, 2][0]
        if r_cv is not None and r_dps is not None:
            points.append([r_cv, r_dps])

    if not points:
        raise Exception("[ERROR] No points for %s" % title)

    points = np.array(points)
    # points = np.log10(points)  # logscale to better visualization

    color_mask = points[:, 0] > points[:, 1]
    not_color_mask = np.logical_not(color_mask)

    plt.plot(points[color_mask, 0], points[color_mask, 1], 'o', c='r')
    plt.plot(points[not_color_mask, 0], points[not_color_mask, 1], 'o', c='y')
    plt.title(title)
    plt.xlabel('Number of evaluations on CV')
    plt.ylabel('Number of evaluations on DPS')

    max_value = max(plt.xlim()[1], plt.ylim()[1])
    min_value = min(plt.xlim()[0], plt.ylim()[0])

    plt.xlim(min_value, max_value)
    plt.ylim(min_value, max_value)

    # Plot your initial diagonal line based on the starting xlims and ylims.
    diag_line, = plt.plot(plt.xlim(), plt.ylim(), ls="--", c=".3")

    # plt.show()
    print "Saving %s" % title
    plt.savefig('../plots%s/num_evaluations-%s.png' % (suffix, title))
    plt.clf()


def main():
    parser = argparse.ArgumentParser(prog=os.path.basename(__file__))
    globals().update(load_config(parser))
    parser.add_argument('--dataset', choices=datasets, required=True)
    parser.add_argument('--strategy', choices=strategies, required=True)

    args = parser.parse_args()

    dataset = args.dataset
    strategy = args.strategy
    title = '%s.%s' % (dataset, strategy)

    results_CV = []
    results_DPS = []

    for error_type in ['error', 'test_error']:
        conn = sqlite3.connect(database_file)
        c = conn.cursor()

        sql_CV = "SELECT seed, %s, num_evaluations FROM results WHERE dataset='%s' AND strategy='%s' AND generation='CV' ORDER BY seed" % (
            error_type, dataset, strategy)
        sql_DPS = "SELECT seed, %s, num_evaluations FROM results WHERE dataset='%s' AND strategy='%s' AND generation='DPS' ORDER BY seed" % (
            error_type, dataset, strategy)

        results_CV = c.execute(sql_CV).fetchall()
        results_DPS = c.execute(sql_DPS).fetchall()

        conn.close()

        if not results_CV or not results_DPS:
            print "Results CV: ", len(results_CV)
            print "Results DPS: ", len(results_DPS)
            raise Exception("[ERROR] No results for dataset='%s' AND strategy='%s'" % (dataset, strategy))

        this_title = "%s.%s" % (title, error_type)
        comparison_plot(results_CV, results_DPS, this_title)

    num_evaluations_plot(results_CV, results_DPS, title)


if __name__ == "__main__":
    main()

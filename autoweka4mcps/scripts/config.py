import importlib
import sqlite3


def load_config(parser):
    parser.add_argument('--config', required=True)
    args, unknown_args = parser.parse_known_args()
    if args.config.endswith('.py'):
        args.config = args.config.replace('.py', '')
    variables = importlib.import_module(args.config).__dict__
    globals().update(variables)
    return variables


def get_results_by_dataset(dataset):
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration, error, test_error, num_evaluations
                 FROM results WHERE dataset='%s'
                 ''' % dataset)
    results = c.fetchall()

    if not results:
        conn.close()
        raise Exception('No results!')

    c.execute('''SELECT seed, error FROM results WHERE dataset='%s' 
                 AND error IS NOT NULL ORDER BY error LIMIT 1''' % dataset)
    try:
        best_error_seed = c.fetchone()[0]
    except:
        best_error_seed = -1

    c.execute('''SELECT seed, test_error FROM results WHERE dataset='%s' 
                 AND test_error IS NOT NULL ORDER BY test_error LIMIT 1''' % dataset)
    try:
        best_test_error_seed = c.fetchone()[0]
    except:
        best_test_error_seed = -1

    conn.close()

    return results, best_error_seed, best_test_error_seed


def get_results_individual(dataset, strategy, generation):
    conn = sqlite3.connect(database_file)
    c = conn.cursor()

    c.execute('''SELECT dataset, strategy, generation, seed, configuration, error, test_error, num_evaluations
                 FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 ''' % (dataset, strategy, generation))
    results = c.fetchall()

    if not results:
        conn.close()
        raise Exception('No results!')

    c.execute('''SELECT seed, error FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 AND error IS NOT NULL ORDER BY error LIMIT 1''' % (dataset, strategy, generation))
    try:
        best_error_seed = c.fetchone()[0]
    except:
        best_error_seed = -1

    c.execute('''SELECT seed, test_error FROM results WHERE dataset='%s' AND strategy='%s' AND generation='%s'
                 AND test_error IS NOT NULL ORDER BY test_error LIMIT 1''' % (dataset, strategy, generation))
    try:
        best_test_error_seed = c.fetchone()[0]
    except:
        best_test_error_seed = -1

    conn.close()

    return results, best_error_seed, best_test_error_seed


def get_results(dataset, strategy, generation):
    if strategy is None or generation is None:
        return get_results_by_dataset(dataset)

    return get_results_individual(dataset, strategy, generation)

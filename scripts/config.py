datasets = [
    'abalone', 'amazon', 'car', 'cifar10', 'cifar10small', 'convex', 'dexter', 'dorothea', 'germancredit', 'gisette',
    'kddcup09appetency', 'krvskp', 'madelon', 'mnist', 'mnistrotationbackimagenew', 'secom',
    'semeion', 'shuttle', 'waveform', 'winequalitywhite', 'yeast'
]

generations = ['CV']

strategies = [#'DEFAULT',
              'RAND',
              'SMAC',
              # 'ROAR',
              'TPE']

number_seeds = 25
seeds = [str(s) for s in range(0, number_seeds)]

methods = [
    'weka.classifiers.bayes.BayesNet',
    'weka.classifiers.bayes.NaiveBayes',
    'weka.classifiers.functions.Logistic',
    'weka.classifiers.functions.MultilayerPerceptron',
    'weka.classifiers.functions.SimpleLogistic',
    'weka.classifiers.lazy.IBk',
    'weka.classifiers.lazy.KStar',
    'weka.classifiers.rules.DecisionTable',
    'weka.classifiers.rules.JRip',
    'weka.classifiers.rules.OneR',
    'weka.classifiers.rules.PART',
    'weka.classifiers.rules.ZeroR',
    'weka.classifiers.trees.DecisionStump',
    'weka.classifiers.trees.J48',
    'weka.classifiers.trees.LMT',
    'weka.classifiers.trees.RandomForest',
    'weka.classifiers.trees.RandomTree',
    'weka.classifiers.trees.REPTree',
    'weka.classifiers.lazy.LWL',
    'weka.classifiers.meta.AdaBoostM1',
    'weka.classifiers.meta.AttributeSelectedClassifier',
    'weka.classifiers.meta.Bagging',
    'weka.classifiers.meta.ClassificationViaRegression',
    'weka.classifiers.meta.LogitBoost',
    'weka.classifiers.meta.MultiClassClassifier',
    'weka.classifiers.meta.MyFilteredClassifier',
    'weka.classifiers.meta.RandomCommittee',
    'weka.classifiers.meta.RandomSubSpace',
    'weka.classifiers.meta.Stacking',
    'weka.classifiers.meta.Vote'
]

is_regression = False
NUM_FOLDS_CONFIG = 10

experiments_folder = 'experiments'
database_file = 'results.db'

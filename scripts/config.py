datasets = [
'abalone', 'amazon', 'car', 'cifar10', 'cifar10small', 'convex', 'dexter', 'dorothea', 'germancredit', 'gisette', 'kddcup09appetency', 
'krvskp', 'madelon', 'mnist', 'mnistrotationbackimagenew', 'secom', 
'semeion', 'shuttle', 'waveform', 'winequalitywhite', 'yeast'
]

generations = ['CV', 'DPS']

strategies = ['DEFAULT',
              'RAND',
              'SMAC',
              # 'ROAR',
              'TPE']

seeds = [str(s) for s in range(0, 25)]

methods = ['LinearRegression', 'MultilayerPerceptron', 'PLSClassifier', 'RBFRegressor', 'SMOreg']

is_regression = False
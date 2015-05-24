from os import system

datasets = ['absorber', 'catalyst_activation', 'debutanizer', 'oxeno-hourly', 'sulfur', 'IndustrialDrier', 'ThermalOxidizerConv']
methods = ['LinearRegression', 'MultilayerPerceptron', 'PLSClassifier', 'RBFRegressor', 'SMOreg']

for d in datasets:
	for m in methods:
		for s in range(0,25):
			command = "qsub -l q=compute /home/msalvador/autoweka/default_experiment.sh %s %s %d" % (d, m, s)
			system(command)


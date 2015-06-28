datasets = ['absorber',
            'catalyst_activation',
            'debutanizer',
            'oxeno-hourly',
            'sulfur',
            'IndustrialDrier',
            'ThermalOxidizerConv']

generations = ['CV', 'DPS']

strategies = ['DEFAULT',
              'RAND',
              'SMAC',
              # 'ROAR',
              'TPE']

seeds = [str(s) for s in range(0, 25)]

methods = ['LinearRegression', 'MultilayerPerceptron', 'PLSClassifier', 'RBFRegressor', 'SMOreg']

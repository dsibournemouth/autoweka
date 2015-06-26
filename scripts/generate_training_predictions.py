from os import system
from datasets import datasets
from strategies import strategies
from generations import generations
from seeds import seeds

for dataset in datasets:
    for strategy in strategies:
        if strategy not in ['DEFAULT', 'RAND']:
            for generation in generations:
                for seed in seeds:
                    d = {"dataset": dataset, "strategy": strategy, "generation": generation}
                    experiment = "{dataset}.{strategy}.{generation}-{dataset}".format(**d)
                    d2 = {"seed": seed, "dataset": dataset, "experiment": experiment}

                    command = '''$MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.meta.MyFilteredClassifier \
                           -l $AUTOWEKA_PATH/experiments/{experiment}/trained.{seed}.model \
                           -T $AUTOWEKA_PATH/datasets/{dataset}-train70perc.arff \
                           -classifications "weka.classifiers.evaluation.output.prediction.CSV \
                           -file $AUTOWEKA_PATH/experiments/{experiment}/training.predictions.{seed}.csv"
                           '''.format(**d2)
                    print command
                    system(command)

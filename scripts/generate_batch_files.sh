# Generate batches
for batch in `seq 1 10`; do $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.filters.unsupervised.instance.RemoveFolds -i test.arff -o test$batch.arff -c last -S -1 -N 10 -F $batch; done

# Copy files from original run
cp -R ../EXPERIMENTS_FOLDER/DATASET.SMAC.CV-DATASET .

# Create scenario.1
cp autoweka.scenario autoweka.scenario.1
vim autoweka.scenario.1
# To change: 
# datasetString
# extraProps
# tunerTimeout


# Duplicate scenarios
for i in `seq 1 8`; do let next=i+1; let next2=i+2; sed "s/test2\./test${next2}\./g" autoweka.scenario.1 | sed "s/test1\./test${next}\./g" > autoweka.scenario.$((i+1)); done

# Create experiment.1
EXPERIMENT=$(echo "${PWD##*/}"); cp $EXPERIMENT.experiment $EXPERIMENT.experiment.1
vim $EXPERIMENT.experiment.1
# To change: 
# name
# datasetString
# callString
# tunerTimeout
# extraProps

# Duplicate experiments
EXPERIMENT=$(echo "${PWD##*/}"); for i in `seq 1 8`; do let next=i+1; let next2=i+2; sed "s/test2\./test${next2}\./g" $EXPERIMENT.experiment.1 | sed "s/test1\./test${next}\./g" | sed "s/scenario\.1/scenario\.${next}/g" | sed "s/batch1/batch${next}/g" | sed "s/adaptive1/adaptive${next}/g" | sed "s/autoweka\/state/adaptive${i}\/state/g" > $EXPERIMENT.experiment.$((i+1)); done

# Run experiments
python launch_experiments.py --config=CONFIG_FILE --dataset=DATASET --strategy=SMAC --batches=10

# Concatenate all predictions
# TODO: extract predictions from batch0 (or maybe run the model on test1.arff?)
for seed in `seq 0 24`; do for i in `seq 1 8`; do cat batch$i/predictions.$seed.csv | grep -v '#' >> final/predictions.$seed.csv; done; cat predictions.$seed.csv | grep -v '#' >> final/predictions.$seed.csv; done

rm final/*; for seed in `seq 0 24`; do for i in `seq 0 9`; do cat batch$i/predictions.$seed.csv | grep -v '#' >> final/predictions.$seed.csv; done; done

# Remove empy lines from prediction files
for seed in `seq 0 24`; do sed -i '/^$/d' final/predictions.$seed.csv; done

# Get the error rate (% missclassified instances)
for seed in `seq 0 24`; do let total=`wc -l final/predictions.$seed.csv | awk {'print $1'}`; let wrong=`grep '+' final/predictions.$seed.csv | wc -l  | awk {'print $1'}`; echo "scale=2; 100*$wrong/$total" | bc -l; done






# Manually parse SMAC trajectories and evaluate models in testing batch
EXPERIMENT=$(echo "${PWD##*/}"); for seed in `seq 0 24`; do for batch in `seq 1 9`; do $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar autoweka.smac.SMACTrajectoryParser -single $PWD $seed -batchNumber $batch && $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar autoweka.TrajectoryPointPredictionRunner $PWD/$EXPERIMENT.trajectories.$seed -savemodel && mv predictions.$seed.csv batch$batch/ && mv trained.$seed.model batch$batch/ && mv $EXPERIMENT.trajectories.$seed batch$batch/; done; done


sed -i 's/test\.arff/test1\.arff/g' batch0/*trajectories*


# Test first batch and save in batch0 folder (classification)
DATASET_PATH="/home/msalvador/autoweka/datasets/catalyst_activation"; for seed in `seq 0 24`; do $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.Evaluation weka.classifiers.misc.SerializedClassifier -T $DATASET_PATH/test1.arff -l batch0/trained.$seed.model -classifications "weka.classifiers.evaluation.output.prediction.CSV -p 0 -file batch0/predictions.$seed.csv"; done

# Test first batch and save in batch0 folder (regression)
DATASET_PATH="/home/msalvador/autoweka/datasets/catalyst_activation"; for seed in `seq 0 24`; do $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.classifiers.Evaluation weka.classifiers.misc.SerializedClassifier -T $DATASET_PATH/test1.arff -l batch0/trained.$seed.model -classifications "weka.classifiers.evaluation.output.prediction.CSV -file batch0/predictions.$seed.csv"; done
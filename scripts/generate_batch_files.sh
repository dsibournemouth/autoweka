
# Duplicate scenarios
for i in `seq 1 8`; do let next=i+1; let next2=i+2; sed "s/test2\./test${next2}\./g" autoweka.scenario.1 | sed "s/test1\./test${next}\./g" > autoweka.scenario.$((i+1)); done

# Duplicate experiments
EXPERIMENT=$(echo "${PWD##*/}"); for i in `seq 1 8`; do let next=i+1; let next2=i+2; sed "s/test2\./test${next2}\./g" $EXPERIMENT.experiment.1 | sed "s/test1\./test${next}\./g" | sed "s/scenario\.1/scenario\.${next}/g" | sed "s/batch1/batch${next}/g" | sed "s/adaptive1/adaptive${next}/g" | sed "s/autoweka\/state/adaptive${i}\/state/g" > $EXPERIMENT.experiment.$((i+1)); done

# Concatenate all predictions
# TODO: extract predictions from batch0 (or maybe run the model on test1.arff?)
for seed in `seq 0 24`; do for i in `seq 1 8`; do cat batch$i/predictions.$seed.csv | grep -v '#' >> final/predictions.$seed.csv; done; cat predictions.$seed.csv | grep -v '#' >> final/predictions.$seed.csv; done

# Get the error rate (% missclassified instances)
for seed in `seq 0 24`; do let total=`wc -l final/predictions.$seed.csv | awk {'print $1'}`; let wrong=`grep '+' final/predictions.$seed.csv | wc -l  | awk {'print $1'}`; echo "scale=2; 100*$wrong/$total" | bc -l; done


# Generate batches
for batch in `seq 1 10`; do $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar weka.filters.unsupervised.instance.RemoveFolds -i test.arff -o test$batch.arff -c last -S -1 -N 10 -F $batch; done

python launch_experiments.py --config=CONFIG_FILE --dataset=DATASET --strategy=SMAC --batches=10



EXPERIMENT=$(echo "${PWD##*/}"); for seed in `seq 0 24`; do for batch in `seq 1 9`; do $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar autoweka.smac.SMACTrajectoryParser -single $PWD $seed -batchNumber $batch && $MY_JAVA_PATH/java -cp $AUTOWEKA_PATH/autoweka.jar autoweka.TrajectoryPointPredictionRunner $PWD/$EXPERIMENT.trajectories.$seed -savemodel && mv predictions.$seed.csv batch$batch/ && mv trained.$seed.model batch$batch/ && mv $EXPERIMENT.trajectories.$seed batch$batch/; done; done



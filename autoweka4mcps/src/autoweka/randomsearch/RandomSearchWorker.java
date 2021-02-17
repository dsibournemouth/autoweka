package autoweka.randomsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import autoweka.ClassParams;
import autoweka.Experiment;
import autoweka.InstanceGenerator;
import autoweka.Parameter;
import autoweka.SubProcessWrapper;
import autoweka.Trajectory;
import autoweka.Trajectory.Point;
import autoweka.TrajectoryGroup;
import autoweka.Util;

class RandomSearchWorker
{
    String mSeed;
    Random mRand;
    float mTimeRemaining;
    Experiment mExperiment;
    ClassParams mParams;
    File mExperimentDir;
    List<String> mInstances;
    Trajectory trajectory;
    TrajectoryGroup trajectoryGroup;

    public static void main(String[] args){
        RandomSearchWorker worker = new RandomSearchWorker(new File(args[0]).getAbsoluteFile().getParentFile(), Experiment.fromXML(args[0]), args[1]);
        worker.run();
    }

    public RandomSearchWorker(File experimentDir, Experiment experiment, String seed)
    {
        mSeed = seed;
        mRand = new Random(Integer.parseInt(seed));

        mInstances = new ArrayList<String>();
        for(String s: InstanceGenerator.create(experiment.instanceGenerator, "__dummy__").getAllInstanceStrings(experiment.instanceGeneratorArgs)){
            mInstances.add(s);
        }
        mInstances.add("default");

        mExperiment = experiment;
        mExperimentDir = experimentDir;
        mTimeRemaining = experiment.tunerTimeout;
        trajectory = new Trajectory(seed);
        trajectoryGroup = new TrajectoryGroup(experiment);

        mParams = new ClassParams(experimentDir.getAbsolutePath() + File.separator + "autoweka.params");
    }

    public void run()
    {
      	
      	double bestError = 1E10;
      	double totalTime = 0;
      	int numTotalEvaluations = 0;
      	
        while(mTimeRemaining > 0)
        {
          RandomSearchResult res = evaluatePoint();
          double error = 0;
          if (res.results.size() > 0) {
                // Skip test error (the last element in the results array)
                for(RandomSearchResult.InstanceResult instanceResult : res.results.subList(0, res.results.size()-1)){
                  error += instanceResult.error;
                  totalTime += instanceResult.time;
                }
                error /= res.results.size()-1;
                numTotalEvaluations += res.results.size();
          } else {
              error = 1E10;
          }
          
          if (error < bestError) {
            bestError = error;
            trajectory.addPoint(new Point(totalTime, error, res.argString));
          }
        }
        trajectory.setEvaluationCounts(numTotalEvaluations, -1, -1);
        
        trajectoryGroup.addTrajectory(trajectory);
        trajectoryGroup.toXML(mExperimentDir + File.separator + mExperimentDir.getName() + ".trajectories." + mSeed);
    }

    private RandomSearchResult evaluatePoint(){
        boolean resultExists = true;
        String argString = null;
        RandomSearchResult res = null;
        while(resultExists){
            Map<String, String> argMap = new HashMap<String, String>();        
            
            for(Parameter param : mParams.getParameters()){
                argMap.put(param.name, param.getRandomValue(mRand));
            }
            argString = Util.argMapToString(mParams.filterParams(argMap));

            //Make the output result
            res = new RandomSearchResult(argString);
            resultExists = res.resultExists(mExperimentDir);
        }

        res.touchResultFile(mExperimentDir);
        System.out.println("Evaluating point with hash '" + res.argHash + "'");

        for(String instance : mInstances){
          try{
            SubProcessWrapper.ErrorAndTime errAndTime = SubProcessWrapper.getErrorAndTime(mExperimentDir, mExperiment, instance, argString, mSeed);
            res.addInstanceResult(instance, errAndTime);
            mTimeRemaining -= errAndTime.time;
            System.out.println("Spent " + errAndTime.time + " getting a response of " + errAndTime.error);
            // Stop processing folds if one of them is not working
            if (errAndTime.error>=1.0E10){
              break;
            }
          }
          catch(Exception e){
            e.printStackTrace();
            break;
          }
        }

        res.saveResultFile(mExperimentDir);
        return res;
    }
}

package autoweka.smac;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import autoweka.ClassParams;
import autoweka.Experiment;
import autoweka.Parameter;
import autoweka.Trajectory;
import autoweka.TrajectoryParser;

public class SMACTrajectoryParser extends TrajectoryParser
{
    // configurationId: configurationParameters
    private Pattern mTrajPattern = Pattern.compile("([\\-\\.\\d]+): (.*)");
    private Pattern mRunsAndResultFileNamePattern = Pattern.compile("runs_and_results-it(\\d+).csv");
    // seed=0:numFolds=10
    private Pattern mInstanceGeneratorArgsPattern = Pattern.compile("seed=\\d+:numFolds=(\\d+)");

    public Trajectory parseTrajectory(Experiment experiment, File folder, String seed)
    {
        int numFolds = 10; //set 10 as default and then parse the value from experiment
      	Matcher matcher = mInstanceGeneratorArgsPattern.matcher(experiment.instanceGeneratorArgs);
      	if(matcher.matches()){
      	  numFolds = Integer.parseInt(matcher.group(1));
      	}
      	
        //Load up the conditional params
        ClassParams params = new ClassParams(folder.getAbsolutePath() + File.separator + "autoweka.params");

        Trajectory traj = new Trajectory(seed);

        try
        {
            File[] files = new File(folder.getAbsolutePath() + File.separator + "out" + File.separator + "autoweka").listFiles();

            // Parse the runs_and_results file
            String runsAndResultsFileName = null;
            int runsAndResultsIteration = -1;
            String path = folder.getAbsolutePath() + File.separator + "out" + File.separator + "autoweka" + File.separator + "state-run" + seed + File.separator;
            files = new File(path).listFiles();
            for(File f: files)
            {
                String s = f.getName();
                matcher = mRunsAndResultFileNamePattern.matcher(s);
                if(matcher.matches())
                {
                    int itr = Integer.parseInt(matcher.group(1));
                    if(itr > runsAndResultsIteration){
                        runsAndResultsFileName = f.getAbsolutePath();
                        runsAndResultsIteration = itr;
                    }
                }
            }

            if(runsAndResultsFileName != null)
            {
              
                // Load the configurations to store them later in the trajectory file
                String line;
                HashMap<Integer, String> configurationParameters = new HashMap<Integer, String>();
                Scanner scanner = new Scanner(new FileInputStream(path + "paramstrings-it"+runsAndResultsIteration+".txt"));
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    matcher = mTrajPattern.matcher(line);
                    if (matcher.matches()) {
                        Integer configurationId = Integer.parseInt(matcher.group(1));
                        String argString = filterArgString(params, matcher.group(2));
                        configurationParameters.put(configurationId, argString);
                    }
                    else {
                      System.out.println("Line not matching: " + line);
                    }
                }
                scanner.close();
              
              	double currentBest = 1e100;
                int numEvals = 0;
                //int numGood = 0;
                int numCompleted = 0;
                int numMemOut = 0;
                int numTimeOut = 0;
                int numCrashed = 0;
                scanner = new Scanner(new FileInputStream(runsAndResultsFileName));
                
                HashMap<Integer,Double> responses = new HashMap<Integer, Double>();   
                HashMap<Integer,Integer> evaluatedFolds = new HashMap<Integer, Integer>();

                //Skip the header
                scanner.nextLine();
                String[] row;
                while(scanner.hasNextLine())
                {
                    //Split the line
                    row = scanner.nextLine().split(",");
                    try {
                        numEvals++;
                        
                        switch (row[13]) {
                          case "SAT":
                            // all good
                            //numGood++;
                            Integer configurationId = Integer.parseInt(row[1]);
                            //Integer instanceId = Integer.parseInt(row[2]);
                            Double response = Double.parseDouble(row[3]);
                            
                            // Store response
                            Double sumResponse = responses.get(configurationId) != null ? responses.get(configurationId)+response : response;
                            responses.put(configurationId, sumResponse);
                            
                            // Increment number of evaluated folds
                            Integer numEvaluatedFolds = evaluatedFolds.get(configurationId) != null ? evaluatedFolds.get(configurationId)+1 : 1;
                            evaluatedFolds.put(configurationId, numEvaluatedFolds);
                            if (numEvaluatedFolds >= numFolds) {
                              // Add the configuration to the trajectory if it has been evaluated in all the folds
                              Float time = Float.parseFloat(row[12]); // SMAC cumulative runtime
                              Double score = responses.get(configurationId) / numFolds;

                              if (score <= currentBest){
                        	currentBest = score;
                        	String args = configurationParameters.get(configurationId);
                        	if (args==null){
                        	  System.err.println("Configuration not found: " + configurationId);
                        	}
                        	traj.addPoint(new Trajectory.Point(time, score, args));
                              }
                              numCompleted++;
                            }
                            break;
                          case "TIMEOUT":
                            numTimeOut++;
                            break;
                          case "MEMOUT":
                            numMemOut++;
                            break;
                          case "CRASHED":
                            numCrashed++;
                            break;
                          default:
                            System.err.println("Unexpected evaluation status: " + row[13]);
                        }
                        
                    } catch (Exception e) {
                        //Whatevs... it's wrong
                        e.printStackTrace();
                    }
                }
                traj.setEvaluationCounts(numEvals, numMemOut, numTimeOut, numCompleted, numCrashed);
                scanner.close();
            }
            else
            {
                System.err.println("Could not find runs_and_results file");
            }
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to parse trajectory", e);
        }
        return traj;
    }

    private String filterArgString(ClassParams params, String args)
    {
        //First, we need to make a map out of everything
        Map<String, Parameter> paramMap = params.getParameterMap();
        Map<String, String> argMap = new HashMap<String, String>();
        String[] splitArgs = args.split(", ");
        for(String argPair: splitArgs)
        {
            //System.out.println(argPair);
            String[] splitArg = argPair.split("=", 2);
            String arg = splitArg[0].trim();
            String value = splitArg[1].trim();
            if(paramMap.get(arg) == null)
                throw new RuntimeException("Unknown argument found in trajectory '" + arg + "'");
            if(value.startsWith("'") && value.endsWith("'"))
            {
                value = value.substring(1, value.length()-1);
            }
            argMap.put(arg, value);
        }

        return autoweka.Util.argMapToString(params.filterParams(argMap));
    }
}


package ca.ubc.cs.beta.aeatk.example.satisfiabilitychecker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.version.VersionTracker;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.WaitableTAECallback;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ec.util.MersenneTwister;

/**
 * A simple utility class that provides the ability to execute a single run against a <code>TargetAlgorithmEvaluator</code>.
 *
 * This class serves two purposes: 
 * <p>
 * From a usage perspective, people should be able to test their wrappers or target algorithms easily
 * <p>
 * From a documentation perspective, this class should serve as an example for using TargetAlgorithmEvaluators and other aspects AEATK.
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class SatisfiabilityChecker 
{

	//SLF4J Logger object (not-initialized on start up in case command line options want to change it)
	private static Logger log;
	
	public static void main(String[] args)
	{
		 
		//JCommander Options object that specifies the main arguments to this project
		//It also includes a @ParametersDelegate for built in Option objects.
		final SatisfiabilityCheckerOptions mainOptions = new SatisfiabilityCheckerOptions();
		
		//Map object that for each available TargetAlgorithmEvaluator gives it's associated options object
		Map<String,AbstractOptions> taeOptions = mainOptions.scenOptions.algoExecOptions.taeOpts.getAvailableTargetAlgorithmEvaluators();

		try {
			
			//Parses the options given in the args array and sets the values
			JCommander jcom;
			try {
			//This will check for help and version arguments 
			jcom = JCommanderHelper.parseCheckingForHelpAndVersion(args, mainOptions,taeOptions);
			//Does any setup work necessary to setup logger.
				mainOptions.logOpts.initializeLogging();
			} finally
			{
				//Initialize the logger *AFTER* the JCommander objects have been parsed
				//So that options that take effect
				log = LoggerFactory.getLogger(SatisfiabilityChecker.class);
			}
		
			//Displays version information
			//See the TargetAlgorithmEvaluatorRunnerVersionInfo class for how to manage your own versions.
			VersionTracker.logVersions();
			
			
			for(String name : jcom.getParameterFilesToRead())
			{
				log.debug("Parsing (default) options from file: {} ", name);
			}
			
			
			//AlgorithmExecutionConfig object represents all the information needed to invoke the target algorithm / wrapper.
			//This includes information such as cutoff time, and the parameter space.
			//Like most domain objects in AEATK, AlgorithmExecutionConfig is IMMUTABLE. 
			AlgorithmExecutionConfiguration execConfig = mainOptions.getAlgorithmExecutionConfig();
			
			
			//Logs the options (since mainOptions implements AbstractOptions a 'nice-ish' printout is created).
			log.debug("==== Configuration====\n {} ", mainOptions);
			
			
			
			TargetAlgorithmEvaluator tae = null;
			
			File f = new File(mainOptions.outputFile);
			
			if(f.exists() && !mainOptions.overwriteOutputFile)
			{
				throw new ParameterException("Output file already exists. Use --overwrite-output-file to overwrite already existing files");
			}
		
			try {
				
				
				
				//Retrieve the target algorithm evaluator with the necessary options
				tae = mainOptions.scenOptions.algoExecOptions.taeOpts.getTargetAlgorithmEvaluator( taeOptions);
				
				
				final List<ProblemInstance> instances;
				if(mainOptions.useTestSetInstances)
				{
					instances = mainOptions.getTrainingAndTestProblemInstances().getTestInstances().getInstances();
				} else
				{
					instances = mainOptions.getTrainingAndTestProblemInstances().getTrainingInstances().getInstances();
				}
				
				
			
				//The following is a common convention used in AEATK
				if(execConfig.isDeterministicAlgorithm())
				{
					if (mainOptions.seed != -1)
					{
						//A simple log message with SLF4J
						log.warn("It is convention to use -1 as the seed for deterministic algorithms");
					}
				} else
				{
					if(mainOptions.seed == -1)
					{
						//A simple log message with SLF4J
						log.warn("It is convention that -1 be used as seed only for deterministic algorithms");
					}
				}
				
				
				List<ProblemInstanceSeedPair> pisps = new ArrayList<ProblemInstanceSeedPair>(); 
				
				//A problem instance seed pair object (IMMUTABLE)
				for(ProblemInstance pi : instances)
				{
					pisps.add( new ProblemInstanceSeedPair(pi, mainOptions.seed));
				}
				//A Configuration Space object it represents the space of allowable configurations (IMMUTABLE).
				//"ParamFile" is a deprecated term for it that is still in use in the code base
				ParameterConfigurationSpace configSpace = execConfig.getParameterConfigurationSpace();
			
				
				//If we are asked to supply a random a configuration, we need to pass a Random object
				Random configSpacePRNG = new MersenneTwister(mainOptions.configSeed);
				
				
				//Converts the string based configuration in the options object, to a point in the above space
				ParameterConfiguration config = configSpace.getParameterConfigurationFromString(mainOptions.config, ParameterStringFormat.NODB_OR_STATEFILE_SYNTAX, configSpacePRNG);
				
				//ParamConfiguration objects implement the Map<String, String> interface (but not all methods are implemented)
				//Other methods have restricted semantics, for instance you must ensure that you are only placing keys with valid values in the map. 
				//They are MUTABLE, but doing this after they have been "used" is likely to cause problems.
				for(Entry<String, String> entry : mainOptions.configSettingsToOverride.entrySet())
				{
					config.put(entry.getKey(), entry.getValue());
				}
			

				List<AlgorithmRunConfiguration> rc = new ArrayList<AlgorithmRunConfiguration>(instances.size());
				for(ProblemInstanceSeedPair pisp : pisps)
				{
					//A RunConfig object stores the information needed to actually request (compare the objects here to the information passed to the wrapper as listed in the Manual)
					//It is also IMMUTABLE
					rc.add(new AlgorithmRunConfiguration(pisp, config,execConfig));
				}
				
				//Observer that gives nice information about current status of runs
				TargetAlgorithmEvaluatorRunObserver obs = new TargetAlgorithmEvaluatorRunObserver()
				{

					private long lastUpdate = 0;
					
					@Override
					public synchronized void currentStatus(List<? extends AlgorithmRunResult> runs) {
						
						if(System.currentTimeMillis() - lastUpdate < 5000)
						{
							return;
						}
				
						lastUpdate = System.currentTimeMillis();
						final NumberFormat nf = NumberFormat.getPercentInstance();
						double currentTime = 0; 
						double maxTime = 0;
						int completed = 0;
						for(AlgorithmRunResult run : runs)
						{
							if(run.isRunCompleted())
							{
								maxTime += run.getRuntime();
								completed++;
							} else
							{
								maxTime += run.getAlgorithmRunConfiguration().getCutoffTime();
							}
						
							currentTime += run.getRuntime();
						}
						
						//log.info("Current Run Information: Completion by CPU Time: {}, Completed Instances: {}/{}", nf.format(currentTime/maxTime), completed , runs.size() );
					}
					
				};
				
				
				
				//Exception 
				final AtomicReference<Exception> exception = new AtomicReference<Exception>();
				
				//Callback
				final TargetAlgorithmEvaluatorCallback taeCallback = new TargetAlgorithmEvaluatorCallback()
				{

					@Override
					public void onSuccess(List<AlgorithmRunResult> runs) 
					{
						File f = new File(mainOptions.outputFile);
						
						if(f.exists() && !mainOptions.overwriteOutputFile)
						{
							throw new ParameterException("Output file already exists. Use --overwrite-output-file to overwrite already existing files");
						}
							
						
						Map<ProblemInstance, AlgorithmRunResult> instanceToRunMap = new HashMap<ProblemInstance, AlgorithmRunResult>();
						
						for(AlgorithmRunResult run : runs)
						{
							instanceToRunMap.put(run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getProblemInstance(), run);
						}
						
						log.info("Writing output to {}", f.getAbsoluteFile());
						try {
							FileWriter fwrite = new FileWriter(f);
							try {
							
								for(ProblemInstance pi : instances)
								{
									AlgorithmRunResult run = instanceToRunMap.get(pi);
									fwrite.append(pi.getInstanceName() + " " + (run.getRunStatus().isDecided() ? run.getRunStatus() : "UNKNOWN") + "\n");
								}
							} finally
							{
								fwrite.close();
							}
						} catch (IOException e) {
							exception.set(e);
						}
					}

					@Override
					public void onFailure(RuntimeException e) {
						exception.set(e);
					}
					
				};
				
				
				WaitableTAECallback wTAE = new WaitableTAECallback(taeCallback);
				tae.evaluateRunsAsync(rc, wTAE, obs);
				
				if(!tae.areRunsPersisted())
				{
					wTAE.waitForCompletion();
				}
				
				
				if(exception.get() != null)
				{
					throw exception.get();
				}
			
				
			} finally
			{
				//We need to tell the TAE we are shutting down
				//Otherwise the program may not exit 
				if(tae != null)
				{
					tae.notifyShutdown();
				}
			}
		} catch(ParameterException e)
		{	
			log.error(e.getMessage());
			if(log.isDebugEnabled())
			{
				log.error("Stack trace:",e);
			}
		} catch(Exception e)
		{
			
			e.printStackTrace();
		}
	}
	



			
}

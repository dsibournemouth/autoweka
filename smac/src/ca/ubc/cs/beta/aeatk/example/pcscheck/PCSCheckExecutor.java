package ca.ubc.cs.beta.aeatk.example.pcscheck;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.version.VersionTracker;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
public class PCSCheckExecutor 
{

	//SLF4J Logger object (not-initialized on start up in case command line options want to change it)
	private static Logger log;
	
	public static void main(String[] args)
	{
		 
		//JCommander Options object that specifies the main arguments to this project
		//It also includes a @ParametersDelegate for built in Option objects.
		PCSCheckOptions mainOptions = new PCSCheckOptions();
		
		//Map object that for each available TargetAlgorithmEvaluator gives it's associated options object
		Map<String,AbstractOptions> taeOptions = mainOptions.scenOptions.algoExecOptions.taeOpts.getAvailableTargetAlgorithmEvaluators();

		try {
			
			//Parses the options given in the args array and sets the values
			JCommander jcom;
			try {
				try
				{
					//This will check for help and version arguments 
					jcom = JCommanderHelper.parseCheckingForHelpAndVersion(args, mainOptions,taeOptions);
				} catch(ParameterException e)
				{
					
					ArrayList<String> lists = new ArrayList<>(Arrays.asList(args));
					lists.add("--algo-exec");
					lists.add("foobar");
					
					try 
					{
						jcom = JCommanderHelper.parseCheckingForHelpAndVersion(lists.toArray(new String[0]), mainOptions,taeOptions);
					} catch(RuntimeException e2)
					{
						throw e;
					}
				}
				
				//Does any setup work necessary to setup logger.
				mainOptions.logOpts.initializeLogging();
			} finally
			{
				//Initialize the logger *AFTER* the JCommander objects have been parsed
				//So that options that take effect
				log = LoggerFactory.getLogger(PCSCheckExecutor.class);
			}
		
	
			AlgorithmExecutionConfiguration execConfig = mainOptions.scenOptions.algoExecOptions.getAlgorithmExecutionConfigSkipDirCheck();
			
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
		
			log.info("PCS file parsed Successfully estimated sized is in [" + configSpace.getLowerBoundOnSize() + "," + configSpace.getUpperBoundOnSize() + "]");
			
			log.info("Supplied configuration {} is {}forbidden", config.getFormattedParameterString() , config.isForbiddenParameterConfiguration() ? "":"NOT ");
			
			if(configSpace.getForbiddenOrdinalAndCategoricalValues().size() > 0)
			{
				log.info("Values of categorical and ordinal parameter values are: {}",configSpace.getForbiddenOrdinalAndCategoricalValues() );
			}
			
			
			//Ignore JVM Warmup
			StopWatch watch = new AutoStartStopWatch();
			int i=0; 
			while(watch.time() < 1000)
			{
				configSpace.getRandomParameterConfiguration(configSpacePRNG);
				i++;
			}
			if( i > 100)
			{
				log.info("In 1000 ms was able to generate {} configurations",i);
			} else
			{
				log.warn("In 1000 ms only able to generate {} configurations you might find that random generation involves a lot of overhead",i);
			}
		
			
			
			
		} catch(ParameterException | IllegalArgumentException e)
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
	

	
	
	




	/**
	 * Encapsulated method for evaluating a run
	 * 
	 * @param runConfig 	runConfig to evaluate
	 * @param tae 			target algorithm evaluator to use
	 */
	public static void processRunConfig(AlgorithmRunConfiguration runConfig, TargetAlgorithmEvaluator tae, final double killTime)
	{
		
		
		TargetAlgorithmEvaluatorRunObserver runStatus = new TargetAlgorithmEvaluatorRunObserver()
		{
			private long lastUpdate = 0;
			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) 
			{
				//As we print to standard out we want to make sure that a frequency that is too high doesn't spam the console
				if(System.currentTimeMillis() - lastUpdate < 1000)
				{
					return;
				}
				
				for(int i=0; i < runs.size(); i++)
				{
					AlgorithmRunResult run = runs.get(i);
					//Log messages with more than 2 arguments, must use pass them as an array.
					Object[] logArguments = { i, run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getProblemInstance(), run.getRunStatus(), run.getRuntime()};
					log.info("Run {} on {} has status =>  {}, {}", logArguments);
					if(run.getRuntime() > killTime)
					{
						log.info("Dynamically killing run");
						run.kill();
					}
				}
				lastUpdate = System.currentTimeMillis();
				
			}
		
		};
		
		
		//Invoke the runs with the observer
		List<AlgorithmRunResult> runResults = tae.evaluateRun(Collections.singletonList(runConfig), runStatus); 
		
		 
		log.info("Run Completed");
		
		for(int i=0; i < runResults.size(); i++)
		{
			//AlgorithmRun objects can be viewed as an "answer" to the RunConfig "question"
			//They are IMMUTABLE.
			AlgorithmRunResult run = runResults.get(i);
		
			//This is the same RunConfig as above
			//But in general you should always use the information in the AlgorithmRun
			AlgorithmRunConfiguration resultRunConfig = run.getAlgorithmRunConfiguration();

			//Object representing whether the run reported SAT, UNSAT, TIMEOUT, etc...
			RunStatus runResult = run.getRunStatus();
		
			double runtime = run.getRuntime();
			double runLength = run.getRunLength();
			double quality = run.getQuality();
			
			//The algorithm must echo back the seed that we request to it (historically this has helped with debugging)
			long resultSeed = run.getResultSeed();
			long requestSeed = resultRunConfig.getProblemInstanceSeedPair().getSeed();
			
			//Additional run data is just a string that the algorithm returned and we will keep track of.
			String additionalData = run.getAdditionalRunData();
			
			if(resultSeed != requestSeed)
			{ 
				//A more complicated SLF4J log message. The {} are replaced with the parameters in order
				log.error("Algorithm Run Result does not have a matching seed, requested: {} , returned: {}", resultSeed, requestSeed );
			}
			
			//The toString() method does not return the actual configuration, this method is the best way to print them
			String configString = resultRunConfig.getParameterConfiguration().getFormattedParameterString(ParameterStringFormat.NODB_OR_STATEFILE_SYNTAX);
			
			//Log messages with more than 2 parameters must have them passed as an array.
			Object[] logArguments = { i, resultRunConfig.getProblemInstanceSeedPair().getProblemInstance(), configString, runResult, runtime, runLength, quality, resultSeed, additionalData};
			log.info("Run {} on {} with config: {} had the result => {}, {}, {}, {}, {}, {}", logArguments);
		}
	}

	
	/**
	 * Logs a run in JSON Format, this is useful for debugging the json-exec command.
	 * @param runConfig
	 */
	private static void printJSON(AlgorithmRunConfiguration runConfig) {
		ArrayList<AlgorithmRunConfiguration> rcs = new ArrayList<>();
		rcs.add(runConfig);

		ObjectMapper map = new ObjectMapper();
		JsonFactory factory = new JsonFactory();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		
		JsonGenerator g;
		try {
			SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
			map.configure(SerializationFeature.INDENT_OUTPUT, true);
			  
			map.registerModule(sModule);
			factory.setCodec(map);
			
			
			g = factory.createGenerator(bout);
			
			
		
			g.writeObject(rcs);
			g.flush();
			
			log.info("----====[JSON Representation of Run Configuration]====-----:\n\n{}\n\n",bout.toString("UTF-8"));
			
		} catch (IOException e) {
			log.error("Unexpected Exception: ", e);
		}
		

	
		
		
	}
			
}

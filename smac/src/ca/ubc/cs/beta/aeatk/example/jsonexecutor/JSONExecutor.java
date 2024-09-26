package ca.ubc.cs.beta.aeatk.example.jsonexecutor;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.version.VersionTracker;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
public class JSONExecutor 
{

	//SLF4J Logger object (not-initialized on start up in case command line options want to change it)
	private static Logger log;
	
	public static void main(String[] args)
	{

		JSONExecutorOptions mainOptions = new JSONExecutorOptions();
		
		
		//Map object that for each available TargetAlgorithmEvaluator gives it's associated options object
		Map<String,AbstractOptions> taeOptions = mainOptions.taeOptions.getAvailableTargetAlgorithmEvaluators();

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
				log = LoggerFactory.getLogger(JSONExecutor.class);
			}
		
	
		
			
			
			
			List<AlgorithmRunResult> results = null;
			try( TargetAlgorithmEvaluator tae = mainOptions.taeOptions.getTargetAlgorithmEvaluator( taeOptions) ) {
				
				log.info("Waiting for an array of AlgorithmRunConfiguration in JSON format to be recieved on STDIN. You can use the algo-test utility --print-json true to get example JSON.");
				JsonFactory jfactory = new JsonFactory();
				
				ObjectMapper map = new ObjectMapper(jfactory);
				SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
				map.registerModule(sModule);

				
				JsonParser jParser = jfactory.createParser(System.in);
				
				
				List<AlgorithmRunConfiguration> runsToDo = new ArrayList<AlgorithmRunConfiguration>(Arrays.asList(map.readValue(jParser, AlgorithmRunConfiguration[].class)));
			
				final AtomicBoolean outputCompleted = new AtomicBoolean(false);
				TargetAlgorithmEvaluatorRunObserver taeRunObserver = new TargetAlgorithmEvaluatorRunObserver()
				{

					@Override
					public void currentStatus(
							List<? extends AlgorithmRunResult> runs) {
						int total = runs.size();
						int completed = 0;
						int notStarted = 0;
						int started = 0;
						for(AlgorithmRunResult run : runs)
						{
							if(run.isRunCompleted())
							{
								completed++;
							} else if (run.getRuntime() > 0 || run.getWallclockExecutionTime() > 0)
							{
								started++;
							} else
							{
								notStarted++;
							}
							
						}
						if(!outputCompleted.get())
						{
							//log.info("Current Run Status, Total: {} , Started: {} , Completed: {}, Not Started: {}", total, started, completed, notStarted);
						}
						
					}
					
				};
				
				if(mainOptions.printStatus)
				{
					log.info("Periodically printing status, use --print-status false to disable");
					results = tae.evaluateRun(runsToDo, taeRunObserver);
				} else
				{
					results = tae.evaluateRun(runsToDo);
				}
					outputCompleted.set(true);
				
								
				
			} 
			
			ObjectMapper map = new ObjectMapper();
			JsonFactory factory = new JsonFactory();
			factory.setCodec(map);
			
			
			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			JsonGenerator g = factory.createGenerator(bout);

			SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
			map.configure(SerializationFeature.INDENT_OUTPUT, true);
			  
			map.registerModule(sModule);
			
			List<AlgorithmRunResult> results2 = new ArrayList<>(results);
			g.writeObject(results2);
			g.flush();
			
			System.out.flush();
			System.out.println("******JSON******\n" + bout.toString("UTF-8") + "\n********");
			System.out.flush();
			
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

			
}

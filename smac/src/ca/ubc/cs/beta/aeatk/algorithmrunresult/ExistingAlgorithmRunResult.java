package ca.ubc.cs.beta.aeatk.algorithmrunresult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
/**
 * Class that is used to take an existing algorithm run (from for instance a string), and create an AlgorithmRun object
 * @author Steve Ramage<seramage@cs.ubc.ca>
 */
public class ExistingAlgorithmRunResult extends AbstractAlgorithmRunResult {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private static transient Logger log = LoggerFactory.getLogger(ExistingAlgorithmRunResult.class);
	
	/**
	 * 
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runLength			The Run Length
	 * @param quality			The Run Quality
	 * @param resultSeed 		The Reported seed
	 * @param additionalRunData	The Additional Run Data
	 * @param wallclockTime		Wallclock time to report
	 */
	public ExistingAlgorithmRunResult(AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runLength, double quality, long resultSeed, String additionalRunData, double wallclockTime)
	{
		super( runConfig,runResult, runtime, runLength, quality, resultSeed, "<Existing Run>", additionalRunData, wallclockTime);
	}
	
	/**
	 * 
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runLength			The Run Length
	 * @param quality			The Run Quality
	 * @param resultSeed 		The Reported seed
	 * @param wallclockTime		Wallclock time to report
	 */

	public ExistingAlgorithmRunResult( AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runLength, double quality, long resultSeed,  double wallclockTime)
	{
		super(runConfig,runResult, runtime, runLength, quality, resultSeed, "<Existing Run>", "", wallclockTime);
	}
	
	
	

	/**
	 * 
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runlength			The Run Length
	 * @param quality			The Run Quality
	 * @param seed 				The Reported seed
	 * @param additionalRunData	The Additional Run Data
	 */

	public ExistingAlgorithmRunResult( AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runlength, double quality, long seed, String additionalRunData)
	{
		this( runConfig, runResult, runtime,runlength, quality, seed, additionalRunData, 0.0);
	}
	

	/**
	 * 
	 * @param runConfig			run configuration we are executing
	 * @param runStatus			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runlength			The Run Length
	 * @param quality			The Run Quality
	 * @param seed 				The Reported seed
	 */
	public ExistingAlgorithmRunResult( AlgorithmRunConfiguration runConfig, RunStatus runStatus, double runtime, double runlength, double quality, long seed)
	{
		this( runConfig, runStatus, runtime,runlength, quality, seed, "", 0.0);
	}
	
	
	
	
	
	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runLength			The Run Length
	 * @param quality			The Run Quality
	 * @param resultSeed 		The Reported seed
	 * @param additionalRunData	The Additional Run Data
	 * @param wallclockTime		Wallclock time to report
	 */
	@Deprecated
	public ExistingAlgorithmRunResult(AlgorithmExecutionConfiguration execConfig, AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runLength, double quality, long resultSeed, String additionalRunData, double wallclockTime)
	{
		super(runConfig,runResult, runtime, runLength, quality, resultSeed, "<Existing Run>", additionalRunData, wallclockTime);
	}
	
	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runLength			The Run Length
	 * @param quality			The Run Quality
	 * @param resultSeed 		The Reported seed
	 * @param wallclockTime		Wallclock time to report
	 */
	@Deprecated
	public ExistingAlgorithmRunResult(AlgorithmExecutionConfiguration execConfig, AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runLength, double quality, long resultSeed,  double wallclockTime)
	{
		
		super(runConfig,runResult, runtime, runLength, quality, resultSeed, "<Existing Run>", "", wallclockTime);
		
	}
	
	
	

	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runlength			The Run Length
	 * @param quality			The Run Quality
	 * @param seed 				The Reported seed
	 * @param additionalRunData	The Additional Run Data
	 */
	@Deprecated
	public ExistingAlgorithmRunResult(AlgorithmExecutionConfiguration execConfig, AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runlength, double quality, long seed, String additionalRunData)
	{
		this(execConfig, runConfig, runResult, runtime,runlength, quality, seed, additionalRunData, 0.0);
	}
	

	/**
	 * 
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param runResult			The RunResult to report
	 * @param runtime 			The Run Time
	 * @param runlength			The Run Length
	 * @param quality			The Run Quality
	 * @param seed 				The Reported seed
	 */
	@Deprecated
	public ExistingAlgorithmRunResult(AlgorithmExecutionConfiguration execConfig, AlgorithmRunConfiguration runConfig, RunStatus runResult, double runtime, double runlength, double quality, long seed)
	{
		this(execConfig, runConfig, runResult, runtime,runlength, quality, seed, "", 0.0);
	}
	
	@Deprecated
	public static ExistingAlgorithmRunResult getRunFromString( AlgorithmRunConfiguration runConfig, String result)
	{
		return getRunFromString(runConfig, result, 0);
	}
	
	/**
	 * Default Constructor
	 * @param execConfig		execution configuration of the object
	 * @param runConfig			run configuration we are executing
	 * @param result			result string to parse. The format of this is currently everything after the : in the result line of {@link CommandLineAlgorithmRun}. We support both the String for the RunResult, as well as the Status Code
	 * @deprecated  the constructor that doesn't take a result string is preferred. 
	 */
	@Deprecated
	public static ExistingAlgorithmRunResult getRunFromString( AlgorithmRunConfiguration runConfig, String result, double wallClockTime) {
		
		//this.rawResultLine = resultLine;
		//this.runCompleted = true;
		String[] resultLine = result.split(",");
		
		try
		{
			RunStatus acResult;
			try {
				acResult = RunStatus.getAutomaticConfiguratorResultForCode(Integer.valueOf(resultLine[0]));
			} catch(NumberFormatException e)
			{
				acResult = RunStatus.getAutomaticConfiguratorResultForKey(resultLine[0]);
			}
			
			
			double runtime = Double.valueOf(resultLine[1].trim());
			double runLength = Double.valueOf(resultLine[2].trim());
			double quality = Double.valueOf(resultLine[3].trim());
			long resultSeed = Long.valueOf(resultLine[4].trim());
			String additionalRunData = "";
			if(resultLine.length == 6)
			{
				additionalRunData = resultLine[5].trim();
			}
			
			
			return new ExistingAlgorithmRunResult(runConfig, acResult, runtime, runLength, quality, resultSeed, additionalRunData,wallClockTime);
			
			
		} catch(ArrayIndexOutOfBoundsException e)
		{ 
			Object[] args = { runConfig, result} ;
			

			log.debug("Malformed Run Result for Execution (ArrayIndexOutOfBoundsException): {}, Instance: {}, Result: {}", args);
			log.debug("Exception:",e);

			
			
			return getAbortResult(runConfig, e.getMessage());
		}catch(NumberFormatException e)
		{
			//There was a problem with the output, we just set this flag
			log.debug("Malformed Run Result for Execution (NumberFormatException):  Instance: {}, Result: {}", runConfig, result);
			log.debug("Exception:",e);

			return getAbortResult(runConfig, e.getMessage());
			
			
		}
		
		

	}
	
	public static ExistingAlgorithmRunResult getAbortResult(AlgorithmRunConfiguration rc, String message)
	{
		return new ExistingAlgorithmRunResult(rc, RunStatus.ABORT, 0, 0, 0, 0, "ERROR:" +message,0);
	}

	@Override
	public void kill() {
		// TODO Auto-generated method stub
		
	}



}

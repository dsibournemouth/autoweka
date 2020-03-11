package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.ipc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;

public class ResponseParser {

	private static final Pattern pattern = Pattern.compile(CommandLineAlgorithmRun.AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
	private static final Pattern oldPattern = Pattern.compile(CommandLineAlgorithmRun.OLD_AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
	
	
	private static final Logger log = LoggerFactory.getLogger(ResponseParser.class);
	/**
	 *	Process a single line of the output looking for a matching line (e.g. Result for ParamILS: ...)
	 *	@param line of program output
	 */
	public static AlgorithmRunResult processLine(String line, AlgorithmRunConfiguration rc, double walltime)
	{
		Matcher matcher = pattern.matcher(line);
		Matcher matcher2 = oldPattern.matcher(line);	

		AlgorithmRunResult run;
		if (matcher.find() || matcher2.find())
		{
		
			String fullLine = line.trim();
			String additionalRunData = "";
			try
			{
			
				String acExecResultString = line.substring(line.indexOf(":") + 1).trim();
				
				String[] results = acExecResultString.split(",");
				for(int i=0; i < results.length; i++)
				{
					results[i] = results[i].trim();
				}
				
				
				
				RunStatus acResult =  RunStatus.getAutomaticConfiguratorResultForKey(results[0]);
				
				if(!acResult.permittedByWrappers())
				{
					throw new IllegalArgumentException(" The Run Result reported is NOT permitted to be output by a wrapper and is for internal SMAC use only.");
				}
				
					
					
				String runtime = results[1].trim();
				String runLength = results[2].trim();
				String bestSolution = results[3].trim();
				String seed = results[4].trim();
				if(results.length <= 5)
				{ //This is a good case

				} else if(results.length == 6)
				{
					additionalRunData = results[5].trim();
				} else
				{
					log.warn("Too many fields were encounted (expected 5 or 6) when parsing line (Additional Run Data cannot have commas): {}\n ",line);
				}
				
				double runLengthD = Double.valueOf(runLength);
				double runtimeD = Double.valueOf(runtime);
				double qualityD = Double.valueOf(bestSolution);
				long resultSeedD = Long.valueOf(seed);
			
				
				run = new ExistingAlgorithmRunResult( rc ,acResult, runtimeD, runLengthD, qualityD, resultSeedD, additionalRunData, walltime );
				
			} catch(NumberFormatException e)
			{	 //Numeric value is probably at fault
				
				run = new ExistingAlgorithmRunResult( rc ,RunStatus.CRASHED, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(),"", walltime );
				
				//this.setCrashResult("Output:" + fullLine + "\n Exception Message: " + e.getMessage() + "\n Name:" + e.getClass().getCanonicalName());
				Object[] args = { CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString(rc), fullLine};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely one of the values of runLength, runtime, quality could not be parsed as a Double, or the seed could not be parsed as a valid long", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunStatus.CRASHED);
					
			} catch(IllegalArgumentException e)
			{ 	//The RunResult probably doesn't match anything
				run = new ExistingAlgorithmRunResult( rc ,RunStatus.CRASHED, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(),"", walltime );
				
				
				ArrayList<String> validValues = new ArrayList<String>();
				for(RunStatus r : RunStatus.values())
				{
					if(r.permittedByWrappers())
					{
						validValues.addAll(r.getAliases());
					}
				}
				Collections.sort(validValues);
				
				String[] validArgs = validValues.toArray(new String[0]);
				
				
				Object[] args = { CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString( rc), fullLine, Arrays.toString(validArgs)};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the Algorithm did not report a result string as one of: {}", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunStatus.CRASHED);
			
			} catch(ArrayIndexOutOfBoundsException e)
			{	//There aren't enough commas in the output
				run = new ExistingAlgorithmRunResult( rc ,RunStatus.CRASHED, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(),"", walltime );
				
				Object[] args = { CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString( rc), fullLine};
				log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the algorithm did not specify all of the required outputs that is <solved>,<runtime>,<runlength>,<quality>,<seed>", args);
				log.error("Exception that occured trying to parse result was: ", e);
				log.error("Run will be counted as {}", RunStatus.CRASHED);
				
			}
		} else
		{
			Object[] args = { CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString( rc), line.trim()};
			
			log.error("Target Algorithm Call failed:{}\nResponse:{}\nComment: Most likely the algorithm did not specify all of the required outputs that is <solved>,<runtime>,<runlength>,<quality>,<seed>", args);
			log.error("Run will be counted as {}", RunStatus.CRASHED);
			
			run = new ExistingAlgorithmRunResult(rc ,RunStatus.CRASHED, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(),"", walltime );
		}
		
		return run;
		
	}
}

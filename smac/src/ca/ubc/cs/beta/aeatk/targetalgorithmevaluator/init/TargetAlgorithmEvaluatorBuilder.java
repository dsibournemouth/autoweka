package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug.CheckForDuplicateRunConfigDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug.LeakingMemoryTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug.LogEveryTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug.RunHashCodeVerifyingAlgorithmEvalutor;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug.UncleanShutdownDetectingTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.TerminateAllRunsOnFileDeleteTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.transform.TransformTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.CallObserverBeforeCompletionTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.CompleteZeroSecondCutoffRunsTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.RetryCrashedRunsTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.StrictlyIncreasingRuntimesTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.UseDynamicCappingExclusivelyTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.prepostcommand.PrePostCommandTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.FileCacheTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.caching.CachingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.forking.ForkingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.AbortOnCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.AbortOnFirstRunCrashTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.CrashedSolutionQualityTransformingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.ExitOnFailureTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.JVMShutdownBlockerTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.ResultOrderCorrectCheckerTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.SATConsistencyTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.SynchronousObserverTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.TimingCheckerTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.VerifySATTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety.WarnOnNoWallOrRuntimeTargetAlgorithmEvaluatorDecorator;

import com.beust.jcommander.ParameterException;


public class TargetAlgorithmEvaluatorBuilder {

	private static Logger log = LoggerFactory.getLogger(TargetAlgorithmEvaluatorBuilder.class);
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behavior
	 * 
	 * @param options 		   		Target Algorithm Evaluator Options
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param taeOptionsMap			A map that contains mappings between the names of TAEs and their configured options object	
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 * @deprecated Use the non wrapped method
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, boolean hashVerifiersAllowed, Map<String, AbstractOptions> taeOptionsMap)
	{
		return getTargetAlgorithmEvaluator(options,  hashVerifiersAllowed, taeOptionsMap, null);
	}
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 			   	Target Algorithm Evaluator Options
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param taeOptionsMap	   		A map that contains mappings between the names of TAEs and their configured options object
	 * @param tae			   		The TAE to use wrap (if not <code>null</code> will use this one instead of SPI)				
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 * @deprecated Use the non wrapped method
	 */
	@Deprecated
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options, boolean hashVerifiersAllowed, Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae)
	{
		return getTargetAlgorithmEvaluator(options, hashVerifiersAllowed, false, taeOptionsMap, tae);
	}
	
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 		   Target Algorithm Evaluator Options
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param ignoreBound	   Whether to ignore bound requests
	 * @param taeOptionsMap	   		A map that contains mappings between the names of TAEs and their configured options object
	 * @param tae			   		The TAE to use wrap (if not <code>null</code> will use this one instead of SPI)				
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options,  boolean hashVerifiersAllowed, boolean ignoreBound,  Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae)
	{
		return getTargetAlgorithmEvaluator(options,  hashVerifiersAllowed, ignoreBound, taeOptionsMap, tae, new File("."), 0);
	}
	/**
	 * Generates the TargetAlgorithmEvaluator with the given runtime behaivor
	 * 
	 * @param options 		   Target Algorithm Evaluator Options
	 * @param hashVerifiersAllowed  Whether we should apply hash verifiers
	 * @param ignoreBound	   Whether to ignore bound requests
	 * @param taeOptionsMap	   		A map that contains mappings between the names of TAEs and their configured options object
	 * @param tae			   		The TAE to use wrap (if not <code>null</code> will use this one instead of SPI)				
	 * @return a configured <code>TargetAlgorithmEvaluator</code>
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(TargetAlgorithmEvaluatorOptions options,  boolean hashVerifiersAllowed, boolean ignoreBound,  Map<String, AbstractOptions> taeOptionsMap, TargetAlgorithmEvaluator tae, File outputDir, int numRun)
	{
		
		if(taeOptionsMap == null)
		{
			throw new IllegalArgumentException("taeOptionsMap must be non-null and contain the option objects for all target algorithm evaluators");
		}
		
		boolean taeLoaded = false;
		
	
		if(tae == null)
		{
			taeLoaded = true;
			
			String taeKey = options.targetAlgorithmEvaluator;
			tae = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator( taeKey,taeOptionsMap);
		} 
		
		if(tae == null)
		{
			throw new IllegalStateException("TAE should have been non-null");
		}
		//===== Note the decorators are not in general commutative
		//Specifically Run Hash codes should only see the same runs the rest of the applications see
		//Additionally retrying of crashed runs should probably happen before Abort on Crash
		
		if(options.exitOnFailure)
		{
		    log.warn("[TAE] EXPERIMENTAL - This java process will exit with the first onFailure called.");
		    if(options.tForkOptions.forkToTAE != null)
		    {
		        log.warn("[TAE] The exitOnFailure may not work with forking slave TAE.");
		    }
		    tae = new ExitOnFailureTargetAlgorithmEvaluatorDecorator(tae);
		}
		
		if(options.uncleanShutdownCheck)
		{
			log.trace("[TAE] Checking for unclean shutdown");
			tae = new UncleanShutdownDetectingTargetAlgorithmEvaluator(tae);
		} else
		{
			log.debug("[TAE] Not Checking for unclean shutdown");
		}
		
		if(options.tForkOptions.forkToTAE != null)
		{
			if(taeLoaded && options.targetAlgorithmEvaluator.equals(options.tForkOptions.forkToTAE))
			{
				throw new ParameterException("Cannot use "+options.tForkOptions.forkToTAE+" as fork when main TAE is "+options.targetAlgorithmEvaluator+".");
			}
			log.warn("EXPERIMENTAL - Observers not supported with forking.");
			log.info("[TAE] EXPERIMENTAL - Forking all runs to {} with policy {} .",options.tForkOptions.forkToTAE, options.tForkOptions.fPolicyOptions.fPolicy);
			
			TargetAlgorithmEvaluator slaveTAE = TargetAlgorithmEvaluatorLoader.getTargetAlgorithmEvaluator(options.tForkOptions.forkToTAE, taeOptionsMap);
			
			tae = new ForkingTargetAlgorithmEvaluatorDecorator(tae, slaveTAE, options.tForkOptions.fPolicyOptions);
		}

		//This TAE should come before the AbortOnCrashTAE
		if(options.retryCount >0)
		{
			log.debug("[TAE] Automatically retrying CRASHED runs {} times " , options.retryCount);
			tae = new RetryCrashedRunsTargetAlgorithmEvaluatorDecorator(options.retryCount, tae);
		} 
		
		if(options.abortOnCrash)
		{
			log.trace("[TAE] Treating all crashes as aborts");
			tae = new AbortOnCrashTargetAlgorithmEvaluator(tae);
		} else if(options.abortOnFirstRunCrash)
		{
			tae = new AbortOnFirstRunCrashTargetAlgorithmEvaluator(tae);
		}
		
		if(options.ttaedo.transform)
		{
			log.debug("[TAE] Using Transforming Target Algorithm Evaluator");
			tae = new TransformTargetAlgorithmEvaluatorDecorator(tae, options.ttaedo);
		}
		
		if(options.verifySAT != null)
		{
			if(options.verifySAT)
			{
				log.trace("[TAE] Verifying SAT Responses");
				tae = new VerifySATTargetAlgorithmEvaluator(tae);
				
			}
		}
		
		if(options.checkSATConsistency)
		{
			log.trace("[TAE] Ensuring SAT Response consistency");
			tae = new SATConsistencyTargetAlgorithmEvaluator(tae, options.checkSATConsistencyException);
		}
		
		
		if(options.trackRunsScheduled)
		{
			String resultFile = outputDir.getAbsolutePath() + File.separator + "dispatched-runs-over-time-" + numRun + ".csv";
			log.debug("[TAE] Tracking all outstanding runs to file {} ", resultFile);
			tae = new OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator(tae, resultFile, options.trackRunsScheduledResolution, "Dispatched");
			
		}
		
		
		if(options.taeStopProcessingOnShutdown)
		{
			log.trace("[TAE] Processing of runs and results will stop on JVM Shutdown");
			tae = new JVMShutdownBlockerTargetAlgorithmEvaluatorDecorator(tae);
		} else
		{
			log.debug("[TAE] Processing of runs and results will continue on JVM Shutdown");
		}
	
		if(!ignoreBound && options.boundRuns)
		{

			log.trace("[TAE] Bounding the number of concurrent target algorithm evaluations to {} ", options.maxConcurrentAlgoExecs);
			tae = new BoundedTargetAlgorithmEvaluator(tae, options.maxConcurrentAlgoExecs);

			
			if(options.trackRunsScheduled)
			{
				String resultFile = outputDir.getAbsolutePath() + File.separator + "queued-runs-over-time-" + numRun + ".csv";
				log.debug("[TAE] Tracking all queued runs to file {} ", resultFile);
				tae = new OutstandingRunLoggingTargetAlgorithmEvaluatorDecorator(tae, resultFile, options.trackRunsScheduledResolution, "Queued");
			}
			
			
		}else if(ignoreBound)
		{
			log.trace("[TAE] Ignoring Bound");
		}
	
		
	
		

	
		if(options.observeWalltimeIfNoRuntime)
		{
			log.debug("[TAE] Using walltime as observer runtime if no runtime is reported, scale {} , delay {} (secs)", options.observeWalltimeScale, options.observeWalltimeDelay);
			tae = new WalltimeAsRuntimeTargetAlgorithmEvaluatorDecorator(tae, options.observeWalltimeScale, options.observeWalltimeDelay);
		}
		
		
		if(options.cacheRuns)
		{
			log.debug("[TAE] Caching TAE enabled");
			tae = new CachingTargetAlgorithmEvaluatorDecorator(tae, options.cacheDebug);
			
			
		}
		
		if(options.useDynamicCappingExclusively)
		{
			log.debug("[TAE] Use Dynamic Capping Exclusively");
			tae = new UseDynamicCappingExclusivelyTargetAlgorithmEvaluatorDecorator(tae);
		}
		
		if(options.reportStrictlyIncreasingRuntimes)
		{
			log.debug("[TAE] Reporting strictly increasing runtimes");
			tae = new StrictlyIncreasingRuntimesTargetAlgorithmEvaluatorDecorator(tae);
		}
		
		
		if(options.checkResultOrderConsistent)
		{
			log.trace("[TAE] Checking that TAE honours the ordering requirement of runs");
			tae = new ResultOrderCorrectCheckerTargetAlgorithmEvaluatorDecorator(tae);
		}
		
		
		
		if(options.filecache)
		{
			log.trace("[TAE] Using a file cache for algorithm runs");

			File fileSourceCache = new File(options.fileCacheSource);

			File fileOutputCache = new File(options.fileCacheOutput);
			if(!fileSourceCache.exists() || !fileSourceCache.isDirectory())
			{
				if(!fileSourceCache.mkdirs())
				{
					throw new ParameterException("Could not create file cache source directory: " + fileSourceCache.getAbsolutePath());
				}
			}

			if(!fileOutputCache.exists() || !fileOutputCache.isDirectory())
			{
				if(!fileOutputCache.mkdirs())
				{
					throw new ParameterException("Could not create file cache output directory: " + fileOutputCache.getAbsolutePath());
				}
			}



			tae = new FileCacheTargetAlgorithmEvaluatorDecorator(tae, fileSourceCache, fileOutputCache, numRun, options.fileCacheCrashOnMiss);
		}
		
		if(options.filterZeroCutoffRuns)
		{
			log.trace("[TAE] Filtering out runs that are for zero seconds");
			tae = new CompleteZeroSecondCutoffRunsTargetAlgorithmEvaluatorDecorator(tae);
		}
		
		
		//==== Run Hash Code Verification should generally be one of the last
		// things we add since it is very sensitive to the actual runs being run. (i.e. a retried run or a change in the run may change a hashCode in a way the logs don't reveal
		if(hashVerifiersAllowed)
		{
			
			if(options.leakMemory)
			{
				LeakingMemoryTargetAlgorithmEvaluator.leakMemoryAmount(options.leakMemoryAmount);
				log.warn("[TAE] Target Algorithm Evaluators will leak memory. I hope you know what you are doing");
				tae = new LeakingMemoryTargetAlgorithmEvaluator(tae);
				
			}
			
			if(options.runHashCodeFile != null)
			{
				log.trace("[TAE] Algorithm Execution will verify run Hash Codes");
				Queue<Integer> runHashCodes = parseRunHashCodes(options.runHashCodeFile);
				tae = new RunHashCodeVerifyingAlgorithmEvalutor(tae, runHashCodes);
				 
			} else
			{
				log.trace("[TAE] Algorithm Execution will NOT verify run Hash Codes");
				
			}

		}
		
		/***
		 * !!!DO NOT ADD ANY DECORATORS THAT CAN CHANGE OR ALTER OBSERVED BEHAIVORS AFTER THIS POINT!!!
		 * !!!DO NOT ADD ANY DECORATORS THAT CAN CHANGE OR ALTER OBSERVED BEHAIVORS AFTER THIS POINT!!!
		 * !!!DO NOT ADD ANY DECORATORS THAT CAN CHANGE OR ALTER OBSERVED BEHAIVORS AFTER THIS POINT!!!
		 * 
		 * See for instance Issue #1945, #1944
		 * 
		 * It can mess up log messages and confuse users...
		 * 
		 */
		
		//==== Doesn't change anything and so is safe after the RunHashCode
		tae = new TimingCheckerTargetAlgorithmEvaluator( tae);		
		
		//==== Doesn't change anything and so is safe after the RunHashCode
		tae = new PrePostCommandTargetAlgorithmEvaluator(tae, options.prePostOptions);
		

		if(!options.skipOutstandingEvaluationsTAE)
		{
			//==== This class must be near the end as it is very sensitive to ordering of other TAEs, it does not change anything
			tae = new OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator(tae);
			log.trace("[TAE] Waiting / Monitoring outstanding target algorithm evaluations is supported");
		} else
		{
			log.debug("[TAE] Waiting / Monitoring outstanding target algorithm evaluations will NOT be supported");
		}
		
		if(options.checkRunConfigsUnique)
		{
			log.trace("[TAE] Checking that every request in a batch is unique");
			tae = new CheckForDuplicateRunConfigDecorator(tae, options.checkRunConfigsUniqueException);
		} else
		{
			log.debug("[TAE] Not Checking that every request to the TAE is unique, this may cause weird errors");
		}
		
		if(options.killCaptimeExceedingRun)
		{
			log.trace("[TAE] Killing runs that exceed there captime by a factor of {} ", options.killCaptimeExceedingRunFactor);
			tae = new KillCaptimeExceedingRunsRunsTargetAlgorithmEvaluatorDecorator(tae, options.killCaptimeExceedingRunFactor);
		}
		
		if(options.fileToWatch != null)
		{
			log.trace("[TAE] Killing runs if {} is deleted", options.fileToWatch);
			tae = new TerminateAllRunsOnFileDeleteTargetAlgorithmEvaluatorDecorator(tae, new File(options.fileToWatch));
		}
		
		if(options.warnIfNoResponseFromTAE > 0)
		{
			log.trace("[TAE] Warning if no response after {} seconds", options.warnIfNoResponseFromTAE);
			tae = new WarnOnNoWallOrRuntimeTargetAlgorithmEvaluatorDecorator(tae, options.warnIfNoResponseFromTAE);
		}
		
		if(options.transformCrashedQuality)
		{
			log.trace("[TAE] Transforming the solution quality of CRASHED runs to {}", options.transformCrashedQualityValue);
			tae = new CrashedSolutionQualityTransformingTargetAlgorithmEvaluatorDecorator(tae, options.transformCrashedQualityValue);
		}
		
		if(options.callObserverBeforeCompletion)
		{
		    tae = new CallObserverBeforeCompletionTargetAlgorithmEvaluatorDecorator(tae);
		}
		
		if(options.synchronousObserver)
		{
			log.trace("[TAE] Synchronizing notifications to the observer");
			tae = new SynchronousObserverTargetAlgorithmEvaluatorDecorator(tae);
		} else
		{
			log.debug("[TAE] Skipping synchronization of observers, this may cause weird threading issues");
		}
		
		
		
		//==== Doesn't change anything and so is safe after RunHashCode
		if(options.logRequestResponses)
		{
			log.trace("[TAE] Logging every request and response");
			tae = new LogEveryTargetAlgorithmEvaluatorDecorator(tae,options.logRequestResponsesRCOnly);
		}
		
		if(options.exitOnFailure)
        {
            tae = new ExitOnFailureTargetAlgorithmEvaluatorDecorator(tae);
        }
		
		log.debug("Final Target Algorithm Built is {}", tae);
		return tae;
	}
	
	
	
	private static Pattern RUN_HASH_CODE_PATTERN = Pattern.compile("^Run Hash Codes:\\d+( After \\d+ runs)?\\z");
	
	private static Queue<Integer> parseRunHashCodes(File runHashCodeFile) 
	{
		log.debug("Run Hash Code File Path {}", runHashCodeFile.getAbsolutePath());
		Queue<Integer> runHashCodeQueue = new LinkedList<Integer>();
		BufferedReader bin = null;
		try {
			try{
				bin = new BufferedReader(new FileReader(runHashCodeFile));
			
				String line;
				int hashCodeCount=0;
				int lineCount = 1;
				while((line = bin.readLine()) != null)
				{
					
					Matcher m = RUN_HASH_CODE_PATTERN.matcher(line);
					if(m.find())
					{
						Object[] array = { ++hashCodeCount, lineCount, line};
						log.debug("Found Run Hash Code #{} on line #{} with contents:{}", array);
						int colonIndex = line.indexOf(":");
						int spaceIndex = line.indexOf(" ", colonIndex);
						String lineSubStr = line.substring(colonIndex+1,spaceIndex);
						runHashCodeQueue.add(Integer.valueOf(lineSubStr));
						
					} 
					lineCount++;
				}
				if(hashCodeCount == 0)
				{
					log.warn("Hash Code File Specified, but we found no hash codes");
				}
			
			} finally
			{
				if(bin != null) bin.close();
			}
			
		} catch(IOException e)
		{
			throw new RuntimeException(e);
		}
		
		
		return runHashCodeQueue;
		
	}

	
	
}

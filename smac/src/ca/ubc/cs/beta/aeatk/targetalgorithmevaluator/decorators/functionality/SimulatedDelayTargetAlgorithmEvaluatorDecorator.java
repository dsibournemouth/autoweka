package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.StatusVariableKillHandler;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Simulates the Delays and Observer information for TAEs that work nearly instantaneously.
 * <br>
 * <b>NOTE:</b>This Decorator assumes that all runs can execute simultaneously and so roughly the delay is given by the maximum runtime of any response.
 * If you need to otherwise serialize or limit these, you should wrap this decorator with the <code>BoundedTargetAlgorithmEvaluator</code>. 
 * <br>
 * <b>NOTE:</b>The instantaneous aspect of the above is KEY. This is written as if calls to evaluateRun() on the wrapped TAE are quick, and while it does start timing before it is invoked, asynchronous calls will become
 * very synchronous. Additionally we do not pass the observer to the wrapper TAE, so there will be no updates to the client or any kills until long after this is complete.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class SimulatedDelayTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	
	private final long observerFrequencyMs;

	//A cached thread pool is used here because another decorator will handle bounding the number of runs, if necessary.
	private final ExecutorService execService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("Simulated Delay Target Algorithm Evaluator Callback Thread"));
	
	private static final Logger log = LoggerFactory.getLogger(SimulatedDelayTargetAlgorithmEvaluatorDecorator.class);
	
	private final AtomicInteger threadsWaiting = new AtomicInteger(0);

	private double timeScalingFactor;
	
	public SimulatedDelayTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae, long observerFrequencyMs, double scalingFactor) {
		super(tae);
		this.observerFrequencyMs = observerFrequencyMs;
		this.timeScalingFactor = scalingFactor;
		if(observerFrequencyMs <= 0) throw new IllegalArgumentException("Observer Frequency cannot be zero");
	}


	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		 
		return evaluateRunConfigs(runConfigs, obs);
		
	}


	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, final TargetAlgorithmEvaluatorRunObserver obs) {
	
		
		execService.execute(new Runnable()
		{
			@Override
			public void run() {
				try {
					handler.onSuccess(evaluateRunConfigs(runConfigs, obs));
				} catch(RuntimeException e)
				{
					handler.onFailure(e);
				}
				
			}
			
		});
		
	}
	

	@Override
	public boolean areRunsObservable()
	{
		//We support a limited form of Observation
		return true;
	}
	
	@Override
	public boolean areRunsPersisted()
	{
		return false;
	}
	
	
	@Override
	public void postDecorateeNotifyShutdown()
	{
		this.execService.shutdown();
		try {
			this.execService.awaitTermination(24, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	
	/**
	 * Evaluate the runConfigs, and notify the observer as appropriate
	 * @param runConfigs 		runConfigs
	 * @param obs				observer
	 * @param asyncReleaseLatch	latch that we will decrement if we sleep (this is used for async evaluation)
	 * @return
	 */
	private List<AlgorithmRunResult> evaluateRunConfigs(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs)
	{
		try {
			threadsWaiting.incrementAndGet();
			
			TimeSimulator timeSim = new TimeSimulator(timeScalingFactor);
			long startTimeInMS = System.currentTimeMillis();
			//We don't pass the Observer to the decorated TAE because it might report too much too soon.
			//We also make this list unmodifiable so that we don't accidentally tamper with it.
			
			Set<String> configIDs = new HashSet<String>();
			
			for(AlgorithmRunConfiguration rc : runConfigs)
			{
				configIDs.add(rc.getParameterConfiguration().getFriendlyIDHex());
			}
			
			
			log.trace("Scheduling runs synchronously for configs {}", configIDs);
			
			final List<AlgorithmRunResult> runsFromWrappedTAE = Collections.unmodifiableList(tae.evaluateRun(runConfigs, null));
			double timeToSleepInSeconds = Double.NEGATIVE_INFINITY;
			//Stores a mapping of Run Config objects to Algorithm Run Objects
			//The kill handlers may modify these.
			final LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> runConfigToAlgorithmRunMap = new LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
			final LinkedHashMap<AlgorithmRunConfiguration, KillHandler> runConfigToKillHandlerMap = new LinkedHashMap<AlgorithmRunConfiguration, KillHandler>();
			
			AlgorithmRunResult mostExpensiveRun = null;
			for(AlgorithmRunResult run : runsFromWrappedTAE)
			{
				double oldTimeToSleep = timeToSleepInSeconds;
				timeToSleepInSeconds = Math.max(timeToSleepInSeconds, Math.max(run.getRuntime(), run.getWallclockExecutionTime()));
				
				if(oldTimeToSleep != timeToSleepInSeconds)
				{
					mostExpensiveRun = run;
				}
				runConfigToKillHandlerMap.put(run.getAlgorithmRunConfiguration(), new StatusVariableKillHandler() );
				runConfigToAlgorithmRunMap.put(run.getAlgorithmRunConfiguration(), new RunningAlgorithmRunResult( run.getAlgorithmRunConfiguration(), 0,0,0, run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getSeed(),0, null));
				
			}
			
			double oRigTimeToSleep = timeToSleepInSeconds;
			timeToSleepInSeconds = timeToSleepInSeconds / this.timeScalingFactor;
			
			Object[] args = {  oRigTimeToSleep, timeScalingFactor, timeToSleepInSeconds,  configIDs, getNicelyFormattedWakeUpTime(timeToSleepInSeconds), threadsWaiting.get()}; 
			log.debug("Simulating {} seconds elapsed with time scaling factor {} for a total of {} seconds of running for configs ({}) . Wake-up estimated in/at: {}  ( ~({}) threads currently waiting )", args);
			
			 
			StopWatch sleepTimeWatch = new AutoStartStopWatch();
			sleepAndNotifyObservers(timeSim, startTimeInMS,  oRigTimeToSleep, obs, runsFromWrappedTAE, runConfigs, runConfigToKillHandlerMap, runConfigToAlgorithmRunMap);
			Object[] args2 = {  oRigTimeToSleep, timeScalingFactor, timeToSleepInSeconds,  configIDs, sleepTimeWatch.stop()/ 1000.0};
			log.debug("Done simulating {} seconds elapsed with time scaling factor {} for a total of {} seconds of running for configs ({}). Total sleep time : {} seconds", args2);
			if(obs == null)
			{ 
				//None of the runResultsChanged so we can return them unmodified
				return runsFromWrappedTAE;
			} else
			{
				//Build a new list of run results based on how the map changed
				List<AlgorithmRunResult> completedRuns = new ArrayList<AlgorithmRunResult>(runsFromWrappedTAE.size());
				for(AlgorithmRunResult run : runsFromWrappedTAE)
				{
					
					AlgorithmRunResult newRun = runConfigToAlgorithmRunMap.get(run.getAlgorithmRunConfiguration());
					if(!newRun.isRunCompleted())
					{
						log.error("Expected all runs to be returned would be done by now, however this run isn't {}.  ", newRun );
						for(AlgorithmRunResult runFromTAE : runsFromWrappedTAE)
						{
							log.error("Response from TAE was this run {}", runFromTAE);
						}
						
						throw new IllegalStateException("Expected that all runs would be completed by now, but not all are");
					}
					
					if(newRun.equals(mostExpensiveRun))
					{
						newRun = new ExistingAlgorithmRunResult(newRun.getAlgorithmRunConfiguration(), newRun.getRunStatus(), newRun.getRuntime(), newRun.getRunLength(),newRun.getQuality(),newRun.getResultSeed(), newRun.getAdditionalRunData(), timeToSleepInSeconds);
					}
					completedRuns.add(newRun);
					
				}
				
				return completedRuns;
			}
			
		} finally
		{
			threadsWaiting.decrementAndGet();
		}
	}

	private void sleepAndNotifyObservers(TimeSimulator timeSimulator, long startTimeInMs, double maxRuntime, TargetAlgorithmEvaluatorRunObserver observer, List<AlgorithmRunResult> runsFromWrappedTAE, List<AlgorithmRunConfiguration> runConfigs, final LinkedHashMap<AlgorithmRunConfiguration, KillHandler> khs, final LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> runResults)
	{
		
		long sleepTimeInMS = (long) (maxRuntime * 1000);
		do {
			long waitTimeRemainingMs;
			
			long currentTimeInMs =  timeSimulator.time();
			if(observer != null)
			{
				updateRunsAndNotifyObserver(startTimeInMs, currentTimeInMs, maxRuntime, observer, runsFromWrappedTAE, runConfigs, khs, runResults);
			}	
			
			
			
			//In case the observers took significant amounts of time, we get the time again
			currentTimeInMs = timeSimulator.time();
			waitTimeRemainingMs =  startTimeInMs - currentTimeInMs +  sleepTimeInMS; 
		
			boolean allKilled = true;
			for(AlgorithmRunConfiguration rc : runConfigs)
			{
				
				if(!runResults.get(rc).getRunStatus().equals(RunStatus.KILLED))
				{
					allKilled = false;
					break;
				}
						
			}
			
			if(waitTimeRemainingMs <= 0 || allKilled)
			{
				break;
			} else
			{
				long sleepTime = Math.min(observerFrequencyMs, waitTimeRemainingMs);
				if(sleepTime > 0)
				{
					try {
						
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						
						//We are interrupted we are just going to return the measured runs
						throw new TargetAlgorithmAbortException(e);
					}
				
				} 	
			}
		}
		while( true );
		
		//Everything should be done at this time 
		//We throw the time into the future a bit to clean up anything that is still running because of a small number of runs.
		long simulatedCurrentTimeInMs =  Long.MAX_VALUE;
		updateRunsAndNotifyObserver(startTimeInMs, simulatedCurrentTimeInMs, maxRuntime, observer, runsFromWrappedTAE, runConfigs, khs, runResults);
			
	}
	
	private void updateRunsAndNotifyObserver(long startTimeInMs, long currentTimeInMs, double maxRuntime, TargetAlgorithmEvaluatorRunObserver observer, List<AlgorithmRunResult> runsFromWrappedTAE, List<AlgorithmRunConfiguration> runConfigs, final LinkedHashMap<AlgorithmRunConfiguration, KillHandler> killHandlers, final LinkedHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> runConfigToAlgorithmRunMap)
	{

		List<AlgorithmRunResult> kars = new ArrayList<AlgorithmRunResult>(runsFromWrappedTAE.size());
		//Update the table
		
		for(AlgorithmRunResult run : runsFromWrappedTAE)
		{
		
			AlgorithmRunConfiguration rc  = run.getAlgorithmRunConfiguration();
			
			double currentRuntime = (currentTimeInMs - startTimeInMs) / 1000.0;
			if(runConfigToAlgorithmRunMap.get(rc).isRunCompleted())
			{
				//We don't need to do anything because the run is already done
			} else if(currentRuntime >=  run.getRuntime())
			{
				//We are done and can simply throw this run on the list
				runConfigToAlgorithmRunMap.put(rc, run);
			} else if(killHandlers.get(rc).isKilled())
			{
				//We should kill this run
				//log.warn("We should kill this run");
				runConfigToAlgorithmRunMap.put(rc, new ExistingAlgorithmRunResult( rc, RunStatus.KILLED, currentRuntime, 0, 0, rc.getProblemInstanceSeedPair().getSeed(),"Killed by " + getClass().getSimpleName(), currentRuntime));
			} else
			{
				//Update the run
				runConfigToAlgorithmRunMap.put(rc, new RunningAlgorithmRunResult( run.getAlgorithmRunConfiguration(), currentRuntime,0,0, run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getSeed(), currentRuntime, killHandlers.get(rc)));
			}
			
			AlgorithmRunResult currentRun = runConfigToAlgorithmRunMap.get(rc);
			
			kars.add(currentRun);

		}	
		
		if(observer != null)
		{
			observer.currentStatus(kars);
		}
	}
	
	
	
	private String getNicelyFormattedWakeUpTime(double timeToSleep)
	{
		
		long time = System.currentTimeMillis()  + (long) (timeToSleep * 1000);
		Date d = new Date(time);

		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
		String releaseTime = df.format(d);
		
		if(timeToSleep * 1000 > 86400000)
		{
			releaseTime =  timeToSleep * 1000 / 86400000 + " days " + releaseTime;
		}
		
		return releaseTime;
	}
	
	
	
	private static class TimeSimulator
	{
		private long initialTime;

		private double scale;
		public TimeSimulator(double scale)
		{
			this.initialTime = System.currentTimeMillis();
			this.scale = scale;
		}
		
		public long time()
		{
			return (long) (initialTime + ((System.currentTimeMillis()) - initialTime) * scale);
		}
	}
	
}

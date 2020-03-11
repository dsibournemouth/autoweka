package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.PartialResultsAggregator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractAsyncTargetAlgorithmEvaluatorDecorator;

/**
 * Executions of runs executed via the lowPriority method, will be aborted and restarted if a high priority run is created.
 *
 * LIMITATIONS: This decorator may cause kill() requests to be ignored, if it decides that a low priority run should be killed,
 * when the client also decides that it should be killed, the fact that the client asked it to be killed will be lost.
 * 
 * 
 * We achieve this effect by doing the following:
 * 
 * 
 * Submission of High Priority Runs: 
 * 
 * When we submit high priority runs, we start a timer that after 5 seconds increments a counter once for every run we submitted.
 * 
 * When we observe runs that have started (runtime > 0 || walltime > 0 || !RUNNING), we decrement that counter. 
 * 
 * That means that after 5 seconds, when the timer fires, it will be positive only if there are runs that haven't started.
 * 
 * If the evaluateRunsAsync() method returns, we consider all runs started.
 *  
 * Submission of Low Priority Runs:
 * 
 * We submit the runs with modified observer and callbacks.
 * 
 * In the observer, if we see that there are blocking runs we kill the runs (once), and temporarily lower the blockingRunsCounter for each started run we kill (and kill all unstarted runs)
 * 
 * When the runs return we increment the counter again (SEE EXAMPLE BELOW), for why.
 * 
 * When the callback fires, we look at runs we killed and didn't complete, and then resubmit them.
 * 
 * EXAMPLE:
 * 
 * If the TAE has 4 cores, and I submit (left is first)  L1,L2,L3,L4,L5,L6
 * 
 * Then they start running (in brackets):
 * 
 * (L1,L2,L3,L4),L5,L6
 * 
 * If I then submit a high priority run:
 * 
 * (L1,L2,L3,L4),L5,L6,H1
 * 
 * We don't want to kill all of L1-L4, only one of them.
 * 
 * So based on which observer fires first, say L5 (which hasn't started):
 * 
 * (L1,L2,L3,L4),L6,H1,L5*
 * 
 * We unfortunately still need to kill a run, so when L5 returns as killed, we increment the counter of blocking methods, since we decremented it.
 * 
 * Then say L3 checks:
 * 
 * (L1,L2,L6,L4),H1,L5*,L3*
 * 
 * We again unfortunately need to kill a run because L6 started instead.
 * 
 * It's possible (though unlikely) that L5 and/or L3 both get killed and restarted again, but say that L4 checks:
 * 
 * (L1,L2,L6,H1),L5*,L3*,L4*.
 *
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class PreemptingTargetAlgorithmEvaluator extends
		AbstractAsyncTargetAlgorithmEvaluatorDecorator {

	
	private  final ScheduledExecutorService execService = Executors.newScheduledThreadPool(1, new SequentiallyNamedThreadFactory(PreemptingTargetAlgorithmEvaluator.class.getSimpleName() + " execution", true));
	
	private  final AtomicInteger blockingMethods = new AtomicInteger();
	
	private static final Logger log = LoggerFactory.getLogger(PreemptingTargetAlgorithmEvaluator.class);
	
	
	private  final AtomicInteger highPriorityRunsSubmitted = new AtomicInteger(0);
	private  final AtomicInteger lowPriorityAndRetriedRunsSubmitted = new AtomicInteger(0);
	private final AtomicDouble lowPriorityCPUTimeLost = new AtomicDouble(0);
	
	private  final AtomicDouble lowPriorityWallTimeLost = new AtomicDouble(0);
	
	private  final AtomicInteger retriedRuns = new AtomicInteger(0);
	
	public PreemptingTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae) {
		super(tae);

	}

	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver observer) {
		
		

		highPriorityRunsSubmitted.addAndGet(runConfigs.size());
		//Keeps track of how many runs we haven't started.
		//A run is started when we observe that it has  runtime > 0, or isCompleted(), _OR_
		//the method returned;
		final AtomicInteger nonStartedRuns = new AtomicInteger(runConfigs.size());
		Runnable incrementRunnable = new Runnable()
		{
			@Override
			public void run() {
				log.trace("Incremented Blocked Runs by {} ", runConfigs.size());
				blockingMethods.addAndGet(runConfigs.size());
			}
			
		};
		
		execService.schedule(incrementRunnable, 2, TimeUnit.SECONDS);
		
		TargetAlgorithmEvaluatorRunObserver taeObserver = new TargetAlgorithmEvaluatorRunObserver()
		{

			private final Set<AlgorithmRunConfiguration> startedRunConfigurations = new HashSet<AlgorithmRunConfiguration>();
			@Override
			public synchronized void currentStatus(List<? extends AlgorithmRunResult> runs) {
				
			
				if(nonStartedRuns.get() > 0)
				{
completeLoop:	
					for(AlgorithmRunResult run : runs)
					{
						if(run.isRunCompleted() || run.getRuntime() > 0 || run.getWallclockExecutionTime() > 0)
						{
							
							if(!startedRunConfigurations.contains(run.getAlgorithmRunConfiguration()))
							{
								//Need to make sure we didn't already count this as started because 
								//the method returned
								
								startedRunConfigurations.add(run.getAlgorithmRunConfiguration());
								
								int presentNonStartedRuns;
								
								//Decrements the nonStartedRuns IFF it's greater than zero.
								do
								{
									presentNonStartedRuns = nonStartedRuns.get();
									
									if(presentNonStartedRuns == 0 )
									{	//All runs have started, but they must have started because the method returned.
										//Since we didn't see the startedRunConfigurations.
										break completeLoop;
									}
									
								}while(!nonStartedRuns.compareAndSet(presentNonStartedRuns, presentNonStartedRuns-1));
								blockingMethods.decrementAndGet();
							
							}
						}
						
						
					}
				}
				
				if(observer != null)
				{
					observer.currentStatus(runs);
				}
				
			}
			
		};
		
		try 
		{
			tae.evaluateRunsAsync(runConfigs, callback, taeObserver);
		} finally
		{
			int remainingNonStartedRuns = nonStartedRuns.getAndSet(0);
			blockingMethods.addAndGet(-remainingNonStartedRuns);
			log.trace("On completion of runs blockingMethods is {}, fixed {} runs", blockingMethods.get(), remainingNonStartedRuns);
		}
		
	}
	
	
	
	private void evaluateRunsAsyncLowPriority(List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver observer) {
		
		
		lowPriorityAndRetriedRunsSubmitted.addAndGet(runConfigs.size());
		final AtomicInteger incrementsToBlockingMethodsWhenDone = new AtomicInteger(0);
		
		final Set<AlgorithmRunConfiguration> killedRuns = Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunConfiguration, Boolean>());
		
		
		final PartialResultsAggregator pra = new PartialResultsAggregator(runConfigs);
		
		
		TargetAlgorithmEvaluatorRunObserver taeObserver = new TargetAlgorithmEvaluatorRunObserver()
		{
			
			@Override
			public synchronized void currentStatus(List<? extends AlgorithmRunResult> runs) {
				
				
				
				if(blockingMethods.get() > 0)
				{ 	
					log.trace("Detected {} blocking methods", blockingMethods.get());
					
					//First determine how runs can possibly be killed.
					int runningCount = 0;
					
					int startedCount = 0;
					for(AlgorithmRunResult run : runs)
					{
						if(!run.isRunCompleted() && !killedRuns.contains(run.getAlgorithmRunConfiguration()))
						{
							runningCount++;
							
							if(isStarted(run))
							{
								startedCount++;
							}
						}
					}
					
					if(runningCount > 0)
					{
						
					
						int runsToTerminate = 0;
						
						//Then determine how many runs we are going to terminate
						//We decrement the blockingMethods one for each time.
						
	escapeLoop:		
						while(true)
						{
							int currentRuns;
							do
							{
								currentRuns = blockingMethods.get();
								
								if(currentRuns == 0)
								{
									break escapeLoop;
								}
							}while(!blockingMethods.compareAndSet(currentRuns, currentRuns-1));
							
							
							
							runsToTerminate++;
							
							if(runsToTerminate == startedCount)
							{
								break;
							}
							
						}
						
					
						
						if(runsToTerminate > 0)
						{
							//We only do this once to keep track of the accurate counting (we wouldn't want to count the same run being killed, as two slots being freed)
							
							
							incrementsToBlockingMethodsWhenDone.addAndGet(runsToTerminate);
							int kills =0;
							
							for(AlgorithmRunResult run : runs )
							{
							
								if(!run.isRunCompleted())
								{
									killedRuns.add(run.getAlgorithmRunConfiguration());
									run.kill();
									kills++;
								}	
								
							}
							
									
							//log.trace("Due to blocking high priority runs, killed {} low priority runs, currently blocking runs {} . When run is completed we will do {} increments ", kills, blockingMethods.get(), incrementsToBlockingMethodsWhenDone.get());
						} 
					} else
					{
						log.trace("No currently running runs detected");
					}
				}
				
				//Continue trying to kill run
				for(AlgorithmRunResult run : runs)
				{
					if(killedRuns.contains(run.getAlgorithmRunConfiguration()))
					{	
						run.kill();
					}
				}
				
				if(observer != null)
				{
					observer.currentStatus(runs);
				}
				
			}
			
		};
		
		TargetAlgorithmEvaluatorCallback taeCallback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				try
				{
					
					for(AlgorithmRunResult run : runs)
					{
						if(!run.getRunStatus().equals(RunStatus.KILLED) || !killedRuns.contains(run.getAlgorithmRunConfiguration()))
						{
							pra.updateCompletedRun(run);
						} else
						{
							lowPriorityCPUTimeLost.addAndGet(run.getRuntime());
							lowPriorityWallTimeLost.addAndGet(run.getWallclockExecutionTime());
						}
					}
					
					
					if(pra.isCompleted())
					{
						if(callback != null)
						{
							callback.onSuccess(pra.getCurrentRunStatusOnCompletion());
						}
					} else
					{
						
						log.debug("Low Priority Runs were killed in place of high priority runs, {} runs will be rescheduled", pra.getOutstandingRunConfigurations().size());
						
						retriedRuns.addAndGet(pra.getOutstandingRunConfigurations().size());
						recursivelyRetryRuns(pra, callback, observer);
					}
					
					
					
				} finally
				{
					blockingMethods.addAndGet(incrementsToBlockingMethodsWhenDone.getAndSet(0));
				}
			}

			@Override
			public void onFailure(RuntimeException e) {
				try 
				{
					
					if(callback != null)
					{
						callback.onFailure(e);
					}
				} finally
				{
					blockingMethods.addAndGet(incrementsToBlockingMethodsWhenDone.getAndSet(0));
				}
				
			}
			
		};
		
		tae.evaluateRunsAsync(runConfigs, taeCallback, taeObserver);
	}
	
	private static boolean isStarted(AlgorithmRunResult run)
	{
		return (run.getRuntime() > 0 || run.getWallclockExecutionTime() >0 || run.isRunCompleted());
	}
	
	private void recursivelyRetryRuns(final PartialResultsAggregator pra, final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver observer)
	{
		
		List<AlgorithmRunConfiguration> outstanding = new ArrayList<>(pra.getOutstandingRunConfigurations());
		
		
		TargetAlgorithmEvaluatorCallback newCallback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				
				pra.updateCompletedRuns(runs);
				
				if(callback != null)
				{
					callback.onSuccess(pra.getCurrentRunStatusOnCompletion());
				}
				
			}

			@Override
			public void onFailure(RuntimeException e) {
				callback.onFailure(e);
			}
			
		};
		
		TargetAlgorithmEvaluatorRunObserver obs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				pra.updateCurrentRunStatus(runs);
				
				if(observer != null)
				{
					observer.currentStatus(pra.getCurrentRunStatusForObserver());
				}
			}
			
		};
		
		nonBlockingLowPriorityTAE.evaluateRunsAsync(outstanding, newCallback, obs);
	}
	
	
	
	
	
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//log.info(getClass().getSimpleName() + " on shutdown we had served {} high priority requests, {} low priority requests. We retried {} low priority requests,"
		//		+ " and consequently lost {} (s) of cpu time and {} (s) of algorithm execution wall time. The current number of high priority runs that are stalled is: {}", this.highPriorityRunsSubmitted.get(), this.lowPriorityAndRetriedRunsSubmitted.get() - this.retriedRuns.get(), this.retriedRuns.get(), this.lowPriorityCPUTimeLost.get(), this.lowPriorityWallTimeLost.get(), this.blockingMethods.get());
	}

	

	
	private final TargetAlgorithmEvaluator lowPriorityTAE = new LowPriorityTargetAlgorithmEvaluatorDecorator();
	private final TargetAlgorithmEvaluator nonBlockingLowPriorityTAE = new NonBlockingAsyncTargetAlgorithmEvaluatorDecorator(lowPriorityTAE);
	
	
	public TargetAlgorithmEvaluator getLowPriorityTargetAlgorithmEvaluator()
	{
		return lowPriorityTAE;
	}
	
	
	private class LowPriorityTargetAlgorithmEvaluatorDecorator extends AbstractAsyncTargetAlgorithmEvaluatorDecorator
	{

		public LowPriorityTargetAlgorithmEvaluatorDecorator() {
			super(PreemptingTargetAlgorithmEvaluator.this);
		}

		@Override
		public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
				final TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer) {
			PreemptingTargetAlgorithmEvaluator.this.evaluateRunsAsyncLowPriority(runConfigs, callback, observer);
		}
		
		@Override
		protected void postDecorateeNotifyShutdown() {
		//NOOP
			
		}
		
	}
	
}

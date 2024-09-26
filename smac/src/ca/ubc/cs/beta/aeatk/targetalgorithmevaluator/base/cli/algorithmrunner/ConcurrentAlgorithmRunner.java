package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.algorithmrunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.StatusVariableKillHandler;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Processes calls to the command line concurrently.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 * 
 */

public class ConcurrentAlgorithmRunner {

	

	private static final Logger log = LoggerFactory.getLogger(ConcurrentAlgorithmRunner.class);
	
	/**
	 * Stores the run configurations of the target algorithm
	 */
	protected final List<AlgorithmRunConfiguration> runConfigs;

	/**
	 * Stores the actual set of runs.
	 */
	protected final List<Callable<AlgorithmRunResult>> runs;
	
	/**
	 * Semaphore which is used for inter thread communication between the observers of the individual runs,
	 * and the master observer which notifies the users callback.
	 */
	private final Semaphore updatedRunMapSemaphore = new Semaphore(0);
	
	/**
	 * Future which keeps track of the observer thread.
	 */
	private final Future<?>  runStatusWatchingFuture;
	
	/**
	 * Used to coordinate the observer, if the observer runs, this latch is released when it is done.
	 */
	private final CountDownLatch observerThreadTerminated = new CountDownLatch(1);
	
	/**
	 * Used to coordinate the observer, the observer terminates right away if this true.
	 */
	private final AtomicBoolean observerThreadStarted = new AtomicBoolean(false);
	/**
	 * Default Constructor 
	 * @param execConfig	execution configuration of target algorithm
	 * @param runConfigs	run configurations to execute
	 * @param numberOfConcurrentExecutions	number of concurrent executions allowed
	 * @param obs 
	 * @param executionIDs 
	 */

	public ConcurrentAlgorithmRunner(final List<AlgorithmRunConfiguration> runConfigs, final int numberOfConcurrentExecutions, final TargetAlgorithmEvaluatorRunObserver obs, final CommandLineTargetAlgorithmEvaluatorOptions options, final BlockingQueue<Integer> executionIDs, final ExecutorService execService) {
		if(runConfigs == null)
		{
			throw new IllegalArgumentException("Arguments cannot be null");
		}

		this.runConfigs = runConfigs;
		this.runs = new ArrayList<Callable<AlgorithmRunResult>>(runConfigs.size());
		
		//maps the run configs to the most recent update we have
		final ConcurrentHashMap<AlgorithmRunConfiguration,AlgorithmRunResult> runConfigToLatestUpdatedRunMap = new ConcurrentHashMap<AlgorithmRunConfiguration,AlgorithmRunResult>(runConfigs.size());
		
		//Maps runconfigs to the index in the supplied list
		final ConcurrentHashMap<AlgorithmRunConfiguration, Integer> runConfigToPositionInListMap = new ConcurrentHashMap<AlgorithmRunConfiguration,Integer>(runConfigs.size());
		
		int i=0; 
		
		//Initializes data structures for observation
		for(final AlgorithmRunConfiguration rc: runConfigs)
		{
			KillHandler killH = new StatusVariableKillHandler();
			
			runConfigToPositionInListMap.put(rc, i);
			runConfigToLatestUpdatedRunMap.put(rc, new RunningAlgorithmRunResult( rc, 0,0,0, rc.getProblemInstanceSeedPair().getSeed(), 0, killH));
			
			TargetAlgorithmEvaluatorRunObserver individualRunObserver = new TargetAlgorithmEvaluatorRunObserver()
			{
				@Override
				public void currentStatus(List<? extends AlgorithmRunResult> runs) {
					
					/**
					 * If the map already contains something for our runConfig that is completed, but
					 * we are not completed then there is some bug or other race condition.
					 * 
					 * TAEs should not notify us of an incompleted run after it has been marked completed..
					 */
					if(runConfigToLatestUpdatedRunMap.get(runs.get(0).getAlgorithmRunConfiguration()).isRunCompleted() && !runs.get(0).isRunCompleted())
					{
						StringBuilder sb = new StringBuilder("Current Run Status being notified: " + runs.get(0).getAlgorithmRunConfiguration());
						sb.append("\n Current status in table").append(runConfigToLatestUpdatedRunMap.get(runs.get(0).getAlgorithmRunConfiguration()).getAlgorithmRunConfiguration());
						IllegalStateException e = new IllegalStateException("RACE CONDITION: " + sb.toString());
						
						//We are logging this here because this may cause a dead lock somewhere else ( since the runs will never finish ), and the exception never handled.
						log.error("Some kind of race condition has occurred", e);
						e.printStackTrace();
						throw e;
					}
					
					runConfigToLatestUpdatedRunMap.put(runs.get(0).getAlgorithmRunConfiguration(), runs.get(0));
					
					updatedRunMapSemaphore.release();
				}
			};

			final Callable<AlgorithmRunResult> run = new CommandLineAlgorithmRun( rc,individualRunObserver, killH, options, executionIDs); 
			runs.add(run);
			i++;
		}
			
		
	
		i++;
		
		//==== Watches the map of runs in this group, and on changes notifies the observer
		Runnable runStatusWatchingThread = new Runnable()
		{

			@Override
			public void run() {
				try {
					
					try 
					{
						if(!observerThreadStarted.compareAndSet(false, true))
						{
							return;
						}
						
						doLoop();
					} finally
					{
						observerThreadTerminated.countDown();
					}
					
						
					
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			
			public void doLoop() throws InterruptedException
			{
				long startOfTimer=0;
				while(true)
				{
						long lastUpdate = System.currentTimeMillis();
						//This will release either because some run updated
						//or because we are done
						
						updatedRunMapSemaphore.acquire();
						
						updatedRunMapSemaphore.drainPermits();
						
						AlgorithmRunResult[] runs = new AlgorithmRunResult[runConfigs.size()];
						
						//We will quit if all runs are done
						boolean outstandingRuns = false;
						
						long delta = System.currentTimeMillis() - startOfTimer;
						
						if(delta < options.observerFrequency)
						{
							Thread.sleep(Math.max(1, options.observerFrequency - delta + 20));
						}
						
						for(Entry<AlgorithmRunConfiguration,AlgorithmRunResult> entries : runConfigToLatestUpdatedRunMap.entrySet())
						{
							AlgorithmRunResult run = entries.getValue();
							if(run.getRunStatus().equals(RunStatus.RUNNING))
							{
								outstandingRuns = true;
							}
							runs[runConfigToPositionInListMap.get(entries.getKey())]=run;
						}
						

						try {
							List<AlgorithmRunResult> runList = Arrays.asList(runs);
							if(obs != null)
							{
								obs.currentStatus(runList);
							}
							startOfTimer = System.currentTimeMillis();
							
						} catch(RuntimeException e)
						{
							log.error("Error occured while notifying observer ", e);
							throw e;
						}
						
						if(!outstandingRuns)
						{
							
							break;
						}
						
					
				}
			}
			
		};
		
		runStatusWatchingFuture = execService.submit(runStatusWatchingThread);	
	}

	public synchronized List<AlgorithmRunResult> run(ExecutorService p) {
		//
		/*
		 * Runs all algorithms in the thread pool
		 * Tells it to shutdown
		 * Waits for it to shutdown
		 * 
		 */
		try {
			
			List<AlgorithmRunResult> results = new ArrayList<AlgorithmRunResult>();
			List<Callable<AlgorithmRunResult>> runsToDo = runs;
			
			
			List<Future<AlgorithmRunResult>> futures = p.invokeAll(runsToDo);
			
			try 
			{
				for(Future<AlgorithmRunResult> futRuns : futures)
				{
					AlgorithmRunResult run;
					try {
						run = futRuns.get();
					} catch (ExecutionException e) 
					{
						if(e.getCause() instanceof TargetAlgorithmAbortException)
						{
							throw (TargetAlgorithmAbortException) e.getCause();
						}
						throw new IllegalStateException("Unexpected exception occurred while trying to run algorithm", e);
					}
					if (run.getRunStatus().equals(RunStatus.ABORT))
					{
						throw new TargetAlgorithmAbortException(run);
					}
					
					results.add(run);
				}
				
				return results;
			} finally
			{
				for(Future<AlgorithmRunResult> future : futures)
				{
					future.cancel(true);
				}
				
				runStatusWatchingFuture.cancel(true);
				
				boolean alreadyStarted = observerThreadStarted.getAndSet(true);
				
				if(alreadyStarted)
				{
					while(!observerThreadTerminated.await(10, TimeUnit.MINUTES))
					{
						log.warn("Awaiting shutdown of Target Algorithm Evaluator Observer Thread did not complete within 10 minutes");
					}
				}
			}
			
			
			
		} catch (InterruptedException e) {
			//TODO We probably need to actually abort properly
			//We can't just let something else do it, I think.
			//Additionally runs are in an invalid state at this point
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while processing runs");
		}
		
		
	}

}

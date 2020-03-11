package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

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
import ca.ubc.cs.beta.aeatk.concurrent.FairMultiPermitSemaphore;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException;
/**
 * Ensures that a Target Algorithm Evaluator gets no more than a certain number of runs issued simultaneously.
 * <p>
 * <b>Implementation Details:</b> This class is particularly tricky with regards to it's synchronization.
 * Effectively we hand the wrapped TargetAlgorithmEvaluator slices of the List&gt;RunConfig&lt; we want evaluated, along with new observers and callbacks. 
 * The observers and callbacks update a shared global data structure and then asynchronously invoke another Runnable to update the clients.
 * 
 * Care must be taken to ensure that we don't notify the client with out of order data. We also don't use the same callback thread to notify the client
 * because it can create deadlocks (if the callback causes more runs to submit, it can get trapped here).
 * <p>
 * In the case of updating the observer, we update the shared data structure of ALL the runs and then notify the client's observer.
 * <p>
 * In the case of processing the callback, we update for all the outstanding runs to be completed, and then execute the client's callback. 
 * <p>
 * <b>Thread Safety:</b> All concurrent requests are serialized via the fair <code>enqueueLock</code> object. Callback and Observers use the runConfig object as a Mutex to prevent concurrent access.
 * Callback and Observers are notified via a new thread. This prevents evaluateRunAsync from being entered in twice by the same thread. 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class BoundedTargetAlgorithmEvaluator extends
	AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator {

	
	/**
	 * Used to serialize requests (so that each run is processed completely in serial instead of being interleaved with other runs)
	 */
	private final ReentrantLock enqueueLock = new ReentrantLock(true);
	
	/**
	 * Stores the number of permitted runs that can go past this TAE at any time
	 */
	private final FairMultiPermitSemaphore availableRuns;

	
	private final static Logger log = LoggerFactory.getLogger(BoundedTargetAlgorithmEvaluator.class);

	private final ExecutorService execService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory("Bounded Target Algorithm Evaluator Callback Thread"));
	
	private final int NUMBER_OF_CONCURRENT_RUNS;

	public static final String KILLED_BY_DECORATOR_ADDL_RUN_INFO = "Kill intercepted by decorator before dispatch to TAE";

	public BoundedTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae, int numberOfConcurrentRuns) 
	{
		super(tae);
		if(numberOfConcurrentRuns <= 0) throw new IllegalArgumentException("Must be able to schedule at least one run");
		this.availableRuns = new FairMultiPermitSemaphore(numberOfConcurrentRuns);
		this.NUMBER_OF_CONCURRENT_RUNS = numberOfConcurrentRuns;
	}

	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback handler, final TargetAlgorithmEvaluatorRunObserver obs) {

		if(runConfigs.isEmpty())
		{
			handler.onSuccess(Collections.<AlgorithmRunResult> emptyList());
			return;
		}
		
		if(Thread.interrupted())
		{
			Thread.currentThread().interrupt();
			handler.onFailure(new TargetAlgorithmEvaluatorShutdownException(new InterruptedException()));
			return;
			
		}
		
		if(this.enqueueLock.isHeldByCurrentThread())
		{
			//This is a paranoid check to ensure that the same thread can't recursively call this method somehow as it breaks the logic
			throw new IllegalStateException("Current Thread already holds the lock");
		}
		try {	
			enqueueLock.lock();
			
			//==== Stores the order of RunConfigs to put in all lists we return to the caller.
			final Map<AlgorithmRunConfiguration, Integer> orderOfRuns = new ConcurrentHashMap<AlgorithmRunConfiguration, Integer>();
			
			//Stores the completed runs
			final Set<AlgorithmRunResult> completedRuns = Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunResult, Boolean>());
			
			//=== Stores outstanding runs 
			final Map<AlgorithmRunConfiguration, AlgorithmRunResult> outstandingRuns  = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
			
			//=== Stores the kill handlers for each run
			//Kill Handlers will keep track of which runs have been requested to be killed
			final Map<AlgorithmRunConfiguration, KillHandler> killHandlers = new ConcurrentHashMap<AlgorithmRunConfiguration, KillHandler>();
			for(int i=0; i < runConfigs.size(); i++)
			{
				AlgorithmRunConfiguration rc = runConfigs.get(i);
				orderOfRuns.put(rc, i);
				KillHandler kh  = new StatusVariableKillHandler();
				killHandlers.put(rc, kh);
				outstandingRuns.put(runConfigs.get(i),new RunningAlgorithmRunResult( rc, 0,0,0,rc.getProblemInstanceSeedPair().getSeed() ,0, kh));
			}
			
			//Observer maps
			final AtomicBoolean completionCallbackFired = new AtomicBoolean(false);

			
			final int totalRunsNeeded = runConfigs.size();
			
			int numberOfDispatchedRuns = 0;
			
			final AtomicBoolean failureOccured = new AtomicBoolean(false);
			
			final AtomicLong lastUpdate = new AtomicLong(0);
			
			
			while((numberOfDispatchedRuns < runConfigs.size()) && !failureOccured.get())
			{
				if(availableRuns.availablePermits() > NUMBER_OF_CONCURRENT_RUNS)
				{
					throw new IllegalStateException("Somehow I now have more permits than I should be limited to");
				}
				
				log.trace("Asking for permission for {} things config id of first: ({})", runConfigs.size() - numberOfDispatchedRuns, runConfigs.get(0).getParameterConfiguration().getFriendlyIDHex());
				int oNumRunConfigToRun;
				try {
					oNumRunConfigToRun = availableRuns.getUpToNPermits(runConfigs.size()-numberOfDispatchedRuns);
				} catch (InterruptedException e) {
					//=== We can just return from this method 
					log.debug("Thread was interrupted while waiting, aborting execution for runs with config id of first: ({})", runConfigs.get(0).getParameterConfiguration().getFriendlyIDHex());
					completionCallbackFired.set(true);
					failureOccured.set(true);
					handler.onFailure(new TargetAlgorithmEvaluatorShutdownException(e));
					Thread.currentThread().interrupt();
					return;
				}
				int numRunConfigToRun = oNumRunConfigToRun;
	
				//log.trace("Asked for permission to run {} things, got permission to run {} things, total completed for this batch {}  config id of first: ({})" , runConfigs.size()-numberOfDispatchedRuns, numRunConfigToRun,numberOfDispatchedRuns, runConfigs.get(0).getParameterConfiguration().getFriendlyIDHex() );
				
				List<AlgorithmRunConfiguration> runsToDo = new ArrayList<AlgorithmRunConfiguration>(numRunConfigToRun);
				
				
				int killInterceptedCount = 0;
				
				//==== We need to get numRunConfigToRun runs to do
				//=== We will skip runs that have been killed as it can be incredibly slow for the TAE to actually check this when we serialize these to the TAE.
				int numberOfRunToSelect=0; 
				while(runsToDo.size() < numRunConfigToRun)
				{
					int index =  numberOfDispatchedRuns + numberOfRunToSelect +  killInterceptedCount;
					
					if(index >= runConfigs.size())
					{
						break;
					}
					
					AlgorithmRunConfiguration possibleRC = runConfigs.get(index);

					//CAUTION: We actually don't do this check for the last run, the reason being that if we kill everything at this point, we have to manually interact with the callbacks, and observers
					//But by always submitting the last run to the TAE and then waiting for the slower killing mechanism, we can get almost all of the speed up for free
					if( index < (runConfigs.size() - 1))
					{
						if(killHandlers.get(possibleRC).isKilled())
						{
							log.trace("Run {} was killed already not dispatching, marking killed", possibleRC);
							killInterceptedCount++;
						
							AlgorithmRunResult completedRun = new ExistingAlgorithmRunResult(possibleRC,RunStatus.KILLED, 0,0,0,possibleRC.getProblemInstanceSeedPair().getSeed(),KILLED_BY_DECORATOR_ADDL_RUN_INFO,0);
							outstandingRuns.put(possibleRC, completedRun);
							completedRuns.add(completedRun);
							
							continue;
						}
					} 
					
					runsToDo.add(possibleRC);
					
					numberOfRunToSelect++;
				}

				numRunConfigToRun = runsToDo.size();
				
				if(oNumRunConfigToRun != numRunConfigToRun)
				{
					log.debug("Runs have been killed preemptively total {} permits immediately available are {}", oNumRunConfigToRun - numRunConfigToRun, this.availableRuns.availablePermits());
					this.availableRuns.releasePermits(oNumRunConfigToRun - numRunConfigToRun);
				}
				
				if(runsToDo.size() == 0)
				{
					throw new IllegalStateException("Runs to do size is now zero, this is a bug and this state is irrecoverable, sorry.");
				}
				
				final AtomicInteger completedCount = new AtomicInteger(0);
				final AtomicInteger releaseCount = new AtomicInteger(0);
				
				TargetAlgorithmEvaluatorCallback callBack = new SubListTargetAlgorithmEvaluatorCallback(availableRuns, numRunConfigToRun, runConfigs, handler, failureOccured, totalRunsNeeded, completedRuns, orderOfRuns, completionCallbackFired, execService, completedCount, releaseCount);
				TargetAlgorithmEvaluatorRunObserver updateMapObserver = new BoundedTargetAlgorithmEvaluatorMapUpdateObserver(availableRuns, numRunConfigToRun, runConfigs, obs, outstandingRuns, orderOfRuns, killHandlers, completionCallbackFired,execService, completedCount, releaseCount, lastUpdate);
				
				tae.evaluateRunsAsync(runsToDo, callBack, updateMapObserver);
			
				numberOfDispatchedRuns+=numRunConfigToRun;
				numberOfDispatchedRuns+=killInterceptedCount;
				
				
				
			}
		} finally {
			enqueueLock.unlock();
		}

	}
	

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}
	
	@Override
	protected void postDecorateeNotifyShutdown()
	{
		this.execService.shutdown();
		
		try {
			this.execService.awaitTermination(365, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			log.warn("Thread shutdown interrupted");
			Thread.currentThread().interrupt();
			return;
		}
		
	}
	
	
	/**
	* Observer that keeps track of the table status and forwards the observation calls to the client
	* Synchronized On: The list of runconfigs that the user provided us
	 */
	static class BoundedTargetAlgorithmEvaluatorMapUpdateObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private final FairMultiPermitSemaphore availableRuns;
		private final List<AlgorithmRunConfiguration> runConfigs;
		private final TargetAlgorithmEvaluatorRunObserver callerRunObserver;
		private final Map<AlgorithmRunConfiguration, AlgorithmRunResult> outstandingRuns;
		private final Map<AlgorithmRunConfiguration, Integer> orderOfRuns;
		private final Map<AlgorithmRunConfiguration, KillHandler> killHandlers;
		private final AtomicBoolean completedCallbackFired;
		private final ExecutorService cachedThreadPool;
		private final AtomicInteger completedCount;
		private final AtomicInteger releaseCount;
		private final int numRunConfigToRun;
		private AtomicLong lastUpdate;
		
		
		BoundedTargetAlgorithmEvaluatorMapUpdateObserver(FairMultiPermitSemaphore availableRuns, int numRunConfigToRun,  List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver callerRunObserver, Map<AlgorithmRunConfiguration, AlgorithmRunResult> outstandingRuns, Map<AlgorithmRunConfiguration, Integer> orderOfRuns, Map<AlgorithmRunConfiguration, KillHandler> killHandlers, AtomicBoolean onSuccessFired, ExecutorService cachedThreadPool, AtomicInteger completedCount, AtomicInteger releaseCount, AtomicLong lastUpdate )
		{
			this.numRunConfigToRun = numRunConfigToRun;
			
			this.availableRuns = availableRuns;
			this.runConfigs = runConfigs;
			this.callerRunObserver = callerRunObserver;
			this.outstandingRuns = outstandingRuns;
			this.orderOfRuns = orderOfRuns;
			this.killHandlers = killHandlers;
			this.completedCallbackFired = onSuccessFired;
			this.cachedThreadPool = cachedThreadPool;
			this.completedCount = completedCount;
			this.releaseCount = releaseCount;
			this.lastUpdate = lastUpdate;
			
			
		}
		
		/**
		 * Updates the table of runs 
		 */
		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs) {
			
			
			
			if(runs.size() != numRunConfigToRun)
			{
				log.error("Um what");
				throw new IllegalStateException("Runs seem to have disappeared, I was expecting to observer " + numRunConfigToRun + " but only saw " + runs.size());
			}
			//System.out.println(this + " and " + runs.size() + " versus " + numRunConfigToRun);
			
			synchronized(runConfigs)
			{
				if(this.completedCallbackFired.get())
				{
					//Success already fired 
					return;
				}
				int completedRuns = 0;
				for(AlgorithmRunResult run : runs)
				{
					outstandingRuns.put(run.getAlgorithmRunConfiguration(),run);
					if(run.isRunCompleted())
					{
						completedRuns++;
					}
				}
				
				int previousCompletedCount = completedCount.get();
				
				
				if(previousCompletedCount > completedRuns)
				{
					IllegalStateException e =  new IllegalStateException("Somehow I determined that there were " + completedRuns + " but previously we detected: " + previousCompletedCount );
					
					throw e;
					
				} 
				
				completedCount.set(completedRuns);
				
				int releasesNeeded = completedCount.get() - releaseCount.get();
				
				if(releasesNeeded < 0)
				{
					throw new IllegalStateException("Somehow I have gotten to a state where I need to take back some releases completed:" + completedRuns + " releaseCount: " + releaseCount.get());
				}
				
				this.releaseCount.addAndGet(releasesNeeded);
				this.availableRuns.releasePermits(releasesNeeded);
				
				
				final List<AlgorithmRunResult> allRunsForCaller = new ArrayList<AlgorithmRunResult>();
				
				if(runConfigs.size() != outstandingRuns.size())
				{
					throw new IllegalStateException("Expected " + runConfigs.size() + " to equal " + outstandingRuns.size());
				}
				
				for(int i=0; i < runConfigs.size(); i++)
				{
					allRunsForCaller.add(runs.get(0));
				}
				
				for(int i=0; i < runConfigs.size(); i++)
				{
					AlgorithmRunResult algoRun = outstandingRuns.get(runConfigs.get(i));
					allRunsForCaller.set(orderOfRuns.get(runConfigs.get(i)),algoRun);
				}
				
				//Forward killing flags to internal run
				for(Entry<AlgorithmRunConfiguration,KillHandler> ent : killHandlers.entrySet())
				{
					if(ent.getValue().isKilled())
					{
						outstandingRuns.get(ent.getKey()).kill();
					}
				}
				
				//=== Invoke callback in another thread 
				final long currentTime = System.currentTimeMillis();
				if(callerRunObserver != null)
				{
					cachedThreadPool.execute(new Runnable()
					{
						@Override
						public void run() {
							synchronized(runConfigs)
							{
								
								
								long lastUpdateValue = lastUpdate.get();
								
								if(lastUpdateValue >= currentTime)
								{
									//A previous observer has fired that was taken 
									//at a point in the future, so this information is stale
									
									return;
									
								} else
								{
									if(!(lastUpdate.compareAndSet(lastUpdateValue, currentTime)))
									{
										throw new IllegalStateException("Inappropriate Synchronization detected on lastUpdate. All updates should have been guarded by a lock on runConfigs but somehow the value has changed");
									}
								}
								
								if(completedCallbackFired.get())
								{
									//Success already fired 
									return;
								}
								try {
									callerRunObserver.currentStatus(allRunsForCaller);
								} catch(Throwable t)
								{
									log.error("UNCAUGHT EXCEPTION: Error occured while notifying observer ", t);
								}
							}
						}
						
					});
					
				}
			}

		}
		
	};
	
	
	/**
	 * This callback updates the objects state upon completion and optionally fires the callback
	 * Synchronized On: The list of runconfigs that the user provided us
	 */
	static class SubListTargetAlgorithmEvaluatorCallback implements TargetAlgorithmEvaluatorCallback
	{
		
		private final FairMultiPermitSemaphore availableRunsSemaphore;
		private final int numRunConfigToRun;
		private final List<AlgorithmRunConfiguration> runConfigs;
		private final TargetAlgorithmEvaluatorCallback calleeCallback;
		private final AtomicBoolean failureOccured;
		private final Set<AlgorithmRunResult> completedRuns;
		private final int totalRunsNeeded;
		private final Map<AlgorithmRunConfiguration, Integer> orderOfRuns;
		private final AtomicBoolean completionCallbackFired;
		private final ExecutorService execService;
		private final AtomicInteger completedCount;
		private final AtomicInteger runPermitsReleased;
		
		

		public SubListTargetAlgorithmEvaluatorCallback(FairMultiPermitSemaphore availableRunsSemaphore, int numRunConfigToRun, List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorCallback calleeCallback, AtomicBoolean failureOccured, int totalRunsNeeded, Set<AlgorithmRunResult> completedRuns, Map<AlgorithmRunConfiguration, Integer> orderOfRuns, AtomicBoolean onSuccessFired, ExecutorService execService, AtomicInteger completedCount, AtomicInteger releaseCount)
		{
			this.availableRunsSemaphore = availableRunsSemaphore;
			this.numRunConfigToRun = numRunConfigToRun;
			this.runConfigs = runConfigs;
			this.calleeCallback = calleeCallback;
			this.failureOccured = failureOccured;
			this.completedRuns = completedRuns;
			this.totalRunsNeeded = totalRunsNeeded; 
			this.orderOfRuns = orderOfRuns;
			this.completionCallbackFired = onSuccessFired;
			this.execService = execService;
			this.completedCount = completedCount;
			this.runPermitsReleased = releaseCount;
			
			
		}
		
		private void releaseRemaining(int completedSize)
		{
			synchronized(runConfigs)
			{
				int previousCompletedCount = completedCount.get();
				if(previousCompletedCount > completedSize)
				{
					throw new IllegalStateException("Somehow I determined that there were " + completedCount.get() + " but previously we detected: " + previousCompletedCount );
				} 
				
				completedCount.set(completedSize);
				
				int releasesNeeded = completedCount.get() - runPermitsReleased.get();
				
				if(releasesNeeded < 0)
				{
					throw new IllegalStateException("Somehow I have gotten to a state where I need to take back some releases completed:" + completedCount.get() + " releaseCount: " + runPermitsReleased.get());
				}
				
				this.runPermitsReleased.addAndGet(releasesNeeded);
				this.availableRunsSemaphore.releasePermits(releasesNeeded);
			}
		}
		
		@Override
		public void onSuccess(List<AlgorithmRunResult> runs) {
			
			
			//availableRuns.releasePermits(rcToRun);
			
			//=== Mutex on the runConfig to prevent multiple calls to onSuccess()
			synchronized(runConfigs)			
			{
				releaseRemaining(numRunConfigToRun);
				
				if(failureOccured.get())
				{
					this.completionCallbackFired.set(true);
					log.debug("Failure occured, silently discarding runs: {}", runs);
					return;
				} 
					
				completedRuns.addAll(runs);
				
				
				if(totalRunsNeeded == completedRuns.size())
				{
					this.completionCallbackFired.set(true);
					final List<AlgorithmRunResult> allRuns = new ArrayList<AlgorithmRunResult>(completedRuns.size());
					
					//=== We need the array to have everything in it before hand
					for(int i=0; i < completedRuns.size(); i++)
					{
						allRuns.add(runs.get(0));
					}
					
					
					for(AlgorithmRunResult run : completedRuns)
					{
						int index = orderOfRuns.get(run.getAlgorithmRunConfiguration());
						allRuns.set(index, run);
					}
					//==== Schedule callback in another thread
					//==== This is to prevent the onSuccess or observers from re-entering evaluateRun
					execService.execute(new Runnable()
					{
						@Override
						public void run() {
							try {
								synchronized(runConfigs)
								{
									try {
										calleeCallback.onSuccess(allRuns);
									} catch(RuntimeException e)
									{
										calleeCallback.onFailure(e);
									}
								}
							} catch(Throwable t)
							{
								log.error("Unknown exception occured ", t);
							}
						}
						
					});
					

				} 
			}
			
			
		}

		@Override
		public void onFailure(final RuntimeException t) {
			//availableRuns.releasePermits(rcToRun);
			synchronized(runConfigs)
			{
				releaseRemaining(numRunConfigToRun);
				this.completionCallbackFired.set(true);
				if(failureOccured.get())
				{
					log.debug("Failure occured already, silently discarding subsequent failures");
				}
				failureOccured.set(true);
				
		
				execService.execute(new Runnable()
				{
					
						@Override
						public void run() {
							try {
								synchronized(runConfigs)
								{
									calleeCallback.onFailure(t);
								}
							} catch(Throwable t)
							{
								log.error("Unknown exception occured ", t);
							}
						}
					
					
				});
			}
		}
			
			
		
		
		
		
	}
	
}

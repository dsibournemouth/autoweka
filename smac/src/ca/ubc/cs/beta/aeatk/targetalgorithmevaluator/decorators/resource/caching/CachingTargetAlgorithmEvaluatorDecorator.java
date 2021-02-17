package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.caching;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.concurrent.ReducableSemaphore;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.logging.CommonMarkers;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator;


/**
 * This Target Algorithm Evaluator Decorator can be used as a cache of runs, it is designed to be high performance and work well in multi-threaded environments.
 * 
 * Specifically it can be used to simplify algorithms or processes that may make repeated
 * sets of run requests. We need to be very careful that the target algorithm evaluator 
 * only sees one request regardless of how many times the request comes in, and regardless
 * of thread interleavings.
 * 
 * Finally it also should be safe with respect to Interruption, that is
 * a thread that is interupted with respect to the client shouldn't be able to negatively affect other threads / requests. 
 * 
 * <br/>
 * <b>Client Usage Notes:</b>
 * <br/>
 * 1) You may have issues with runs being submitted that are killed and the observer seeing some weird effects, like runs that are killed and have a lower response value.
 * You should use the {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaulator.decorators.helpers.StrictlyIncreasingRuntimesTargetAlgorithmEvaluatorDecorator} to correct this
 * <br/>
 * 2) You can get a higher cache hit rate if you use {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.ExclusivelyTargetAlgorithmEvaluatorDecorator}
 * to ensure that runs map to the same RunConfig
 * <br/>
 * 
 * <br>
 * <b>KNOWN ISSUES</b>:<br>
 * 1) All runs here are final, if a run is CRASHED you cannot rerun it (the rerun decorator should be applied underneath this one)
 * <br>
 * 2) If an exception or error occurs, callbacks are not notified as fast as possible. 
 * <br>
 * 3) If interrupted the thread may not have it's objects cleaned up properly this is unfortunate,
 * as it will leak. If this is actually affecting you then it may be fixed, but I suspect no one will
 * ever notice.
 * <br>
 * <b>Implementation Details</b>
 * Care must be taken to ensure that we don't notify the client with out of order data. We also don't use the same callback thread to notify the client
 * because it can create deadlocks (if the callback causes more runs to submit, it can get trapped here). 
 * <p>
 * In the case of updating the observer, we update the shared data structure of ALL the runs and then notify the client's observer, which makes custom versions of the runs.
 * <p>
 * In the case of processing the callback, we update for all the outstanding runs to be completed, and then execute the client's callback. 
 * <p>
 * The callback and observer threads coordinate on the EvaluationToken. The Callback thread, sets a flag and then grabs a lock when it's sending the final callback.
 * The observer thread gets the lock and then checks the flag, if set, or if it's a duplicate update (no new information), drops it.
 * <p>
 * 
 * Killing is handled by keeping track of a counter of interested runs, if this hits zero and the observer sees it the kill is forwarded.
 * 
 * A good way to understand how this class works is actually probably to look in the Git History. The first few commits,
 * only did the request caching, and didn't support observation. The next commit supported observation, but ignored killing.
 * The last commit handled killing.
 * 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class CachingTargetAlgorithmEvaluatorDecorator extends AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final AtomicInteger cacheRequests = new AtomicInteger(0);
	private final AtomicInteger cacheRequestMisses = new AtomicInteger(0);
	
	//This will be greater than cache misses because of killed runs
	private final AtomicInteger submittedToDecoratee = new AtomicInteger(0);
	
	
	private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
	
	/**
	 * Runs are enqueued here for another thread to retrieve
	 */
	private final LinkedBlockingQueue<List<AlgorithmRunConfiguration>> submissionQueue = new LinkedBlockingQueue<List<AlgorithmRunConfiguration>>();
	
	
	/**
	 * Executor service for submitting runs to the wrapped TAEs
	 */
	private final Executor submissionExecService = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS, new SequentiallyNamedThreadFactory("Caching Target Algorithm Evaulator Submission Thread"));
	
	/**
	 * Queue that contains callbacks with every run done
	 */
	private final LinkedBlockingQueue<EvaluationRequestToken> tokenToCallbackQueue = new LinkedBlockingQueue<EvaluationRequestToken>();
	
	/**
	 * Executor service for invoking callbacks
	 */
	private final Executor callbackExecService = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS, new SequentiallyNamedThreadFactory("Caching Target Algorithm Evaulator Callback Thread"));
	
	/**
	 * Queue that contains callbacks with every run done
	 */
	private final LinkedBlockingQueue<EvaluationRequestToken> tokenToObserverQueue = new LinkedBlockingQueue<EvaluationRequestToken>();
	
	/**
	 * Executor service for invoking callbacks
	 */
	private final Executor observerExecService = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS, new SequentiallyNamedThreadFactory("Caching Target Algorithm Evaulator Observer Thread"));
	
	
	
	/**
	 * Threads coordinate on this map, if they put something in this map
	 * it means that thread is responsible for ensuring that the run config is delivered
	 * 
	 * [Key: Used for Coordination Between Threads on Job Submission, the thread that creates this element is the one that must submit it]
	 * [Value: Used for coordination between threads on job re-submission, the thread that takes the permit on this job, is the one that must submit it].
	 *  
	 * Populated: On call to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: Entries are never removed
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, ReducableSemaphore> runConfigsSubmittedToWrappedDecoratorMap = new ConcurrentHashMap<AlgorithmRunConfiguration, ReducableSemaphore>();
	
	
	/***
	 * This map is used to determine whether every outstanding run for a callback is done
	 * 
	 * [Used for Coordination Between Threads of decrementing counter]
	 * Populated: on call to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: After callback is fired remove the key from map
	 */
	private final ConcurrentHashMap<EvaluationRequestToken, Set<AlgorithmRunConfiguration>> outstandingRunsForTokenMap = new ConcurrentHashMap<EvaluationRequestToken, Set<AlgorithmRunConfiguration>>();
	
	/***
	 * This map is used to determine whether every outstanding run for a callback is done
	 * 
	 * [Used for Coordination Between Threads of scheduling callback]
	 * Populated: on call to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: After callback is fired remove the key from map
	 */
	private final ConcurrentHashMap<EvaluationRequestToken, AtomicInteger> outstandingRunsCountForTokenMap = new ConcurrentHashMap<EvaluationRequestToken, AtomicInteger>();
	
	
	/**
	 * Stores for every callback, the entire set of runconfigs associated with it, this is mainly so that we have a completed list after
	 * 
	 * Populated: on call to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: After callback is fired remove the key from map
	 */
	private final ConcurrentHashMap<EvaluationRequestToken, List<AlgorithmRunConfiguration>> allRunConfigsForTokenMap = new ConcurrentHashMap<EvaluationRequestToken, List<AlgorithmRunConfiguration>>();
	
	
	/**
	 * This map is used to determine which callbacks need to be notified on an individual run completion
	 * 
	 * Populated: on call to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: During callback firing the inner set has elements removed, the outter map is never cleaned up
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, Set<EvaluationRequestToken>> runConfigToTokenMap = new ConcurrentHashMap<AlgorithmRunConfiguration, Set<EvaluationRequestToken>>();
	
	/**
	 * This map is used to map an individual request token to the callback to be notified (the same callback could be used for multiple requests)
	 * 
	 * Populated: on call to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: During callback firing the token is removed.
	 */
	private final ConcurrentHashMap<EvaluationRequestToken, TargetAlgorithmEvaluatorCallback> evalRequestToCallbackMap = new ConcurrentHashMap<EvaluationRequestToken, TargetAlgorithmEvaluatorCallback>();
	
	
	/**
	 * This map is used to map an individual request token to the observer that should be notified (the same observer could be used for multiple requests)
	 * Populated: When entries are completed in {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Cleanup: During callback firing the token is removed
	 */
	private final ConcurrentHashMap<EvaluationRequestToken, TargetAlgorithmEvaluatorRunObserver> evalRequestToObserverMap = new ConcurrentHashMap<EvaluationRequestToken, TargetAlgorithmEvaluatorRunObserver>();
	
	

	/**
	 * This map stores a mapping from run config to whether the run is currently outstanding
	 * 
	 * 
	 * [Used for coordination of resubmitting runs, if we get the semaphore for the runconfig (it may be that the run already went passed), so we also check if it's currently submitted before resubmitting it.
	 * Runs can only set to false once we have an entry in completedRuns or killedRunsMap ]
	 * 
	 * Populated: When entries are created in {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)}
	 * Updated: Initially created with true, and when the run comes back set to false. May be true again if the run was killed
	 * Clean up: Keys are never removed
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AtomicBoolean> outstandingRunConfigMap = new ConcurrentHashMap<AlgorithmRunConfiguration, AtomicBoolean>();
	
	
	
	/**
	 * This map stores the set of completed run configs
	 * A run is completed if has a result in either the completed runs map, or the completed exceptions map (they should be populated first), it does not count
	 * as completed if the run is in the completedKilledRunsMap. (This is necessary to prevent clients who don't kill their runs from seeing the killed runs)
	 * 
	 * Populated: When entries are completed in {@link SubmissionOnCompleteHandler#onComplete()}
	 * Cleanup: Entries are never removed
	 */
	private final Set<AlgorithmRunConfiguration> completedRunConfigs = Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunConfiguration, Boolean>());
	
	/**
	 * This map stores the completed runs (NO KILLED RUNS)
	 * 
	 * Populated: When entries are completed: {@link SubmissionOnCompleteHandler#onSuccess()}
	 * Cleanup: Entries are never removed
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> completedRunsMap = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
	
	
	/**
	 * This map stores completed exceptions
	 * 
	 * Populated: When entries are completed: {@link SubmissionOnCompleteHandler#onFailure()}
	 * Cleanup: Entries are never removed
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, RuntimeException> completedExceptionMap = new ConcurrentHashMap<AlgorithmRunConfiguration, RuntimeException>();
	
	/**
	 * This map stores killed runs
	 * 
	 * Populated: When entries are killed
	 * Updated: When entries are killed with a higher runtime
	 * 
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> killedRunsMap = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
	
	
	/**
	 * This maps stores the set of runs that have been killed for a token
	 */
	private final ConcurrentHashMap<EvaluationRequestToken, Set<AlgorithmRunConfiguration>> killedRunsForToken = new ConcurrentHashMap<EvaluationRequestToken, Set<AlgorithmRunConfiguration>>();
	
	/**
	 * This map stores the number of evaluations which are still interested in this run config.
	 * 
	 * [Synchronization point: When this hits zero, the thing that turned it to zero will then set the kill flag.]
	 * Populated: When entries are created
	 * Updated: Increment whenever we submit a run, decremented when we kill it.
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AtomicInteger> runConfigToInterestedEvaluationsCounter = new ConcurrentHashMap<AlgorithmRunConfiguration, AtomicInteger>();
	
	/**
	 * Single instance of the null observer
	 */
	private static final NullTargetAlgorithmEvaluatorRunObserver NULL_OBSERVER = new NullTargetAlgorithmEvaluatorRunObserver();
	
	/**
	 * This map stores our current status of runs
	 * 
	 * Populated: When entries are running
	 * Updated: When Entries are Running (NOT WHEN THEY ARE COMPLETE HOWEVER)
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> runConfigToLiveLatestStatusMap = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
	
	
	/**
	 * If true, when an error is detected we will basically stop processing
	 */
	private final boolean shutdownOnError;
	
	private final AtomicReference<RuntimeException> storedException = new AtomicReference<RuntimeException>();
			
	private final AtomicBoolean errorDetected = new AtomicBoolean(false);
	
	private final boolean notifyCallbacksOnError = false;
	
	
	private final Thread debugThread; 
	public CachingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae)
	{
		this(tae, false);
	}
	
	public CachingTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, boolean logDebugMessages) {
		super(tae);
		
		shutdownOnError = true;
		for(int i=0; i < AVAILABLE_PROCESSORS; i++)
		{
			submissionExecService.execute(new RunSubmitter());
			callbackExecService.execute(new CallbackInvoker());
			observerExecService.execute(new ObserverInvoker());
		}
		
		
		debugThread = new Thread(new Runnable()
		{

			@Override
			public void run() {
				while(true)
				{
					try{
						try {
							Thread.sleep(60000);
						} finally
						{
							debugMessage();
						}
					} catch (InterruptedException e) {
						//e.printStackTrace();
						Thread.currentThread().interrupt();
						return;
					}
				}
				
			}
			
		});
		
		if(logDebugMessages)
		{
			debugThread.setDaemon(true);
			debugThread.start();
		}
	
	}

	
	
	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> rcs, final TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer) 
	{
		
		try 
		{
			if(shutdownOnError && errorDetected.get())
			{
				//log.info("Target Algorithm Evaluator has detected error and is unavailable");
				return;
			}
			
			//Sanitation of inputs
			if(rcs.isEmpty())
			{
				callback.onSuccess(Collections.<AlgorithmRunResult> emptyList());
				return;
			}
			
			
			if(new HashSet<AlgorithmRunConfiguration>(rcs).size() != rcs.size())
			{
				throw new IllegalArgumentException("Run Configurations list has duplicates in it, this isn't legal");
			}
			final List<AlgorithmRunConfiguration> runConfigs = Collections.unmodifiableList(rcs);
			
		
			if(observer == null)
			{
				observer = NULL_OBSERVER;
			}
			final TargetAlgorithmEvaluatorRunObserver obs = observer;
	
			
			//Data initialization
			/**
			 * Updates all shared data structures for this request
			 */
			
			//Evaluation tracking
			final EvaluationRequestToken evalToken = new EvaluationRequestToken();
			evalRequestToCallbackMap.put(evalToken, callback);
			evalRequestToObserverMap.put(evalToken, obs);
			

			Set<AlgorithmRunConfiguration> outstandingRunsForTokenSet = Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunConfiguration, Boolean>());
			AtomicInteger outstandingRunsCountForToken = new AtomicInteger(rcs.size());
			outstandingRunsForTokenSet.addAll(runConfigs);
			
			outstandingRunsForTokenMap.put(evalToken, outstandingRunsForTokenSet);
			outstandingRunsCountForTokenMap.put(evalToken,  outstandingRunsCountForToken);
			allRunConfigsForTokenMap.put(evalToken, runConfigs);
			killedRunsForToken.put(evalToken, Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunConfiguration, Boolean>()));
			
			
			
			List<AlgorithmRunConfiguration> runConfigsCurrentThreadSubmits = new ArrayList<AlgorithmRunConfiguration>(runConfigs.size());
			
			
			
			//We avoid the object creation every time through the loop, probably premature optimization.
			//In the loop we will recreate these when these instances are used.
			ReducableSemaphore runsSubmittedToWrappedTAECompletedSemaphore = new ReducableSemaphore(0);
			Set<EvaluationRequestToken> callbacksForRunConfig = Collections.newSetFromMap(new ConcurrentHashMap<EvaluationRequestToken,Boolean>());
			
			for(AlgorithmRunConfiguration rc : runConfigs )
			{	
				
				Semaphore value = runConfigsSubmittedToWrappedDecoratorMap.putIfAbsent(rc, runsSubmittedToWrappedTAECompletedSemaphore);
				
				//New value inserted
				if(value == null)
				{
					runConfigsCurrentThreadSubmits.add(rc);
					runsSubmittedToWrappedTAECompletedSemaphore = new ReducableSemaphore(0);
					
					outstandingRunConfigMap.putIfAbsent(rc, new AtomicBoolean(true));
					
				}
				
				//Just because are responsible for submitting, doesn't mean we created these guys. 
				runConfigToLiveLatestStatusMap.putIfAbsent(rc, new RunningAlgorithmRunResult(rc, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(), 0, new NullKillHandler()));
				
				
				//Flag our interest in a run, iff there is already at least one person interested.
				AtomicInteger oldValue = runConfigToInterestedEvaluationsCounter.putIfAbsent(rc, new AtomicInteger(1));
				if(oldValue != null)
				{
					runConfigToInterestedEvaluationsCounter.get(rc).incrementAndGet();
				} else
				{
					//Inserted new value, we are responsible for submitting
				}	
				
				Set<EvaluationRequestToken> oSet =  runConfigToTokenMap.putIfAbsent(rc, callbacksForRunConfig);
				
				if(oSet == null)
				{
					oSet = callbacksForRunConfig;
					//Create a new set only when needed.
					callbacksForRunConfig = Collections.newSetFromMap(new ConcurrentHashMap<EvaluationRequestToken,Boolean>());
				} 
				
				oSet.add(evalToken);
			}
			
			int requests = cacheRequests.addAndGet(runConfigs.size());
			int misses = cacheRequestMisses.addAndGet(runConfigsCurrentThreadSubmits.size());
			NumberFormat nf = NumberFormat.getPercentInstance();
			
			//log.trace("Cache Local misses: {}, Local request: {},  Global misses {}, Global Requests {}, Hit Rate {} ", runConfigsCurrentThreadSubmits.size(), runConfigs.size(),  misses, requests, nf.format( ((double) requests - misses) / requests)  );
			
			
			
			
			if(runConfigsCurrentThreadSubmits.size() > 0)
			{
				submissionQueue.add(runConfigsCurrentThreadSubmits);
			}
			
			
			//log.trace("Token {} submitted with outstanding runs: {} map size {}: {}", 
			//		evalToken,
			//		outstandingRunsCountForToken.get(),
			//		outstandingRunsForTokenSet.size(),
			//		outstandingRunsForTokenSet);
			/**
			 * Wait until everything is submitted
			 */
			for(AlgorithmRunConfiguration rc : runConfigs)
			{
				try {
					runConfigsSubmittedToWrappedDecoratorMap.get(rc).acquire();
					runConfigsSubmittedToWrappedDecoratorMap.get(rc).release();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			
			
			//log.trace("Token {} has completed submission: {} map size{} :{}", 
			//		evalToken,
			//		outstandingRunsCountForToken.get(),
			//		outstandingRunsForTokenSet.size(),
			//		outstandingRunsForTokenSet);
			
			for(AlgorithmRunConfiguration rc : runConfigs)
			{
				if(completedRunConfigs.contains(rc) || killedRunsMap.containsKey(rc))
				{
					if(processRunConfigurationForRequestToken(evalToken,rc))
					{
						try {
							runConfigsSubmittedToWrappedDecoratorMap.get(rc).acquire();
							runConfigsSubmittedToWrappedDecoratorMap.get(rc).release();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}
				} 
			
			}
		} catch(RuntimeException e)
		{
			if(errorDetected.compareAndSet(false, true))
			{
				storedException.set(e);
				e.printStackTrace();
			}
			
			throw e;

		}
	}

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		//Convert the synchronous request into an asynchronous request
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}
	
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		
		NumberFormat nf = NumberFormat.getPercentInstance();
		int misses = cacheRequestMisses.get();
		int requests =  cacheRequests.get();
		int submittedToNextTAE = this.submittedToDecoratee.get();
		
		//log.info("Cache misses {}, Submitted to Decoratee: {}, Cache requests {}, Hit Rate {} ", misses, submittedToNextTAE, requests, nf.format( ((double) requests - misses) / requests)  );
		
		debugThread.interrupt();

		
	}

	
	/**
	 * Removes a runconfig from the set of outstanding callbacks, this method should be called when we suspect the run is done it may be called more than one for a token, and should be a noop if it isn't.
	 * 
	 * @param callback
	 * @param rc
	 * @return <code>true</code> if we had to resubmit the RC, false otherwise
	 */
	private final boolean processRunConfigurationForRequestToken(EvaluationRequestToken token, AlgorithmRunConfiguration rc)
	{

		if(shutdownOnError && errorDetected.get())
		{
			//log.info("Observer notification detected error, and has shutdown");
			return false;
		}
		
		
		Set<AlgorithmRunConfiguration> outstandingRunsForCallback = outstandingRunsForTokenMap.get(token);
		
		if(outstandingRunsForCallback == null)
		{
			return false;
		}
		
		//log.trace("Token {} called for {} , map has {} elements {} ", token, rc, outstandingRunsForCallback.size(), outstandingRunsForCallback);
		
		
		
		log.trace("Token {} called for {} still running ", token, rc);
		
		boolean rcRemoved = outstandingRunsForCallback.remove(rc);
		
		if(rcRemoved)
		{	

			//We are responsible for verifying this run is ready for the callback
			boolean runIsMarkedCompleted = completedRunConfigs.contains(rc);
			
			boolean tokensKilledRunsContainRC = killedRunsForToken.get(token).contains(rc);

			if(!runIsMarkedCompleted && !tokensKilledRunsContainRC)
			{
				
				if(!killedRunsMap.containsKey(rc))
				{
					//System.err.println("Removed " + token + "==>" + rc + " (remaining) " + (outstandingRunsCountForTokenMap.get(token).get()-1) + " " + outstandingRunsForCallback + " " + runIsMarkedCompleted + "," + tokensKilledRunsContainRC + "KILLED RUNS");
					
					//Error condition has happened, the run isn't finished, yet it isn't killed either
					log.error("Run not completed, but not killed either token: {} run {}", token, rc);
					log.error("Error",killedRunsMap);
					
					//This is a fatal error so we will throw it a
					
					if(shutdownOnError) throw new IllegalStateException("Run not completed, but not killed either:" + rc + " token: " + token + " " + runIsMarkedCompleted + "," + tokensKilledRunsContainRC);

					
				} else
				{					
					//System.err.println("Removed " + token + "==>" + rc + " (remaining) " + (outstandingRunsCountForTokenMap.get(token).get()-1) + " " + outstandingRunsForCallback + " " + runIsMarkedCompleted + "," + tokensKilledRunsContainRC + "KILLED RUNS ELSE");
					//Run killed without our permission, it must be resubmitted

					ReducableSemaphore semi = runConfigsSubmittedToWrappedDecoratorMap.get(rc);
					
					//If we acquire the permit then we are responsible for submitting the run
					boolean shouldResubmit = semi.tryAcquire();
					
					outstandingRunsForCallback.add(rc);
					if(shouldResubmit)
					{
						boolean changedToOutstanding = outstandingRunConfigMap.get(rc).compareAndSet(false, true);
						if(changedToOutstanding)
						{
							log.debug("Run was killed but caller: {} didn't expect it to be so, rescheduling.", rc);
							submissionQueue.add(Collections.singletonList(rc));
						} else
						{
							semi.release();
						}
						
					} else
					{
						log.debug("Run was killed but caller: {} didn't expect it to be so. Run will be rescheduled by someone else.", rc);

					}
					
				
				}
				return true;
				
				
			} else
			{

				
				//Run is done, or we asked for it to be killed.
				int remaining = outstandingRunsCountForTokenMap.get(token).decrementAndGet();
				
				//log.trace("Remaining runs for {} are {} after removing {}", token, remaining, rc);
				if(remaining < 0)
				{
					log.error("Desynchronization detected as the number of remaining elements seems to be less than zero for token: {} and rc: {}", token, rc);
					if(shutdownOnError)  throw new IllegalStateException("Desynchronization error as the number of remaining elements is less than zero:" + rc + " token: " + token);
				}
				
				if(remaining == 0)
				{
					try {
						
						tokenToCallbackQueue.add(token);
					} catch(IllegalStateException e)
					{
						e.printStackTrace();
						log.error("Exception occurred while adding stuff for callback", e);
						
						throw e;
					}
				}
			}
		} else
		{
			log.trace("Token {} called for {} was spurious ", token, rc);
		}
		
		return false;
		
	}
	
	/**
	 * Runnables for submitting jobs to the next TAE
	 * 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private class RunSubmitter implements Runnable
	{

		@Override
		public void run() {
			
			//These guys are stateless so we can share them
			final SubmissionObserver tObs = new SubmissionObserver();
			
			
			while(true)
			{
				try
				{
					List<AlgorithmRunConfiguration> rcs;
					try {
						rcs = submissionQueue.take();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
						
					
					for(AlgorithmRunConfiguration rc : rcs)
					{
						if(!outstandingRunConfigMap.get(rc).get())
						{
							log.error("Outstanding run being submitted {} is not marked outstanding, synchronization error detected", rc);
							
							RuntimeException e = new IllegalStateException("Outstanding run being submitted" + rc + "is not marked outstanding, synchronization error detected");
							
							if(errorDetected.compareAndSet(false, true))
							{
								storedException.set(e);
								e.printStackTrace();
							}
							
							if(shutdownOnError) throw e;
							
						} else
						{
							//log.info("Runconfig {} has outstandingFlag {}", rc, outstandingRunConfigMap.get(rc));
						}
					}
					tae.evaluateRunsAsync(rcs, new SubmissionOnCompleteHandler(rcs), tObs);
					
					submittedToDecoratee.addAndGet(rcs.size());
					//Release anyone who is waiting for this to be submitted.
					log.trace("Runs have been successfully submitted to TAE: {}", rcs);
					for(AlgorithmRunConfiguration rc : rcs)
					{
						Semaphore lt = runConfigsSubmittedToWrappedDecoratorMap.get(rc);
						if(lt == null)
						{
							log.error("No Semaphores for run config, this is a violation of our invariant {}" ,rc);
							
							if(shutdownOnError) throw new IllegalStateException("No Semaphores for run config, this is a violation of our invariant" + rc);
						} else if(lt.availablePermits() != 0)
						{
							log.error("Semaphores seemingly has the wrong value of latches left for rc  {}  value {} ", rc, lt);
							
							if(shutdownOnError)  throw new IllegalStateException("Semaphores seemingly has the wrong value of latches left for "+rc+" value "+ lt);
							
						} else
						{
							lt.release();
						}
					}
				} catch(RuntimeException e)
				{
					if(errorDetected.compareAndSet(false, true))
					{
						storedException.set(e);
						e.printStackTrace();
					}
					throw e;
				}
				
				
				if(shutdownOnError && errorDetected.get())
				{
					//log.info("Observer notification detected error, and has shutdown");
					return;
				}
			}
			
		}
		
	}
	
	/**
	 * Callback that is fired once the batch of runs we submitted are done
	 * 
	 * This essentially determines which callbacks should now be fired and queues them.
	 * 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 */
	private class SubmissionOnCompleteHandler implements TargetAlgorithmEvaluatorCallback
	{
		private final List<AlgorithmRunConfiguration> submissionRunConfigs;
		
		public SubmissionOnCompleteHandler(List<AlgorithmRunConfiguration> rcs)
		{
			this.submissionRunConfigs = rcs;
		}

		@Override
		public void onSuccess(List<AlgorithmRunResult> runs) 
		{

			try 
			{
				if(shutdownOnError && errorDetected.get())
				{
					//log.info("Target Algorithm Evaluator has detected error and is unavailable");
					return;
				}
				
				
				for(AlgorithmRunResult run : runs)
				{
					
					AlgorithmRunConfiguration rc = run.getAlgorithmRunConfiguration();
					
					if(run.getRunStatus().equals(RunStatus.KILLED))
					{
						log.debug("Inserting killed run: {}", run);
						
						killedRunsMap.put(rc, run);
						
					} else
					{
						completedRunsMap.put(rc, run);
						completedRunConfigs.add(rc);
						
					}
					
					
					
					AtomicBoolean b = outstandingRunConfigMap.get(rc);
					log.trace("RunConfig in onSuccess() current value is {} ==> {}",rc , b);
					if(!b.compareAndSet(true, false))
					{
						log.error("Marking outstanding run {} as not outstanding didn't work, synchronization error detected", run);
						
						RuntimeException e = new IllegalStateException("Outstanding run " + rc + " wasn't set as running, this is a synchronization error");
						
						if(errorDetected.compareAndSet(false, true))
						{
							storedException.set(e);
							e.printStackTrace();
						}
						
						if(shutdownOnError) 
						{
							throw e;
						}
					} else
					{
						log.trace("Marked run as no longer outstanding: {}", rc);
					}
					
					
					for(EvaluationRequestToken t : runConfigToTokenMap.get(rc))
					{
						//System.err.println(t + "==>" + run );
						processRunConfigurationForRequestToken(t,rc);
					}
					
				}
			} catch(RuntimeException e)
			{
				if(errorDetected.compareAndSet(false, true))
				{
					storedException.set(e);
					e.printStackTrace();
				}
				throw e;
			}
			
			
		}

		@Override
		public void onFailure(RuntimeException e) 
		{
			try 
			{
			
				for(AlgorithmRunConfiguration rc : submissionRunConfigs)
				{
					AtomicBoolean b = outstandingRunConfigMap.get(rc);
					log.trace("RunConfig in onFailure() current value is {} ==> {}",rc , b);
					if(!b.compareAndSet(true, false))
					{
						log.error("Marking outstanding run {} as not outstanding didn't work, synchronization error detected", rc);
						
						RuntimeException e2 = new IllegalStateException("Outstanding run " + rc + " wasn't set as running, this is a synchronization error");
						
						if(errorDetected.compareAndSet(false, true))
						{
							storedException.set(e2);
							e.printStackTrace();
						}
						
						if(shutdownOnError) 
						{
							throw e2;
						}
					} else
					{
						log.trace("Marked run as no longer outstanding: {}", rc);
					}
				}
				for(AlgorithmRunConfiguration rc : submissionRunConfigs)
				{
					completedExceptionMap.put(rc, e);
				}
				
				
				for(AlgorithmRunConfiguration rc : submissionRunConfigs)
				{
					completedRunConfigs.add(rc);
					for(EvaluationRequestToken t : runConfigToTokenMap.get(rc))
					{
						processRunConfigurationForRequestToken(t,rc);
					}
				}
			} catch(RuntimeException e2)
			{
				if(errorDetected.compareAndSet(false, true))
				{
					storedException.set(e2);
					e2.printStackTrace();
				}
				throw e;
			}
		}
		

	}

	
	
	/**
	 * Observer registered with the wrapped TAE
	 * 
	 * We assume that this Observer is STATELESS elsewhere.
	 * 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 */
	private class SubmissionObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs)
		{
			
			try 
			{
				Set<EvaluationRequestToken> updateTokens = new HashSet<EvaluationRequestToken>();
				for(AlgorithmRunResult run : runs)
				{
					AlgorithmRunConfiguration rc = run.getAlgorithmRunConfiguration();
										
					runConfigToLiveLatestStatusMap.put(rc, new RunningAlgorithmRunResult(rc, run.getRuntime(), run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getWallclockExecutionTime(), new NullKillHandler()));
					
					for(EvaluationRequestToken token : runConfigToTokenMap.get(rc))
					{
						
						updateTokens.add(token);
					}
					
					
					
					int interestedEvaluations = runConfigToInterestedEvaluationsCounter.get(rc).get();
					if(interestedEvaluations == 0)
					{
						//log.debug("Run Config {} has no one else interested... terminating", rc);
						run.kill();
					}
					
					if(interestedEvaluations < 0)
					{
						log.error("Run Config {} has negative interested parties, how odd: {}", rc, interestedEvaluations);
						System.err.println("Run Config "+rc + " has negative interested parties, how odd: " + interestedEvaluations);
						
					}
					
				}
				
				for(EvaluationRequestToken token : updateTokens)
				{
					token.updatedRuns();
					tokenToObserverQueue.add(token);
				}
			} catch(RuntimeException e)
			{
				if(errorDetected.compareAndSet(false, true))
				{
					storedException.set(e);
					e.printStackTrace();
				}
				throw e;
			}
			
			
		}
		
	}
	
	/**
	 * Runnable that notifies observers
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private class ObserverInvoker implements Runnable
	{
		public void run()
		{
			while(true)
			{
				EvaluationRequestToken token;
				try {
					token = tokenToObserverQueue.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
					
				
				if(shutdownOnError && errorDetected.get())
				{
					log.info("Observer notification detected error, and has shutdown");
					return;
				}
				
				notifyObserverOfToken(token);
				
				
					
			}
		}
	}
	
	
	private void notifyObserverOfToken(EvaluationRequestToken token)
	{
		TargetAlgorithmEvaluatorRunObserver obs = evalRequestToObserverMap.get(token);
		
		if(obs == null)
		{	//Probably doesn't matter but just in case
			return;
		}
	
		boolean lockAcquired = token.tryLock();
		
		if(lockAcquired)
		{
			try 
			{
				//Observer can only fire before the callback
				//callback sets this before acquiring lock, we have the lock so 
				//callback will wait for us.
				if(token.callbackFired())
				{
					return;
				}
				
				//Token controls last time stamp we notified, so that we don't send duplicate updates
				if(!token.shouldNotify())
				{
					return;
				}
				
				try 
				{
					
					List<AlgorithmRunConfiguration> rcs = allRunConfigsForTokenMap.get(token);
					
					List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>(rcs.size());
					
					for(AlgorithmRunConfiguration rc : rcs)
					{
						
						AlgorithmRunResult krun;
						
						boolean rcCompleted = completedRunConfigs.contains(rc);
						if(!rcCompleted)
						{
							//We should use the live version
							AlgorithmRunResult liveRun = runConfigToLiveLatestStatusMap.get(rc);
							
							
							if(liveRun == null)
							{
								//Exception has probably occurred
								return;
							}
							
							krun = new RunningAlgorithmRunResult(liveRun.getAlgorithmRunConfiguration(), liveRun.getRuntime(), liveRun.getRunLength(), liveRun.getQuality(), liveRun.getResultSeed(), liveRun.getWallclockExecutionTime(), new ExternalCallerKillHandler(token, rc));
						} else
						{
							
							
							
							AlgorithmRunResult run = completedRunsMap.get(rc);
							
							if(run != null)
							{
								krun = run;
							} else
							{
								if(!completedExceptionMap.containsKey(rc))
								{
									System.err.println("No exception handy?");
								}
								
								return;
							}
							
						}
						
						runs.add(krun); 
					}
					
					obs.currentStatus(runs);
					
				} catch(RuntimeException e)
				{
					if(errorDetected.compareAndSet(false, true))
					{
						storedException.set(e);
						e.printStackTrace();
					}
					throw e;
				}
			} finally
			{
				token.unlock();
			}
		}
	}
	
	/**
	 * Runnable that notifies the callbacks of runs being completed
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private class CallbackInvoker implements Runnable
	{
		public void run()
		{
			while(true)
			{
				EvaluationRequestToken token;
				try {
					token = tokenToCallbackQueue.take();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				
				if(shutdownOnError && errorDetected.get())
				{
					log.info("Callback Invoker detected error, and has shutdown");
					return;
				}
				
				//Set the fired flag before we get the lock. Once we have the lock, we know observers get the lock first and then check the flag.
				token.fireCallback();
				token.lock();
				try 
				{
					TargetAlgorithmEvaluatorCallback callback = evalRequestToCallbackMap.get(token);
					try 
					{
						List<AlgorithmRunConfiguration> rcs = allRunConfigsForTokenMap.get(token);
						
						List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>(rcs.size());
						
						for(AlgorithmRunConfiguration rc : rcs)
						{
							
							
							if(!completedRunConfigs.contains(rc) && !killedRunsForToken.get(token).contains(rc))
							{

								log.error("Run is not marked as completed, we didn't kill it, yet callback is being fired: {}", rc);
								if(shutdownOnError)
								{
									
									if(errorDetected.compareAndSet(false, true))
									{
										RuntimeException e = new IllegalStateException("Callback fired but not all runs are complete");
										storedException.set(e);
										e.printStackTrace();
									}
								}
							}
						
							AlgorithmRunResult run = completedRunsMap.get(rc);
							
							
							if(run == null)
							{
								run = killedRunsMap.get(rc);
							} 
							
							if(!killedRunsForToken.get(token).contains(rc))
							{
								runConfigToInterestedEvaluationsCounter.get(rc).decrementAndGet();
							}
									
							if(run != null)
							{
								runs.add(run); 
							} else
							{
								throw completedExceptionMap.get(rc);
							}
							
							//
							runConfigToTokenMap.get(rc).remove(token);
						}
						
						
						callback.onSuccess(runs);
						
					} catch(RuntimeException e)
					{
						callback.onFailure(e);
					}
				} finally
				{
					
					allRunConfigsForTokenMap.remove(token);
					outstandingRunsCountForTokenMap.remove(token);
					
					evalRequestToCallbackMap.remove(token);
					
					//Remove this last so that other calls to removeRC() don't throw an NPE
					
					outstandingRunsForTokenMap.remove(token);
					token.unlock();
				}
			}
		}
		
	}
	
	/**
	 * Token class that essentially allows us to differentiate between requests with the same callback
	 * 
	 * It stores some fields so that we can coordinate between the callback being fired and the observer being notified.
	 * 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private static final class EvaluationRequestToken
	{
		
		private final ReentrantLock lock = new ReentrantLock();
		
		private final AtomicBoolean callbackFired = new AtomicBoolean();
		
		
		private final AtomicLong lastChangeUpdate = new AtomicLong(0);
		
		private final AtomicLong lastNotification = new AtomicLong(0);
		
		public boolean callbackFired()
		{
			return callbackFired.get();
		}
		
		public void fireCallback()
		{
			callbackFired.set(true);
		}
		
		public boolean tryLock()
		{
			return lock.tryLock();
		}
		
		public void lock()
		{
			lock.lock();
		}
		
		public void unlock()
		{
			lock.unlock();
		}
		
		public void updatedRuns()
		{
			lastChangeUpdate.set(System.currentTimeMillis());
		}
		
		public boolean shouldNotify()
		{
			
			long lastNotifyTime = lastNotification.get();
			while(true)
			{
				
				long lastChange = lastChangeUpdate.get();
				
				
				if(lastNotifyTime < lastChange)
				{
					long currentTime = System.currentTimeMillis();
					
					if(lastNotification.compareAndSet(lastNotifyTime, currentTime))
					{
						return true;
					} else
					{
						continue;
					}
					
				} else
				{
					return false;
				}
				
			}
			
			
			
		}
		
		@Override
		public String toString()
		{
			return "ExecToken:" + String.format("0x%X", hashCode());
		}
	}
	
	
	/**
	 * Generates some kind of debug message
	 */
	private final void debugMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		String banner = "====================";
		sb.append(banner).append("[ " + getClass().getSimpleName() + " Status ]").append(banner);
		
		sb.append("\nDecorated TAE Reports:" + tae.getNumberOfOutstandingBatches() + " Batches, "  + tae.getNumberOfOutstandingRuns() + " runs");
		
		sb.append("\nOutstanding RunConfigs: ");
		
		try 
		{
			int count = 0; 
			for(Entry<AlgorithmRunConfiguration, AtomicBoolean> ent : this.outstandingRunConfigMap.entrySet())
			{
				
				if(ent.getValue().get())
				{
					count++;
					
					if(count < 5)
					{
						sb.append(ent.getKey() + ",");
					} else if(count == 5)
					{
						sb.append("...");
					}
				}
			}
			
			sb.append("\nOutstanding RunConfigs count:" + count);
			
			
			sb.append("\nOutstanding Evaluations (from outstandingRunsCountForTokenMap): ").append(this.outstandingRunsCountForTokenMap.size());
			
			if(this.outstandingRunsCountForTokenMap.size() < 200)
			{
				sb.append("Outstanding Tokens to Size Display:");
				for(Entry<EvaluationRequestToken, AtomicInteger> ent : outstandingRunsCountForTokenMap.entrySet())
				{
					sb.append("\n" + ent.getKey() + "=> "+ ent.getValue());
					sb.append("  Specific Runs Size: " + this.outstandingRunsForTokenMap.get(ent.getKey()).size());
					
					if(this.outstandingRunsForTokenMap.get(ent.getKey()).size() < 5)
					{
						sb.append("\n Outstanding Runs for Token :" + this.outstandingRunsForTokenMap.get(ent.getKey()) );
					}
					sb.append("\n" + this.outstandingRunsForTokenMap.get(ent.getKey()));
					
					for(AlgorithmRunConfiguration rc : this.outstandingRunsForTokenMap.get(ent.getKey()))
					{
						sb.append("\n " + rc + "=>");
						if(completedRunConfigs.contains(rc))
						{
							
							if(completedExceptionMap.get(rc) != null)
							{
								sb.append("RunConfig has Error?:" + completedExceptionMap.get(rc));
							} else if(completedRunsMap.get(rc) != null)
							{
								sb.append("Outstanding RunConfig Completed?:" + completedExceptionMap.get(rc));
							} else if(killedRunsMap.get(rc) != null)
							{
								sb.append("Run Config is killed, and completed:?" + killedRunsMap.get(rc) );
							} else
							{
								sb.append("No idea where the completed run is?");
							}
							
						} else if(killedRunsMap.get(rc) != null)
						{
							sb.append("==> Killed properly " + killedRunsMap.get(rc));
							
							sb.append((this.killedRunsForToken.get(ent.getKey()).contains(rc) ? "[Expected to be killed]" : "[Didn't want killed]"));
							
							sb.append("Interested parties:" + runConfigToInterestedEvaluationsCounter.get(rc));
						} else if(outstandingRunConfigMap.get(rc).get())
						{
							sb.append("Still outstanding.");
						} else
						{
							sb.append("Not outstanding, yet no where to be found");
						}
					}
					
	
				}
			}
		} catch(RuntimeException e)
		{
			sb.append("Error occurred trying to make debug information ");
			e.printStackTrace();
		}
		
		System.err.println(sb.toString());
		log.info(CommonMarkers.SKIP_CONSOLE_PRINTING, sb.toString());
		
	}

	/**
	 * Null Observer that is used if we don't have one to prevent an NPE.
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private final static class NullTargetAlgorithmEvaluatorRunObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		@Override
		public void currentStatus(List<? extends AlgorithmRunResult> runs)
		{
			//NOOP
		}
		
	}
	
	/**
	 * This class is used in the AlgorithmRun objects handed to the caller, it essentially flags this run as killed, and if this is the first time
	 * decreases the number of interested evaluations. 
	 * 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private final class ExternalCallerKillHandler implements KillHandler
	{

		private final EvaluationRequestToken token;
		private final AlgorithmRunConfiguration rc;
		
		private ExternalCallerKillHandler(EvaluationRequestToken token, AlgorithmRunConfiguration rc)
		{
			this.token = token;
			this.rc = rc;
		}
		
		
		@Override
		public void kill() 
		{

			if(killedRunsForToken.get(token).add(rc))
			{
				
				log.debug("Run {} has been killed for token: {}", rc, token);
				//We are the first thread to request it be killed, so we decrement the counter
				
				int remainingInterestedJobs = runConfigToInterestedEvaluationsCounter.get(rc).decrementAndGet();
				//System.err.println("Other interested jobs:" + remainingInterestedJobs);
				if(remainingInterestedJobs > 0)
				{
					
					//You could possibly short circuit here and call removeRC()
				} else if (remainingInterestedJobs == 0)
				{
					//Else
				} else
				{
					RuntimeException e = new IllegalStateException("Interested parties is seemingly negative for runConfig:" + rc  + " value " +  remainingInterestedJobs);
					if(errorDetected.compareAndSet(false, true))
					{
						storedException.set(e);
						e.printStackTrace();
					}
					throw e;
				}
				
			}
			
		}

		@Override
		public boolean isKilled() {
			try {
				return killedRunsForToken.get(token).contains(rc);
			} catch(RuntimeException e)
			{
				
				if(errorDetected.compareAndSet(false, true))
				{
					storedException.set(e);
					e.printStackTrace();
				}
				//throw e;
				
				return false;
			}
		}
		
	}
	
	
	/**
	 * Kill Handler that doesn't do anything, it is used as a place holder instead of Null. It is expected that things are never called
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private final static class NullKillHandler implements KillHandler
	{

		@Override
		public void kill() {
			throw new IllegalStateException("Wasn't expecting this to be called");
		}

		@Override
		public boolean isKilled() {
			throw new IllegalStateException("Wasn't expecting this to be called");
		}
		
	}
	
	@Override
	public boolean isRunFinal()
	{
		return true;
	}


	
}

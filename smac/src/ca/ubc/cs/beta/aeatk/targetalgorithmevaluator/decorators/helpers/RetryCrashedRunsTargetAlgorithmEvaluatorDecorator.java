package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.StatusVariableKillHandler;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator;

/**
 * Retries crashed runs some number of times
 * 
 * This should be transparent to the end user, so all runs must appear in order, and the run count should not show the retried runs.

 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class RetryCrashedRunsTargetAlgorithmEvaluatorDecorator extends AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator {

	/**
	 * Number of runs we have done
	 */
	private AtomicInteger runCount = new AtomicInteger(0);
	
	/**
	 * Number of crashes we saw
	 */
	private final AtomicInteger crashedRunCount = new AtomicInteger(0);
	
	/**
	 * Number of crashes we reported
	 */
	private final AtomicInteger crashedRunCountReported = new AtomicInteger(0);
	
	/**
	 * Number of times we shoud retry
	 */
	private final int retryCount; 
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Generate a one time warning on a crash
	 */
	private final AtomicBoolean warningOnRetry = new AtomicBoolean(false);
	
	/**
	 * Total wasted walltime
	 */
	private final AtomicDouble walltime = new AtomicDouble();
	
	/**
	 * Total wasted CPU Time
	 */
	private final AtomicDouble runtime = new AtomicDouble();
	
	public RetryCrashedRunsTargetAlgorithmEvaluatorDecorator(int retryCount, TargetAlgorithmEvaluator tae) {
		super(tae);
		if(retryCount < 0)
		{
			throw new IllegalArgumentException("Retry Count should be atleast 0");
		}
		this.retryCount = retryCount;
		
		if(tae.isRunFinal())
		{
			log.warn("Target Algorithm Evaluator {} issues final runs, retrying will be a waste of time", tae.getClass().getSimpleName());
		}
	}
	
	
	
	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> rcs,
			final TargetAlgorithmEvaluatorCallback handler, final TargetAlgorithmEvaluatorRunObserver obs) {
		
		final List<AlgorithmRunConfiguration> runConfigs = Collections.unmodifiableList(rcs);
		
		final Map<AlgorithmRunConfiguration, AlgorithmRunResult> observerRunStatusMap = new ConcurrentHashMap<>();
		
		
		
		final Map<AlgorithmRunConfiguration, StatusVariableKillHandler> killHandlers = new ConcurrentHashMap<>();
		
		
		for(AlgorithmRunConfiguration rc : runConfigs)
		{
			StatusVariableKillHandler kh = new StatusVariableKillHandler();
			killHandlers.put(rc, kh);
			observerRunStatusMap.put(rc, new RunningAlgorithmRunResult(rc, 0,0,0,0,0,kh));
		}
		
		TargetAlgorithmEvaluatorRunObserver noCrashedRunsNotifiedRunObserver = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				if(obs == null) return;
				
				/**
				 * NOTE: This runs object may actually not contain all the runs we need, as we may not see every run, on subsequent retries
				 */
				for(AlgorithmRunResult run : runs)
				{

					if(run.isRunCompleted() && !run.getRunStatus().equals(RunStatus.CRASHED))
					{
						observerRunStatusMap.put(run.getAlgorithmRunConfiguration(),run);
					} else if(run.getRunStatus().equals(RunStatus.CRASHED))
					{
						//NOOP
					} else if (run.getRunStatus().equals(RunStatus.RUNNING))
					{
						RunningAlgorithmRunResult runningRun = new RunningAlgorithmRunResult(run.getAlgorithmRunConfiguration(), run.getRuntime(), run.getRunLength(), run.getQuality(), run.getResultSeed(), run.getWallclockExecutionTime(), killHandlers.get(run.getAlgorithmRunConfiguration()));
						observerRunStatusMap.put(run.getAlgorithmRunConfiguration(), runningRun);
					} else
					{
						throw new IllegalStateException("Expected that the run is either running or crashed or completed, something must have changed");
					}
					
				}

				List<AlgorithmRunResult> runsToPassForward = new ArrayList<>(runConfigs.size());
				
				
				for(AlgorithmRunConfiguration runConfig : runConfigs)
				{
					runsToPassForward.add(observerRunStatusMap.get(runConfig));
				}
				
				try {
					obs.currentStatus(runsToPassForward);
				} finally
				{
					for(AlgorithmRunResult run : runs)
					{
						if (run.getRunStatus().equals(RunStatus.RUNNING))
						{
							if(killHandlers.get(run.getAlgorithmRunConfiguration()).isKilled())
							{
								run.kill();
							}
						}
					}
				}
				
				
				
			}
			
		};
		
		final Map<AlgorithmRunConfiguration, AlgorithmRunResult> callbackRunStatusMap = new ConcurrentHashMap<>();
		
		
		TargetAlgorithmEvaluatorCallback callback = new RetryingTargetAlgorithmEvaluatorCallback(retryCount, callbackRunStatusMap, handler, runConfigs, tae, obs, noCrashedRunsNotifiedRunObserver);
		tae.evaluateRunsAsync(runConfigs, callback, noCrashedRunsNotifiedRunObserver);
	}
	
	
	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}

	@Override
	public int getRunCount() {
		//Override this because internal TAE's have probably seen more runs
		return runCount.get();
	}

	@Override
	public void seek(List<AlgorithmRunResult> runs)
	{
		tae.seek(runs);
		runCount.addAndGet(runs.size());
	}



	@Override
	protected void postDecorateeNotifyShutdown() 
	{
		if(crashedRunCount.get() > 0)
		{
			//log.info("During execution we received (and subsequently retried up to {} times) {} crashed runs, we ultimately reported {} runs back to the caller, this caused a loss of {} reported CPU seconds and {} wall-clock seconds", retryCount, crashedRunCount.get(),crashedRunCountReported.get(),  runtime.get(), walltime.get() );
		}
	}
	
	
	/**
	 * Callback which resubmits runs up to some limit 
	 * @author Steve Ramage <seramage@cs.ubc.ca>
	 *
	 */
	private class RetryingTargetAlgorithmEvaluatorCallback implements TargetAlgorithmEvaluatorCallback
	{

		private final int retriesLeft;
		private final Map<AlgorithmRunConfiguration, AlgorithmRunResult> knownResults;
		private final TargetAlgorithmEvaluatorCallback originalCallback;
		private final List<AlgorithmRunConfiguration> originalRunConfigurations;
		private final TargetAlgorithmEvaluator tae;
		private final TargetAlgorithmEvaluatorRunObserver clientsTaeObs;
		private final TargetAlgorithmEvaluatorRunObserver taeObs;

		public RetryingTargetAlgorithmEvaluatorCallback(int retriesLeft, Map<AlgorithmRunConfiguration, AlgorithmRunResult> knownResults, TargetAlgorithmEvaluatorCallback originalCallback, List<AlgorithmRunConfiguration> originalRunConfigurations, TargetAlgorithmEvaluator tae, TargetAlgorithmEvaluatorRunObserver clientsTaeObs, TargetAlgorithmEvaluatorRunObserver taeObs)
		{
			this.retriesLeft = retriesLeft;
			
			if(retriesLeft < 0)
			{
				throw new IllegalStateException("Must have atleast zero retries left (in which case we won't retry)");
			}
			this.knownResults = knownResults;
			this.originalCallback = originalCallback;
			this.originalRunConfigurations = originalRunConfigurations;
			this.clientsTaeObs = clientsTaeObs;
			this.tae = tae;
			this.taeObs = taeObs;
		}
		
		@Override
		public void onSuccess(List<AlgorithmRunResult> runs)
		{
			List<AlgorithmRunConfiguration> runConfigsToRetry = new ArrayList<AlgorithmRunConfiguration>(runs.size());
			
			boolean crashedRunsToRetry = false;
			for(AlgorithmRunResult runResult : runs)
			{
				
				if(!runResult.getRunStatus().equals(RunStatus.CRASHED) || retriesLeft == 0)
				{
					
					knownResults.put(runResult.getAlgorithmRunConfiguration(), runResult);
				} else if(runResult.getRunStatus().equals(RunStatus.CRASHED) && retriesLeft > 0)
				{
					
					
					
					crashedRunsToRetry = true;
					crashedRunCount.incrementAndGet();
					runtime.addAndGet(runResult.getRuntime());
					walltime.addAndGet(runResult.getWallclockExecutionTime());
					
					runConfigsToRetry.add(runResult.getAlgorithmRunConfiguration());
				} else
				{
					throw new IllegalStateException("Expected the two conditionals above this to cover all cases");
				}
			}
			
			
			if(crashedRunsToRetry)
			{
				if(warningOnRetry.compareAndSet(false, true))
				{
					log.warn("We have detected a crashed run and will automatically retrying it, other alerts will only be logged at DEBUG level: {} ", CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString(runConfigsToRetry.get(0)));
				} else
				{
					log.debug("Retrying {} crashed runs (Attempts {} left)", runConfigsToRetry.size(), retriesLeft);
				}
				
				TargetAlgorithmEvaluatorCallback taeCallback = new RetryingTargetAlgorithmEvaluatorCallback(retriesLeft-1, knownResults, originalCallback, originalRunConfigurations, tae, clientsTaeObs, taeObs);
				tae.evaluateRunsAsync(runConfigsToRetry, taeCallback , taeObs);
			} else
			{
				List<AlgorithmRunResult> runResults = new ArrayList<>(originalRunConfigurations.size());
				
			
				for(AlgorithmRunConfiguration rc : originalRunConfigurations)
				{
					AlgorithmRunResult runResult = knownResults.get(rc);
					
					if(runResult == null)
					{
						throw new IllegalStateException("Expected that run configuration : " + rc + " would have a result in map with size: " + knownResults.size() + " and contents: " + knownResults);
					} else
					{
						if(runResult.getRunStatus().equals(RunStatus.CRASHED))
						{
							crashedRunCountReported.incrementAndGet(); 
						}
					
						runResults.add(runResult);
					}
				}
				
				if(runResults.size() != originalRunConfigurations.size())
				{
					throw new IllegalStateException("Expected that the number of runs returned would equal the number of run configurations requested, instead got: " + runResults.size() + " vs: " + originalRunConfigurations.size() );
				}
				
				/**
				 * The TAE observer will not actually notify of any CRASHED runs, 
				 * We should notify the observer of the final run results, so we will explicitly do that here.
				 */
				if(clientsTaeObs != null)
				{
					clientsTaeObs.currentStatus(runResults);
				}
				runCount.addAndGet(runResults.size());
				originalCallback.onSuccess(runResults);
			}
		}

		@Override
		public void onFailure(RuntimeException e) {
			originalCallback.onFailure(e);
		}
		
	}
	
}

package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.concurrent.ReducableSemaphore;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Target Algorithm Evaluator that implements the monitoring outstanding evalutions
 * <b>NOTE:</b> You need to be VERY careful with respect to the decorator order, things like the BoundedTargetAlgorithmEvaluator if applied on this will break
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	
	private final ReducableSemaphore outstandingRunBlocks = new ReducableSemaphore(1);
	
	private final AtomicInteger outstandingRuns = new AtomicInteger(0);
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	
	private final Object lock = new Object();
	
	private final AtomicInteger runCount = new AtomicInteger();
	
	public OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
	}


	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		try{
			
			logReduce(runConfigs);
			preRun(runConfigs);
			List<AlgorithmRunResult> runs =  tae.evaluateRun(runConfigs, obs);
			postRun(runConfigs);
			runCount.addAndGet(runs.size());
			return runs;
		} finally
		{
			logRelease(runConfigs);
		}
		
	}

	private void logReduce(List<AlgorithmRunConfiguration> runConfigs)
	{
		outstandingRunBlocks.reducePermits();
		outstandingRuns.addAndGet(runConfigs.size());
		/*
		if(log.isTraceEnabled())
		{
			RunConfig rc = null;
			if(runConfigs.size() > 0)
			{
				rc = runConfigs.get(0);
			}
			log.trace("Reducing Permits by 1 now for {}, Dirty Read: {}", rc,  outstandingRunBlocks.availablePermits());	
		}
		*/
	}
	
	private void logRelease(List<AlgorithmRunConfiguration> runConfigs)
	{
	
		outstandingRuns.addAndGet(-1*runConfigs.size());
		outstandingRunBlocks.release();
		
		/*
		if(log.isTraceEnabled())
		{
			RunConfig rc = null;
			if(runConfigs.size() > 0)
			{
				rc = runConfigs.get(0);
			}
			log.trace("Releasing Permits by 1 now for {}, Dirty Read: {}", rc,  outstandingRunBlocks.availablePermits());	
		}
		*/
	}

	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		
		logReduce(runConfigs);
		preRun(runConfigs);
		TargetAlgorithmEvaluatorCallback callback = new TargetAlgorithmEvaluatorCallback()
		{

			AtomicBoolean bool = new AtomicBoolean(false);
			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				postRun(runConfigs);
				runCount.addAndGet(runs.size());
				handler.onSuccess(runs);
				
				//Release happens after because it is still outstanding at this point until the callback has fired.
				synchronized (this) {
					if(!bool.get())
						{
							bool.set(true);
							logRelease(runConfigs);
						}
					}
				
			}

			@Override
			public void onFailure(RuntimeException t) {
				try {
					postRun(runConfigs);
					handler.onFailure(t);
					
				} finally
				{
					//Release happens after because it is still outstanding at this point until the callback has fired.
					synchronized (this) {
						if(!bool.get())
							{
								bool.set(true);
								logRelease(runConfigs);
							}
						}
				}
				
			}
			
		};
		tae.evaluateRunsAsync(runConfigs, callback, obs);
		
	}

	/**
	 * Waits for there to be no outstanding runs
	 * <b>NOTE:</b> This isn't the same as waiting for a shutdown, this waits until the number of runs in progress is zero, it can later go higher again.
	 */
	@Override
	public void waitForOutstandingEvaluations()
	{
		try {
			outstandingRunBlocks.acquire();
		} catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return;
		}
		
		outstandingRunBlocks.release();
	
	}
	
	@Override
	public int getNumberOfOutstandingEvaluations()
	{
		return 1 - outstandingRunBlocks.availablePermits();
	}
	
	@Override 
	public int getNumberOfOutstandingRuns()
	{
		return this.outstandingRuns.get();
	}
	
	@Override
	public int getNumberOfOutstandingBatches()
	{
		return 1 - outstandingRunBlocks.availablePermits();
	}
	
	@Override
	public int getRunCount()
	{
		return 	runCount.get();
	}
	
	/***
	 * Additional template methods
	 */
	
	protected void preRun(List<AlgorithmRunConfiguration> runConfigs)
	{
		//Template Method
	}
	
	protected void postRun(List<AlgorithmRunConfiguration> runConfigs)
	{
		//Template Method
	}
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
	
}

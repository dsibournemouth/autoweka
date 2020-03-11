package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/***
 * Generates a warning if the TAE does not seem have generated any 'feedback'.
 * <br/>
 * This is primarily to gaurd against workers or an IPC mechanism where the other side is not doing anything
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class WarnOnNoWallOrRuntimeTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator{

	
	private final ExecutorService execService = Executors.newFixedThreadPool(1,new SequentiallyNamedThreadFactory("TAE Warning Checking Thread",true)); 
	
	private volatile boolean observed = false;
	
	
	private final Semaphore runsSubmitted = new Semaphore(0);
	private final Semaphore runsObserved = new Semaphore(0);
	
	public WarnOnNoWallOrRuntimeTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae, final int timeToWaitInSeconds) {
		super(tae);
		
		//=== 
		execService.execute(new Runnable()
		{

			@Override
			public void run() {
				
				try {
					try {
					
						runsSubmitted.acquire();
						StopWatch watch = new AutoStartStopWatch();
						boolean acquired = runsObserved.tryAcquire(timeToWaitInSeconds, TimeUnit.SECONDS);
						watch.stop();
						if(!acquired)
						{
							Logger log = LoggerFactory.getLogger(getClass());
							log.warn("Runs have been submitted to Target Algorithm Evaluator but we have not seen any progress (no observed runs have any non-zero walltime or runtime) after {} seconds of waiting, it's possible that the observer frequency is too low, or something is wrong with the Target Algorithm Evaluator (for instance some require that you start external processes and will hang if you don't).", watch.time() / 1000 );
						}
					} catch (InterruptedException e) {
						//==== Do nothing because we probably are just being shutdown
						Thread.currentThread().interrupt();
					} 
				}
				finally
				{
					execService.shutdown();
				}
			}
			
		});
		
	}
	

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs)
	{
		
		if(observed)
		{
			return tae.evaluateRun(runConfigs, obs);
		} else
		{
			runsSubmitted.release();
			List<AlgorithmRunResult> runs = (tae.evaluateRun(runConfigs, new WarningObserver(obs)));
			markObserved();
			return runs;
		}
		
		
		
		
	}
	
	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,	final TargetAlgorithmEvaluatorCallback oHandler, TargetAlgorithmEvaluatorRunObserver obs) 
	{
		
		if(observed)
		{
			tae.evaluateRunsAsync(runConfigs, oHandler, obs);
		} else
		{
			runsSubmitted.release();
			TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
			{
				private final TargetAlgorithmEvaluatorCallback handler = oHandler;

				@Override
				public void onSuccess(List<AlgorithmRunResult> runs) {
						markObserved();		
						handler.onSuccess(runs);
				}

				@Override
				public void onFailure(RuntimeException t) {
						markObserved();
						handler.onFailure(t);
				}
			};
			
			tae.evaluateRunsAsync(runConfigs, myHandler, new WarningObserver(obs));
		}

	}

	private class WarningObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private TargetAlgorithmEvaluatorRunObserver obs;
		WarningObserver(TargetAlgorithmEvaluatorRunObserver obs)
		{
			this.obs = obs;
		}
		
		@Override
		public synchronized void currentStatus(List<? extends AlgorithmRunResult> runs) 
		{
			
			if(obs != null)
			{
				obs.currentStatus(runs);
			}
			
			if(!observed)
			{
				for(AlgorithmRunResult run : runs)
				{
					if(run.getWallclockExecutionTime() > 0) 
					{
						markObserved();
						return;
					}
					
					if(run.getWallclockExecutionTime() > 0) 
					{
						markObserved();
						return;
					}
					
				}
			}
		}
		
		
	}
	
	@Override
	public final void postDecorateeNotifyShutdown()
	{
		this.execService.shutdownNow();
	}
	
	private final synchronized void markObserved()
	{
		observed = true;
		runsObserved.release();
		if(!execService.isShutdown())
		{
			execService.shutdownNow();
		}
	}
}

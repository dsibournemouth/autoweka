package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.concurrent.threadfactory.SequentiallyNamedThreadFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator;

/**
 * This decorator ensures that the {@link #evaluateRunsAsync()} method's never block. Obviously {@link #evaluateRun()} will still block.
 * 
 * This can be useful when you do not want more than a certain amount of runs 
 * to go past a certain point, but don't actually want to wait for them.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class NonBlockingAsyncTargetAlgorithmEvaluatorDecorator extends	AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator {

	private final ExecutorService execService = Executors.newCachedThreadPool(new SequentiallyNamedThreadFactory(getClass().getSimpleName() + " Processor", true));
			
	private final BlockingQueue<Triple> queue = new LinkedBlockingQueue<Triple>();
	
	public NonBlockingAsyncTargetAlgorithmEvaluatorDecorator(final TargetAlgorithmEvaluator tae) {
		this(tae, 1);
	}
	
	public NonBlockingAsyncTargetAlgorithmEvaluatorDecorator(final TargetAlgorithmEvaluator tae, int threads) {
		super(tae);
		
		for(int i=0; i < threads; i++)
		{
			execService.execute(new Runnable()
			{

				@Override
				public void run() {
					
					
					try {
						while(true)
						{
							Triple t;
							t = queue.take();
							tae.evaluateRunsAsync(t.runConfigs, t.callback, t.observer);
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
					
					
					
				}
				
			});
		}
		
	}
	

	@Override
	public void postDecorateeNotifyShutdown()
	{
		execService.shutdownNow();
	}

	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer) {
		try {
			queue.put(new Triple(runConfigs, callback, observer));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}


	private class Triple
	{
		final List<AlgorithmRunConfiguration> runConfigs;
		final TargetAlgorithmEvaluatorCallback callback;
		final TargetAlgorithmEvaluatorRunObserver observer;
		
		public Triple(List<AlgorithmRunConfiguration> runConfigs2,
				TargetAlgorithmEvaluatorCallback callback2,
				TargetAlgorithmEvaluatorRunObserver observer2) {
			
			this.runConfigs = runConfigs2;
			this.callback = callback2;
			this.observer = observer2;
				
		}
		
		
	}
	
	
	
}

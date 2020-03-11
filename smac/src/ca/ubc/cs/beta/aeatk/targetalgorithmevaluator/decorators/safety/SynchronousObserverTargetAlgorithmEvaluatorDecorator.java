package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;


import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Decorates the observer that is supplied to the TAE to ensure that it is only run once at a time.
 * <p>
 * <b>NOTE:</b> The primary purpose of this decorator is to provide "some" guarantees with respect to thread safety of the observer.
 * <p>
 * <b>WARN:<b> This only ensures that for the set of runs the observations are consistent, it doesn't work if you use the same instance of the observer
 * for multiple runs, for that you should make the observer be synchronized.
 * <p>
 * By synchronizing here we guarantee that the observer won't be called simultaneously in two threads, but really the reason to do this  
 * is the guarantee that changes to local state made by the observer are visible if it is called in a different thread later (a few TAEs do this),
 * in general this is probably redundant in most cases but it simplifies the observers for clients. 
 * <p>For more information see Chapter 16 of Java Concurrency in Practice. 
 *
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class SynchronousObserverTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	public SynchronousObserverTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
	}

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return (tae.evaluateRun(runConfigs, ((obs != null) ? new SynchronousObserver(obs): null)));
	}
	
	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback oHandler, TargetAlgorithmEvaluatorRunObserver obs) 
	{
		tae.evaluateRunsAsync(runConfigs, oHandler, ((obs != null) ? new SynchronousObserver(obs): null));
	}

	private class SynchronousObserver implements TargetAlgorithmEvaluatorRunObserver
	{

		private TargetAlgorithmEvaluatorRunObserver obs;
		SynchronousObserver(TargetAlgorithmEvaluatorRunObserver obs)
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
		}
		
		
	}
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}

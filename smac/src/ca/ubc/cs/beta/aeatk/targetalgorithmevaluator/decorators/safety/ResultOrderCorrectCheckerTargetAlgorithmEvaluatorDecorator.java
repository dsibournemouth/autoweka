package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.Collections;
import java.util.List;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * This TAE Decorator ensures that the order of results in correct
 * 
 * Specifically it throws an error if order of the RunConfig objects in the  <code>List&gt;AlgorithmRun&lt;</code> doesn't match the order we submitted them in the <code>List&gt;RunConfig&lt;</code> 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class ResultOrderCorrectCheckerTargetAlgorithmEvaluatorDecorator extends AbstractTargetAlgorithmEvaluatorDecorator {
	
	public ResultOrderCorrectCheckerTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae)
	{
		super(tae);
	}
	
	
	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		List<AlgorithmRunResult> runs = tae.evaluateRun(Collections.unmodifiableList(runConfigs), obs);
		
		runOrderIsConsistent(runConfigs, runs);
		
		return runs;	
	}


	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		TargetAlgorithmEvaluatorCallback callback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) 
			{
				try {
					runOrderIsConsistent(runConfigs,runs);
					handler.onSuccess(runs);
				} catch(RuntimeException e)
				{
					handler.onFailure(e);
				}
			}

			@Override
			public void onFailure(RuntimeException t) 
			{
				handler.onFailure(t);
			}
			
		};
		
		
		tae.evaluateRunsAsync(Collections.unmodifiableList(runConfigs), callback, obs);
	}

	
	private void runOrderIsConsistent(List<AlgorithmRunConfiguration> runConfigs, List<AlgorithmRunResult> runs)
	{
		if(runConfigs.size() != runs.size())
		{
			throw new IllegalStateException("TAE did not return the correct sized results, submitted: " + runConfigs.size() + " got back: " + runs.size());
		}
		
		for(int i=0; i < runConfigs.size(); i++)
		{
			if(runs.get(i).getAlgorithmRunConfiguration().equals(runConfigs.get(i)))
			{
				continue;
			} else
			{
				throw new IllegalStateException("TAE did not return results in the correct order entry (" + i + ") was RunConfig: " + runConfigs.get(i) + " but the resulting run was :" + runs.get(i).getAlgorithmRunConfiguration());
			}
		}
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}

}

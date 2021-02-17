package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.List;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;

/**
 * Modifies Target Algorithm Evaluators to treat CRASHES as aborts
 * @author Steve Ramage <seramage@cs.ubc.ca> 
 *
 */
@ThreadSafe
public class AbortOnCrashTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	public AbortOnCrashTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae) {
		super(tae);
		
	}
	
	

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return validate(super.evaluateRun(runConfigs, obs));
	}
	

	private List<AlgorithmRunResult> validate(List<AlgorithmRunResult> runs)
	{
		
		for(AlgorithmRunResult run : runs)
		{
			if(run.getRunStatus().equals(RunStatus.CRASHED))
			{
				throw new TargetAlgorithmAbortException("Target Algorithm Run Reported Crashed: " + run.toString());
			}
		}
		return runs;
	}
	
	
	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		
		
		TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				try {
					validate(runs);
					handler.onSuccess(runs);
				} catch(TargetAlgorithmAbortException e)
				{
					handler.onFailure(e);
				}
				
			}

			@Override
			public void onFailure(RuntimeException t) {
				handler.onFailure(t);
				
			}
			
		};
		
		tae.evaluateRunsAsync(runConfigs, myHandler, obs);
	}



	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}

}

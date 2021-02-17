package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.safety;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * If the first run is a crash we will abort otherwise we ignore it
 * 
 * @author Steve Ramage 
 *
 */
@ThreadSafe
public class AbortOnFirstRunCrashTargetAlgorithmEvaluator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	public AbortOnFirstRunCrashTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae) {
		super(tae);
		
	}
	
	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return validate(super.evaluateRun(runConfigs, obs));
	}
	
	private final AtomicBoolean firstRunChecked = new AtomicBoolean(false);
	
	private List<AlgorithmRunResult> validate(List<AlgorithmRunResult> runs)
	{
		if(runs.size() == 0)
		{
			return runs;
		}
	
		if(firstRunChecked.getAndSet(true)) 
		{
			return runs;
		} else
		{		
		
			if(runs.get(0).getRunStatus().equals(RunStatus.CRASHED))
			{
				if(runs.get(0).getAdditionalRunData().startsWith("ERROR:"))
				{
					throw new TargetAlgorithmAbortException(runs.get(0).getAdditionalRunData().substring(6));
					
				} else
				{
					throw new TargetAlgorithmAbortException("First Run Crashed : " + runs.toString());
				}
				 
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

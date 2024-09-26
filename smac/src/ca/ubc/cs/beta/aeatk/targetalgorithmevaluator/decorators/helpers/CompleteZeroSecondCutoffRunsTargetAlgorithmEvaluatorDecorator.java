package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers;

import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.PartialResultsAggregator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractAsyncTargetAlgorithmEvaluatorDecorator;

public class CompleteZeroSecondCutoffRunsTargetAlgorithmEvaluatorDecorator
		extends AbstractAsyncTargetAlgorithmEvaluatorDecorator {

	public CompleteZeroSecondCutoffRunsTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
	}

	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runs, final TargetAlgorithmEvaluatorCallback callback, final TargetAlgorithmEvaluatorRunObserver obs)
	{
		final PartialResultsAggregator pra = new PartialResultsAggregator(runs);
		
		for(AlgorithmRunConfiguration rc : runs)
		{
			if(rc.getCutoffTime() <= 0)
			{
				AlgorithmRunResult run = new ExistingAlgorithmRunResult(rc, RunStatus.TIMEOUT, 0, 0, 0, rc.getProblemInstanceSeedPair().getSeed(), "Processed by " + getClass().getCanonicalName(), 0);
				pra.updateCompletedRun(run);
			}	
		}
		
		
		TargetAlgorithmEvaluatorCallback taeCallback = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				pra.updateCompletedRuns(runs);
				callback.onSuccess(pra.getCurrentRunStatusOnCompletion());
				
			}

			@Override
			public void onFailure(RuntimeException e) {
				
				callback.onFailure(e);
			}
			
		};
		
		
		TargetAlgorithmEvaluatorRunObserver newObs = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				pra.updateCurrentRunStatus(runs);
				
				if(obs != null)
				{
					obs.currentStatus(pra.getCurrentRunStatusForObserver());
				}
			}
			
		};
		
		
		List<AlgorithmRunConfiguration> rcsToDo = pra.getOutstandingRunConfigurationsAsList();
		
		if(rcsToDo.size() > 0)
		{
			tae.evaluateRunsAsync(rcsToDo, taeCallback, newObs);
		} else
		{
			taeCallback.onSuccess(pra.getCurrentRunStatusOnCompletion());
		}
		
	}
	
	
	@Override
	protected void postDecorateeNotifyShutdown() {
		// TODO Auto-generated method stub

	}

}

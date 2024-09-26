package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

public abstract class AbstractAsyncTargetAlgorithmEvaluator extends AbstractTargetAlgorithmEvaluator implements TargetAlgorithmEvaluator{


	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		 return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs,this,obs);
	}

	
}

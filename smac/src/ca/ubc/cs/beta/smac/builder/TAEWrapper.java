package ca.ubc.cs.beta.smac.builder;

import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;

public interface TAEWrapper {

	public TargetAlgorithmEvaluator wrap(TargetAlgorithmEvaluator tae);
}

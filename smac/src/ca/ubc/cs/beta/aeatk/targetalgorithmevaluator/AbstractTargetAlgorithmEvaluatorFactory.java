package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.Map;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

/**
 * Abstract Target Algorithm Evaluator Factory that has a default implementation for getting the TargetAlgorithmEvaluator
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public abstract class AbstractTargetAlgorithmEvaluatorFactory implements
		TargetAlgorithmEvaluatorFactory {

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(
			Map<String, AbstractOptions> optionsMap) {
		return this.getTargetAlgorithmEvaluator( optionsMap.get(this.getName()));
	}
	

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator() {
		return this.getTargetAlgorithmEvaluator(this.getOptionObject());
	}

	
}

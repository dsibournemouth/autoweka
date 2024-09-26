package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.constant;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class ConstantTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory 
{

	@Override
	public String getName() {
		return "CONSTANT";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator( AbstractOptions options) {
		return new ConstantTargetAlgorithmEvaluator( (ConstantTargetAlgorithmEvaluatorOptions) options);
	}

	@Override
	public AbstractOptions getOptionObject() {
		return new ConstantTargetAlgorithmEvaluatorOptions();
	}

}

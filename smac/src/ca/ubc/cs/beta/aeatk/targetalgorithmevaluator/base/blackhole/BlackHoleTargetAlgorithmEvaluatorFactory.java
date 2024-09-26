package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.blackhole;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class BlackHoleTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory {

	@Override
	public String getName() {
		return "BLACKHOLE";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AbstractOptions options) {

		return new BlackHoleTargetAlgorithmEvaluator( (BlackHoleTargetAlgorithmEvaluatorOptions) options);
	}

	@Override
	public BlackHoleTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new BlackHoleTargetAlgorithmEvaluatorOptions();
		
	}

}

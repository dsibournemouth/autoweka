package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.analytic;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.SimulatedDelayTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)

public class AnalyticTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  {

	@Override
	public String getName() {
		return "ANALYTIC";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator( AbstractOptions options) {
		
		AnalyticTargetAlgorithmEvaluatorOptions analyticOptions = (AnalyticTargetAlgorithmEvaluatorOptions) options;
		
		TargetAlgorithmEvaluator tae = new AnalyticTargetAlgorithmEvaluator(analyticOptions.func);
		
		if(analyticOptions.simulateDelay)
		{
			tae = new SimulatedDelayTargetAlgorithmEvaluatorDecorator(tae, analyticOptions.observerFrequency, analyticOptions.scaleDelay);
		}
		
		if(analyticOptions.cores > 0)
		{
			tae = new BoundedTargetAlgorithmEvaluator(tae, analyticOptions.cores);
		}
		
		return tae;
	}

	@Override
	public AnalyticTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new AnalyticTargetAlgorithmEvaluatorOptions();
	}

}

package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.random;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.SimulatedDelayTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.BoundedTargetAlgorithmEvaluator;

@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class RandomResponseTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  
{

	@Override
	public String getName() {
		return "RANDOM";
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AbstractOptions options) {
		RandomResponseTargetAlgorithmEvaluatorOptions randomOptions = (RandomResponseTargetAlgorithmEvaluatorOptions) options;
		
		TargetAlgorithmEvaluator tae =  new RandomResponseTargetAlgorithmEvaluator(randomOptions);
		
		if(randomOptions.simulateDelay)
		{
			tae = new SimulatedDelayTargetAlgorithmEvaluatorDecorator(tae, randomOptions.observerFrequency, randomOptions.scaleDelay);
		}
		
		if(randomOptions.cores > 0)
		{
			tae = new BoundedTargetAlgorithmEvaluator(tae, randomOptions.cores);
		}
		
		return tae;
		
	}

	@Override
	public RandomResponseTargetAlgorithmEvaluatorOptions getOptionObject() {
		return new RandomResponseTargetAlgorithmEvaluatorOptions();
	}
	
	

}

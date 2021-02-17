package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.forking;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.TAEValidator;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(title="Forking Target Algorithm Evaluator Decorator Options", description="This Target Algorithm Evaluator Decorator allows you to delegate some runs to another TAE, denoted the slave TAE. Several policies are implemented (or will be upon request/need). The first two duplicate the run on the slave, and the primary motivation is performance of very short runs, where overhead of dispatch to the primary might be surprisingly high. The next two (to be implemented), would allow some runs to simply done by the slave, either before the master or after the master.")
public class ForkingTargetAlgorithmEvaluatorDecoratorOptions extends AbstractOptions {
	
	@UsageTextField(level=OptionLevel.ADVANCED, defaultValues="Forking of requests is disabled")
	@Parameter(names={"--fork-to-tae"}, description="If not null, runs will also be submitted to this other TAE at the same time. The first TAE that returns an answer is used.", validateWith=TAEValidator.class)
	public String forkToTAE = null;
	
	@ParametersDelegate
	public ForkingTargetAlgorithmEvaluatorDecoratorPolicyOptions fPolicyOptions = new ForkingTargetAlgorithmEvaluatorDecoratorPolicyOptions();
}

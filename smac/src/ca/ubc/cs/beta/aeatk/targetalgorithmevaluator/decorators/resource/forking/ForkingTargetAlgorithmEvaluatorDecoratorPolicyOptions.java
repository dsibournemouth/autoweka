package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.forking;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

/**
 * Most options for the Forking Target Algorithm Evaluator go in here,
 * except for those that the constructor can't handle (for instance which TAE to fork).
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@UsageTextField(hiddenSection = true)
public class ForkingTargetAlgorithmEvaluatorDecoratorPolicyOptions extends AbstractOptions{

	@UsageTextField(level=OptionLevel.ADVANCED, defaultValues="Must be explicitly set if the forkToTAE is not null")
	@Parameter(names={"--fork-to-tae-policy"}, description="Selects the policy that we will fork with. For instance DUPLICATE_ON_SLAVE will simply submit runs to the slave as well. DUPLICATE_ON_SLAVE_QUICK will submit the runs to the slave, but with a reduced cutoff time")
	public ForkingPolicy fPolicy = null;
	
	@UsageTextField(level=OptionLevel.ADVANCED, defaultValues="5 seconds")
	@Parameter(names={"--fork-to-tae-duplicate-on-slave-quick-timeout"}, description="What timeout to use when the DUPLICATE_ON_SLAVE_QUICK policy.", validateWith=FixedPositiveInteger.class)
	public int duplicateOnSlaveQuickTimeout = 5;
	
	
	public enum ForkingPolicy 
	{
		DUPLICATE_ON_SLAVE,
		DUPLICATE_ON_SLAVE_QUICK
	}
}

package ca.ubc.cs.beta.aeatk.initialization.doublingcapping;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(hiddenSection = true)
public class DoublingCappingInitializationProcedureOptions extends AbstractOptions{

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--doubling-capping-challengers", description="Number of challengers to use with the doubling capping mechanism", validateWith=FixedPositiveInteger.class)
	public int numberOfChallengers = 2;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--doubling-capping-runs-per-challenger", description="Number of runs each challenger will get with the doubling capping initilization strategy", validateWith=FixedPositiveInteger.class)
	public int numberOfRunsPerChallenger = 2;

	
	
}

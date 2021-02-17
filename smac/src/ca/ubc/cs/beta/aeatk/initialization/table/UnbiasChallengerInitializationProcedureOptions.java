package ca.ubc.cs.beta.aeatk.initialization.table;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(hiddenSection = true)
public class UnbiasChallengerInitializationProcedureOptions extends AbstractOptions{

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--unbiased-capping-challengers", description="Number of challengers we will consider during initialization", validateWith=FixedPositiveInteger.class)
	public int numberOfChallengers = 2;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--unbiased-capping-runs-per-challenger", description="Number of runs we will consider during initalization per challenger", validateWith=FixedPositiveInteger.class)
	public int numberOfRunsPerChallenger = 2;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--unbiased-capping-cpulimit", description="Amount of CPU Time to spend constructing table in initialization phase", validateWith=FixedPositiveInteger.class)
	public int cpulimit;
	
}

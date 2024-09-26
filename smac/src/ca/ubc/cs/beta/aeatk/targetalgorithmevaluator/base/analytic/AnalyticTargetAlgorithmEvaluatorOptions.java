package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.analytic;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ZeroInfinityOpenInterval;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(title="Analytic Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator uses an analytic function to generate a runtime. Most of the function definitions come from Test functions for optimization needs, by Marcin Molga, Czesław Smutnicki (http://www.zsd.ict.pwr.wroc.pl/files/docs/functions.pdf). NOTE: Some functions have been shifted vertically so that there response values are always positive.", level=OptionLevel.ADVANCED)
public class AnalyticTargetAlgorithmEvaluatorOptions extends AbstractOptions {
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--analytic-simulate-delay", description = "If set to true the TAE will simulate the wallclock delay")
	public boolean simulateDelay = false;
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--analytic-simulate-cores", description = "If set to greater than 0, the TAE will serialize requests so that no more than these number will execute concurrently. ", validateWith=NonNegativeInteger.class)
	public int cores = 0;
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--analytic-observer-frequency", description="How often to notify observer of updates (in milli-seconds)", validateWith=FixedPositiveInteger.class)
	public int observerFrequency = 100;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--analytic-function", description="Which analytic function to use")
	public AnalyticFunctions func = AnalyticFunctions.CAMELBACK;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--analytic-scale-simulate-delay", description="Divide the simulated delay by this value", validateWith=ZeroInfinityOpenInterval.class)
	public double scaleDelay =1.0;

	
}

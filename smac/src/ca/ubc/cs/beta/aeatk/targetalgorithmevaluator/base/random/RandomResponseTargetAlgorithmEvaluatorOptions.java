package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.random;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ZeroInfinityHalfOpenIntervalRight;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ZeroInfinityOpenInterval;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;

@UsageTextField(title="Random Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator randomly generates responses from a uniform distribution", level=OptionLevel.DEVELOPER)
public class RandomResponseTargetAlgorithmEvaluatorOptions extends AbstractOptions {

	@Parameter(names="--random-simulate-delay", description = "If set to true the TAE will simulate the wallclock delay")
	public boolean simulateDelay = false;
	
	@Parameter(names="--random-additional-run-data", description="Additional Run Data to return")
	public String additionalRunData = "";
	
	@Parameter(names="--random-simulate-cores", description = "If set to greater than 0, the TAE will serialize requests so that no more than these number will execute concurrently. ", validateWith=NonNegativeInteger.class)
	public int cores = 0;
	
	@Parameter(names="--random-max-response", description="The maximum runtime we will generate", validateWith=ZeroInfinityHalfOpenIntervalRight.class)
	public double maxResponse = 10.0;

	@Parameter(names="--random-min-response", description="The minimum runtime we will generate (values less than 0.01 will be rounded up to 0.01)", validateWith=ZeroInfinityHalfOpenIntervalRight.class)
	public double minResponse = 0.0;
	
	@Parameter(names="--random-observer-frequency", description="How often to notify observer of updates (in milli-seconds)", validateWith=FixedPositiveInteger.class)
	public int observerFrequency = 500;
	
	
	
	
	
	@Parameter(names="--random-trend-coefficient", description="The Nth sample will be drawn from Max(0,Uniform(min,max) + N*(trend-coefficient)) distribution. This allows you to have the response values increase or decrease over time.")
	public double trendCoefficient = 0.0;

	@UsageTextField(defaultValues = "Current Time in Milliseconds", level=OptionLevel.DEVELOPER)
	@Parameter(names="--random-sample-seed", description="Seed to use when generate random responses")
	public long seed = System.currentTimeMillis();

	@Parameter(names="--random-report-persistent", description="Determines whether the TAE reports whether it is persistent", hidden=true)
	public boolean persistent = false;

	@Parameter(names="--random-scale-simulate-delay", description="Divide the simulated delay by this value", validateWith=ZeroInfinityOpenInterval.class)
	public double scaleDelay = 1.0;
	
	//Shuffle the Algorithm Runs List before it returns.
	
	//DO NOT EXPOSE THIS ARGUMENT TO THE COMMAND LINE AS IT EXISTS ONLY FOR TESTING THAT WHEN THIS HAPPENS
	//IT CAN BE CAUGHT
	public boolean shuffleResponses;

	//DO NOT EXPOSE THIS ARGUMENT TO THE COMMAND LINE AS IT'S VERY UGLY
	public long sleepInternally =0; 
	
}

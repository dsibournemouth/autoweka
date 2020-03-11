package ca.ubc.cs.beta.aeatk.smac;


import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.*;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

/**
 * Options object controlling validation
 * 
 */
@UsageTextField(title="Validation Options", description="Options that control validation")
public class ValidationOptions extends AbstractOptions{


	@UsageTextField(domain="[0, Infinity) U {-1}",defaultValues="Auto Detect", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--max-timestamp","--maxTimestamp"}, description="maximimum relative timestamp in the trajectory file to configure against. -1 means auto-detect", required=false)
	public double maxTimestamp = -1;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--min-timestamp","--minTimestamp"}, description="minimum relative timestamp in the trajectory file to configure against.", required=false, validateWith=ZeroInfinityHalfOpenIntervalRight.class)
	public double minTimestamp = 0;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--mult-factor","--multFactor"}, description="base of the geometric progression of timestamps to validate (for instance by default it is maxTimestamp, maxTimestamp/2, maxTimestamp/4,... whiletimestamp >= minTimestamp )", validateWith=ZeroInfinityOpenInterval.class)
	public double multFactor = 2;
	
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--num-test-instances","--numTestInstances","--numberOfTestInstances"}, description = "Deprecated/Broken: Check results carefully: number of instances to test against (will execute min of this, and number of instances in test instance file). To disable validation in SMAC see the --doValidation option", validateWith=FixedPositiveInteger.class)
	public int numberOfTestInstances = Integer.MAX_VALUE;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--num-seeds-per-test-instance","--numSeedsPerTestInstance","--numberOfSeedsPerTestInstance"}, description="Deprecated/Broken: number of test seeds to use per instance during validation", validateWith=FixedPositiveInteger.class)
	public int numberOfTestSeedsPerInstance = 1000;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--num-validation-runs","--numValidationRuns","--numberOfValidationRuns"}, description = "approximate number of validation runs to do", validateWith=NonNegativeInteger.class)
	public int numberOfValidationRuns = 1;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--output-file-suffix","--outputFileSuffix"}, description="Suffix to add to validation run files (for grouping)")
	public String outputFileSuffix = "";
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--save-state-file","--saveStateFile"}, description="Save a state file consisting of all the runs we did")
	public boolean saveStateFile;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--validate-by-wallclock-time","--validateByWallClockTime"}, description="Validate runs by wall-clock time")
	public boolean useWallClockTime = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--validate-all","--validateAll"},description="Validate every entry in the trajectory file (overrides other validation options)")
	public boolean validateAll = false;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--validate-only-if-tunertime-reached","--validateOnlyIfTunerTimeReached"}, description="If the walltime in the trajectory file hasn't hit this entry we won't bother validating", validateWith=ZeroInfinityHalfOpenIntervalRight.class)
	public double validateOnlyIfTunerTimeReached = 0.0;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--validate-only-if-walltime-reached","--validateOnlyIfWallTimeReached"}, description="If the walltime in the trajectory file hasn't hit this entry we won't bother validating", validateWith=ZeroInfinityHalfOpenIntervalRight.class)
	public double validateOnlyIfWallTimeReached = 0.0;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--validate-only-last-incumbent","--validateOnlyLastIncumbent"}, description="validate only the last incumbent found")
	public boolean validateOnlyLastIncumbent = true;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--validation-headers","--validationHeaders"}, description="put headers on output CSV files for validation")
	public boolean validationHeaders = true;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--validation-rounding-mode","--validationRoundingMode"}, description="selects whether to round the number of validation (to next multiple of numTestInstances")
	public ValidationRoundingMode validationRoundingMode = ValidationRoundingMode.UP;
	
}

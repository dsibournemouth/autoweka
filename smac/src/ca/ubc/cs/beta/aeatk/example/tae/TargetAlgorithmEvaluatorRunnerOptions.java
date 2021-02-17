package ca.ubc.cs.beta.aeatk.example.tae;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aeatk.logging.LoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.LongGreaterThanNegativeTwoValidator;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions.TrainTestInstances;

/**
 * A JCommander Options object that controls the command line options
 * available for this utility. 
 * 
 * For more information see: <a href="http://jcommander.org/">JCommander</a>.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */

//The noarg option controls what happens if we start the application with no arguments
@UsageTextField(title="Target Algorithm Evaluator Running Options", description=" Utility that allows for making a single run against a target algorithm evaluator ", noarg=TargetAlgorithmEvaluatorRunnerNoArgumentHandler.class)
public class TargetAlgorithmEvaluatorRunnerOptions extends AbstractOptions {
	
	/**
	 * A defaults file that allows users to store various preferences on disk
	 * For instance they could change this utility to default to a RANDOM configuration instead of the DEFAULT configuration by putting 
	 * config = RANDOM 
	 * in the file.
	 */
	@UsageTextField(defaultValues="~/.aeatk/taerunner.opt")
	@Parameter(names="--taeRunnerDefaultsFile", description="file that contains default settings for TAE Runner")
	@ParameterFile(ignoreFileNotExists = true) 
	public File smacDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "taerunner.opt");
	
	
	/**
	 * Delegate that allows this application to read scenario options
	 */
	@ParametersDelegate
	public ScenarioOptions scenOptions = new ScenarioOptions();
	
	/**
	 * Delegate that controls logging
	 */
	@ParametersDelegate
	public LoggingOptions logOpts = new ConsoleOnlyLoggingOptions();
	
	/**
	 * THis parameter is not required (but if set to true, would be)
	 */
	@Parameter(names="--instance", description="Instance name to test", required=false)
	public String instanceName = null; 
	
	enum InstanceSelection
	{
		FIRST,
		RANDOM
	}

	/**
	 * Controls how we select the instance from the instance list
	 */
	@Parameter(names="--instance-selection", description="Determines how we select instances from the instance file")
	public InstanceSelection instanceSelection = InstanceSelection.RANDOM;
	
	/**
	 * This parameter has a validator that enforces certain values
	 */
	@Parameter(names="--seed", description="Seed to run instance with", validateWith=LongGreaterThanNegativeTwoValidator.class)
	public long seed = 1; 
	
	
	@Parameter(names="--config", description="Configuration to run (Use DEFAULT for the default, RANDOM for a random, or otherwise -name 'value' syntax)")
	public String config = "DEFAULT";
	
	@Parameter(names="--config-seed", description="Seed to use if we generate a RANDOM configuration")
	public int configSeed = 0;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--print-json", description="If true will print the JSON representation of a run")
	public boolean printJSON = false;
	/**
	 * This is a dynamic parameter
	 */
	@DynamicParameter(names="-P", description="Name value pairs in the form: (-Pname=value) of the specific configuration to override. This is useful if you'd like to change a setting of the default , or try a random with a set value)")
	public Map<String, String> configSettingsToOverride = new HashMap<String, String>();

	@Parameter(names="--kill-time", description="Kill the run dynamically after this much runtime")
	public double killTime = Double.POSITIVE_INFINITY;

	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig() {
		return this.scenOptions.algoExecOptions.getAlgorithmExecutionConfig(null);
	}
	
	@ParametersDelegate
	HelpOptions help = new HelpOptions();

	public TrainTestInstances getTrainingAndTestProblemInstances() throws IOException {
		return scenOptions.getTrainingAndTestProblemInstances("./", 0, 0, true, false, false, false);
	}
	
}

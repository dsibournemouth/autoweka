package ca.ubc.cs.beta.aeatk.example.statemerge;

import java.io.File;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aeatk.logging.LoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.model.ModelBuildingOptions;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.RandomForestOptions;
import ca.ubc.cs.beta.aeatk.options.scenario.ScenarioOptions;

@UsageTextField(title="State File Merge Utility", description="Merges many different state files", noarg=StateMergeNoArgumentHandler.class)
public class StateMergeOptions extends AbstractOptions {

	@ParametersDelegate
	ScenarioOptions scenOpts = new ScenarioOptions();
	
	@Parameter(names="--directories", description="Directories to search for state files", variableArity = true)
	public List<String> directories =Collections.singletonList(".");
	
	@Parameter(names="--up-to-iteration", description="Only restore runs up to iteration ")
	public int iterationLimit = Integer.MAX_VALUE;
	
	@Parameter(names="--up-to-tunertime", description="Only restore runs up to tuner time limit")
	public int tunerTime = Integer.MAX_VALUE;
	
	@ParametersDelegate 
	public HelpOptions helpOptions = new HelpOptions();
	
	@UsageTextField(defaultValues="false if scenario is deterministic, true otherwise")
	@Parameter(names="--replace-seeds", description="If true, existing seeds for problem instances will be replaced by new seeds starting from 1. (every run for the same pisp will map to the same new pisps)")
	public Boolean replaceSeeds = null;
	
	
	@ParametersDelegate
	public LoggingOptions logOpts = new ConsoleOnlyLoggingOptions();
	
	@Parameter(names="--seed", description="Seed to use for randomization")
	public int seed = 1;
	
	@ParametersDelegate
	public ModelBuildingOptions mbo = new ModelBuildingOptions();
	
	@ParametersDelegate
	public RandomForestOptions rfo = new RandomForestOptions();
	
	@UsageTextField(defaultValues="<current working directory>", level=OptionLevel.BASIC)
	@Parameter(names={"--experiment-dir","--experimentDir","-e"}, description="root directory for experiments folder")
	public String experimentDir = System.getProperty("user.dir") + File.separator + "";

	@Parameter(names={"--repair-smac-invariant","--repair"}, description="If true we will ensure that the incumbent has all runs that are saved in the state file, using a random forest to pick the best run. ")
	public boolean repairMaxRunsForIncumbentInvariant = true;
	
	@Parameter(names={"--restore-args","--restore-scenario-arguments"}, description="A string that will be used to restore the individual state files, this allows you to override options in the saved scenarios for instance \"--cutoffTime 5.0\" would change the cutoff time on all restored scenarios")
	public String restoreScenarioArguments = "";
	
	
	
}

package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli;

import java.io.File;

import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;


@UsageTextField(title="Command Line Target Algorithm Evaluator Options", description="This Target Algorithm Evaluator executes commands via the command line and the standard wrapper interface. ")
public class CommandLineTargetAlgorithmEvaluatorOptions extends AbstractOptions {
	

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names="--cli-observer-frequency", description="How often to notify observer of updates (in milli-seconds)", validateWith=FixedPositiveInteger.class)
	public int observerFrequency = 500;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--cli-concurrent-execution", description="Whether to allow concurrent execution ")
	public boolean concurrentExecution = true;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--cli-cores", description="Number of cores to use to execute runs. In other words the number of requests to run at a given time.", validateWith=FixedPositiveInteger.class)
	public int cores = 1;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cli-log-all-call-strings","--log-all-call-strings","--logAllCallStrings"}, description="log every call string")
	public boolean logAllCallStrings = false;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cli-log-all-results","--cli-log-all-call-results", "--log-all-call-results", "--log-all-results"}, description="log all the result lines")
	public boolean logAllResultLines = false;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cli-log-all-calls","--cli-log-all-call-strings-and-results","--log-all-calls", "--log-all-call-strings-and-results"}, description="log all the call strings and result lines")
	public boolean logAllCallsAndResultLines = false;
	
	
	public boolean logAllCallStrings()
	{
		//boolean logAllCallStrings = this.logAllCallStrings == null ? false : this.logAllCallStrings;
		//boolean logAllCallResults = this.logAllResultLines == null ? false : this.logAllResultLines;
		//boolean logAllCallsAndResultLines = this.logAllCallStrings == null ? false : this.logAllCallsAndResultLines;

		return logAllCallStrings || logAllCallsAndResultLines;
				
	}
	
	public boolean logAllCallResults()
	{
		//boolean logAllCallStrings = this.logAllCallStrings == null ? false : this.logAllCallStrings;
		//boolean logAllCallResults = this.logAllResultLines == null ? false : this.logAllResultLines;
		//boolean logAllCallsAndResultLines = this.logAllCallStrings == null ? false : this.logAllCallsAndResultLines;
		return logAllResultLines| logAllCallsAndResultLines;
		
	}

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--cli-log-all-process-output","--log-all-process-output","--logAllProcessOutput"}, description="log all process output")
	public boolean logAllProcessOutput = false;
	
	//@UsageTextField(level=OptionLevel.INTERMEDIATE)
	//@Parameter(names={"--log-all-call-strings-and-results","--cli-log-all-call-strings-and-result"}, description="log every call string")
	//public boolean logAllCallStringsAndResults = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-listen-for-updates"}, description="If true will create a socket and set environment variables so that we can have updates of CPU time")
	public boolean listenForUpdates = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-call-params-with-quotes"}, description="If true calls to the target algorithm will have parameters that are quoted \"'3'\" instead of \"3\". Older versions of the code passed arguments with '. This has been removed and will be deprecated in the future ")
	public boolean paramArgumentsContainQuotes = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-kill-by-environment-cmd"}, description="If not null, this script will be executed with three arguments, the first a key, the second a value, the third our best guess at a pid (-1 means we couldn't guess). They represent environment name and value, and the script should find every process with that name and value set and terminate it. Do not assume that the key is static as it may change based on existing environment variables. Example scripts may be available in example_scripts/env_kill/")
	public String pgEnvKillCommand = null;
	
	
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-pg-nice-kill-cmd"}, description="Command to execute to try and ask the process group to terminate nicely (generally a SIGTERM in Unix). Note %pid will be replaced with the PID we determine.")
	public String pgNiceKillCommand = "bash -c \"kill -s TERM -%pid\"";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-pg-force-kill-cmd"}, description="Command to execute to try and ask the process group to terminate nicely (generally a SIGKILL in Unix). Note %pid will be replaced with the PID we determine.")
	public String pgForceKillCommand = "bash -c \"kill -s KILL -%pid\"";
	
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-proc-nice-kill-cmd"}, description="Command to execute to try and ask the process to terminate nicely (generally a SIGTERM in Unix). Note %pid will be replaced with the PID we determine.")
	public String procNiceKillCommand = "kill -s TERM %pid";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-proc-force-kill-cmd"}, description="Command to execute to try and ask the process to terminate nicely (generally a SIGTERM in Unix). Note %pid will be replaced with the PID we determine.")
	public String procForceKillCommand = "kill -s KILL %pid";
	
	
	@UsageTextField(defaultValues="~/.aeatk/cli-tae.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--cli-default-file"}, description="file that contains default settings for CLI Target Algorithm Evaluator (it is recommended that you use this file to set the kill commands)")
	@ParameterFile(ignoreFileNotExists = true) 
	public File smacDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "cli-tae.opt");
	
	
	
	
	
	
}

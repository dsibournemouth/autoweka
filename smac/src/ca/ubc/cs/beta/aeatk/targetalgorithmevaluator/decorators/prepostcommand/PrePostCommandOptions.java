package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.prepostcommand;

import java.io.File;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ReadableDirectoryValidator;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(hiddenSection = true)
public class PrePostCommandOptions extends AbstractOptions {

	@UsageTextField(defaultValues="", level=OptionLevel.DEVELOPER)
	@Parameter(names={"--pre-scenario-command","--preScenarioCommand","--pre_cmd"}, description="Command that will run on startup")
	public String preCommand;
	
	@UsageTextField(defaultValues="", level=OptionLevel.DEVELOPER)
	@Parameter(names={"--post-scenario-command","--postScenarioCommand","--post_cmd"}, description="Command that will run on shutdown")
	public String postCommand;
	
	@UsageTextField(defaultValues="", level=OptionLevel.DEVELOPER)
	@Parameter(names={"--exception-on-prepost-command","--exceptionOnPrePostCommand"}, description="Throw an abort ")
	public boolean exceptionOnError=false;
	
	@UsageTextField(defaultValues="", level=OptionLevel.DEVELOPER)
	@Parameter(names={"--prepost-log-output","--logOutput"}, description="Log all the output from the pre and post commands")
	public boolean logOutput = true;

	
	@UsageTextField(defaultValues="Current Working Directory", level=OptionLevel.DEVELOPER)
	@Parameter(names={"--prepost-exec-dir","--prePostExecDir"}, description="Execution Directory for Pre/Post commands", converter=ReadableDirectoryValidator.class)
	public File directory = new File(".");
	
}

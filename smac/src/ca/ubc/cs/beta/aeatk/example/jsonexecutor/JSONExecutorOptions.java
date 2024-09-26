package ca.ubc.cs.beta.aeatk.example.jsonexecutor;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;

@UsageTextField(title="JSON Executor", description="Utility that reads a set of runs from standard in, in JSON format, and outputs the results in JSON when done")
public class JSONExecutorOptions extends AbstractOptions{

	@ParametersDelegate 
	public TargetAlgorithmEvaluatorOptions taeOptions = new TargetAlgorithmEvaluatorOptions();
	
	@ParametersDelegate
	public HelpOptions helpOptions = new HelpOptions();
	
	@ParametersDelegate
	public ConsoleOnlyLoggingOptions logOpts = new ConsoleOnlyLoggingOptions();

	@Parameter(names="--print-status", description="If true, then periodically the status of outstanding runs will be logged")
	public boolean printStatus = true;
	
	
}

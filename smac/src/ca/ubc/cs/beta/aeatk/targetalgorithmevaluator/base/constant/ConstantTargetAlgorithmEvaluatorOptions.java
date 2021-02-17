package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.constant;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

@UsageTextField(title="Constant Target Algorithm Evaluator Options", description="Parameters for the Constant Target Algorithm Evaluator", level=OptionLevel.DEVELOPER)
public class ConstantTargetAlgorithmEvaluatorOptions extends AbstractOptions{

	
	@Parameter(names="--constant-run-result", description="Run Result To return")
	public RunStatus runResult = RunStatus.SAT;
	
	@Parameter(names="--constant-runtime", description="Runtime to return")
	public double runtime = 1.00;
	
	@Parameter(names="--constant-run-quality", description="Quality to return")
	public double quality = 0;
	
	@Parameter(names="--constant-run-length", description="Runlength to return")
	public double runlength = 0;
	
	@Parameter(names="--constant-additional-run-data", description="Additional Run Data to return")
	public String additionalRunData = "";
	


}

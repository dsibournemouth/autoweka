package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli;

import org.mangosdk.spi.ProviderFor;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
@ProviderFor(TargetAlgorithmEvaluatorFactory.class)
public class CommandLineTargetAlgorithmEvaluatorFactory extends AbstractTargetAlgorithmEvaluatorFactory  {

	
	public static String NAME = "CLI";
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator( AbstractOptions options) {

		CommandLineTargetAlgorithmEvaluatorOptions cliOpts = (CommandLineTargetAlgorithmEvaluatorOptions) options;		
		TargetAlgorithmEvaluator tae =  new CommandLineTargetAlgorithmEvaluator( cliOpts );
		
		return tae;
	}

	@Override
	public CommandLineTargetAlgorithmEvaluatorOptions getOptionObject()
	{
		return new CommandLineTargetAlgorithmEvaluatorOptions();
	}

	public static CommandLineTargetAlgorithmEvaluatorOptions getCLIOPT()
	{
		return new CommandLineTargetAlgorithmEvaluatorOptions();
	}

	public static TargetAlgorithmEvaluator getCLITAE()

	{
		
		CommandLineTargetAlgorithmEvaluatorOptions opts = new CommandLineTargetAlgorithmEvaluatorOptions();
		opts.logAllCallStrings = true;
		opts.logAllProcessOutput = true;
		
		return new CommandLineTargetAlgorithmEvaluator( opts );
	}

	public static TargetAlgorithmEvaluator getCLITAE(CommandLineTargetAlgorithmEvaluatorOptions options)
	{
		return new CommandLineTargetAlgorithmEvaluator(options);
	}
	
	public static TargetAlgorithmEvaluator getCLITAE(int observerFrequency)
	{
		CommandLineTargetAlgorithmEvaluatorOptions options = new CommandLineTargetAlgorithmEvaluatorOptions();
		options.observerFrequency = observerFrequency;
		options.logAllProcessOutput = true;
		return new CommandLineTargetAlgorithmEvaluator(options);
	}
	
}

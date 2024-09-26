package ca.ubc.cs.beta.aeatk.example.tae;

import ca.ubc.cs.beta.aeatk.misc.options.NoArgumentHandler;

public class TargetAlgorithmEvaluatorRunnerNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("algo-test is a utility that allows testing of wrappers, it will execute exactly one run. ").append("\n\n");

		sb.append("  Basic Usage:\n");
		sb.append("  algo-test --scenarioFile <file> \n\n");
	
		sb.append("  Specifying the configuration to run\n");
		sb.append("  algo-test --scenarioFile <file> --config \"-name 'value' -name 'value'...\"\n\n");

		sb.append("  Specifying the instance \n");
		sb.append("  algo-test --scenarioFile <file> --instance <instance> \n\n");

		sb.append("  Without a scenario file: \n");
		sb.append("  algo-test --algo-exec <executable> --algo-cutoff-time <timelimit> --algo-exec-dir <dir> --param-file <filename> --instance <instance>\n\n");
		
		sb.append("  Full version information is available with :\n");
		sb.append("  algo-test -v\n\n");
		
		sb.append("  A full command line reference is available with:\n");
		sb.append("  algo-test --help\n\n");
			  
	
		System.out.println(sb);
		return true;
	}

}

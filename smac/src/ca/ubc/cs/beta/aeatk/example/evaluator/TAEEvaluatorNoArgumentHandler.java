package ca.ubc.cs.beta.aeatk.example.evaluator;

import ca.ubc.cs.beta.aeatk.misc.options.NoArgumentHandler;

public class TAEEvaluatorNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("TAE-evaluator is a utility that allows the execution of an algorithm (defined as an algorithm execution config) on a set of instances with a TAE.  ").append("\n\n");

		sb.append("Basic Usage:\n\n");
		
		sb.append("--algorithm-name <name> --algo-cutoff-time <absolute maximum cutoff time> --algo-exec <algorithm execution callstring> --algo-exec-dir <directory in which to execute algorithm> --param-file <algorithm parameter space file> --config <config to use, ex. DEFAULT> --instances <file containing list of instances> --cutoff <cutoff time> --seed <seed>\n\n");

		System.out.println(sb);
		return true;
	}

}

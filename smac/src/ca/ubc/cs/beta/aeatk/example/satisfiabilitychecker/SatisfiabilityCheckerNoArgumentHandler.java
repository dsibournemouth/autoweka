package ca.ubc.cs.beta.aeatk.example.satisfiabilitychecker;

import ca.ubc.cs.beta.aeatk.misc.options.NoArgumentHandler;

public class SatisfiabilityCheckerNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("sat-check is a utility that determines SAT / UNSAT for a list of instances and writes it to file (it is very similar to algo-test) ").append("\n\n");

		sb.append("  Basic Usage:\n");
		sb.append("  sat-check --scenarioFile <file> --output-file <file>\n\n");
	
		sb.append("  Specifying the configuration to run\n");
		sb.append("  sat-check --scenarioFile <file> --config \"-name 'value' -name 'value'...\" --output-file <file>\n\n");

		sb.append("  Overwrite output files:\n");
		sb.append("  sat-check --scenarioFile <file> --output-file <file> --overwrite-output-file true\n\n");
	
		
	
		sb.append("  Without a scenario file: \n");
		sb.append("  sat-check --algo-exec <executable> --algo-cutoff-time <timelimit> --algo-exec-dir <dir> --param-file <filename> --instanceFile <file>  --output-file <file>\n\n");
		
		sb.append("  Full version information is available with :\n");
		sb.append("  sat-check -v\n\n");
		
		sb.append("  A full command line reference is available with:\n");
		sb.append("  sat-check --help\n\n");
			  
	
		System.out.println(sb);
		return true;
	}

}

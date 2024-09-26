package ca.ubc.cs.beta.aeatk.example.pcscheck;

import ca.ubc.cs.beta.aeatk.misc.options.NoArgumentHandler;

public class PCSCheckNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("pcs-check is a utility that allows testing of PCS file, it will execute exactly one run. ").append("\n\n");

		sb.append("  Checking PCS File:\n");
		sb.append("  pcs-check --pcs-file <file> \n\n");

		sb.append("  Checking PCS File:\n");
		sb.append("  pcs-check --scenarioFile <file> \n\n");
		
		sb.append("  Specifying the configuration to run\n");
		sb.append("  pcs-check --scenarioFile <file> --config \"-name 'value' -name 'value'...\"\n\n");

		sb.append("  Overriding values in a configuration \n");
		sb.append("  pcs-check --scenarioFile <file> -Pname=value -Pname=value\n\n");
		
		sb.append("  Full version information is available with :\n");
		sb.append("  pcs-check -v\n\n");
		
		sb.append("  A full command line reference is available with:\n");
		sb.append("  pcs-check --help\n\n");

		
	
		System.out.println(sb);
		return true;
	}

}

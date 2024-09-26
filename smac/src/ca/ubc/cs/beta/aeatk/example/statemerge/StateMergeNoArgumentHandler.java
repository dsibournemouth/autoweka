package ca.ubc.cs.beta.aeatk.example.statemerge;

import ca.ubc.cs.beta.aeatk.misc.options.NoArgumentHandler;

public class StateMergeNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("state-merge is a utility that allows mergeing of state files.  ").append("\n\n");

		sb.append("  Basic Usage:\n\n");
		
		sb.append("  --directories <dirToScan> --scenario-file <scenfile> --outdir <outdir> [ --restore-args \"\" ]\n\n");
		
		sb.append("  Details:\n");
		sb.append("\t<dirToScan> will be searched for run and result files if none are found it will recursively look in subdirectories until some are found\n");
		sb.append("\t<scenfile> scenario file to use\n");
		sb.append("\t<outdir> output directory to write the files to\n");
		sb.append("\tIf you need to specify additional scenario options for the state folders use the --restore-args argument and place them in a string");
		sb.append("\n\n  More help is available with:\n");
		sb.append("\tstate-merge --help\n\n");
		sb.append("  Advanced Usage:\n");
		
		sb.append("\tBy default the utility will merge runs in a way that preserves the invariant that there exists a parameter configuration that has run on every problem instance seed pair\n");
		sb.append("\tIf you would like to just merge runs without preserving this add the following:\n\n");
		sb.append("  --repair-smac-invariant false\n");
		sb.append("\n[NOTE]: There is no way to merge runs for the same configuration and problem instance seed pair, duplicates will be dropped\n");
		
			  
	
		System.out.println(sb);
		return true;
	}

}

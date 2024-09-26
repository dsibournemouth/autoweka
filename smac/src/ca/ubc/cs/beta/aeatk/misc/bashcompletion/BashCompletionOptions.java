package ca.ubc.cs.beta.aeatk.misc.bashcompletion;

import com.beust.jcommander.Parameter;

public class BashCompletionOptions {

	@Parameter(names="--class", required = true, description="Class to generate auto configuration for")
	public String clazz;
	
	@Parameter(names="--commandName", required = true, description="Command that will have the auto completion generated for")
	public String commandName;
	
	@Parameter(names="--outputFile", required = true, description="Output File to append the bash completion to")
	public String outputFile;
	
	@Parameter(names="--debug")
	public boolean debug = false;
	
}

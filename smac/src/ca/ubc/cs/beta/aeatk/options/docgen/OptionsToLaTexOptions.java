package ca.ubc.cs.beta.aeatk.options.docgen;

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;

import com.beust.jcommander.Parameter;

public class OptionsToLaTexOptions {
	
	@Parameter(names="--class", required = true, description="Class to generate auto configuration for")
	public String clazz;
	
	@Parameter(names="--file-to-write", required = true, description="Output File to append the bash completion to")
	public String outputFile;

	@Parameter(names="--show-tae-options", description="If true show the TAE options as well")
	public boolean tae = true;
	
	@Parameter(names="--level", description="Help level to write options for")
	public OptionLevel level = OptionLevel.DEVELOPER;

	@Parameter(names="--aliases", description="Whether to show the Aliases or not")
	public boolean aliases = true;
}

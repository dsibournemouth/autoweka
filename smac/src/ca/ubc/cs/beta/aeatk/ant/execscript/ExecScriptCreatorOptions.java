package ca.ubc.cs.beta.aeatk.ant.execscript;

import java.io.File;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

public class ExecScriptCreatorOptions extends AbstractOptions {

	@Parameter(names="--class", description="Name of java class", required=true)
	public String clazz;
	
	@Parameter(names="--skip-class-check", description="Don't actually check if the class exists")
	public boolean skipClassCheck;
	
	@Parameter(names="--name", description="Name of program", required=true)
	public String nameOfProgram;

	@Parameter(names="--file-to-write", description="File to output script to (if directory will use name of program as a name)")
	public String filename = (new File("")).getAbsolutePath();
	
	@Parameter(names="--bat-file", description="Also output a windows .bat file")
	public boolean batFile = true;
	
	@Parameter(names="--default-mem", description="Default amount of RAM to reserve in MB", validateWith=FixedPositiveInteger.class)
	public int mem = 128;
	
	@Parameter(names="--print-mem", description="Print the amount of RAM to reserve")
	public boolean printMem = true;
}

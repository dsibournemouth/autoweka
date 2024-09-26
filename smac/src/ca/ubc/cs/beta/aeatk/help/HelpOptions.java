package ca.ubc.cs.beta.aeatk.help;

import java.io.File;

import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
@UsageTextField(hiddenSection=true)
/***
 * Options that present help to the user
 * <br/>
 * <b>Implementation Note:</b> Nothing every actually will check these values. This
 * help options objects really only sets the parameter names. To get this behaviour to fully work
 * you should pass the arguments through {@link ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper#checkForHelpAndVersion}
 * <br/>
 * You might ask why we have these options then, it is so that they are displayed.
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class HelpOptions extends AbstractOptions{
	
	/**
	 * Note most of these actually will never be read as we will silently scan for them in the input arguments to avoid logging
	 */
	@UsageTextField(defaultValues="", domain="", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--show-hidden","--showHiddenParameters"}, description="show hidden parameters that no one has use for, and probably just break SMAC (no-arguments)")
	public boolean showHiddenParameters = false;
	
	@UsageTextField(defaultValues="", domain="" , level=OptionLevel.BASIC)
	@Parameter(names={"--help","-?","/?","-h"}, description="show help")
	public boolean showHelp = false;

	@Parameter(names={"--help-level"}, description="Show options at this level or lower")
	public OptionLevel helpLevel = OptionLevel.BASIC;
	
	@UsageTextField(defaultValues="", domain="", level=OptionLevel.BASIC)
	@Parameter(names={"-v","--version"}, description="print version and exit")
	public boolean showVersion = false;

	@UsageTextField(defaultValues="~/.aeatk/help.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--help-default-file","--helpDefaultsFile"}, description="file that contains default settings for SMAC")
	@ParameterFile(ignoreFileNotExists = true) 
	public File helpDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "help.opt");
	
}

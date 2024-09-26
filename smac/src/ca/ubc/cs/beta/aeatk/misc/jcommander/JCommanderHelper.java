package ca.ubc.cs.beta.aeatk.misc.jcommander;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.logging.CommonMarkers;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageSection;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.aeatk.misc.spi.SPIClassLoaderHelper;
import ca.ubc.cs.beta.aeatk.misc.version.VersionTracker;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.docgen.OptionsToUsage;
import ca.ubc.cs.beta.aeatk.options.docgen.UsageSectionGenerator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public final class JCommanderHelper
{

	public static JCommander getJCommander(AbstractOptions opts, Map<String, AbstractOptions> taeOpts)
	{

		ArrayList<Object> allOptions = new ArrayList<Object>();
		
		allOptions.add(opts);
		for(Entry<String, AbstractOptions> ent : taeOpts.entrySet())
		{
			if(ent.getValue() != null)
			{
				allOptions.add(ent.getValue());
			}
		}
		JCommander com = new JCommander(allOptions.toArray(), true, true);
		return com;
		
	}

	
	public static JCommander parseCheckingForHelpAndVersion(String[] args,AbstractOptions options )
	{
		return parseCheckingForHelpAndVersion(args, options,Collections.<String, AbstractOptions> emptyMap());
	}
	
	
	public static JCommander parseCheckingForHelpAndVersion(String[] args,
			AbstractOptions options, Map<String, AbstractOptions> emptyMap) {
		JCommander jcom = getJCommanderAndCheckForHelp(args, options, emptyMap);
		jcom.parse(args);
		return jcom;
	}


	public static void checkForHelpAndVersion(String[] args, AbstractOptions options, Map<String, AbstractOptions> taeOpts)
	{
		
		
		//=== The arguments that we are searching for come from this class here.
		@SuppressWarnings("unused")
		HelpOptions helpOption = null;
		//=== I do this just in case the class is moved, so that javadoc isn't left out of date.
		
		
		OptionLevel levelToDisplay = OptionLevel.BASIC;
		for(int i=0; i < args.length; i++)
		{
			if(args[i].trim().equals("--help-level"))
			{
				if(i == args.length - 1)
				{
					throw new ParameterException("--help-level argument requires an argument");
				}
				
				try {
					levelToDisplay = OptionLevel.valueOf(args[i+1].toUpperCase().trim());
				} catch(IllegalArgumentException e)
				{
					throw new ParameterException("--help-level has illegal value, must be one of: " + Arrays.toString(OptionLevel.values()));
				}
			}
		}
		
		try {
			Set<String> possibleValues = new HashSet<String>(Arrays.asList(args));
			
			String[] hiddenNames = {"--show-hidden","--showHiddenParameters"};
			for(String helpName : hiddenNames)
			{
				if(possibleValues.contains(helpName))
				{
					OptionsToUsage.usage(UsageSectionGenerator.getUsageSections(options, taeOpts), true, levelToDisplay);
					System.exit(AEATKReturnValues.SUCCESS);
				}
			}
			
			String[] helpNames =  {"--help","-?","/?","-h","--help-level"};
			for(String helpName : helpNames)
			{
				
				
				if(possibleValues.contains(helpName))
				{
					List<UsageSection> secs = UsageSectionGenerator.getUsageSections(options, taeOpts);
					boolean quit= false;
					for(UsageSection sec  : secs)
					{
						quit |= sec.getHandler().handleNoArguments();
						
					}
					
					OptionsToUsage.usage(UsageSectionGenerator.getUsageSections(options, taeOpts), false, levelToDisplay);
					System.exit(AEATKReturnValues.SUCCESS);
				}
			}
			
			String[] versionNames = {"-v","--version"};
			for(String helpName : versionNames)
			{
				if(possibleValues.contains(helpName))
				{
					//Turn off logging
					System.setProperty("logback.configurationFile", "logback-off.xml");
					VersionTracker.setClassLoader(SPIClassLoaderHelper.getClassLoader());
					System.out.println("**** Version Information ****");
					System.out.println(VersionTracker.getVersionInformation());
					
					
					System.exit(AEATKReturnValues.SUCCESS);
				}
			}
			
			
			
			
		} catch (Exception e) {
			
			throw new IllegalStateException(e);
		}
		
		
		
	}

	/**
	 * Returns a JCommander object after screening for parameters that are asking for help or version information 
	 *  
	 * 
	 * @param args
	 * @param mainOptions
	 * @param taeOptions
	 * 
	 * @return
	 */
	public static JCommander getJCommanderAndCheckForHelp(String[] args,AbstractOptions mainOptions) {
		return getJCommanderAndCheckForHelp(args, mainOptions, Collections.<String, AbstractOptions> emptyMap());
	}
	
	
	/**
	 * Returns a JCommander object after screening for parameters that are asking for help or version information 
	 *  
	 * 
	 * @param args
	 * @param mainOptions
	 * @param taeOptions
	 * 
	 * @return
	 */
	public static JCommander getJCommanderAndCheckForHelp(String[] args,AbstractOptions mainOptions,Map<String, AbstractOptions> taeOptions) {
		JCommander jcom = getJCommander(mainOptions, taeOptions);
		if(args.length == 0)
		{
			List<UsageSection> secs = UsageSectionGenerator.getUsageSections(mainOptions, taeOptions);
			boolean quit= false;
			for(UsageSection sec  : secs)
			{
				quit |= sec.getHandler().handleNoArguments();
				
			}
			
			if(quit)
			{
				System.exit(AEATKReturnValues.PARAMETER_EXCEPTION);
			}
		}
		checkForHelpAndVersion(args, mainOptions, taeOptions);
		return jcom;
		
		
	}
	
	public static void logCallString(String[] args, Class<?> c) {
		Logger log = LoggerFactory.getLogger(JCommanderHelper.class);
		StringBuilder sb = new StringBuilder("java -cp ");
		sb.append(System.getProperty("java.class.path")).append(" ");
		sb.append(c.getCanonicalName()).append(" ");
		for(String arg : args)
		{
			boolean escape = false;
			if(arg.contains(" "))
			{
				escape = true;
				arg = arg.replaceAll(" ", "\\ ");
			}
			
			
			if(escape) sb.append("\"");
			sb.append(arg);
			if(escape) 	sb.append("\"");
			sb.append(" ");
		}
		
		log.info("Call String:");
		log.info("{}", sb.toString());
	}


	public static void logCallString(String[] args, String name) {
		Logger log = LoggerFactory.getLogger(JCommanderHelper.class);
		
		StringBuilder sb = new StringBuilder(name);

		if(System.getProperty("os.name").toLowerCase().contains("win"))
		{
			sb.append(".bat");
		}
		sb.append(" ");
		
		for(String arg : args)
		{
			boolean escape = false;
			if(arg.contains(" "))
			{
				escape = true;
				arg = arg.replaceAll(" ", "\\ ");
			}
			
			
			if(escape) sb.append("\"");
			sb.append(arg);
			if(escape) 	sb.append("\"");
			sb.append(" ");
		}
		
		log.info("Call String: {}", sb.toString());
		
	}

	
	public static JCommander getJCommander(AbstractOptions t) {
		return getJCommander(t, Collections.<String, AbstractOptions> emptyMap());
	}


	public static void logConfiguration(JCommander jcom)
	{
		Logger log = LoggerFactory.getLogger(JCommanderHelper.class);
		StringBuilder sb = new StringBuilder();
		for(Object o : jcom.getObjects())
		{
			sb.append(o.toString()).append("\n");
		}
			
		log.debug("==========Configuration Options==========\n{}", sb.toString());
		
	}
	
	public static void logConfigurationInfoToFile(JCommander jcom)
	{
		Logger log = LoggerFactory.getLogger(JCommanderHelper.class);
		StringBuilder sb = new StringBuilder();
		for(Object o : jcom.getObjects())
		{
			sb.append(o.toString()).append("\n");
		}
			
		log.info(CommonMarkers.SKIP_CONSOLE_PRINTING,"==========Configuration Options==========\n{}", sb.toString());
	}
	
	
	
	
	
}
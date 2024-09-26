package ca.ubc.cs.beta.aeatk.logging;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;
/**
 * Parameter delegate that initializes the logback logger to log to file and console
 * <br>
 * <b>Usage:</b> Prior to any Loggers being created call the init() method, it will set the appropriate System properties
 * that can be read in an appropriate logback.xml file that is on the classpath (generally in conf/)
 * <p>
 * 
 * This differs from SimpleLogFileLoggingOptions in that you specify a full filename, it does not implement logging options.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@UsageTextField(hiddenSection=true)
public class ExplicitLogFileLoggingOptions  extends AbstractOptions {

	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--console-log-level","--consoleLogLevel"},description="default log level of console output (this cannot be more verbose than the logLevel)")
	public LogLevel consoleLogLevel = LogLevel.INFO;
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--log-level","--logLevel"},description="messages will only be logged if they are of this severity or higher.")
	public LogLevel logLevel = LogLevel.INFO;	
	
	//This isn't meant to be an option, you can simply change this value before calling initializeLogging
	public boolean suppressLogLevelConsistencyWarning = false;
	
	
	
	
	public ExplicitLogFileLoggingOptions()
	{
		
	}
	
	public void initializeLogging(String completeOutputDir, String filename)
	{
	
		if(completeOutputDir == null)
		{
			completeOutputDir = (new File("")).getAbsolutePath();
		} 
			
		System.setProperty("LOGLEVEL", logLevel.name());
		
		
	
		System.setProperty("OUTPUTDIR",completeOutputDir);
		System.setProperty("STDOUT-LEVEL", consoleLogLevel.name());
		System.setProperty("ROOT-LEVEL",logLevel.name());
		
		
		
		String logLocation = completeOutputDir +File.separator + filename;
		
		System.setProperty("RUNLOG", logLocation);
	
		
		//System.out.println("*****************************\nLogging to: " + logLocation +  "\n*****************************");
	
		
		
		if(System.getProperty(ConsoleOnlyLoggingOptions.LOGBACK_CONFIGURATION_FILE_PROPERTY)!= null)
		{
			Logger log = LoggerFactory.getLogger(getClass());
			log.debug("System property for logback.configurationFile has been found already set as {} , logging will follow this file", System.getProperty(ConsoleOnlyLoggingOptions.LOGBACK_CONFIGURATION_FILE_PROPERTY));
		} else
		{
			
			String newXML = this.getClass().getPackage().getName().replace(".","/") + "/"+  "singlefile-logback.xml";
			
			
			
			System.setProperty(ConsoleOnlyLoggingOptions.LOGBACK_CONFIGURATION_FILE_PROPERTY, newXML);
			
			Logger log = LoggerFactory.getLogger(getClass());
			if(log.isTraceEnabled())
			{
				log.trace("Logging initialized to use file:" + newXML);
			} else
			{
				log.debug("Logging initialized");
			}
			
		}
		//Generally has the format: ${OUTPUTDIR}/${RUNGROUPDIR}/log-run${NUMRUN}.txt
		Logger log = LoggerFactory.getLogger(ExplicitLogFileLoggingOptions.class);
		log.info("Logging to: {}",logLocation);
		
		
		if(!suppressLogLevelConsistencyWarning && logLevel.lessVerbose(consoleLogLevel))
		{
			log.warn("The console has been set to be more verbose than the log. This is generally an error, except if you have modified the logback.xml to have certain loggers be more specific");
			
		}
			
		
	}
}

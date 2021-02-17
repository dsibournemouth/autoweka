package ca.ubc.cs.beta.aeatk.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

/**
 * Parameter delegate that initializes the logback logger to log only to console
 * <br>
 * <b>Usage:</b> Prior to any Loggers being created call the init() method, it will set the appropriate System properties
 * that can be read in an appropriate logback.xml file that is on the classpath (generally in conf/)
 * <p>
 * Unlike the {@link ComplexLoggingOptions}, this one only logs to console and doesn't read a logback.xml file 
 * (it reads an internal one actually).
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */

@UsageTextField(hiddenSection=true)
public class ConsoleOnlyLoggingOptions extends AbstractOptions implements LoggingOptions{

	static final String LOGBACK_CONFIGURATION_FILE_PROPERTY ="logback.configurationFile"; 
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--log-level","--logLevel","--console-log-level","--consoleLogLevel"},description="messages will only be logged if they are of this severity or higher.")
	public LogLevel logLevel = LogLevel.INFO;	
	
	@Override
	public void initializeLogging(String completeOutputDir, int numRun)
	{
		this.initializeLogging();
	}
	
	public void initializeLogging()
	{
		System.setProperty("LOGLEVEL", logLevel.name());
		
		if(System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY)!= null)
		{
			Logger log = LoggerFactory.getLogger(getClass());
			
			log.trace("System property for logback.configurationFile has been found already set as {} , logging will follow this file", System.getProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY));
		} else
		{
			
			
			String newXML = this.getClass().getPackage().getName().replace(".", "/") + "/"+  "consoleonly-logback.xml";
			
			
			System.setProperty(LOGBACK_CONFIGURATION_FILE_PROPERTY, newXML);
			
			Logger log = LoggerFactory.getLogger(getClass());
			if(log.isTraceEnabled())
			{
				log.trace("Logging initialized to use file:" + newXML);
			} else
			{
				log.debug("Logging initialized");
			}
			
		}
	}

}

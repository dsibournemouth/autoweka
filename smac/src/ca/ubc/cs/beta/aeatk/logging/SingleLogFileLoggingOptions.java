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
 * There is an example logback.xml file at the end of this file. The expectation is that files will be written as
 * log-runN.txt 
 * 
 * <b>Tip:</b> You can set debug="true" at the top of your logback.xml file, and then step through your code
 * when the debug messages print for the first time you have created your first logger. <br/>
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@UsageTextField(hiddenSection=true)
public class SingleLogFileLoggingOptions  extends AbstractOptions implements LoggingOptions{

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
	
	
	private final String prefix; 
	public SingleLogFileLoggingOptions()
	{
		prefix = "";
	}
	
	public SingleLogFileLoggingOptions(String prefix)
	{
		if(prefix.matches("\\s"))
		{
			throw new IllegalArgumentException("Prefix cannot contain whitespace");
		}
				
		this.prefix = prefix + "-";
	}
	
	public void initializeLogging(String completeOutputDir, int numRun)
	{
	
		if(completeOutputDir == null)
		{
			completeOutputDir = (new File("")).getAbsolutePath();
		} 
			
		System.setProperty("LOGLEVEL", logLevel.name());
		
		
	
		System.setProperty("OUTPUTDIR",completeOutputDir);
		System.setProperty("NUMRUN", String.valueOf(numRun));
		System.setProperty("STDOUT-LEVEL", consoleLogLevel.name());
		System.setProperty("ROOT-LEVEL",logLevel.name());
		
		
		
		String logLocation = getLogLocation(completeOutputDir, numRun);
		
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
		Logger log = LoggerFactory.getLogger(SingleLogFileLoggingOptions.class);
		log.info("Logging to: {}",logLocation);
		
		
		if(!suppressLogLevelConsistencyWarning && logLevel.lessVerbose(consoleLogLevel))
		{
			log.warn("The console has been set to be more verbose than the log. This is generally an error, except if you have modified the logback.xml to have certain loggers be more specific");
			
		}
			
		
	}
	
	public String getLogLocation(String completeOutputDir, int numRun)
	{

		if(completeOutputDir == null)
		{
			completeOutputDir = (new File("")).getAbsolutePath();
		} 
		return completeOutputDir + File.separator+  "log-"+prefix + numRun+ ".txt";
	}

	@Override
	public void initializeLogging() {
		this.initializeLogging(new File(".").getAbsolutePath(), 0);
		
	}

}
/*
<?xml version="1.0" encoding="UTF-8" ?>
<configuration debug="false" >
  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
  <file>${RUNLOG}</file>
  <append>false</append>
  <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
  </encoder>
</appender>
  
  
  <appender name="FILE-WARN" class="ch.qos.logback.core.FileAppender">
  <file>${WARNLOG}</file>
  <append>false</append>
  <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>WARN</level>
  </filter>
  <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  
  
  <appender name="FILE-ERR" class="ch.qos.logback.core.FileAppender">
  <file>${ERRLOG}</file>
  <append>false</append>
  <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>ERROR</level>
  </filter>
  <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>
  

  
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>${STDOUT-LEVEL}</level>
    </filter>
    <encoder>
      <pattern>[%-5level] %msg%n</pattern>
    </encoder>
    
  </appender>
  
  <root level="${ROOT-LEVEL}">
    <appender-ref ref="STDOUT" />
    <appender-ref ref="FILE" />
    <appender-ref ref="FILE-WARN"/>
    <appender-ref ref="FILE-ERR"/>
  </root>
    
</configuration>
*/

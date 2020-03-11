package ca.ubc.cs.beta.aeatk.logging;

/**
 * Utility Enum that controls the log level
 * @author sjr
 *
 *<b>NOTE:</b> The order of these MUST be from more to less verbose
 *as some validation of the configuration options depend on the use of ordinal()
 */
public enum LogLevel {
	TRACE,
	DEBUG,
	INFO,
	WARN,
	ERROR,
	OFF;
	
	
	public boolean lessVerbose(LogLevel logLevel)
	{
		return ordinal() > logLevel.ordinal();
	}
}

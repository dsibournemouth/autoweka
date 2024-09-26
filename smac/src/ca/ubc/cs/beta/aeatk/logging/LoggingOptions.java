package ca.ubc.cs.beta.aeatk.logging;

/**
 * Interface that provides methods for initializing logging.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface LoggingOptions {

	/**
	 * Initialize the logger
	 * @param completeOutputDir output directory it should write files to
	 * @param numRun the number of this run also used in the filename
	 */
	public void initializeLogging(String completeOutputDir, int numRun);
	
	/**
	 * Initialize the logger
	 * 
	 * If output directory and numRun are required, they will be set to the current directory and zero respectively.
	 */
	public void initializeLogging();
	
}

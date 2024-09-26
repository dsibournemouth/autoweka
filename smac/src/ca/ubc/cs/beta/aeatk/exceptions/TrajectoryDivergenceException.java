package ca.ubc.cs.beta.aeatk.exceptions;

/**
 * Exception thrown when our trajectory has diverged from a preset one.
 * @author sjr
 *
 */
public class TrajectoryDivergenceException extends SMACException {

	private static final long serialVersionUID = -104669424346723440L;
	
	/**
	 * Constructor for when hash codes don't match
	 * @param expectedHashCode hashCode We Expected
	 * @param computedHashCode hashCode We Computed
	 * @param runNumber	Run Number
	 */
	public TrajectoryDivergenceException(int expectedHashCode, int computedHashCode, int runNumber)
	{
		super("Expected Hash Code:" + expectedHashCode + " Computed Hash Code:" + computedHashCode + " Run Number: " + runNumber);
	}

	/**
	 * Constructs a more generic option with a string
	 * @param string message to report
	 */
	public TrajectoryDivergenceException(String string) {
		super(string);
	}
	
}

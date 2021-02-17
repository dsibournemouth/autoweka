package ca.ubc.cs.beta.aeatk.exceptions;

public class IllegalWrapperOutputException extends IllegalArgumentException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8887570449032849445L;

	/**
	 * Default constructor
	 * @param error 		 error with the result line
	 * @param resultLine	 result line text
	 */
	public IllegalWrapperOutputException(String error, String resultLine)
	{
		super("Illegal Wrapper Output Detected: " + error + " on result line: " + resultLine + " please consult the manual for more information");
	}
}

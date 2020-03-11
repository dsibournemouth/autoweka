package ca.ubc.cs.beta.aeatk.exceptions;

/**
 * Exception that is thrown when a DeveloperMadeABooBooException,
 * has been thrown and we are doing something that causes yet ANOTHER
 * impossible exception to be thrown (probably should be disjoint) from
 * the first.  
 * 
 * 
 * @author Steve Ramage 
 */
public class OhTheHumanityException extends DeveloperMadeABooBooException {


	/**
	 * 
	 */
	private static final long serialVersionUID = -5902224652374475503L;

	public OhTheHumanityException(DeveloperMadeABooBooException e)
	{
		super(e);
	}
	
	public String getMessage()
	{
		
		String message = "An impossible exception occured and then we tried to do something super critical, ANOTHER impossible exception occured\n" +
		"To make amends, if you see this exception in a \"master\" release of code, where neither this, nor the inner exception was caused by some external plugin not maintained by the authors\n" + 
		"You can have your name immortalized here: (Only first person per issue, please send a stack trace, and log, etc...)\n" + 
		"-----=====[ Victims ]=====-----\n" +
		"You must be the first\n\n" + super.getMessage();
				
		return message;
	}
}

package ca.ubc.cs.beta.aeatk.exceptions;

/**
 * RuntimeException that we can use to wrap checked exceptions
 * that we THINK can't POSSIBLY ever be thrown.
 * 
 * @author Steve Ramage 
 *
 */
public class DeveloperMadeABooBooException extends SMACException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3936364520582947589L;

	public DeveloperMadeABooBooException(Exception e)
	{
		super("The developers of SMAC thought this exception would never EVER happen, honest and aren't our faces red now", e);
	}

	public DeveloperMadeABooBooException(String string) {
		super(string);
	}

}

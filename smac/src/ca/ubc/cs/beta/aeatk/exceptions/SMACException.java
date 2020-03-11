package ca.ubc.cs.beta.aeatk.exceptions;

/**
 * Base Exception Class
 * @author sjr
 *
 */
public class SMACException extends RuntimeException {

	
	private static final long serialVersionUID = 289088929376423563L;
	
	//We don't allow blind SMACException objects to be created directly
	protected SMACException()
	{
		super();
	}
	
	
	public SMACException(String message)
	{
		super(message);
	}


	public SMACException(Exception e) {
		super(e);
	}


	public SMACException(String s, Exception e) {
		super(s,e);
	}
}

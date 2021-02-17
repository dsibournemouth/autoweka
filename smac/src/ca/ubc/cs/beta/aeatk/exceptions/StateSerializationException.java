package ca.ubc.cs.beta.aeatk.exceptions;

/**
 * Exception thrown when serialization encounters a problem
 * @author sjr
 *
 */
public class StateSerializationException extends SMACException {

	
	private static final long serialVersionUID = 4394135089834489593L;

	public StateSerializationException(Exception e)
	{
		super(e);
	}
	
	public StateSerializationException(String s,Exception e)
	{
		super(s,e);
	}

	public StateSerializationException(String s) {
		super(s);
	}
	
	
}

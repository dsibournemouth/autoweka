package ca.ubc.cs.beta.smac.exceptions;

public class SMACRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6012822616367601464L;
	
	public SMACRuntimeException(String message)
	{
		super(message);
	}
	public SMACRuntimeException(Throwable t) {
		super(t);
	}

}

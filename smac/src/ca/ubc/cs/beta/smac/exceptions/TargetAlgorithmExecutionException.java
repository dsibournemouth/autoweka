package ca.ubc.cs.beta.smac.exceptions;

public class TargetAlgorithmExecutionException extends SMACRuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8701769256842561265L;

	public TargetAlgorithmExecutionException(Throwable t)
	{
		super(t);
	}

	public TargetAlgorithmExecutionException(String message) {
		super(message);
		
	}

}

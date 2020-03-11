package ca.ubc.cs.beta.aeatk.exceptions;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

public class DuplicateRunException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6560952886591079120L;
	
	private final AlgorithmRunResult run;
	public DuplicateRunException(String message, AlgorithmRunResult r)
	{
		super(message +"\n"+ r.getAlgorithmRunConfiguration().toString());
		this.run = r;
		
	}

	public AlgorithmRunResult getRun()
	{
		return run;
	}
}

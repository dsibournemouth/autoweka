package ca.ubc.cs.beta.aeatk.exceptions;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;


public class OutOfTimeException extends SMACException {

	
	private final AlgorithmRunResult run;
	/**
	 * 
	 */
	private static final long serialVersionUID = 3562273461188581045L;

	public OutOfTimeException(AlgorithmRunResult run) {
		super("SMAC is out of time.");
		this.run = run;
	}

	public OutOfTimeException() {
		super("Out of time");
		this.run = null;
		// TODO Auto-generated constructor stub
	}

	public AlgorithmRunResult getAlgorithmRun()
	{
		return run;
	}
}

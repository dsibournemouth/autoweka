package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.prepostcommand;

import java.io.IOException;

public class PrePostCommandErrorException extends IllegalStateException {

	public PrePostCommandErrorException(String string, IOException e) {
		super(string, e);
	}

	public PrePostCommandErrorException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}

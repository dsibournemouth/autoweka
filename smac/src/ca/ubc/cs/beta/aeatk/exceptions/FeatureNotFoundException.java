package ca.ubc.cs.beta.aeatk.exceptions;

import com.beust.jcommander.ParameterException;

public class FeatureNotFoundException extends ParameterException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -411240689292928297L;

	public FeatureNotFoundException(String string) {
		super(string);
	}

}

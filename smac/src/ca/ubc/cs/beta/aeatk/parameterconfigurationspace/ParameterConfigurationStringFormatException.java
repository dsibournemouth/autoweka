package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

public class ParameterConfigurationStringFormatException extends RuntimeException {

	public ParameterConfigurationStringFormatException(String string,
			RuntimeException e) {
		super(string, e);
	}

	public ParameterConfigurationStringFormatException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8666575841906423444L;

}

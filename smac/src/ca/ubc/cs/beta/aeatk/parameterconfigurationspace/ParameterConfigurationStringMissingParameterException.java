package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

public class ParameterConfigurationStringMissingParameterException extends ParameterConfigurationStringFormatException {

	public ParameterConfigurationStringMissingParameterException(String string,
			RuntimeException e) {
		super(string, e);
	}

	public ParameterConfigurationStringMissingParameterException(String string) {
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8666575841906423444L;

}

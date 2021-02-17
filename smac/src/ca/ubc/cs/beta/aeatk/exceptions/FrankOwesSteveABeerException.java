package ca.ubc.cs.beta.aeatk.exceptions;

public class FrankOwesSteveABeerException extends DeveloperMadeABooBooException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3478231398088023043L;

	public FrankOwesSteveABeerException(Exception e) {
		super(e);
	}

	public FrankOwesSteveABeerException(String s) {
		super(s);
	}

	public String getMessage()
	{
		return "Please e-mail the developers as one of them said if this exception was ever seen by anyone, they would by the other one a beer " + super.getMessage();
	}
	
	
}

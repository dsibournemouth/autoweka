package ca.ubc.cs.beta.aeatk.misc.options;

public class NoopNoArgumentHandler implements NoArgumentHandler {

	@Override
	public boolean handleNoArguments() {
		return false;
	}

}

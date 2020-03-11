package ca.ubc.cs.beta.aeatk.misc.options;

/**
 * Interface that lets you specify a handler that is called if the program is called with no arguments
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface NoArgumentHandler {

	/**
	 * Return true if we should quit the program after all handlers called
	 * The default logic is if any of these say yes, then we will quit
	 * @return  <code>true</code> if the program should terminate, <code>false</code> otherwise.
	 */
	public boolean handleNoArguments();
	
}

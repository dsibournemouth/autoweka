package ca.ubc.cs.beta.aeatk.misc.options;

/**
 * Interface that allows JCommander validators and converters to give us a human readable form of what arguments they allow.
 *  
 * @author Steve Ramage 
 */
public interface DomainDisplay {

	/**
	 * Human readable form of the valid arguments
	 * @return human readable form of the valid arguments
	 */
	public String getDomain();
	
}

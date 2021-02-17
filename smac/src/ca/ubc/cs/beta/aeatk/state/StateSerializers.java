package ca.ubc.cs.beta.aeatk.state;

/**
 * Enumeration that lists the various StateSerializers supported
 * <br>
 * <b>Note:</b> If you add more of these, you should also modify the <code>StateFactoryHelper</code> 
 * class. 
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 * @see StateFactoryHelper 
 */
public enum StateSerializers {
	/**
	 * Null State Serializer basically returns empty objects and does nothing when saving
	 */
	NULL,
	
	/**
	 * State Serializers used for MATLAB format to disk 
	 */
	LEGACY
}

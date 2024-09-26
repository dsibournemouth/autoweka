package ca.ubc.cs.beta.aeatk.state;

import java.io.File;

import ca.ubc.cs.beta.aeatk.state.legacy.LegacyStateFactory;
import ca.ubc.cs.beta.aeatk.state.nullFactory.NullStateFactory;

/**
 * Helper class for creating StateFactories
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public final class StateFactoryHelper {

	/**
	 * Retrieves a State Serializer with the required configuration
	 * <br>
	 * <b>Note:</b> If additional State Serializers start cropping up, it
	 * might be necessary to refactor this into using SPI and the TAE structure
	 * for options. For now this seems unlikely. 
	 * 
	 * 
	 * 
	 * @param serializer 	The State Serializer to use
	 * @param stateLocation A root directory for state information
	 * @return factory 		An initialized factory
	 * @see StateSerializers
	 */
	public static StateFactory getStateFactory(StateSerializers serializer,  File stateLocation )
	{
		
		
		
		switch(serializer)
		{
			case NULL:
				return new NullStateFactory();
			case LEGACY:
				return new LegacyStateFactory(stateLocation.getAbsolutePath(), null);
		}
		
		throw new IllegalArgumentException("State Serializer specified is not supported");
		
		
	}
}

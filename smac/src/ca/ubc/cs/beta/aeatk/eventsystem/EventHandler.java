package ca.ubc.cs.beta.aeatk.eventsystem;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;

/**
 * Event Handler Interface
 * 
 * Implementations of these will get invoked if they are registered with corresponding events.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 * @param <T> Type of event to handle
 */
public interface EventHandler<T extends AutomaticConfiguratorEvent> {


	/**
	 * Method invoked when an event occurs
	 * @param event
	 */
	public void handleEvent(T event);

}

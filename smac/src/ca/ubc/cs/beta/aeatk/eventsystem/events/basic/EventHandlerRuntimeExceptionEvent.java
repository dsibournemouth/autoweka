package ca.ubc.cs.beta.aeatk.eventsystem.events.basic;

import ca.ubc.cs.beta.aeatk.eventsystem.events.AutomaticConfiguratorEvent;

/**
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class EventHandlerRuntimeExceptionEvent extends AutomaticConfiguratorEvent{

	
	private final RuntimeException e;
	private final AutomaticConfiguratorEvent event;
	
	public EventHandlerRuntimeExceptionEvent(RuntimeException e, AutomaticConfiguratorEvent event)
	{
		
		this.e = e;
		this.event = event;
	}
	
	public RuntimeException getRuntimeException()
	{
		return e;
	}
	
	
	public AutomaticConfiguratorEvent getTriggeringEvent()
	{
		return event;
	}

}

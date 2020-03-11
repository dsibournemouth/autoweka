package ca.ubc.cs.beta.aeatk.eventsystem.exceptions;

public class EventManagerShutdownException extends IllegalStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7931378003970884586L;

	public EventManagerShutdownException()
	{
		super("Event Manager has previously been shutdown and you cannot fire new events, or register new handlers to it");
	}
}

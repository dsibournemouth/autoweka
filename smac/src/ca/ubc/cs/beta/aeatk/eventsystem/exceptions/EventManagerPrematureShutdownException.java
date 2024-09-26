package ca.ubc.cs.beta.aeatk.eventsystem.exceptions;

public class EventManagerPrematureShutdownException extends IllegalStateException {
	
	private static final long serialVersionUID = 7931378003970884586L;

	public EventManagerPrematureShutdownException()
	{
		super("Event Manager shutdown prematurely (dispatch thread probably died) and you cannot fire new events, or register new handlers to it");
	}
}

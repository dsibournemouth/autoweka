package ca.ubc.cs.beta.aeatk.eventsystem.exceptions;

public class EventFlushDeadLockException extends IllegalStateException {



	/**
	 * 
	 */
	private static final long serialVersionUID = -3208777459514309585L;

	public EventFlushDeadLockException() {
		super("Deadlock detected while processing events (chances are you called flush() as a side effect of processing an event): ");
		
	}


}

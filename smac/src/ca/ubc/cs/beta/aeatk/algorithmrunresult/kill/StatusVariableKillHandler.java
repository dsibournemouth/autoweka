package ca.ubc.cs.beta.aeatk.algorithmrunresult.kill;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
/**
 * {@link KillHandler} that simply stores the kill() in a flag that will later be checked
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class StatusVariableKillHandler implements KillHandler {

	private volatile boolean isKilled = false;
	@Override
	public void kill() {
		isKilled = true;
		
	}

	@Override
	public boolean isKilled() {
		return isKilled;
		
	}

}

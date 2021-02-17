package ca.ubc.cs.beta.aeatk.algorithmrunresult.kill;

/**
 * Interface that controls being killing {@link KillableAlgorithmRun}
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface KillHandler {

	/**
	 * Signals that the run should be killed
	 */
	public void kill();
	
	/**
	 * 
	 * @return <code>true</code> if {@link KillHandler#kill()}  has been invoked 
	 */
	public boolean isKilled();
}

package ca.ubc.cs.beta.aeatk.state;

import java.io.Serializable;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;

/**
 * Contains accessor methods to restore the state to the requested iteration.
 * NOTE: Implementations can NOT return null for objects they don't have. 
 * They may return default or 'empty' objects in certain cases however
 * 
 * (For instance they could return a new RunHistory object with no runs)
 * 
 * @author seramage
 *
 */
public interface StateDeserializer {
	
	/**
	 * Retrieves the RunHistory object
	 * @return runHistory object
	 */
	public RunHistory getRunHistory();
	
	
	/**
	 * Returns the iteration represented by the state
	 * @return iteration
	 */
	public int getIteration();

	/**
	 * Returns the incumbent configuration in the state
	 * @return incumbent configuration
	 */
	public ParameterConfiguration getIncumbent();
	
	/**
	 * Returns a map of the incumbent 
	 * @return a mapping of objects that can be used to store state
	 */
	public Map<String, Serializable> getObjectStateMap();
	
}

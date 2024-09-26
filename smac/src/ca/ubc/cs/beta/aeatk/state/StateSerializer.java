package ca.ubc.cs.beta.aeatk.state;

import java.io.Serializable;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;

/**
 * Interface for saving aspects of the state
 * 
 * After either/both of the RunHistory/PRNG are set call the save() method to write to disk.
 * 
 * @author seramage
 *
 */
public interface StateSerializer {

	/**
	 * Sets the runHistory to be associated with this State
	 * @param runHistory	runHistory object to save
	 */
	public void setRunHistory(RunHistory runHistory);
	
	/**
	 * Sets the object map to be associated with this state
	 * @param objectState
	 */
	public void setObjectStateMap(Map<String, Serializable> objectState);
	
	/**
	 * Saves the state to the persistence device
	 * 
	 */
	public void save();

	/**
	 * Sets the incumbent configuration at this state
	 * @param config	configuration to mark as the incumbent
	 */
	public void setIncumbent(ParameterConfiguration config);
		
}

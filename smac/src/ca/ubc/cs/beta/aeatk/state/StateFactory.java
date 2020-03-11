package ca.ubc.cs.beta.aeatk.state;

import java.io.File;
import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.exceptions.StateSerializationException;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;



/**
 * Factory for getting objects related to state. The semantics of the id string are up to individual implementations. In theory clients should
 * have the property that the id something is saved with can be used to retrieve it. But I suppose this might be too strong. Primarily the user should
 * be informed of the state being saved, and it should be clear (like a filename), how to use that id to get it back.
 * 
 * 
 * @author seramage
 *
 */
public interface StateFactory {
	
	/**
	 * Gets a State Deserializer 
	 * 
	 * @param id						An identifier for this state
	 * @param restoreIteration			Iteration to restore
	 * @param configSpace				Configuration Space to Restore Into
	 * @param instances					List of Instances we are configuring over
	 * @param execConfig 				Execution Config of the target algorithm
	 * @return object capable of restoring state
	 * @throws StateSerializationException when an error occurs restoring the state
	 */
	public StateDeserializer getStateDeserializer(String id, int restoreIteration, 	ParameterConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfiguration execConfig, RunHistory rh) throws StateSerializationException;
	
	
	/**
	 * Gets a State Serializer
	 * @param id			An Identifier for this state
	 * @param iteration		Iteration to restore
	 * @return Object which can be serialized
	 * @throws StateSerializationException when an error occurs saving the state
	 */
	public StateSerializer getStateSerializer(String id, int iteration) throws StateSerializationException;
	

	/**
	 * Purges all the previous states that were saved
	 * 
	 * <b>Implementation Note:</b> This is an optional method, and it is not defined what specifically this method does
	 * if requested the user is hoping to free up resources. What should be true though is that after this method is called
	 * a user should still be able to restore to the most recent iteration. This method can do nothing necessary 
	 * 
	 */
	public void purgePreviousStates();


	
	/**
	 * Copies the file to the State Directory
	 * 
	 * @param name name of the file to write
	 * @param f source file
	 */
	public void copyFileToStateDir(String name, File f);
	
	


	
}

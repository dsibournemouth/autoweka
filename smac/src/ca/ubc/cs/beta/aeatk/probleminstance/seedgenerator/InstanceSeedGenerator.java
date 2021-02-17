package ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;

/**
 * Generates Random Numbers for use with Problem Instances
 *
 */
public interface InstanceSeedGenerator extends Serializable {

	/**
	 * Re-initalzes this <code>InstanceSeedGenerator</code> to it's initial state
	 */
	public void reinit();

	/**
	 * Returns the next seed for the given instance
	 * @param pi problemInstance to get the seed for
	 * @return next seed
	 */
	public int getNextSeed(ProblemInstance pi);


	/**
	 * Checks if there is another seed available for this instance
	 * @param pi ProblemInstance to check seed availibility for
	 * @return	<code>true</code> if atleast one more seed is available, <code>false</code> otherwise
	 */
	public boolean hasNextSeed(ProblemInstance pi);

	/**
	 * Returns the the order in which ProblemInstances were declared in with there seeds.
	 *  
	 * In other words if you wanted to execute instances in order they appear in an instance seed file, use this list
	 *
	 * @return list of instances as they are declared and redeclared in the instance file
	 */
	public List<ProblemInstance> getProblemInstanceOrder(Collection<ProblemInstance> instances);

	
	/**
	 * Returns the initial number of seeds available accross all instances 
	 * 
	 * @return total number seeds that are first available (not necessarily the number left)
	 */
	public int getInitialInstanceSeedCount();
	
	/**
	 * Returns whether or not all instances were initialized with the same number of seeds
	 * 
	 * @deprecated This method exists primarily because RunHistory objects rely on an invariant that
	 * all instances have the same number of seeds, when this is fixed, this will go. In general
	 * for test sets and the like this is not an error condition. 
	 * 
	 * @return <code>true</code> if and only if all instances have the same number of seeds
	 */
	@Deprecated
	public boolean allInstancesHaveSameNumberOfSeeds();

	/**
	 * Takes the following problem instance seed pair and prevents it from ever being returned
	 * 
	 * @param pi  		the problem instance
	 * @param seed		seed
	 */
	public void take(ProblemInstance pi, long seed);
	
	
}
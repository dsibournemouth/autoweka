package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.Collection;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;

/**
 * Thread safe implementation of the Run History object
 * <p>
 * <b>Note:</b> All methods are properly guarded already, 
 * this interface adds the ability to get a global lock for making
 * compound actions atomic.
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface ThreadSafeRunHistory extends RunHistory {

	
	/**
	 * Appends a group of runs to the RunHistory atomically
	 * 
	 * @param runs	runs to log, the runs are processed in order of the collection iterator
	 * 
	 * @throws DuplicateRunException  - This object is only consistent as so far as the data structures are concerned. The runs may still be partially applied however.
	 */
	public void append(Collection<AlgorithmRunResult> runs) throws DuplicateRunException;
	
	/**
	 * Requests a read lock on the data structure
	 */
	public void readLock();
	
	/**
	 * Releases a read lock on the data structure
	 */
	public void releaseReadLock();

	
	

	
	
}

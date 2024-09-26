package ca.ubc.cs.beta.aeatk.algorithmrunresult;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.json.serializers.AlgorithmRunResultJson;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

/**
 * Represents an execution of a target algorithm. 
 * 
 * All implementations should be immutable
 * 
 * NOTE: The following invariants exist, and implementations that don't follow this may have unexpected results
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@JsonSerialize(using=AlgorithmRunResultJson.AlgorithmRunSerializer.class)
@JsonDeserialize(using=AlgorithmRunResultJson.AlgorithmRunDeserializer.class)
public interface AlgorithmRunResult extends Serializable {

	/**
	 * Returns the AlgorithmExecutionConfig of the run
	 * @deprecated It is now accessible via {@link #getAlgorithmRunConfiguration()}
	 * @return AlgorithmExecutionConfig of the run
	 * 
	 */
	
	/**
	 * Return the run configuration associated with the AlgorithmRun
	 * @return run configuration of this run
	 */
	public AlgorithmRunConfiguration getAlgorithmRunConfiguration();

	/**
	 * Get the Run Status
	 * 
	 * <b>Implementation Notes:</b>
	 * 
	 *  The Run Status should be TIMEOUT if the cutoff time is zero, and implementations may not do anything else but return this run. 
	 *  
	 *  The Run Status should NEVER be RUNNING, unless this is an appropriate subtype that supports Killing.
	 *  
	 *  If the status is RUNNING then isRunComplete() should return <code>false</code> otherwise it should return </code>true</code>
	 *  
	 * @return RunResult for run
	 * @throws IllegalStateException if the run has not completed
	 */
	public RunStatus getRunStatus();

	/**
	 * Get reported runtime of run 
	 * 
	 * @return double for the runtime (>= 0) && < Infinity
	 * @throws IllegalStateException if the run has not completed
	 */
	public double getRuntime();

	/**
	 * Get the reported run length
	 * 
	 * @return double for the runlength ( >= 0 && < Infinity) || -1 
	 * @throws IllegalStateException if the run has not completed
	 */
	public double getRunLength();

	/**
	 * Get the reported quality 
	 * 
	 * @return double for the quality ( > -Infinity && < +Infinity)
	 * @throws IllegalStateException if the run has not completed
	 */
	public double getQuality();

	/**
	 * Get the seed that was returned
	 * 
	 * NOTE: For well behaved programs this should always be the seed in the ProblemInstanceSeedPair of RunConfig
	 * @return seed reported by algorithm
	 * @throws IllegalStateException if the run has not completed
	 */
	public long getResultSeed();

	/**
	 * Note: This should always return a well-formatted result line. It may NOT necessarily correspond to values
	 * that the methods return. 
	 * 
	 * (i.e. You should be able to use this output as standard output without any validation, but it may not correspond to what we got this time)
	 * 
	 * Some extreme examples are when we clean up messy wrappers output (for instance SAT >= timeout). Depending on how the cleanup is done, this
	 * may change the result flagged, or we may massage the timeout. 
	 * 
	 * @return string representing a close approximation of this run that is guaranteed to be parsable.
	 * @throws IllegalStateException if the run has not completed
	 */
	public String getResultLine();
	
	
	/**
	 * If this run is currently RUNNING request that it should be killed, otherwise do nothing.
	 * <br/><br/>
	 * <b>NOTE:</b> This method should only be called within the scope (or synchronized with) a {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver#currentStatus(java.util.List)} call.
	 * Specifically what this means, is that if you somehow save the run from the observer call, and later call it, the call may then be ignored. The reason is that implementations may choose to handle this in different ways,
	 * generally it will be some flag variable. When they notify you again later, they may use a different flag variable. As a result if you later chose to kill a run, the {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator} is
	 * under <b>NO</b> obligation to check it. 
	 * 
	 */
	public void kill();
	
	/**
	 * Returns a (comma free) String from the algorithm run with additional data
	 * 
	 * This data generally has no meaning for SMAC but should be saved and restored in the run history file
	 * 
	 * @return a string (possibly empty but never null) that has the additional run data in it. (This string will also generally be trimed())
	 * 
	 */
	public String getAdditionalRunData(); 
	
	/**
	 * Runs this AlgorithmRun
	 * 
	 * Subsequent calls to this should be noop, and are not error conditions.
	 * 
	 * If this method successfully returns it's guaranteed that isRunCompleted() is true
	 */
	//public void run();
	
	/**
	 * Runs this Algorithm Run
	 * 
	 * Subsequent calls to this should be a noop, and are not error conditions
	 * 
	 * If this method successfully returns it's guaranteed that isRunCompleted is true
	 * 
	 * @return null (always)
	 */
	//public Object call();
	

	/**
	 * Returns true if the run is complete
	 * <b>Implementation Note:</b>This should always be the same as run.getRunResult().equals(RunResult.RUNNING)
	 * 
	 * @return <code>true</code> if this run has finished executing, <code>false</code> otherwise
	 * 
	 * 
	 */
	public boolean isRunCompleted();

	/**
	 * Returns the raw output of the line we matched (if any), this is for debug purposes only
	 * and there is no requirement that this actually return any particular string.
	 * <p>
	 * <b>Implementation Note:</b> An example where this is useful is if you use a weaker regex to match a possible output, and 
	 * then the stronger parsing fails. The weaker regex match could be returned here
	 * 
	 * @return string possibly containing a raw result
	 */
	public abstract String rawResultLine();
	
	/**
	 * Returns the amount of wallclock time the algorithm executed for
	 * <p>
	 * <b>Implementation Note:</b> This is NOT the runtime of the reported algorithm and may be less than or greater than in certain circumstances
	 * <p>
	 * In cases where the algorithm can determine that it won't solve the algorithm in a given time it may very well be less than the reported RunTime
	 * <p>
	 * In cases where the algorithm has a lot of overhead this may be drastically higher than the algorithm reports.
	 * 
	 * @return amount of time in seconds the algorithm ran for in seconds
	 */
	public double getWallclockExecutionTime();
	
	
	/**
	 * Returns <code>true</code> iff the run is a lower bound on performance
	 */
	
	public boolean isCensoredEarly();
	
	/**
	 * Shortcut method for returning the Parameter Configuration that this AlgorithmRunResult contains 
	 */
	public ParameterConfiguration getParameterConfiguration();
	
	/**
	 * Shortcut method for returning the AlgorithmExecutionConfiguration that this AlgorithmRunResult contains 
	 */
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfiguration();
	
	/**
	 * Shortcut method for returning the problem instance seed pair that this AlgorithmRunResult contains
	 */
	public ProblemInstanceSeedPair getProblemInstanceSeedPair();
	
	/**
	 * Shortcut method for returning the problem instance for that this AlgorithmRunResult contains
	 * @return
	 */
	public ProblemInstance getProblemInstance();
}

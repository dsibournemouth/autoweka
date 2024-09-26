package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

/**
 * Executes Target Algorithm Runs (Converts between RunConfig objects to AlgorithmRun objects)
 * <p>
 * <b>Implementation Details</b>
 * <p>
 * Clients should subtype this interface if they want to allow programs to execute algorithms through
 * some other method. All implementations MUST have a constructor that takes a {@link ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration} object.
 * <p>
 * Additionally client implementations should probably not validate the output of AlgorithmRuns but rely on other decorators to do this for them.
 * <p>
 * <b>NOTE:</b>Implementations MUST be thread safe, and ideally concurrent calls to evaluateRun() should all be serialized in such a way 
 * that honours the concurrency requirements of the evaluator (in other words, if concurrency is limited to N processors, then 
 * regardless of how many times evaluateRun is called concurrently only N actual runs of the target algorithm should be running at any given time)
 * <p>
 * 

 * @see ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory
 * @see ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException
 * @see ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 * 
 */
public interface TargetAlgorithmEvaluator extends AutoCloseable{
 
	/**
	 * Evaluate a run configuration
	 * <br/>
	 * <b>Implementation Note:</b> Any implementation of this method MUST be the same as calling {@link TargetAlgorithmEvaluator#evaluateRun(List, TargetAlgorithmEvaluatorRunObserver)} with that same run in the list.
	 * <br>
	 * <br>
	 * <b>Thread Interruption Handling:</b>All implementations must ensure that if the thread is interrupted, the interrupt status is restored, not swallowed (See JCIP §5.4). Implementations may or may not
	 * support interruption of requests, and it is entirely <font color="red"><i>UNDEFINED</i></font> whether the runs are still processed or not. Consequently you are recommended to use the observer to terminate runs.
	 * 
	 * @param runConfig RunConfig to evaluate
	 * @return	list containing the <code>AlgorithmRun<code>
	 * @throws ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException
	 * @throws ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException
	 */
	public List<AlgorithmRunResult> evaluateRun(AlgorithmRunConfiguration runConfig);

	/**
	 * Evaluate a list of run configurations
	 * <br/>
	 * <b>Implementation Note:</b> Any implementation of this method MUST be the same as calling {@link TargetAlgorithmEvaluator#evaluateRun(List, TargetAlgorithmEvaluatorRunObserver)} with that same run in the list.
	 * <br>
	 * <br>
	 * <b>Thread Interruption Handling:</b>All implementations must ensure that if the thread is interrupted, the interrupt status is restored, not swallowed (See JCIP §5.4). Implementations may or may not
	 * support interruption of requests, and it is entirely <font color="red"><i>UNDEFINED</i></font> whether the runs are still processed or not. Consequently you are recommended to use the observer to terminate runs.
	 * 
	 * 
	 * @param runConfigs a list containing zero or more unique run configurations to evaluate
	 * @return	list of the exact same size as input containing the <code>AlgorithmRun</code> objects in the same order as runConfigs
	 * @throws ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException
	 * @throws ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException
	 */
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs);

	/**
	 * Evaluate a list of run configurations
	 * <br>
	 * <br>
	 * <b>Thread Interruption Handling:</b>All implementations must ensure that if the thread is interrupted, the interrupt status is restored, not swallowed (See JCIP §5.4). Implementations may or may not
	 * support interruption of requests, and it is entirely <font color="red"><i>UNDEFINED</i></font> whether the runs are still processed or not. Consequently you are recommended to use the observer to terminate runs.
	 * 
	 * @param runConfigs	a list containing zero or more unique run configurations to evaluate
	 * @param observer 	 	observer that will be notified of the current run status
	 * @return	list of the exact same size as input containing the <code>AlgorithmRun</code> objects in the same order as runConfigs
	 * @throws ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException
	 * @throws ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException
	 */
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver observer);
	
	
	/**
	 * Evaluates the given configuration, and when complete the handler is invoked.
	 * 
	 * <p>
	 * <b>Note:</b>You are guaranteed that when this method returns your runs have been 'delivered'
	 * to the eventual processor. In other words if the runs are dispatched to some external
	 * processing system, you can safely shutdown after this method call completes and know that they have been
	 * delivered. Additionally if the runs are already complete (for persistent TAEs), the call back is guaranteed to fire to completion <i>before</i> the program exits
	 * normally (that is you can do a normal shutdown, and the onSuccess method should fire)
	 * <p>
	 * <b>Usage Note:</b> The callback should expect to see the following exceptions {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException} and {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException}
	 * <p>
	 * <b>Implementation Note:</b> Any implementation of this method MUST be the same as calling {@link TargetAlgorithmEvaluator#evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)} with that same run in the list.
	 * <br>
	 * <br>
	 * <b>Thread Interruption Handling:</b>All implementations must ensure that if the thread is interrupted, the interrupt status is restored, not swallowed (See JCIP §5.4). Implementations may or may not
	 * support interruption of requests, and it is entirely <font color="red"><i>UNDEFINED</i></font> whether the runs are still processed or not. Consequently you are recommended to use the observer to terminate runs.
	 * 
	 * 
	 * @param runConfig  run configuration to evaluate
	 * @param callback    handler to invoke on completion or failure
	 */
	public void evaluateRunsAsync(AlgorithmRunConfiguration runConfig, TargetAlgorithmEvaluatorCallback callback );
	
	/**
	 * Evaluates the given configuration, and when complete the handler is invoked
	 * <p>
	 * <b>Note:</b>You are guaranteed that when this method returns your runs have been 'delivered'
	 * to the eventual processor. In other words if the runs are dispatched to some external
	 * processing system, you can safely shutdown after this method call completes and know that they have been
	 * delivered. Additionally if the runs are already complete (for persistent TAEs), the call back is guaranteed to fire to completion <i>before</i> the program exits
	 * normally (that is you can do a normal shutdown, and the onSuccess method should fire)
	 * <p>
	 * <b>Usage Note:</b> The callback should expect to see the following exceptions {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException} and {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException}
	 * <p>
	 * <b>Implementation Note:</b> Any implementation of this method MUST be the same as calling {@link TargetAlgorithmEvaluator#evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback, TargetAlgorithmEvaluatorRunObserver)} with that same run in the list.
	 * <br>
	 * <br>
	 * <b>Thread Interruption Handling:</b>All implementations must ensure that if the thread is interrupted, the interrupt status is restored, not swallowed (See JCIP §5.4). Implementations may or may not
	 * support interruption of requests, and it is entirely <font color="red"><i>UNDEFINED</i></font> whether the runs are still processed or not. Consequently you are recommended to use the observer to terminate runs.
	 * 
	 * @param runConfigs list of zero or more unique run configuration to evaluate
	 * @param callback   handler to invoke on completion or failure
	 */
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorCallback callback);

	/**
	 * Evaluates the given configuration, and when complete the handler is invoked
	 * <p>
	 * <b>Note:</b>You are guaranteed that when this method returns your runs have been 'delivered'
	 * to the eventual processor. In other words if the runs are dispatched to some external
	 * processing system, you can safely shutdown after this method call completes and know that they have been
	 * delivered. Additionally if the runs are already complete (for persistent TAEs), the call back is guaranteed to fire to completion <i>before</i> the program exits
	 * normally (that is you can do a normal shutdown, and the onSuccess method should fire)
	 * <p>
	 * <b>Usage Note:</b> The callback should expect to see the following exceptions {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException} {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmEvaluatorShutdownException}
	 * <br>
	 * <br>
	 * <b>Thread Interruption Handling:</b>All implementations must ensure that if the thread is interrupted, the interrupt status is restored, not swallowed (See JCIP §5.4). Implementations may or may not
	 * support interruption of requests, and it is entirely <font color="red"><i>UNDEFINED</i></font> whether the runs are still processed or not. Consequently you are recommended to use the observer to terminate runs.
	 * 
	 * @param runConfigs list of zero or more unique run configuration to evaluate
	 * @param callback   handler to invoke on completion or failure
	 * @param observer	 observer that will be notified of the current run status
	 */
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer);
	

	/**
	 * Blocks waiting for the number of incomplete or outstanding evaluations to be zero. Complete is defined as all {@link #evaluateRun(List)} methods have returned to the caller and all the callbacks to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback)} having returned.
	 * 
	 * <b>NOTE:</b> This is NOT the same as waiting for the TAE to shutdown or be ready to shutdown, just that this TAE has no outstanding runs
	 * <br/>
	 * <b>IMPLEMENTATION NOTE:</b> You generally don't need to implement this method, but instead wrap your TAE with a {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator}
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	public void waitForOutstandingEvaluations();
	
	/**
	 * Returns the total number of incomplete or outstanding evaluations. Completion is defined as all {@link #evaluateRun(List)} methods have returned to the caller and all the callbacks to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback)} having returned.
	 * <br/>
	 * <b>NOTE:</b> This is NOT the number of runConfigs to be evaluated but the number of requests, and just because this returns zero doesn't mean it can't increase in the future.
	 * <br/>
	 * <b>IMPLEMENTATION NOTE:</b> You generally don't need to implement this method, but instead wrap your TAE with a {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator}
	 * 
	 * @deprecated This will be replaced by the method {{@link #getNumberOfOutstandingBatches()} at some point, it's the same thing with a clearer name.
	 * @return number of outstanding evaluations
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Deprecated
	public int getNumberOfOutstandingEvaluations();
	
	/**
	 * Returns the total number of incomplete or outstanding batches. Completion is defined as all {@link #evaluateRun(List)} methods have returned to the caller and all the callbacks to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback)} having returned.
	 * <br/>
	 * <b>NOTE:</b> This is NOT the number of runConfigs to be evaluated but the number of requests, and just because this returns zero doesn't mean it can't increase in the future.
	 * <br/>
	 * <b>IMPLEMENTATION NOTE:</b> You generally don't need to implement this method, but instead wrap your TAE with a {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator}
	 * 
	 * @return number of outstanding evaluations
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	public int getNumberOfOutstandingBatches();

	/**
	 * Returns the total number of incomplete or outstanding batches. Complete is defined as all {@link #evaluateRun(List)} methods have returned to the caller and all the callbacks to {@link #evaluateRunsAsync(List, TargetAlgorithmEvaluatorCallback)} having returned.
	 * <br/>
	 * <b>IMPLEMENTATION NOTE:</b> You generally don't need to implement this method, but instead wrap your TAE with a {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator}
	 * 
	 * @return number of outstanding evaluations
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	public int getNumberOfOutstandingRuns();
	
	
	/**
	 * Returns the number of target algorithm runs that we have executed
	 * @return	total number of runs evaluated
	 */
	public int getRunCount();
	
	
	/**
	 * May optionally return a unique number that should roughly correspond to a unique sequence of run requests.
	 *  
	 * [i.e. A user seeing the same sequence of run codes, should be confident that the runs by the Automatic Configurator are 
	 * identical]. Note: This method is optional and may just return zero. 
	 * 
	 * 
	 * @return runHashCode computed
	 * 
	 */
	public int getRunHash();
	
	/**
	 * Sets the runCount to the given parameter
	 * 
	 * This is useful when we are restoring the state of SMAC 
	 * 
	 * @see ca.ubc.cs.beta.aeatk.state.StateFactory
	 * @param runs
	 */
	public void seek(List<AlgorithmRunResult> runs);

	/**
	 * Returns a String that ought to be useful to the user to reproduce the results of a given run for a given
	 * runConfig.
	 * 
	 * For CommandLineTargetAlgorithmEvaluator this generally means a sample execution string for this algorithm.
	 * 
	 * For other evaluators it's implementation dependent, and you are free to return whatever <em>non-null</em> string you want.
	 *
	 * If you are lazy, or it really is meaningless for your implementation (say there is no other way to execute this except via SMAC)
	 * you should return <b>N/A</b> 
	 * 
	 * @param runConfig run configuration to generate a call string for
	 * @return string something the user can execute directly if necessary to reproduce the results
	 */
	public String getManualCallString(AlgorithmRunConfiguration runConfig);

	/**
	 * Notifies the TargetAlgorithmEvaluator that we are shutting down
	 * <p> 
	 * <b>Implementation Note:</b> Depending on what the TargetAlgorithmEvaluator does this can be a noop, the only purpose
	 * is to allow TargetAlgorithmEvaluators to shutdown any thread pools, that will prevent the JVM from exiting. The 
	 * TargetAlgorithmEvaluator may also choose to keep resources running for other reasons, and this method 
	 * should NOT be interpreted as requesting the TargetAlgorithmEvalutor to shutdown. 
	 * <p>
	 * Example: If this TAE were to allow for sharing of resources between multiple independent SMAC runs, a call to this method
	 * should NOT be taken as a requirement to shutdown the TAE, only that there is one less client using it. Once it recieved
	 * sufficient shutdown notices, it could then decide to shutdown.
	 * <p>
	 * Finally, if this method throws an exception, chances are the client will not catch it and will crash.
	 */
	public void notifyShutdown();
	
	/**
	 * Returns <code>true</code> if the TargetAlgorithmEvaluator run requests are final, that is
	 * rerunning the same request again would give you an identical answer.
	 * <p>
	 * <b>Implementation Note:</b> This is primarily of use to prevent decorators from trying to 
	 * get a different answer if they don't like the first one (for instance retry crashing runs, etc).
	 *
	 * @return <code>true</code> if run answers are final
	 */
	public boolean isRunFinal();
	
	/**
	 * Returns <code>true</code> if all the runs made to the TargetAlgorithmEvaluator will be persisted.
	 * <p> 
	 *<b>Implementation Note:</b> This is used to allow some programs to basically hand-off execution to some
	 * external process, say a pool of workers, and then if re-executed can get the same answer later. 
	 *
	 * @return <code>true</code> if runs can be retrieved externally of this currently running program
	 */
	public boolean areRunsPersisted();
	
	/**
	 * Returns <code>true</code> if all the runs made to the TargetAlgorithmEvaluator are observable
	 * <p>
	 * <b>Implementation Note:</b> The notification of observers is made on a best-effort basis,
	 * if this TAE just won't notify us then it should return false. This can allow for better logging 
	 * or experience for the user 
	 */
	public boolean areRunsObservable();
	
	/**
	 * This method exists to allow TargetAlgorithmEvaluators to be used in try-with-resources in Java 7.
	 * This method MUST be the same as invoking {@link #notifyShutdown()}
	 */
	@Override
	public void close();
	
	
	
}

package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

/**
 * Handler interface for Deferred Target Algorithm Evaluator runs
 * <br>
 * <b>TAE Implementor Note:</b> If the onSuccess() method throws an exception, you should call the onFailure() method,
 * this primarily simplifies the implementations of decorators.
 * <br>
 * <b>Client Note:</b> Objects that implement this should NOT override equals() or hashCode(), it may mess up internal
 * data structures {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.caching.CachingTargetAlgorithmEvaluatorDecorator} in particular
 * 
 *
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface TargetAlgorithmEvaluatorCallback {

	/**
	 * Invoked if/when the runs complete
	 * @param runs the list of completed runs
	 */
	public void onSuccess(List<AlgorithmRunResult> runs);
	
	/**
	 * Invoked if/when there is a failure
	 * @param e throwable that occurred
	 */
	public void onFailure(RuntimeException e);
	
	
	
}

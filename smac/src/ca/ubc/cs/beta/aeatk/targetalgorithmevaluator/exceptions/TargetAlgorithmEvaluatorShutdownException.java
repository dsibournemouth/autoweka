package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions;

/**
 * Occurs when the TAE has been shutdown, or when the current thread has been interrupted
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class TargetAlgorithmEvaluatorShutdownException extends RuntimeException {

	
	private static final long serialVersionUID = 6812069375285515103L;

	public TargetAlgorithmEvaluatorShutdownException(Exception e) {
		super(e);
	}

	
	public TargetAlgorithmEvaluatorShutdownException() {
		super("Target Algorithm Evaluator has been shutdown");
	}
}

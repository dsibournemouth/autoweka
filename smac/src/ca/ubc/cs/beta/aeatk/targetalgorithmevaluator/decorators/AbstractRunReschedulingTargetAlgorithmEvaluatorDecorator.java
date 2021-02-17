package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators;

import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;

/**
 * Implementors of this class *reschedule* some runs, as such it means that
 * you cannot wait or get an accurate count of runs from them.
 * 
 * Examples:
 * 
 * 
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public abstract class AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator extends
		AbstractTargetAlgorithmEvaluatorDecorator {

	public AbstractRunReschedulingTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae) {
		super(tae);
	}
	
	
	public void throwException()
	{
		 throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting/observing/reporting the number of outstanding evaluations. This is because this Target Algorithm Evaluator may schedule runs internally that should not be "
				+ "apparent to outside observers. You should rewrap this class with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class.getCanonicalName() );
		
	}
	/**
	 * We need to throw this now because even if the lower level supplies it, we may break it.
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Override
	public void waitForOutstandingEvaluations()
	{
		throwException();
	}
	
	/**
	 * We need to throw this now because even if the lower level supplies it, we may break it.
	 */
	@Override
	public int getNumberOfOutstandingEvaluations()
	{
		throwException();
		return -1;
	}
	
	@Override
	public int getNumberOfOutstandingRuns()
	{
		throwException();
		return -1;
	}
	

	
	@Override
	public int getNumberOfOutstandingBatches() {
		throwException();
		return -1;
	}

	

	@Override
	public int getRunCount() {
		throwException();
		return -1;
	}
	
	

}

package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

/**
 * TAECallback that has a wait method() that lets you block until it is complete
 * 
 * @author Steve Ramage
 *
 */
public class WaitableTAECallback implements TargetAlgorithmEvaluatorCallback {

	
	private final CountDownLatch completeCount = new CountDownLatch(1);
	private final TargetAlgorithmEvaluatorCallback handler;
			
	public WaitableTAECallback(TargetAlgorithmEvaluatorCallback handler)
	{
		this.handler = handler;
	}
	@Override
	public void onSuccess(List<AlgorithmRunResult> runs) {
		
		handler.onSuccess(runs);
		completeCount.countDown();
		

	}

	@Override
	public void onFailure(RuntimeException t) {
		try {
			handler.onFailure(t);
		} finally
		{
			completeCount.countDown();
		}
	}

	/**
	 * Wait for the handlers to successfully finish
	 */
	public void waitForCompletion()
	{
		try 
		{
			completeCount.await();
		} catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}
	
}

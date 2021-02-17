package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators;

import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
/**
 * Abstract Decorator class for {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator}
 * 
 * <br/>
 * <b>Implementation Note:</b>  Almost every decorator that is doing something interesting, will
 * in fact redirect evaluateRun(RunConfig) to it's own local evaluateRun(List<RunConfig>) method.
 * You should not rely on evaluateRun() being called directly.
 *  
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public abstract class AbstractTargetAlgorithmEvaluatorDecorator implements	TargetAlgorithmEvaluator {

	protected final TargetAlgorithmEvaluator tae;

	public AbstractTargetAlgorithmEvaluatorDecorator(TargetAlgorithmEvaluator tae)
	{
		this.tae = tae;
	}
	
	
	@Override
	public final List<AlgorithmRunResult> evaluateRun(AlgorithmRunConfiguration runConfig) {
		return evaluateRun(Collections.singletonList(runConfig));
	}

	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs) {
		return evaluateRun(runConfigs, null);
	}


	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver observer) {
		return tae.evaluateRun(runConfigs, observer);
	}

	@Override
	public final void evaluateRunsAsync(AlgorithmRunConfiguration runConfig, TargetAlgorithmEvaluatorCallback callback) {
		evaluateRunsAsync(Collections.singletonList(runConfig), callback);
	}

	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs, final TargetAlgorithmEvaluatorCallback callback) {
		evaluateRunsAsync(runConfigs, callback, null);
	}

	
	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			final TargetAlgorithmEvaluatorCallback callback, TargetAlgorithmEvaluatorRunObserver observer) {
		tae.evaluateRunsAsync(runConfigs, callback, observer);
	}

	@Override
	public int getRunCount() {
		return tae.getRunCount();
	}

	@Override
	public int getRunHash() {
		return tae.getRunHash();
	}

	@Override
	public void seek(List<AlgorithmRunResult> runs) {
		tae.seek(runs);

	}
	@Override
	public String getManualCallString(AlgorithmRunConfiguration runConfig) {
		return tae.getManualCallString(runConfig);
	}
	
	
	/**
	 * Generally this method shouldn't be used, as you should wait for the parent to shutdown.
	 */
	protected void preDecorateeNotifyShutdown()
	{
		//Template method
	}
	
	@Override
	public final void notifyShutdown()
	{
		
		
		try {
			preDecorateeNotifyShutdown();
		} catch(RuntimeException e)
		{
			//Don't bother doing anything with the pre shutdown error, for no reason other than I'm lazy
			//Feel free to fix this logic.
			LoggerFactory.getLogger(getClass()).error("Error occured while shutting down", e);
		}
		
		
		//We assume the first exception has priority 
		//because maybe some other crashes have caused the problem
		RuntimeException first = null;
		
		try {
			tae.notifyShutdown();
		} catch(RuntimeException e)
		{
			first = e;
		}
		
		try {
			postDecorateeNotifyShutdown();
			
			if(first != null)
			{
				throw first;
			}
		} catch(RuntimeException e)
		{
			if(first != null)
			{
				LoggerFactory.getLogger(getClass()).error("Error occured while shutting down", e);
				throw first;
			} else
			{
				throw e;
			}
		}
		
	}
	
	protected abstract void postDecorateeNotifyShutdown();

	
	
	
	@Override
	public boolean isRunFinal()
	{
		return tae.isRunFinal();
	}
	
	@Override
	public boolean areRunsPersisted()
	{
		return tae.areRunsPersisted();
	}
	
	@Override
	public boolean areRunsObservable()
	{
		return tae.areRunsObservable();
	}
	
	@Override
	public final String toString()
	{
		return this.getClass().getSimpleName() + "( 0x" + Integer.toHexString(System.identityHashCode(this)) + " ) ==> [ " + tae.toString() + " ]";
	}

	@Override
	public void waitForOutstandingEvaluations()
	{
		tae.waitForOutstandingEvaluations();
	}
	
	@Override
	public int getNumberOfOutstandingEvaluations()
	{
		return tae.getNumberOfOutstandingBatches();
	}
	
	@Override
	public int getNumberOfOutstandingBatches() {
		return tae.getNumberOfOutstandingBatches();
	}


	@Override
	public int getNumberOfOutstandingRuns() {
		return tae.getNumberOfOutstandingRuns();
	}
	
	@Override
	public final void close()
	{
		this.notifyShutdown();
	}
	

}

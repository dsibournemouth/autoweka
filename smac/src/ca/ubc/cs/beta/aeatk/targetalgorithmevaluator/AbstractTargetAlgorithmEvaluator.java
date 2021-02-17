package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;

/**
 * Abstract Target Algorithm Evalutar
 * <p>
 * This class implements the default noop operation
 * 
 * @author Steve Ramage 
 */
@ThreadSafe
public abstract class AbstractTargetAlgorithmEvaluator implements TargetAlgorithmEvaluator {
	
	protected final AtomicInteger runCount = new AtomicInteger(0);

	/**
	 * Default Constructor
	 * @param execConfig	execution configuration of the target algorithm
	 */
	public AbstractTargetAlgorithmEvaluator()
	{
	
	}
	
	/**
	 * Evaluate a sequence of run configurations
	 * @param runConfigs 			a list containing zero or more run configurations to evaluate
	 * @param runStatusObserver  	observer that will be notified of the current run status
	 * @return	list of the exact same size as input containing the <code>AlgorithmRun</code> objects in the same order as runConfigs
	 * @throws TargetAlgorithmAbortException
	 */
	public abstract List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver runStatusObserver);
	
	/**
	 * Evaluates the given configuration, and when complete the handler is invoked
	 * <p>
	 * <b>Note:</b>You are guaranteed that when this method returns your runs have been 'delivered'
	 * to the eventual processor. In other words if the runs are dispatched to some external
	 * processing system, you can safely shutdown after this method call completes and know that they have been
	 * delivered. Additionally if the runs are already complete (for persistent TAEs), the call back is guaranteed to fire to completion <i>before</i> 
	 * this method is returned.
	 * 
	 * @param runConfigs 		list of zero or more run configuration to evaluate
	 * @param taeCallback   	handler to invoke on completion or failure
	 * @param runStatusObserver observer that will be notified of the current run status
	 */
	public abstract void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorCallback taeCallback, TargetAlgorithmEvaluatorRunObserver runStatusObserver);
	
	
	@Override
	public final List<AlgorithmRunResult> evaluateRun(AlgorithmRunConfiguration run) 
	{
		return evaluateRun(Collections.singletonList(run), null);
	}
	
	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs)
	{
		return evaluateRun(runConfigs, null);
	}
	
	@Override
	public final void evaluateRunsAsync(AlgorithmRunConfiguration runConfig, TargetAlgorithmEvaluatorCallback handler) {
		evaluateRunsAsync(Collections.singletonList(runConfig), handler);
	}

	
	@Override
	public final void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			TargetAlgorithmEvaluatorCallback handler) {
				evaluateRunsAsync(runConfigs, handler, null);
			}

	
	@Override
	public int getRunCount()
	{
		return runCount.get();
	}
	

	@Override
	public int getRunHash()
	{
		return 0;
	}

	@Override
	public void seek(List<AlgorithmRunResult> runs) 
	{
		runCount.set(runs.size());	
	}

	protected void addRuns(List<AlgorithmRunResult> runs)
	{
		runCount.addAndGet(runs.size());
	}

	@Override
	public String getManualCallString(AlgorithmRunConfiguration runConfig) {
		
		AlgorithmExecutionConfiguration execConfig = runConfig.getAlgorithmExecutionConfiguration();
		StringBuilder sb = new StringBuilder();
		
		
		String commandSeparator = ";";
		
		if(System.getProperty("os.name").toLowerCase().contains("win"))
		{
			commandSeparator = "&";
		}
		if(execConfig.getAlgorithmExecutionDirectory().matches(".*\\s.*"))
		{
			sb.append("cd \"").append(execConfig.getAlgorithmExecutionDirectory()).append("\"" +commandSeparator+ " ");
		} else
		{
			sb.append("cd ").append(execConfig.getAlgorithmExecutionDirectory()).append(commandSeparator + " ");
		}
		
		sb.append(CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString(runConfig));
		sb.append("");
		
		return sb.toString();
	}
	
	/**
	 * Blocks waiting for all runs that have been invoked via evaluateRun or evaluateRunAsync to complete
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Override
	public void waitForOutstandingEvaluations()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}
	
	
	/**
	 * Returns the total number of outstanding evaluations, that is the number of calls to evaluateRun or evaluateRunAsync to complete
	 * <b>NOTE:</b> This is NOT the number of runConfigs to be evaluated but the number of requests
	 * 
	 * @return number of outstanding evaluations
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Override
	public int getNumberOfOutstandingEvaluations()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}

	
	/**
	 * Returns the total number of outstanding evaluations, that is the number of calls to evaluateRun or evaluateRunAsync to complete
	 * <b>NOTE:</b> This is NOT the number of runConfigs to be evaluated but the number of requests
	 * 
	 * @return number of outstanding evaluations
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Override
	public int getNumberOfOutstandingRuns()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}
	
	
	
	/**
	 * Returns the total number of outstanding evaluations, that is the number of calls to evaluateRun or evaluateRunAsync to complete
	 * <b>NOTE:</b> This is NOT the number of runConfigs to be evaluated but the number of requests
	 * 
	 * @return number of outstanding evaluations
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
	@Override
	public int getNumberOfOutstandingBatches()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}
	
	

	@Override
	public final String toString()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public final void close()
	{
		this.notifyShutdown();
	}
}

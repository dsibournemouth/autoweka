package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;

import java.util.Collections;
import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator;

/**
 * Utility TargetAlgorithmEvaluator that wraps two, and checks that they always return the same value.
 * 
 * Mainly used for testing and development.
 * 
 * @author Steve Ramage 
 *
 */
public class EqualTargetAlgorithmEvaluatorTester implements
		TargetAlgorithmEvaluator {

	private final TargetAlgorithmEvaluator tae1;
	private final TargetAlgorithmEvaluator tae2;

	public EqualTargetAlgorithmEvaluatorTester(TargetAlgorithmEvaluator tae1, TargetAlgorithmEvaluator tae2)
	{
		this.tae1 = tae1;
		this.tae2 = tae2;
	}
	
	@Override
	public List<AlgorithmRunResult> evaluateRun(AlgorithmRunConfiguration run) {
		return this.evaluateRun(Collections.singletonList(run));	
	}

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs) {
		return evaluateRun(runConfigs, null);
	}

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		
		List<AlgorithmRunResult> runTae1 = tae1.evaluateRun(runConfigs, obs);
		List<AlgorithmRunResult> runTae2 = tae2.evaluateRun(runConfigs, obs);

		if(runTae1.size() != runTae2.size()) throw new IllegalStateException("Run sizes did not match");
		
		for(int i=0; i<runTae1.size(); i++)
		{
			if(!runTae1.get(i).equals(runTae2.get(i)))
			{
				throw new IllegalStateException(runTae1.get(i) + " did not equals " + runTae2.get(i) );
				
			}
			
			if(Math.abs(runTae1.get(i).getRuntime() - runTae2.get(i).getRuntime()) > 0.1) throw new IllegalStateException("Runtimes did not agree");
			if(runTae1.get(i).getResultSeed() != runTae2.get(i).getResultSeed()) throw new IllegalStateException("Result Seeds did not agree");
			if(Math.abs(runTae1.get(i).getQuality() - runTae2.get(i).getQuality()) > 0.1) throw new IllegalStateException("Quality did not agree");
			if(!runTae1.get(i).getRunStatus().equals(runTae2.get(i).getRunStatus())) throw new IllegalStateException("Run Results did not agree");
			
			
			
			
			
		}
		
		return runTae1;
	}

	@Override
	public int getRunCount() {

		if(tae1.getRunCount() != tae2.getRunCount()) throw new IllegalStateException("RunCount should have been the same between two target algorithm evaluators");
		return tae1.getRunCount();
	}

	@Override
	public int getRunHash() {
		if(tae1.getRunHash() != tae2.getRunHash())  throw new IllegalStateException("Run Hash should have been the same between two target algorithm evaluators");
		return tae1.getRunHash();
	}

	@Override
	public void seek(List<AlgorithmRunResult> runs) {
		tae1.seek(runs);
		tae2.seek(runs);

	}

	@Override
	public String getManualCallString(AlgorithmRunConfiguration runConfig) {
		return tae1.getManualCallString(runConfig);
	}

	@Override
	public void notifyShutdown() {
		tae1.notifyShutdown();
		tae2.notifyShutdown();
	}

	@Override
	public void evaluateRunsAsync(AlgorithmRunConfiguration runConfig, TargetAlgorithmEvaluatorCallback handler) {
		throw new UnsupportedOperationException("This TAE does not support Asynchronous Execution at the moment");
		
	}

	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			TargetAlgorithmEvaluatorCallback handler) {
				evaluateRunsAsync(runConfigs, handler, null);
			}

	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		//Drop obs if this is implemented
		throw new UnsupportedOperationException("This TAE does not support Asynchronous Execution at the moment");
	}

	@Override
	public boolean isRunFinal() {
		return false;
	}

	@Override
	public boolean areRunsPersisted() {
		return false;
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}


	/**
	 * Blocks waiting for all runs that have been invoked via evaluateRun or evaluateRunAsync to complete
	 * @throws UnsupportedOperationException - if the TAE does not support this operation 
	 */
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
	public int getNumberOfOutstandingEvaluations()
	{
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}

	@Override
	public int getNumberOfOutstandingBatches() {
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}

	@Override
	public int getNumberOfOutstandingRuns() {
		throw new UnsupportedOperationException(this.getClass().getCanonicalName() + " does NOT support waiting or observing the number of outstanding evaluations, you should probably wrap this TargetAlgorithmEvaluator with an instance of " + OutstandingEvaluationsTargetAlgorithmEvaluatorDecorator.class );
	}
	
	@Override
	public final void close()
	{
		this.notifyShutdown();
	}
}

/***
 * THis was hanging around somewhere probably just delete it if you see it
 * 
 * public class DebugTargetAlgorithmEvaluator implements TargetAlgorithmEvaluator {

	
	private TargetAlgorithmEvaluator tae1;
	private TargetAlgorithmEvaluator tae2;

	public DebugTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae1, TargetAlgorithmEvaluator tae2)
	{
		this.tae1 = tae1;
		this.tae2 = tae2;
	}
	@Override
	public List<AlgorithmRun> evaluateRun(RunConfig run) {
		List<AlgorithmRun> listA = tae1.evaluateRun(run);
		List<AlgorithmRun> listB = tae2.evaluateRun(run);
		
		
		assertEquals(listA.size(), listB.size());
		for(int i=0; i < listA.size(); i++)
		{
			assertEquals(listA.get(i).getRunConfig(), listB.get(i).getRunConfig());
			assertEquals(listA.get(i).getRuntime(), listB.get(i).getRuntime());
			assertEquals(listA.get(i).getRunResult(), listB.get(i).getRunResult());
		}
		return listA;
	}

	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs) {
		return evaluateRun(runConfigs,null);
	}
	@Override
	public List<AlgorithmRun> evaluateRun(List<RunConfig> runConfigs, CurrentRunStatusObserver obs) {
		List<AlgorithmRun> listA = tae1.evaluateRun(runConfigs, obs);
		List<AlgorithmRun> listB = tae2.evaluateRun(runConfigs, obs);
		
		
		assertEquals(listA.size(), listB.size());
		for(int i=0; i < listA.size(); i++)
		{
			assertEquals(listA.get(i).getRunConfig(), listB.get(i).getRunConfig());
			assertEquals(listA.get(i).getRuntime(), listB.get(i).getRuntime());
			assertEquals(listA.get(i).getRunResult(), listB.get(i).getRunResult());
		}
		return listA;
	}
	

	@Override
	public int getRunCount() {
		return assertEquals(tae1.getRunCount(),tae2.getRunCount());
	}

	@Override
	public int getRunHash() {

		return assertEquals(tae2.getRunHash(), tae2.getRunHash());
	}
	
	public Object assertEquals(Object a, Object b)
	{
		if(!a.equals(b))
		{
			throw new IllegalArgumentException("Not Equals");
		}
		return a;
		
	}

	public int assertEquals(int a, int b)
	{
		if(a != b)
		{
			throw new IllegalArgumentException("Not Equals");
		}
		
		return a;
	}
	
	public double assertEquals(double a, double b)
	{
		if( a != b ) 
		{
			throw new IllegalArgumentException("Not Equals");
		}
		return a;
	}
	@Override
	public void seek(List<AlgorithmRun> runs) {
		tae1.seek(runs);
		tae2.seek(runs);
	}
	
	
	@Override
	public String getManualCallString(RunConfig runConfig) {
		String callString = tae2.getManualCallString(runConfig);
		if(tae1.getManualCallString(runConfig).equals(callString))
		{
			return callString;
		} else
		{
			throw new IllegalArgumentException("Not Equals");
		}
	}
	@Override
	public void notifyShutdown() {
		tae1.notifyShutdown();
		tae2.notifyShutdown();	
	}
	@Override
	public void evaluateRunsAsync(RunConfig runConfig,
			TAECallback handler) {
		throw new UnsupportedOperationException("Not Implemented at the moment");
		
	}
	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			TAECallback handler) {
				evaluateRunsAsync(runConfigs, handler, null);
			}
	@Override
	public void evaluateRunsAsync(List<RunConfig> runConfigs,
			TAECallback handler, CurrentRunStatusObserver obs) {
		//If this is implemented simply drop the obs when passing to the wrapped TAEs
		throw new UnsupportedOperationException("Not Implemented at the moment");
		
	}
	@Override
	public boolean isRunFinal() {
		return false;
	}
	@Override
	public boolean areRunsPersisted() {
		return false;
	}
	@Override
	public boolean areRunsObservable() {
		return false;
	}

}
*/


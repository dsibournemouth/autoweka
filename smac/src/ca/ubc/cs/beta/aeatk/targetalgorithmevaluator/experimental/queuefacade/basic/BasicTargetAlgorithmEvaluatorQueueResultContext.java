package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.basic;

import java.util.List;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.general.TargetAlgorithmEvaluatorQueueResultContext;

@ThreadSafe
public class BasicTargetAlgorithmEvaluatorQueueResultContext implements TargetAlgorithmEvaluatorQueueResultContext {

	private volatile RuntimeException runtimeException;
	private volatile List<AlgorithmRunConfiguration> runConfigs;
	private volatile List<AlgorithmRunResult> runs;

	@Override
	public List<AlgorithmRunResult> getAlgorithmRuns() {
		return runs;
	}

	@Override
	public void setAlgorithmRuns(List<AlgorithmRunResult> runs) {
		this.runs = runs;
		
	}

	@Override
	public List<AlgorithmRunConfiguration> getRunConfigs() {
		return runConfigs;
	}

	@Override
	public void setRunConfigs(List<AlgorithmRunConfiguration> runConfigs) {
		this.runConfigs = runConfigs;
		
	}

	@Override
	public void setRuntimeException(RuntimeException t) {
		this.runtimeException=t;
		
	}

	@Override
	public RuntimeException getRuntimeException() {
		return this.runtimeException; 
	}

}

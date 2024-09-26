package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.general;

import java.util.List;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;

/**
 * Interface that needs to be implemented / subtyped to provide context
 * <p>
 * The point of this interface is it allows you to save along with the set of runs
 * other information you need to process them correctly without having to save them elsewhere.
 * <p>
 * A default implementation that provides no additional information is: 
 * {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueueResultContext}
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public interface TargetAlgorithmEvaluatorQueueResultContext {

	public List<AlgorithmRunResult> getAlgorithmRuns();
	
	public void setAlgorithmRuns(List<AlgorithmRunResult> runs);
	
	public List<AlgorithmRunConfiguration> getRunConfigs();
	
	public void setRunConfigs(List<AlgorithmRunConfiguration> runConfigs);
	
	public void setRuntimeException(RuntimeException t);
	
	public RuntimeException getRuntimeException();
}

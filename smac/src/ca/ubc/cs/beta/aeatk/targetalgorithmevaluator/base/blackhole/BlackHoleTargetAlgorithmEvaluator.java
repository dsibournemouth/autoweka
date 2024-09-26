package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.blackhole;

import java.util.List;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractAsyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;

@ThreadSafe
/***
 * Blackhole Target Algorithm Evaluator implementation
 * 
 * This {@link ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator} simply never completes
 * the runs, it can be useful for unit tests and other edge cases.
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class BlackHoleTargetAlgorithmEvaluator extends AbstractAsyncTargetAlgorithmEvaluator{

	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private final BlackHoleTargetAlgorithmEvaluatorOptions options;
	
	public BlackHoleTargetAlgorithmEvaluator(BlackHoleTargetAlgorithmEvaluatorOptions options) {

		this.options = options;
		if(options.warnings)
		{
			log.warn("Blackhole Target Algorithm Evaluator actually just drops runs so your application is probably just going to hang. ");
		}
		
	}

	@Override
	public void notifyShutdown() {
		//NO SHUTDOWN NEEDED;
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

	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs,
			TargetAlgorithmEvaluatorCallback taeCallback,
			TargetAlgorithmEvaluatorRunObserver runStatusObserver) {
		
			if(options.warnings)
			{
				log.warn("Blackhole has silently dropped a set of {} runs ", runConfigs.size());
			}
		
	}

}

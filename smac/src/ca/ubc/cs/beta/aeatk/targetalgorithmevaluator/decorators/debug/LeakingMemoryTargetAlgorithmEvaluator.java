package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;

import java.util.ArrayList;
import java.util.List;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;

/**
 * Leaks some amount of memory for every run
 * 
 * @author Steve Ramage 
 *
 */
@ThreadSafe
public class LeakingMemoryTargetAlgorithmEvaluator extends AbstractTargetAlgorithmEvaluatorDecorator {

	private final List<byte[]> leakedMemory = new ArrayList<byte[]>();
	private volatile long totalLeaked = 0;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	private static volatile int memoryToLeak = 1024;
	
	
	public static void leakMemoryAmount(int newAmount)
	{
		if(newAmount < 0) throw new IllegalArgumentException("I'm not supplying a user-friendly error to something that is designed to leak memory. Don't use this, or at the very least have the good sense to leak a positive amount of memory");
		memoryToLeak = newAmount;
	}
	
	public LeakingMemoryTargetAlgorithmEvaluator(TargetAlgorithmEvaluator tae) {
		super(tae);
	}

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		List<AlgorithmRunResult> runs = tae.evaluateRun(runConfigs, obs);
		leak(runConfigs.size());
		return runs;
	}


	@Override
	public void evaluateRunsAsync(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorCallback handler, TargetAlgorithmEvaluatorRunObserver obs) {
		tae.evaluateRunsAsync(runConfigs, handler, obs);
		leak(runConfigs.size());
	}

	private synchronized void leak(int size)
	{
		totalLeaked += size * memoryToLeak;
		leakedMemory.add(new byte[size * memoryToLeak]);
		if(leakedMemory.size () < 5 || Math.random() > 0.999)
		{
			log.warn("Leaking >= {} bytes of memory, total leaked: {} MB",size * memoryToLeak, totalLeaked/1024/1024);
		}
	}
	

	@Override
	protected void postDecorateeNotifyShutdown() {
		leakedMemory.clear();
		totalLeaked = 0;
	}
	
	
}

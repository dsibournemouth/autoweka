package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.preloaded;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.misc.associatedvalue.AssociatedValue;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;

public class PreloadedResponseTargetAlgorithmEvaluator extends AbstractSyncTargetAlgorithmEvaluator {
	
	
	private final Queue<AssociatedValue<RunStatus, Double>> myQueue;
	
	private final PreloadedResponseTargetAlgorithmEvaluatorOptions opts;
	
	
	public PreloadedResponseTargetAlgorithmEvaluator(Queue<AssociatedValue<RunStatus, Double>> myQueue, PreloadedResponseTargetAlgorithmEvaluatorOptions opts) {
		
		this.myQueue = 	myQueue;
		this.opts = opts;
		
		
	}

	@Override
	public boolean isRunFinal() {
		return true;
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
	protected void subtypeShutdown() {
		//Nothing necessary
	}
	
	@Override
	public synchronized List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs,
			TargetAlgorithmEvaluatorRunObserver obs) {
		List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>();
		for(AlgorithmRunConfiguration rc : runConfigs)
		{
	
			AssociatedValue<RunStatus, Double> v = myQueue.poll();
			if(v == null) throw new IllegalStateException("Error out of existing runs");

			runs.add(new ExistingAlgorithmRunResult(rc, v.getAssociatedValue() , v.getValue() , opts.runLength ,opts.quality, rc.getProblemInstanceSeedPair().getSeed(), opts.additionalRunData));
		}

		return runs;
		
	}
		
	

}

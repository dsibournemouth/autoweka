package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.debug;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.TrajectoryDivergenceException;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractForEachRunTargetAlgorithmEvaluatorDecorator;

/**
 * 
 * @author sjr
 *
 */
@ThreadSafe
public class RunHashCodeVerifyingAlgorithmEvalutor extends AbstractForEachRunTargetAlgorithmEvaluatorDecorator {

	private final Queue<Integer> runHashQueue;
	private int hashCodesOfRuns = 0;
	private int runNumber = 0;
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Marker runHash = MarkerFactory.getMarker("RUN_HASH");
	
	
	public RunHashCodeVerifyingAlgorithmEvalutor(TargetAlgorithmEvaluator tae) {
		this(tae, new LinkedList<Integer>());
	}
	
	public RunHashCodeVerifyingAlgorithmEvalutor(TargetAlgorithmEvaluator tae, Queue<Integer> runHashes) {
		super(tae);
		
		
		this.runHashQueue = runHashes;
		
		log.trace("Created with {} hash codes to verify", runHashQueue.size());
		if(runHashQueue.size() == 0)
		{
			outOfHashCodesDisplayed = true;
		}
	}
	

	boolean outOfHashCodesDisplayed = false;

	@Override
	public synchronized void seek(List<AlgorithmRunResult> runs)
	{
		super.seek(runs);
		processRuns(runs);
		
	}

	@Override
	protected synchronized AlgorithmRunResult processRun(AlgorithmRunResult run) {
		runNumber++;
		int hashCode = run.hashCode();
	
		hashCode =  (hashCode == Integer.MIN_VALUE) ? 0 : hashCode;  
		
		hashCodesOfRuns = (31*hashCodesOfRuns + Math.abs( hashCode)% 32452867) % 32452867 	; //Some prime around 2^25 (to prevent overflows in computation)
		log.trace(runHash, "Run Hash Codes:{} After {} runs",hashCodesOfRuns, runNumber);
		
		Integer expectedHashCode = runHashQueue.poll();
		if(expectedHashCode == null)
		{
			if(!outOfHashCodesDisplayed)
			{
				log.debug("No More Hash Codes To Verify");
				outOfHashCodesDisplayed = true;
			}
		} else if(hashCodesOfRuns != expectedHashCode)
		{
			throw new TrajectoryDivergenceException(expectedHashCode, hashCodesOfRuns, runNumber);
		} else
		{
			log.trace("Hash Code {} matched {}", expectedHashCode, hashCodesOfRuns);
		}
		return run;
	}

	@Override
	protected void postDecorateeNotifyShutdown() {
		//No cleanup necessary
	}
}

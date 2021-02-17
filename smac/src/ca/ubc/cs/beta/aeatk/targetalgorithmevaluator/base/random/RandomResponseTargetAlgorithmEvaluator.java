package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.jcip.annotations.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.AbstractSyncTargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ec.util.MersenneTwister;

/***
 * Random Target Algorithm Evaluator
 * 
 * Generates random responses to Run Configs
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class RandomResponseTargetAlgorithmEvaluator extends
		AbstractSyncTargetAlgorithmEvaluator {

	private final double scale;
	private final double trendCoefficient;
	private final double minValue;
	
	
	private boolean persistent;
	//Controls whether we will BREAK our TAE by shuffling the runs
	private boolean shuffleRuns;
	
	//private final Random rand;
	private final String additionalRunData;
	private final long sleepInternally;
	private final long seed;
	
	private static final Logger log = LoggerFactory.getLogger(RandomResponseTargetAlgorithmEvaluator.class);
	
	private final Random shuffleRand;
	public RandomResponseTargetAlgorithmEvaluator (RandomResponseTargetAlgorithmEvaluatorOptions options) {
		
		
		if(options.maxResponse - options.minResponse < 0)
		{
			throw new ParameterException("Maximum response must be greater than the minimum response");
		}
		this.scale = options.maxResponse - options.minResponse;
		this.minValue = options.minResponse;
		

		this.trendCoefficient = options.trendCoefficient;

		log.debug("Target Algorithm Evaluator initialized with seed: {} ", options.seed);
		this.seed = options.seed;
		this.shuffleRuns = options.shuffleResponses;
		this.shuffleRand = new MersenneTwister(seed);
		this.persistent = options.persistent;
		this.additionalRunData = options.additionalRunData;
		this.sleepInternally = options.sleepInternally;

	}

	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		List<AlgorithmRunResult> ar = new ArrayList<AlgorithmRunResult>(runConfigs.size());
		
		
		for(AlgorithmRunConfiguration rc : runConfigs)
		{
			Random rand = new MersenneTwister(seed ^ rc.getProblemInstanceSeedPair().getSeed());
			double time = Math.max(0.01, ((rand.nextDouble()*this.scale)  + this.minValue) + (this.trendCoefficient * this.getRunCount()));
			
			if(time >= rc.getCutoffTime())
			{
				ar.add(new ExistingAlgorithmRunResult(rc, RunStatus.TIMEOUT,  rc.getCutoffTime() ,-1,0, rc.getProblemInstanceSeedPair().getSeed(), this.additionalRunData));
			} else
			{
				ar.add(new ExistingAlgorithmRunResult(rc, RunStatus.SAT,  time ,-1,0, rc.getProblemInstanceSeedPair().getSeed(), this.additionalRunData));
			}
			this.runCount.incrementAndGet();
		}
		
		if(shuffleRuns)
		{
			Collections.shuffle(ar, shuffleRand);
		}
		
		if(sleepInternally > 0)
		{
			try {
				Thread.sleep(sleepInternally);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		return ar;
	}

	@Override
	public boolean isRunFinal() {
		return false;
	}

	@Override
	public boolean areRunsPersisted() {
		return persistent;
	}

	@Override
	protected void subtypeShutdown() {
		//No shutdown necessary
	}

	@Override
	public boolean areRunsObservable() {
		return false;
	}

}

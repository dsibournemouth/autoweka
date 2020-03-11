package ca.ubc.cs.beta.aeatk.initialization.table;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aeatk.misc.associatedvalue.Pair;
import ca.ubc.cs.beta.aeatk.objectives.ObjectiveHelper;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;
import ca.ubc.cs.beta.smac.executors.LoggerUtil;
import net.jcip.annotations.NotThreadSafe;


@NotThreadSafe
public class UnbiasChallengerInitializationProcedure implements InitializationProcedure {

	private final ThreadSafeRunHistory runHistory;
	private final ParameterConfiguration initialIncumbent;
	private final TargetAlgorithmEvaluator tae;
	private final UnbiasChallengerInitializationProcedureOptions opts;
	private final Logger log = LoggerFactory.getLogger(UnbiasChallengerInitializationProcedure.class);
	
	private final List<ProblemInstance> instances;
	private final InstanceSeedGenerator insc;
	private volatile ParameterConfiguration incumbent;
	
	private final double cutoffTime;
	private final SeedableRandomPool pool;
	
	private final ParameterConfigurationSpace configSpace;
	
	private final int numberOfChallengers;
	private final int numberOfRunsPerChallenger;
	
	
	private final ObjectiveHelper objHelp;
	private final double cpuTimeLimit ;
	private final AlgorithmExecutionConfiguration execConfig;

	public UnbiasChallengerInitializationProcedure(ThreadSafeRunHistory runHistory, ParameterConfiguration initialIncumbent, TargetAlgorithmEvaluator tae, AlgorithmExecutionConfiguration execConfig, UnbiasChallengerInitializationProcedureOptions opts, InstanceSeedGenerator insc, List<ProblemInstance> instances,  int maxIncumbentRuns , TerminationCondition termCond, double cutoffTime, SeedableRandomPool pool, boolean deterministicInstanceOrdering, ObjectiveHelper objHelp)
	{
		this.runHistory =runHistory;
		this.initialIncumbent = initialIncumbent;
		this.tae = tae;
		this.opts = opts;
		this.instances = instances;
		this.execConfig = execConfig;
		this.insc = insc;
		this.incumbent = initialIncumbent;
		//termCond;
		this.cutoffTime = cutoffTime;
		this.pool = pool;
		
		if(deterministicInstanceOrdering)
		{
			throw new IllegalArgumentException("Deterministic Instance Ordering is not supported at this time with the UnbiasChallengerInitializationProcedure");
		}
		
		this.configSpace = initialIncumbent.getParameterConfigurationSpace();
		
		this.cpuTimeLimit = opts.cpulimit;
		if(cpuTimeLimit <= 0)
		{
			throw new ParameterException("Time must be greater than zero");
		}
		//this.numberOfChallengers = opts.numberOfChallengers;
		//this.numberOfInstances = opts.numberOfInstances
		
		
		this.numberOfRunsPerChallenger = opts.numberOfRunsPerChallenger;
		this.numberOfChallengers = opts.numberOfChallengers;
		
		if(maxIncumbentRuns < numberOfRunsPerChallenger)
		{
			throw new ParameterException("Number of runs per challenger is less than the number permitted:" + maxIncumbentRuns);
		}
		
		if(!insc.allInstancesHaveSameNumberOfSeeds())
		{
			throw new ParameterException("All instances are required to have the same number of seeds available");
		}
		
		if(numberOfChallengers > this.configSpace.getUpperBoundOnSize())
		{
			throw new ParameterException("Too many challengers have been requested, configuration space size is at most " + this.configSpace.getUpperBoundOnSize() + " but we want to use " + numberOfChallengers);
		}
		
		
		if(numberOfChallengers > this.configSpace.getLowerBoundOnSize() / 10)
		{
			log.warn("Configuration space size ({}) isn't much bigger than the number of challengers we are using in initialization ({}), this isn't an error but depending on conditionality and forbidden rules, we may not be able to satisfy this requirement", configSpace.getLowerBoundOnSize(), numberOfChallengers);
		}
			
		
		if(numberOfRunsPerChallenger * numberOfChallengers <= 0)
		{
			throw new ParameterException("Challengers requested " + numberOfChallengers + " runsPerChallenger:" + numberOfRunsPerChallenger + " must both be positive");
		}
		this.objHelp = objHelp;
		
	}
	
	
	@Override
	public void run() {
                   
            LoggerUtil.log("SMAC Unbieased Challenger INITIALIZATION");
		try 
		{
			
		
			Random rand = pool.getRandom("UNBIASED_CHALLENGER_TABLE_INITIALIZATION");
		
			List<ProblemInstanceSeedPair> selectedPisps = getProblemInstanceSeedPairs(rand);
			
			Set<ProblemInstance> selectedPis = new HashSet<ProblemInstance>();
			
			for(ProblemInstanceSeedPair pisp : selectedPisps)
			{
				selectedPis.add(pisp.getProblemInstance());
			}
			
			Set<ParameterConfiguration> thetas = getParameterConfigurations(rand, Collections.singleton(this.initialIncumbent));		
			
			List<Pair<ProblemInstanceSeedPair, ParameterConfiguration>> pispConfigs = createPairs(rand, selectedPisps, thetas);
			
		
			//TargetAlgorithmEvaluatorQueueFacade<UnbiasedChallengerInitializationProcedureContext> tque =  new TargetAlgorithmEvaluatorQueueFacade<UnbiasedChallengerInitializationProcedureContext>(tae, true); 
			
			final List<AlgorithmRunResult> incumbentRuns = scheduleInitialIncumbent(selectedPisps);
			
			initializeRuns(pispConfigs);
			
			
			log.trace("Waiting for all outstanding evaluations to complete");
			tae.waitForOutstandingEvaluations();
			log.debug("All outstanding runs completed, inspecting best configuration");
			
			
			
			if(incumbentRuns.size() == 0)
			{
				throw new IllegalStateException("Expected Default Configuration to have finished running at this point");
			}
			
			try {
				runHistory.append(incumbentRuns);
			} catch (DuplicateRunException e) {
				throw new IllegalStateException(e);
			}
			
			
			
			ParameterConfiguration bestConfiguration = selectMinimumConfiguration(selectedPis, thetas);
			
			double minCost = runHistory.getEmpiricalCostUpperBound(bestConfiguration, selectedPis , this.cutoffTime);
			double incCost = runHistory.getEmpiricalCost(initialIncumbent, selectedPis, this.cutoffTime);
			
			if(incCost > minCost)
			{
				//log.debug("Challenger {} ({}) looks better than initial incumbent {} ({}) scheduling censored runs ( {} vs. {} )", runHistory.getThetaIdx(bestConfiguration),bestConfiguration, runHistory.getThetaIdx(initialIncumbent) , initialIncumbent,  minCost, incCost);

				Set<ProblemInstanceSeedPair> pispsToRun = new HashSet<ProblemInstanceSeedPair>();
				
				pispsToRun.addAll(runHistory.getEarlyCensoredProblemInstanceSeedPairs(bestConfiguration));
				
				List<AlgorithmRunConfiguration> rcs = new ArrayList<AlgorithmRunConfiguration>();
				for(ProblemInstanceSeedPair pisp : pispsToRun)
				{
					rcs.add(new AlgorithmRunConfiguration( pisp, this.cutoffTime, bestConfiguration,execConfig));
				}
				
				log.debug("Solved runs {} ", runHistory.getAlgorithmRunsExcludingRedundant(bestConfiguration));
				log.debug("Unsolved {}",rcs);
				//log.debug("Scheduling {} incomplete runs for {} ({}) ", rcs.size() , runHistory.getThetaIdx(bestConfiguration), bestConfiguration );
				
				List<AlgorithmRunResult> completedRuns = tae.evaluateRun(rcs);
				try {
					runHistory.append(completedRuns);
				} catch (DuplicateRunException e) {
					throw new IllegalStateException(e);
				}
				
				minCost = runHistory.getEmpiricalCostUpperBound(bestConfiguration, selectedPis , this.cutoffTime);
			}
			
			if(incCost > minCost)
			{
				log.debug("Best challengers performance on set is {} versus initial incumbent {}, setting new incumbent", minCost, incCost);
				this.incumbent = bestConfiguration;
				
			} else
			{
				
				log.debug("Best challengers performance on set is {} versus initial incumbent {}, leaving incumbent alone", minCost, incCost);
				this.incumbent = initialIncumbent;
			}
			
			//log.debug("Initialization procedure completed ({} total runs, total cpu time used {}), selected incumbent is {} ({})", this.runHistory.getAlgorithmRunDataExcludingRedundant().size(), this.runHistory.getTotalRunCost(), this.runHistory.getThetaIdx(this.incumbent), this.incumbent.getFriendlyIDHex());
			
		} catch(InterruptedException e)
		{
			Thread.currentThread().interrupt();
			this.incumbent = initialIncumbent;
			return;
			
		}
		
	}


	/**
	 * @param selectedPis
	 * @param thetas
	 * @return
	 */
	private ParameterConfiguration selectMinimumConfiguration(
			Set<ProblemInstance> selectedPis, Set<ParameterConfiguration> thetas) {
		double minCost = Double.MAX_VALUE;
		ParameterConfiguration bestConfiguration = null;
		
		for(ParameterConfiguration config : thetas)
		{
			double cost = runHistory.getEmpiricalCost(config, selectedPis , this.cutoffTime);
			
			if(bestConfiguration == null)
			{
				bestConfiguration = config;
			}
			
			if(minCost > cost)
			{
				bestConfiguration = config;
				minCost = cost;
			}
			
		}
		return bestConfiguration;
	}


	/**
	 * Preforms the bulk of the initialization of runs
	 * @param pispConfigs 	The pairs that will be initialized
	 * @throws InterruptedException
	 */
	private void initializeRuns(List<Pair<ProblemInstanceSeedPair, ParameterConfiguration>> pispConfigs) throws InterruptedException 
	{

		final BlockingQueue<Pair<ProblemInstanceSeedPair, ParameterConfiguration>> unSolvedPispThetasQueue = new LinkedBlockingQueue<Pair<ProblemInstanceSeedPair, ParameterConfiguration>>();
		
		unSolvedPispThetasQueue.addAll(pispConfigs);
		
		final Set<Pair<ProblemInstanceSeedPair, ParameterConfiguration>> solvedPisps = Collections.newSetFromMap(new ConcurrentHashMap<Pair<ProblemInstanceSeedPair, ParameterConfiguration>, Boolean>());
		
		final AtomicBoolean killNow = new AtomicBoolean(false);
		
		TargetAlgorithmEvaluatorRunObserver killAtTimeoutObserver = new TargetAlgorithmEvaluatorRunObserver()
		{

			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				
				if(killNow.get())
				{
					for(AlgorithmRunResult run : runs)
					{
						if(run.getRunStatus() == RunStatus.RUNNING)
						{
							log.trace("Killing run {} ", run);
							run.kill();
						}
					}
				}
				
			}
			
		};
		
		final int allPairsSize = pispConfigs.size();
		
		final Semaphore runCompleted = new Semaphore(unSolvedPispThetasQueue.size());
		
		
		/**
		 * Algorithm is as follows:
		 * 
		 *  while(true)
		 *  {
		 *  	for(each pispConfig )
		 *  	
		 *  
		 *  
		 *  
		 *  
		 *  }
		 * 
		 */
		
		Map<Pair<ProblemInstanceSeedPair, ParameterConfiguration>, Double> nextCutoffTime = new ConcurrentHashMap<Pair<ProblemInstanceSeedPair, ParameterConfiguration>, Double>();

		for(Pair<ProblemInstanceSeedPair, ParameterConfiguration> p : pispConfigs)
		{
			nextCutoffTime.put(p, Double.valueOf(-1));
		}
	
outOfInitialization:
		while(true)
		{
			
			//Submit everything until you can't submit any more.
			for(int i=0; i < pispConfigs.size(); i++)
			{
				
				while(!runCompleted.tryAcquire(1, TimeUnit.SECONDS));
				
				
				final Pair<ProblemInstanceSeedPair, ParameterConfiguration> pair = unSolvedPispThetasQueue.poll();
		
				if(pair == null)
				{					
					//Everything is solved so we are done
					if(solvedPisps.size() == allPairsSize)
					{
						log.trace("All runs are considered done");
						break outOfInitialization;
					}
					
					//Something finished, but we don't have anything unfinished, so we will continue;
					i--;
					continue;
				}

				TargetAlgorithmEvaluatorCallback taeCallback = new TargetAlgorithmEvaluatorCallback() {
					
					@Override
					public void onSuccess(List<AlgorithmRunResult> runs) {
						
						try {
							runHistory.append(runs);
						} catch (DuplicateRunException e) {
							throw new IllegalStateException(e);
						}

						//== Either it was decided or it has a run at kappaMax
						if(runs.get(0).getRunStatus().isDecided() || !runs.get(0).getAlgorithmRunConfiguration().hasCutoffLessThanMax())
						{
							if(runs.get(0).getRunStatus().isDecided())
							{
								log.debug("Run completed successfully: {} " ,runs.get(0));
							}
							solvedPisps.add(pair);
						} else
						{
							unSolvedPispThetasQueue.add(pair);
						}
						
						runCompleted.release();
					}
					
					@Override
					public void onFailure(RuntimeException e) {
						log.error("Error occurred during initialization", e);
						
					}
				};
				
				double kappa = getNextKappa(pair,allPairsSize,solvedPisps.size(),nextCutoffTime.get(pair));
				
				if(runHistory.getTotalRunCost() > this.cpuTimeLimit)
				{
					
					//=== Expressly log the date, in case the logger timestamps are behind.
					SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.SSS");
					//log.debug("Initialization procedure has used {}, time limit: {}, halting procedure at {}...", runHistory.getTotalRunCost(), this.cpuTimeLimit, sdf.format(new Date()));
					
					killNow.set(true);
					
					break outOfInitialization;
				}
				
				if(kappa < 0)
				{
					throw new IllegalStateException("Still continuing with run, but the kappa time is zero.");
				}
				
				nextCutoffTime.put(pair, kappa);
				tae.evaluateRunsAsync(Collections.singletonList(new AlgorithmRunConfiguration(pair.getFirst(), kappa, pair.getSecond(), execConfig)), taeCallback, killAtTimeoutObserver);
			
				
			}
		
		}
	}


	/**
	 * 
	 * @param pair
	 * @param allPairsSize
	 * @param solvedPisSize
	 * @param lastKappa
	 * @return
	 */
	public double getNextKappa(Pair<ProblemInstanceSeedPair, ParameterConfiguration> pair, int allPairsSize, int solvedPisSize, double lastKappa)
	{
		
		double kappa;
		
		double kappaFromShareRemaining = (this.cpuTimeLimit - this.runHistory.getTotalRunCost())/(allPairsSize-solvedPisSize);
		double kappaNextStep = 2*lastKappa;
		double kappaMax = this.cutoffTime;
		kappa = Math.min(Math.max(kappaNextStep, kappaFromShareRemaining), kappaMax);
	
		return Math.max(kappa,1);
		
		
	}
	
	@Override
	public ParameterConfiguration getIncumbent() {
		return incumbent;
	}	
	
	/**
	 * Schedules the runs for the initial incumbent
	 * @param selectedPisps
	 * @return
	 */
	private List<AlgorithmRunResult> scheduleInitialIncumbent(List<ProblemInstanceSeedPair> selectedPisps) 
	{
		
		final List<AlgorithmRunResult> incumbentRuns = Collections.synchronizedList(new ArrayList<AlgorithmRunResult>(selectedPisps.size()));
		
		List<AlgorithmRunConfiguration> rcs = new ArrayList<AlgorithmRunConfiguration>(selectedPisps.size());
		
		for(ProblemInstanceSeedPair pisp : selectedPisps)
		{
			rcs.add(new AlgorithmRunConfiguration(pisp, cutoffTime, initialIncumbent,execConfig));
		}
		
		runHistory.getOrCreateThetaIdx(initialIncumbent);
		log.debug("Scheduling {} runs for initial configuration", rcs.size());
		tae.evaluateRunsAsync(rcs, new TargetAlgorithmEvaluatorCallback() {

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {
				log.debug("Default configuration runs are done");
				incumbentRuns.addAll(runs);
			}

			@Override
			public void onFailure(RuntimeException e) {
				log.error("Error occurred during initialization", e);
				
			}
			
		});
		return incumbentRuns;
	}
	

	/**
	 * Creates a shuffled list of pair objects
	 * 
	 * @param rand
	 * @param selectedPisps
	 * @param thetas
	 * @return
	 */
	private List<Pair<ProblemInstanceSeedPair, ParameterConfiguration>> createPairs(Random rand, List<ProblemInstanceSeedPair> selectedPisps,Set<ParameterConfiguration> thetas) {
		List<Pair<ProblemInstanceSeedPair, ParameterConfiguration>> pispConfigs = new ArrayList<Pair<ProblemInstanceSeedPair, ParameterConfiguration>>(thetas.size() * selectedPisps.size() );
		for(ParameterConfiguration config : thetas)
		{
			for(ProblemInstanceSeedPair pisp : selectedPisps)
			{
				pispConfigs.add(new Pair<ProblemInstanceSeedPair, ParameterConfiguration>(pisp, config));
			}
		}
		
		Collections.shuffle(pispConfigs,rand);
		return pispConfigs;
	}


	/**
	 * Returns a set of distinct parameter configurations
	 * 
	 * @param rand
	 * @return
	 */
	private Set<ParameterConfiguration> getParameterConfigurations(Random rand, Set<ParameterConfiguration> excluded) {
		log.debug("Generating {} configurations for use in initialization", this.numberOfChallengers);
		Set<ParameterConfiguration> thetas = new HashSet<ParameterConfiguration>(this.numberOfChallengers);
		
		thetas.addAll(excluded);
		//=== Create Thetas for table
		while((thetas.size() -  excluded.size()) < numberOfChallengers )
		{
			boolean created = false;
			for(int i=0; i < 1000; i++)
			{
				ParameterConfiguration config = configSpace.getRandomParameterConfiguration(rand);

				if(config.equals(configSpace.getDefaultConfiguration()))
				{
					i--;
					continue;
				}
					
				boolean newConfiguration = thetas.add(config);
				
				if(newConfiguration)
				{
					created = true;
					break;
				}
			}
			
			if(!created)
			{
				throw new IllegalStateException("After 1000 attempts we were unable to generate another unique configuration. We already have " + thetas.size() + " this can happen if you request too many configurations in initialization, or if the space is incredibly conditional / has many forbidden configurations." );
			}
			
		}
		thetas.removeAll(excluded);
		return thetas;
	}


	/**
	 * Returns a set of problem instance seed pairs. The most frequently occurring instance should only occur at most once more than the least frequently occuring.
	 * @param rand
	 * @return List of problem instance seed pairs
	 */
	private List<ProblemInstanceSeedPair> getProblemInstanceSeedPairs(Random rand) {
		List<ProblemInstanceSeedPair> selectedPisps = new ArrayList<ProblemInstanceSeedPair>(); 
		
		log.debug("Generating {} Problem Instance Seed Pairs for use in initialization", numberOfRunsPerChallenger);
		

		List<ProblemInstance> shuffledPis = new ArrayList<ProblemInstance>(instances);
		
		Collections.shuffle(shuffledPis,rand);
		
		//=== Create PISPS for table
		while(selectedPisps.size() < numberOfRunsPerChallenger)
		{
			for(ProblemInstance pi : shuffledPis)
			{
				
				if(!insc.hasNextSeed(pi))
				{
					throw new IllegalStateException("Should not have been able to generate this many requests for configurations");
				}
				
				long seed = insc.getNextSeed(pi);
				ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi, seed);
				selectedPisps.add(pisp);
				
				if(selectedPisps.size() >= numberOfRunsPerChallenger)
				{
					break;
				}
				
			}
		}
		return selectedPisps;
	}

    @Override
    public int getInitConfigIndex() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setInitConfigIndex(int initConfigIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setIncumbent(ParameterConfiguration incumbent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
	

}

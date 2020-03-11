package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.portfolio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunStatus;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.RunningAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.kill.KillHandler;
import ca.ubc.cs.beta.aeatk.misc.associatedvalue.Pair;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/**
 * Tries to solve the target algorithm using a portfolio of other solvers & configurations.
 * 
 * For every run that comes in, a new set of runs is created with the associated parameter solvers & configurations 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>, Alexandre Fréchette <afrechet@cs.ubc.ca>
 *
 */
public class PortfolioTargetAlgorithmEvaluatorDecorator extends	AbstractTargetAlgorithmEvaluatorDecorator {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
	private final List<Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>> fPortfolio;
	private final RunObjective fRunObj;
	
	private final boolean fSubmitOriginalRun;
	
	private final PortfolioRunKillingPolicy fPortfolioRunKillingPolicy;
	
	/**
	 * Construct a portfolio consisting of the given configurations.
	 * @param aTAE - TAE to use to execute each portfolio run.
	 * @param aParamConfigs - list of configurations to use.
	 * @param aRunObj - the VBS portfolio run objective (used to select the best run out of the portfolio runs).
	 * @param aSubmitOriginalRun - whether to submit the original run in addition to the portfolio runs.
	 * @param aPortfolioRunKillingPolicy - the killing policy to use on portfolio runs.
	 * @return a portfolio target algorithm evaluator decorator initialized with the given parameters.
	 */
	public static PortfolioTargetAlgorithmEvaluatorDecorator constructParamConfigPortfolioTargetAlgorithmEvaluatorDecorator(
            TargetAlgorithmEvaluator aTAE,
            List<ParameterConfiguration> aParamConfigs,
            RunObjective aRunObj,
            boolean aSubmitOriginalRun,
            PortfolioRunKillingPolicy aPortfolioRunKillingPolicy
            )
    {
        List<Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>> portfolio = new ArrayList<Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>>();
    
        for(ParameterConfiguration paramConfig : aParamConfigs)
        {
            portfolio.add(new Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>(null,paramConfig));
        }
        
        return new PortfolioTargetAlgorithmEvaluatorDecorator(aTAE, portfolio, aRunObj, aSubmitOriginalRun,aPortfolioRunKillingPolicy);
    }   
	
	/**
     * Construct a portfolio consisting of the given solvers on their default configurations.
     * @param aTAE - TAE to use to execute each portfolio run.
     * @param aParamConfigs - list of solvers to use.
     * @param aRunObj - the VBS portfolio run objective (used to select the best run out of the portfolio runs).
     * @param aSubmitOriginalRun - whether to submit the original run in addition to the portfolio runs.
     * @param aPortfolioRunKillingPolicy - the killing policy to use on portfolio runs.
     * @return a portfolio target algorithm evaluator decorator initialized with the given parameters.
     */
    public static PortfolioTargetAlgorithmEvaluatorDecorator constructExecConfigPortfolioTargetAlgorithmEvaluatorDecorator(
            TargetAlgorithmEvaluator aTAE,
            List<AlgorithmExecutionConfiguration> aExecConfig,
            RunObjective aRunObj,
            boolean aSubmitOriginalRun,
            PortfolioRunKillingPolicy aPortfolioRunKillingPolicy
            )
    {
        List<Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>> portfolio = new ArrayList<Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>>();
    
        for(AlgorithmExecutionConfiguration execConfig : aExecConfig)
        {
            portfolio.add(new Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>(execConfig,execConfig.getParameterConfigurationSpace().getDefaultConfiguration()));
        }
        
        return new PortfolioTargetAlgorithmEvaluatorDecorator(aTAE, portfolio, aRunObj, aSubmitOriginalRun, aPortfolioRunKillingPolicy);
    }  
	
    /**
     * Construct a portfolio consisting of the given solvers with their corresponding given configurations. 
     * @param aTAE - TAE to use to execute each portfolio run.
     * @param aParamConfigs - list of solvers to use.
     * @param aRunObj - the VBS portfolio run objective (used to select the best run out of the portfolio runs).
     * @param aSubmitOriginalRun - whether to submit the original run in addition to the portfolio runs.
     * @param aPortfolioRunKillingPolicy - the killing policy to use on portfolio runs.
     */
	public PortfolioTargetAlgorithmEvaluatorDecorator(
	        TargetAlgorithmEvaluator aTAE,
	        List<Pair<AlgorithmExecutionConfiguration,ParameterConfiguration>> aPortfolio,
	        RunObjective aRunObj,
	        boolean aSubmitOriginalRun,
	        PortfolioRunKillingPolicy aPortfolioRunKillingPolicy
	        )
	{
	    super(aTAE);
	    
	    //Verify portfolio.
	    if(aPortfolio.size() == 0)
        {
            throw new IllegalArgumentException("Portfolio must contain at least one configuration");
        }
	    
	    if(new HashSet<>(aPortfolio).size() != aPortfolio.size())
	    {
	        throw new IllegalArgumentException("Portfolio must be constructed with distinct execution configuration / parameter configuration pairs, duplicates detected.");
	    }
	    
	    for(Pair<AlgorithmExecutionConfiguration,ParameterConfiguration> portfolioEntry : aPortfolio)
	    {
	        AlgorithmExecutionConfiguration execConfig = portfolioEntry.getFirst();
	        ParameterConfiguration paramConfig = portfolioEntry.getSecond();
	        
	        if(paramConfig == null)
            {
                throw new IllegalArgumentException("Portfolio parameter configuration cannot be null.");
            }
	        else if(paramConfig.isForbiddenParameterConfiguration())
	        {
	            throw new IllegalArgumentException("A portfolio entry contains a forbidden param configuration.");
	        }
	        else if(execConfig != null && execConfig.getParameterConfigurationSpace().getParameterConfigurationFromString(paramConfig.getFormattedParameterString(ParameterStringFormat.NODB_SYNTAX), ParameterStringFormat.NODB_SYNTAX).isForbiddenParameterConfiguration())
	        {
	            throw new IllegalArgumentException("A portfolio entry contains an incompatible exec config / param config pair.");
	        }
	    }
	    
	    this.fPortfolio = aPortfolio;
	    
	    this.fRunObj = aRunObj;
        
	    this.fSubmitOriginalRun = aSubmitOriginalRun;
	    
	    this.fPortfolioRunKillingPolicy = aPortfolioRunKillingPolicy;
	    
	}
	
	
	@Override
	public final List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(runConfigs, this, obs);
	}
	
	
	@Override
	public final void evaluateRunsAsync(
	        final List<AlgorithmRunConfiguration> aOriginalRunConfigs,
	        final TargetAlgorithmEvaluatorCallback aHandler,
	        final TargetAlgorithmEvaluatorRunObserver aObserver
	        )
	{
		
		/** WHAT DOES THIS COMMENT MEAN? **/
		//We need to make sure wrapped versions are called in the same order
		//as there unwrapped versions.
		
		if(aOriginalRunConfigs.isEmpty())
		{
			aHandler.onSuccess(Collections.<AlgorithmRunResult> emptyList());
			return;
		}
		
		final int portfolioRunsPerOriginalRun = fSubmitOriginalRun ? fPortfolio.size()+1 : fPortfolio.size();
		
		/**
		 * Note that for every run we need to do say runs [1,2,3,4] we are submitting them in the following order
		 * [1, 1A, 1B, 1C, 1D, 1E, 2, 2A, 2B, 2C, 2D, 2E, ...]
		 * 
		 */
		//TODO: This class should have an option and should work in all cases correctly when you do and do not
		//want the original run to be submitted.
		final List<AlgorithmRunConfiguration> portfolioRunConfigs = new ArrayList<AlgorithmRunConfiguration>();
		
		for(AlgorithmRunConfiguration rc : aOriginalRunConfigs)
		{
		    if(fSubmitOriginalRun)
		    {
		        //Adding original run
		        portfolioRunConfigs.add(rc);
		    }
			
			//Adding other portfolio runs run.
			for(Pair<AlgorithmExecutionConfiguration,ParameterConfiguration> portfolioEntry : fPortfolio)
			{
			    AlgorithmExecutionConfiguration execConfig = portfolioEntry.getFirst();
	            AlgorithmExecutionConfiguration runExecConfig = execConfig != null ? execConfig : rc.getAlgorithmExecutionConfiguration();
	            
	            ParameterConfiguration runParamConfig = portfolioEntry.getSecond();
	            
	            if(runExecConfig.getParameterConfigurationSpace().getParameterConfigurationFromString(runParamConfig.getFormattedParameterString(ParameterStringFormat.NODB_SYNTAX), ParameterStringFormat.NODB_SYNTAX).isForbiddenParameterConfiguration())
	            {
	                throw new IllegalArgumentException("Substituting paramater config and exec config for run ended in a forbidden configuration.");
	            }
	            
	            portfolioRunConfigs.add(new AlgorithmRunConfiguration(
				        rc.getProblemInstanceSeedPair(),
				        rc.getCutoffTime(),
				        runParamConfig,
				        runExecConfig
				        ));
			}
		}
		
		if(new HashSet<>(portfolioRunConfigs).size() != portfolioRunConfigs.size())
		{
		    throw new IllegalStateException("Portfolio TAE decorator would submit duplicated runs.");
		}
		else if(portfolioRunConfigs.size() != portfolioRunsPerOriginalRun * aOriginalRunConfigs.size())
		{
		    throw new IllegalStateException("The number of runs to submit is not a multiple of the portfolio size.");
		}
		
		TargetAlgorithmEvaluatorCallback myHandler = getPortfolioCallback(aHandler, aOriginalRunConfigs, portfolioRunsPerOriginalRun);
		TargetAlgorithmEvaluatorRunObserver myObs = getPortfolioObserver(aObserver, aOriginalRunConfigs, portfolioRunsPerOriginalRun);
		
		log.trace("Portfolio request translated runs {} to {} ", aOriginalRunConfigs.size(), portfolioRunConfigs.size() );
		
		tae.evaluateRunsAsync(portfolioRunConfigs, myHandler, myObs);

	}
	
	/**
	 * @param aOriginalCallback - the original callback. 
	 * @param aOriginalRunConfigs - the original runs.
	 * @param aPortfolioRunsPerOriginalRun - the number of portfolio runs per original run.
	 * @return the portfolio callback in charge of calling the original callback on the compiled portfolio runs.
	 */
	private TargetAlgorithmEvaluatorCallback getPortfolioCallback(
	        final TargetAlgorithmEvaluatorCallback aOriginalCallback, 
	        final List<AlgorithmRunConfiguration> aOriginalRunConfigs,
	        final int aPortfolioRunsPerOriginalRun
	        )
	{
	    return new TargetAlgorithmEvaluatorCallback()
        {
            private final TargetAlgorithmEvaluatorCallback fHandler = aOriginalCallback;

            @Override
            public void onSuccess(List<AlgorithmRunResult> runs) 
            {   
                if(runs.size() != aOriginalRunConfigs.size() * aPortfolioRunsPerOriginalRun)
                {
                    throw new IllegalArgumentException("Provided "+runs.size()+" runs, not all the "+aOriginalRunConfigs.size()+"*"+aPortfolioRunsPerOriginalRun+" (="+aOriginalRunConfigs.size()*aPortfolioRunsPerOriginalRun+") portfolio runs submitted (from "+aOriginalRunConfigs.size()+" original runs).");
                }
                
                List<AlgorithmRunResult> listToReturn = new ArrayList<>();
                        
                for(int i=0; i < aOriginalRunConfigs.size(); i ++)
                {
                    /**
                    * We assume that our returned runs for this batch are in [i, i+portfolioSize+1)
                    * For instance if we are asked for 4 runs [1,2,3,4] , then we will submit 16 in order of [1A, 1B, 1C, 1D, 2A, 2B, 2C 2D,...]-
                    * So to determine the result of the portfolio we should look at runs nA, nB, nC, nD,...
                    * 
                    * getAlgorithmRunForCallback handles the logic for merging the runs
                    **/
                    listToReturn.add(getAlgorithmRunForCallback(aOriginalRunConfigs.get(i),runs.subList(i*aPortfolioRunsPerOriginalRun, (i+1)*aPortfolioRunsPerOriginalRun)));
                }
                fHandler.onSuccess(listToReturn);
            }
            
            private AlgorithmRunResult getAlgorithmRunForCallback(final AlgorithmRunConfiguration originalRunConfig, final List<AlgorithmRunResult> runs)
            {
                //Run with lowest objective
                AlgorithmRunResult bestRun = runs.get(0);                
                
                for(AlgorithmRunResult run : runs)
                {
                    if(fRunObj.getObjective(run) < fRunObj.getObjective(bestRun))
                    {
                        bestRun = run;
                    }
                }
                
                if (fPortfolioRunKillingPolicy.equals(PortfolioRunKillingPolicy.SLOWERDIES)) {

                    //Run with lowest objective that didn't time out.
                    AlgorithmRunResult bestSolvedRun = null;

                    for(AlgorithmRunResult run : runs)
                    {
                        if(!run.isCensoredEarly())
                        {
                            if(bestSolvedRun == null)
                            {
                                bestSolvedRun = run;
                            } 
                            else
                            {
                                if(fRunObj.getObjective(run) < fRunObj.getObjective(bestSolvedRun))
                                {
                                    bestSolvedRun = run;
                                }
                            }
                        }
                    }


                    if(bestSolvedRun != null && !bestSolvedRun.equals(bestRun))
                    {
                        //log.error("Best solved run {} doesn't equal best run {} in responses: {} ", bestSolvedRun, bestRun, runs );
                        throw new IllegalStateException("Values that are killed or timeout are better than those that are solved");
                    }

                }
                
                log.trace("Best run is {} out of {}", bestRun, runs);
                
                String additionalRunData = bestRun.getAdditionalRunData().isEmpty() ? "Returned from from portfolio, configuration: " + bestRun.getAlgorithmRunConfiguration().getParameterConfiguration().getFriendlyID() + " my performance: " : bestRun.getAdditionalRunData(); 
                
                return new ExistingAlgorithmRunResult(
                        originalRunConfig,
                        bestRun.getRunStatus(),
                        bestRun.getRuntime(),
                        bestRun.getRunLength(),
                        bestRun.getQuality(),
                        bestRun.getResultSeed(),
                        additionalRunData,
                        bestRun.getWallclockExecutionTime());
            }
            @Override
            public void onFailure(RuntimeException t) {
                fHandler.onFailure(t);
            }
        };
	}
	
	
    /**
     * @param aOriginalObserver - the original observer. 
     * @param aOriginalRunConfigs - the original runs.
     * @param aPortfolioRunsPerOriginalRun - the number of portfolio runs per original run.
     * @return the portfolio observer in charge of calling the original observer on the compiled portfolio runs, as well as pre-emptively killing portfolio runs when other corresponding runs are better.
     */
	private TargetAlgorithmEvaluatorRunObserver getPortfolioObserver(
            final TargetAlgorithmEvaluatorRunObserver aOriginalObserver, 
            final List<AlgorithmRunConfiguration> aOriginalRunConfigs,
            final int aPortfolioRunsPerOriginalRun
            )
	{
	    return new TargetAlgorithmEvaluatorRunObserver()
        {
	        
            Set<AlgorithmRunResult> killedRuns = Collections.newSetFromMap(new ConcurrentHashMap<AlgorithmRunResult,Boolean>()); 
            @Override
            public void currentStatus(final List<? extends AlgorithmRunResult> runs) {
                
                if(runs.size() != aOriginalRunConfigs.size() * aPortfolioRunsPerOriginalRun)
                {
                    throw new IllegalArgumentException("Provided "+runs.size()+" runs, not all the "+aOriginalRunConfigs.size()+"*"+aPortfolioRunsPerOriginalRun+" (="+aOriginalRunConfigs.size()*aPortfolioRunsPerOriginalRun+") portfolio runs submitted (from "+aOriginalRunConfigs.size()+" original runs).");
                }
                
                List<AlgorithmRunResult> kRunsToClient = new ArrayList<AlgorithmRunResult>();
                                
                for(int i=0; i < aOriginalRunConfigs.size(); i++)
                {
                    /**
                     * See similar line in callback, we basically split the runs into all the runs for the requested run config.
                     */
                    kRunsToClient.add(getAlgorithmRunForPortfolioObserver(aOriginalRunConfigs.get(i),runs.subList(i*aPortfolioRunsPerOriginalRun, (i+1)*aPortfolioRunsPerOriginalRun))); 
                }
                
                if(aOriginalObserver != null)
                {
                    aOriginalObserver.currentStatus(kRunsToClient);
                }
            }
            
            private AlgorithmRunResult getAlgorithmRunForPortfolioObserver(final AlgorithmRunConfiguration originalRunConfig, final List<? extends AlgorithmRunResult> runs)
            {
                boolean atleastOneRunStillRunning = false;

                //Best run that is solved
                AlgorithmRunResult bestSolvedRun = null;
                
                //Run with lowest run objective
                AlgorithmRunResult bestRun = runs.get(0);
                
                for(AlgorithmRunResult krun : runs)
                {
                    if(krun.isRunCompleted())
                    {
                        if( (bestSolvedRun == null) || (fRunObj.getObjective(krun) < fRunObj.getObjective(bestSolvedRun)))
                        {
                            bestSolvedRun = krun;
                        }
                    } else
                    {
                        atleastOneRunStillRunning = true;
                    }
                    
                    if(fRunObj.getObjective(krun) < fRunObj.getObjective(bestRun))
                    {
                        bestRun = krun;
                    }
                    
                }
                    
                if(bestSolvedRun != null)
                {
                    for(AlgorithmRunResult krun : runs)
                    {
                        if(krun.getRunStatus().equals(RunStatus.RUNNING))
                        {
                            if(fPortfolioRunKillingPolicy.killRun(krun, bestSolvedRun, fRunObj))
                            {
                                if(killedRuns.add(krun))
                                {
                                    log.trace("Run {} seems to be dominated by run {}, killing...", krun, bestSolvedRun);
                                }
                                krun.kill();
                            }
                        }
                    }
                }
                

                
                if(atleastOneRunStillRunning)
                {
                    KillHandler kh = new KillHandler()
                    {
                        AtomicBoolean bKill = new AtomicBoolean(false);

                        @Override
                        public void kill() {
                            bKill.getAndSet(true);
                            
                            for(AlgorithmRunResult run : runs)
                            {
                                run.kill();
                            }
                        }

                        @Override
                        public boolean isKilled() {
                            return bKill.get();
                            
                        }
                        
                    };
                    return new RunningAlgorithmRunResult(originalRunConfig, bestRun.getRuntime(), bestRun.getRunLength(), bestRun.getQuality(), bestRun.getResultSeed(), bestRun.getWallclockExecutionTime(), kh);
                } else
                {
                    return new ExistingAlgorithmRunResult(originalRunConfig, bestRun.getRunStatus(), bestRun.getRuntime(), bestRun.getRunLength(), bestRun.getQuality(), bestRun.getResultSeed(), bestRun.getWallclockExecutionTime());
                }
            }
        };
	}
		
	@Override
	protected void postDecorateeNotifyShutdown() {
		//Not shutdown necessary.
	}	

}

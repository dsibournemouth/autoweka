package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.functionality.portfolio;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;

/**
 * Strategy enum for how portfolio runs are killed on observation.
 * @author afrechet
 */
public enum PortfolioRunKillingPolicy 
{
    
    /**
     * Killing policy where the slowest (RUNNING) runs are killed. Say run objective is runtime. Then if
     * a run terminated at 10s, then a running run of with runtime of 30s will be killed, but one with runtime of 5s won't.
     * The idea is to find the absolute fastest run in the portfolio.
     */
    SLOWERDIES
    {
        @Override
        public boolean killRun(AlgorithmRunResult aRun,
                AlgorithmRunResult aBestSolvedRun,
                RunObjective aRunObjective) 
        {
            return (aRunObjective.getObjective(aRun) > aRunObjective.getObjective(aBestSolvedRun));
        }
    },
    /**
     * Killing policy where a run must be killed if a solved run already exists. The idea is for the portfolio to solve
     * an instance "as fast as possible".
     */
    FIRSTSURVIVES
    {
        @Override
        public boolean killRun(AlgorithmRunResult aRun,
                AlgorithmRunResult aBestSolvedRun,
                RunObjective aRunObjective) 
        {
            return !aRun.equals(aBestSolvedRun);
        }
    };
    
    /**
     * @param aRun - run to be evaluated.
     * @param aBestSolvedRun - best solved run by the portfolio.
     * @param aRunObjective - the run objective the portfolio is optimized for.
     * @return true iff the run must be killed.
     */
    public abstract boolean killRun(AlgorithmRunResult aRun, AlgorithmRunResult aBestSolvedRun, RunObjective aRunObjective);
    
}

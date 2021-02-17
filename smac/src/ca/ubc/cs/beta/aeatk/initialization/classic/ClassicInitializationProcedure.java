package ca.ubc.cs.beta.aeatk.initialization.classic;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.exceptions.OutOfTimeException;
import ca.ubc.cs.beta.aeatk.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistoryHelper;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aeatk.smac.SMACOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;
import ca.ubc.cs.beta.smac.executors.LoggerUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

public class ClassicInitializationProcedure implements InitializationProcedure {

    private final ThreadSafeRunHistory runHistory;
    private final ParameterConfiguration initialIncumbent;
    private final TargetAlgorithmEvaluator tae;
    private final ClassicInitializationProcedureOptions opts;
    private final Logger log = LoggerFactory.getLogger(ClassicInitializationProcedure.class);
    private final int maxIncumbentRuns;
    private final List<ProblemInstance> instances;
    private final InstanceSeedGenerator insc;
    private ParameterConfiguration incumbent;
    private final TerminationCondition termCond;
    private final double cutoffTime;
    private final SeedableRandomPool pool;
    private boolean deterministicInstanceOrdering;
    private final AlgorithmExecutionConfiguration algorithmExecutionConfig;
    private List<AlgorithmRunResult> algorithmRunResults;
    
    
    public ClassicInitializationProcedure(ThreadSafeRunHistory runHistory, ParameterConfiguration initialIncumbent, TargetAlgorithmEvaluator tae, ClassicInitializationProcedureOptions opts, InstanceSeedGenerator insc, List<ProblemInstance> instances, int maxIncumbentRuns, TerminationCondition termCond, double cutoffTime, SeedableRandomPool pool, boolean deterministicInstanceOrdering, AlgorithmExecutionConfiguration execConfig) {

        this.runHistory = runHistory;
        this.initialIncumbent = initialIncumbent;
        this.initialIncumbent.lock();
        this.tae = tae;
        this.opts = opts;
        this.instances = instances;
        this.maxIncumbentRuns = maxIncumbentRuns;
        this.insc = insc;
        this.incumbent = initialIncumbent;
        this.termCond = termCond;
        this.cutoffTime = cutoffTime;
        this.pool = pool;
        this.deterministicInstanceOrdering = deterministicInstanceOrdering;
        this.algorithmExecutionConfig = execConfig;

    }

    @Override
    public void run() {

        LoggerUtil.log("SMAC CLASSIC INITIALIZATION");
        log.debug("Using Classic Initialization");

        //ParameterConfiguration incumbent = this.initialIncumbent;
        // ParameterConfiguration incumbent = listOfParameterConfigurations.get(initConfigIndex);
        log.trace("Configuration Set as Incumbent: {}", incumbent);

        //iteration = 0;
        int N = opts.initialIncumbentRuns;

        LoggerUtil.log("opts.initialIncumbentRuns: " + opts.initialIncumbentRuns);
        N = Math.min(N, insc.getInitialInstanceSeedCount());

        LoggerUtil.log("insc.getInitialInstanceSeedCount(): " + insc.getInitialInstanceSeedCount());

        N = Math.min(N, maxIncumbentRuns);
        LoggerUtil.log("maxIncumbentRuns: " + maxIncumbentRuns);

        log.debug("Scheduling default configuration for {} runs", N);

        // List<AlgorithmRunConfiguration> listOfIncumbentRunConfigs = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            /**
             * Evaluate Default Configuration
             */
            // incumbent = listOfParameterConfigurations.get(initConfigIndex);

            ProblemInstanceSeedPair pisp = RunHistoryHelper.getRandomInstanceSeedWithFewestRunsFor(runHistory, insc, incumbent, instances, pool.getRandom("CLASSIC_INITIALIZATION"), deterministicInstanceOrdering);

            AlgorithmRunConfiguration incumbentRunConfig = new AlgorithmRunConfiguration(pisp, cutoffTime, incumbent, algorithmExecutionConfig);
            //listOfIncumbentRunConfigs.add(incumbentRunConfig);

            LoggerUtil.log("RANDOM CONFIG FOR INIT: ");

            //Create initial row
            for (String activeParam : incumbent.getActiveParameters()) {
                LoggerUtil.log("Active Params : " + activeParam);
            }
            try {
                algorithmRunResults = evaluateRun(incumbentRunConfig);
            } catch (OutOfTimeException e) {
                log.warn("Ran out of time while evaluating the default configuration on the first run, this is most likely a configuration error");
                //Ignore this exception
                //Force the incumbent to be logged in RunHistory and then we will timeout next
                throw new IllegalStateException("Out of time on the first run");

            }

            //}
//        try {
//            evaluateRun(listOfIncumbentRunConfigs);
//        } catch (OutOfTimeException e) {
//            log.warn("Ran out of time while evaluating the default configuration on the first run, this is most likely a configuration error");
//            //Ignore this exception
//            //Force the incumbent to be logged in RunHistory and then we will timeout next
//            throw new IllegalStateException("Out of time on the first run");
//
        }
        //incumbent = listOfParameterConfigurations.get(0);
    }

    @Override
    public ParameterConfiguration getIncumbent() {
        return incumbent;
    }

    protected List<AlgorithmRunResult> evaluateRun(AlgorithmRunConfiguration runConfig) {
        return evaluateRun(Collections.singletonList(runConfig));
    }

    protected List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> runConfigs) {

        if (termCond.haveToStop()) {
            throw new OutOfTimeException();
        }
        log.debug("Initialization: Scheduling {} run(s):", runConfigs.size());
        for (AlgorithmRunConfiguration rc : runConfigs) {

            Object[] args = {runHistory.getThetaIdx(rc.getParameterConfiguration()) != -1 ? " " + runHistory.getThetaIdx(rc.getParameterConfiguration()) : "", rc.getParameterConfiguration(), rc.getProblemInstanceSeedPair().getProblemInstance().getInstanceID(), rc.getProblemInstanceSeedPair().getSeed(), rc.getCutoffTime()};
            LoggerUtil.log("Initialization: Scheduling run for config{} ({}) on instance {} with seed {} and captime {}: " + Arrays.toString(args));
        }

        List<AlgorithmRunResult> completedRuns = tae.evaluateRun(runConfigs);

        for (AlgorithmRunResult run : completedRuns) {
            AlgorithmRunConfiguration rc = run.getAlgorithmRunConfiguration();
            Object[] args = {runHistory.getThetaIdx(rc.getParameterConfiguration()) != -1 ? " " + runHistory.getThetaIdx(rc.getParameterConfiguration()) : "", rc.getParameterConfiguration(), rc.getProblemInstanceSeedPair().getProblemInstance().getInstanceID(), rc.getProblemInstanceSeedPair().getSeed(), rc.getCutoffTime(), run.getResultLine(), run.getWallclockExecutionTime()};
            LoggerUtil.log("Initialization: Completed run for config{} ({}) on instance {} with seed {} and captime {} => Result: {}, wallclock time: {} seconds: " + Arrays.toString(args));
        }

        updateRunHistory(completedRuns);
        return completedRuns;
    }

    /**
     *
     * @return the input parameter (unmodified, simply for syntactic convience)
     */
    protected List<AlgorithmRunResult> updateRunHistory(List<AlgorithmRunResult> runs) {
        for (AlgorithmRunResult run : runs) {
            try {
                runHistory.append(run);
            } catch (DuplicateRunException e) {
                //We are trying to log a duplicate run
                throw new IllegalStateException(e);
            }
        }
        return runs;
    }


    @Override
    public void setIncumbent(ParameterConfiguration incumbent) {
        this.incumbent = incumbent;
    }

    @Override
    public List<AlgorithmRunResult> getListOfAlgorithmRunResults() {
       return algorithmRunResults;
    }

}

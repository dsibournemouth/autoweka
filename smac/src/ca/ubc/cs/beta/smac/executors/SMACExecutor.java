package ca.ubc.cs.beta.smac.executors;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.exceptions.StateSerializationException;
import ca.ubc.cs.beta.aeatk.exceptions.TrajectoryDivergenceException;
import ca.ubc.cs.beta.aeatk.logging.CommonMarkers;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.returnvalues.AEATKReturnValues;
import ca.ubc.cs.beta.aeatk.misc.spi.SPIClassLoaderHelper;
import ca.ubc.cs.beta.aeatk.misc.version.JavaVersionInfo;
import ca.ubc.cs.beta.aeatk.misc.version.OSVersionInfo;
import ca.ubc.cs.beta.aeatk.misc.version.VersionTracker;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions.TrainTestInstances;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.smac.SMACOptions;
import ca.ubc.cs.beta.aeatk.state.StateFactoryOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.exceptions.TargetAlgorithmAbortException;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aeatk.termination.TerminationCondition;
import ca.ubc.cs.beta.aeatk.trajectoryfile.TrajectoryFile;
import ca.ubc.cs.beta.aeatk.trajectoryfile.TrajectoryFileEntry;
import ca.ubc.cs.beta.smac.builder.SMACBuilder;
import ca.ubc.cs.beta.smac.configurator.AbstractAlgorithmFramework;
import ca.ubc.cs.beta.smac.misc.version.SMACVersionInfo;
import ca.ubc.cs.beta.smac.validation.ValidationResult;
import ca.ubc.cs.beta.smac.validation.Validator;
import ch.qos.logback.classic.util.LoggerNameUtil;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.util.ArrayList;

public class SMACExecutor {

    private static Logger log;
    private static Marker exception;
    private static Marker stackTrace;

    private static String logLocation = "<NO LOG LOCATION SPECIFIED, FAILURE MUST HAVE OCCURED EARLY>";

    /*
	private static List<ProblemInstance> instances;
	private static List<ProblemInstance> testInstances;
	
	private static InstanceSeedGenerator instanceSeedGen;
	private static InstanceSeedGenerator testInstanceSeedGen;
	
	
	
	private static String instanceFileAbsolutePath;
	private static String instanceFeatureFileAbsolutePath;
     */
    private static InstanceListWithSeeds trainingILWS;
    private static InstanceListWithSeeds testingILWS;

    private static Map<String, AbstractOptions> taeOptions;
    private static SeedableRandomPool pool;

    private static String outputDir;

    /**
     * Executes SMAC then exits the JVM {
     *
     * @see System.exit()}
     *
     * @param args string arguments
     */
    public static void main(String[] args) {
        int returnValue = oldMain(args);

        if (log != null) {
            log.debug("Returning with value: {}", returnValue);
        }

        System.exit(returnValue);
    }

    /**
     * Executes SMAC according to the given arguments
     *
     * @param args string input arguments
     * @return return value for operating system
     */
    public static int oldMain(String[] args) {
        /*
		 * WARNING: DO NOT LOG ANYTHING UNTIL AFTER WE HAVE PARSED THE CLI OPTIONS
		 * AS THE CLI OPTIONS USE A TRICK TO ALLOW LOGGING TO BE CONFIGURABLE ON THE CLI
		 * IF YOU LOG PRIOR TO IT ACTIVATING, IT WILL BE IGNORED 
         */
        try {

            SMACOptions options = parseCLIOptions(args);

            int currentSeed = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--seed")) {
                    currentSeed = Integer.parseInt(args[i + 1]);
                    break;
                }
            }

            List<SMACOptions> listOfSMACOptions = new ArrayList<>();
            listOfSMACOptions.add(options);

            for (int s = (currentSeed + 1); s < (currentSeed + 5); s++) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("--seed")) {
                        args[i + 1] = String.valueOf(s);
                        break;
                    }
                }
                SMACOptions optionsSeedS = parseCLIOptions(args);
                listOfSMACOptions.add(optionsSeedS);

            }

            SMACBuilder smacBuilder = new SMACBuilder();

            //EventManager eventManager = smacBuilder.getEventManager();
            AlgorithmExecutionConfiguration execConfig = options.getAlgorithmExecutionConfig();

            AbstractAlgorithmFramework smac;

            smac = smacBuilder.getAutomaticConfigurator(execConfig, trainingILWS, listOfSMACOptions, taeOptions, outputDir, pool);

            StopWatch watch = new AutoStartStopWatch();

            LoggerUtil.log("SMAC is READY: ");

//                        for (String argStr : args) {
//                            LoggerUtil.log("args: " + argStr);
//                        }
            smac.run();

            watch.stop();
            smacBuilder.getLogRuntimeStatistics().logLastRuntimeStatistics();

            pool.logUsage();

            ParameterConfiguration incumbent = smac.getIncumbent();
            RunHistory runHistory = smac.runHistory();
            TerminationCondition tc = smac.getTerminationCondition();

            final DecimalFormat df0 = new DecimalFormat("0");
            String callString = smac.getCallString();
//			log.info("\n=======================================================================================\n"
//					+ "SMAC has finished. Reason: {}\n" 
//					+ "Total number of runs performed: {}, total configurations tried: {}.\n"   
//					+ "Total CPU time used: {} s, total wallclock time used: {} s.\n"
//					+ "SMAC's final incumbent: config {} (internal ID: {}), with estimated {}: {}, based on {} run(s) on {} training instance(s).\n"
//					+ "Sample call for this final incumbent:\n{}\n"
//					//+ "Total number of runs performed: {}, total CPU time used: {} s, total wallclock time used: {} s, total configurations tried: {}.\n"
//					+ "=======================================================================================" ,
//					smac.getTerminationReason(), 
//					runHistory.getAlgorithmRunsIncludingRedundant().size(),
//					
//					runHistory.getAllParameterConfigurationsRan().size(),
//					df0.format(tc.getTunerTime()),
//					df0.format(tc.getWallTime()),
//					runHistory.getThetaIdx(incumbent), incumbent,
//					smac.getObjectiveToReport(),
//					smac.getEmpericalPerformance(incumbent),
//					runHistory.getAlgorithmRunsExcludingRedundant(incumbent).size(),
//					runHistory.getProblemInstancesRan(incumbent).size(),
//					callString.trim());
//                        

            log.info("\n=======================================================================================\n"
                    + "SMAC has finished. Reason: {}\n"
                    + "Total number of runs performed: {}, total configurations tried: {}.\n"
                    + "Total CPU time used: {} s, total wallclock time used: {} s.\n"
                    + "SMAC's final incumbent: config {} (internal ID: {}), with estimated {}: {}, based on {} run(s) on {} training instance(s).\n"
                    + "Sample call for this final incumbent:\n{}\n"
                    //+ "Total number of runs performed: {}, total CPU time used: {} s, total wallclock time used: {} s, total configurations tried: {}.\n"
                    + "=======================================================================================");

            List<TrajectoryFileEntry> tfes = smacBuilder.getTrajectoryFileLogger().getTrajectoryFileEntries();

            SortedMap<TrajectoryFileEntry, ValidationResult> performance;
            options.doValidation = (options.validationOptions.numberOfValidationRuns > 0) ? options.doValidation : false;
            if (options.doValidation) {

                log.info("Now starting offline validation.");

                //Don't use the same TargetAlgorithmEvaluator as above as it may have runhashcode and other crap that is probably not applicable for validation
                if (options.validationOptions.maxTimestamp == -1) {
                    if (options.validationOptions.useWallClockTime) {
                        if (options.scenarioConfig.limitOptions.runtimeLimit < Integer.MAX_VALUE) {
                            options.validationOptions.maxTimestamp = options.scenarioConfig.limitOptions.runtimeLimit;
                        } else {
                            options.validationOptions.maxTimestamp = watch.time() / 1000.0;
                        }
                    } else {
                        options.validationOptions.maxTimestamp = options.scenarioConfig.limitOptions.tunerTimeout;
                    }

                }

                options.scenarioConfig.algoExecOptions.taeOpts.turnOffCrashes();

                int coreHint = 1;
                if (options.validationCores != null && options.validationCores > 0) {
                    log.debug("Validation will use {} cores", options.validationCores);
                    options.scenarioConfig.algoExecOptions.taeOpts.maxConcurrentAlgoExecs = options.validationCores;
                    ((CommandLineTargetAlgorithmEvaluatorOptions) taeOptions.get(CommandLineTargetAlgorithmEvaluatorFactory.NAME)).cores = options.validationCores;
                    coreHint = options.validationCores;
                }

                TargetAlgorithmEvaluator validatingTae = TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(options.scenarioConfig.algoExecOptions.taeOpts, false, taeOptions);
                try {

                    List<ProblemInstance> testInstances = testingILWS.getInstances();
                    InstanceSeedGenerator testInstanceSeedGen = testingILWS.getSeedGen();

                    TrajectoryFile trajFile = new TrajectoryFile(new File(outputDir + File.separator + "traj-run-" + options.seedOptions.numRun + ".txt"), tfes);
                    performance = (new Validator()).simpleValidate(testInstances, options.validationOptions, options.scenarioConfig.algoExecOptions.cutoffTime, testInstanceSeedGen, validatingTae, outputDir, options.scenarioConfig.getRunObjective(), options.scenarioConfig.getIntraInstanceObjective(), options.scenarioConfig.interInstanceObj, trajFile, true, coreHint, execConfig);

//					performance  = (new Validator()).simpleValidate(testInstances,options.validationOptions,options.scenarioConfig.algoExecOptions.cutoffTime, testInstanceSeedGen, validatingTae, outputDir, options.scenarioConfig.getRunObjective(), options.scenarioConfig.getIntraInstanceObjective(), options.scenarioConfig.interInstanceObj, trajFile,true, coreHint,execConfig);
                } finally {
                    validatingTae.notifyShutdown();
                }

            } else {
                performance = new TreeMap<TrajectoryFileEntry, ValidationResult>();
                performance.put(tfes.get(tfes.size() - 1), new ValidationResult(Double.POSITIVE_INFINITY, Collections.<ProblemInstanceSeedPair>emptyList()));

            }

            String validationMessage = "";
            if (options.doValidation) {
                if (options.validationOptions.validateOnlyLastIncumbent) {

                    Set<ProblemInstance> pis = new HashSet<ProblemInstance>();
                    int pispCount = 0;
                    for (ProblemInstanceSeedPair pisp : performance.get(performance.lastKey()).getPISPS()) {
                        pispCount++;
                        pis.add(pisp.getProblemInstance());
                    }

                    validationMessage = "Estimated mean quality of final incumbent config " + runHistory.getThetaIdx(incumbent) + " (internal ID: " + incumbent + ") on test set: " + performance.get(performance.lastKey()).getPerformance() + ", based on " + pispCount + " run(s) on " + pis.size() + " test instance(s).\n";

                    /*
							 * smac.getTerminationReason(), 
					runHistory.getAlgorithmRunsIncludingRedundant().size(),
					
					runHistory.getAllParameterConfigurationsRan().size(),
					df0.format(tc.getTunerTime()),
					df0.format(tc.getWallTime()),
					runHistory.getThetaIdx(incumbent), incumbent,
					smac.getObjectiveToReport(),
					smac.getEmpericalPerformance(incumbent),
					runHistory.getAlgorithmRunsExcludingRedundant(incumbent).size(),
					runHistory.getProblemInstancesRan(incumbent).size(),
					callString.trim());
					
                     */
                } else {
                    validationMessage = smac.logIncumbentPerformance(performance);
                }

            }

            smacBuilder.getEventManager().shutdown();

            if (options.doValidation) {
//		log.info("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
//					+"{}Sample call for the final incumbent:\n{}\n"
//					+ "Additional information about run {} in:{}\n"
//					+ "----------------------------------------------------------------------------------------------------------------------------------------------------------------------------",
//					validationMessage,
//				    callString,
//				    options.seedOptions.numRun,
//				    outputDir);
//                
                log.info("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "{}Sample call for the final incumbent:\n{}\n"
                        + "Additional information about run {} in:{}\n"
                        + "----------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

            } else {
                log.info("\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n"
                        + "Additional information about run {} in:{}\n"
                        + "----------------------------------------------------------------------------------------------------------------------------------------------------------------------------",
                        options.seedOptions.numRun,
                        outputDir);

            }

            //log.info("SMAC has finished. Reason: {}",smac.getTerminationReason() );
            //log.info("SMAC"+ (options.doValidation ? " & Validation" : "" ) +  " Completed Successfully. Log: " + logLocation);
            /*
			log.info("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n"+
			"SMAC's final incumbent: {}.\n"+
			/*
			[INFO ] Training performance: estimated mean quality 0.4236244493298802, based on 1 run(s) on 1 instance(s).
			[INFO ] Test performance: estimated mean quality 0.4236244493298802, based on 1 run(s) on 1 instance(s).
			[INFO ] Sample call for this final incumbent:
			cd /home/hutter/smac-v2.06.02-development-665; examples/branin/wrapper.rb no_instance 0 1.7976931348623157E308 2147483647 -1 -x2 '2.9229911729079787' -x1 '5.134529068134288'
             */
            //For additional information, see run 1 in: /home/hutter/smac-v2.06.02-development-665/smac-output/scenario-SMAC-ac-false-cores1-cutoff1.7976931348623157E308-2014-03-31
            return AEATKReturnValues.SUCCESS;
        } catch (Throwable t) {
            System.out.flush();
            System.err.flush();

            System.err.println("Error occurred while running SMAC\n>Error Message:" + t.getMessage() + "\n>Encountered Exception:" + t.getClass().getSimpleName() + "\n>Error Log Location: " + logLocation);
            System.err.flush();

            if (log != null) {

                log.error(exception, "Message: {}", t.getMessage());

                if (t instanceof ParameterException) {
                    log.info("Note that some options are read from files in ~/.aeatk/");
                    log.debug("Exception stack trace", t);

                } else if (t instanceof TargetAlgorithmAbortException) {

                    log.error(CommonMarkers.SKIP_CONSOLE_PRINTING, "A serious problem occured during target algorithm execution and we are aborting execution ", t);

                    log.error("We tried to call the target algorithm wrapper, but this call failed.");
                    log.error("The problem is (most likely) somewhere in the wrapper or with the arguments to SMAC.");
                    log.error("The easiest way to debug this problem is to manually execute the call we tried and see why it did not return the correct result");
                    log.error("The required output of the wrapper is something like \"Result for ParamILS: x,x,x,x,x\".);");
                    //log.error("Specifically the regex we are matching is {}", CommandLineAlgorithmRun.AUTOMATIC_CONFIGURATOR_RESULT_REGEX);
                } else {
                    log.info("Maybe try running in DEBUG mode if you are missing information");

                    log.error(exception, "Exception:{}", t.getClass().getCanonicalName());
                    StringWriter sWriter = new StringWriter();
                    PrintWriter writer = new PrintWriter(sWriter);
                    t.printStackTrace(writer);
                    log.error(stackTrace, "StackTrace:{}", sWriter.toString());
                }

                log.info("Exiting SMAC with failure. Log: " + logLocation);
                log.info("For a list of available commands use:  --help");
                //log.info("Please see above for the available options. Further information is available in the following documents:");
                log.info("The Quickstart guide at available at: http://www.cs.ubc.ca/labs/beta/Projects/SMAC/ or alternatively (doc/quickstart.html) gives simple examples for getting up and running.");
                log.info("The FAQ (doc/faq.pdf) contains commonly asked questions regarding troubleshooting, and usage.");
                log.info("The Manual (doc/manual.pdf) contains detailed information on file format semantics.");
                log.info("If you are stuck, please ask a question in the SMAC forum: https://groups.google.com/forum/#!forum/smac-forum");

                t = t.getCause();
            } else {
                if (t instanceof ParameterException) {

                    System.err.println(t.getMessage());
                    t.printStackTrace();
                } else {
                    t.printStackTrace();
                }

            }

            if (t instanceof ParameterException) {
                return AEATKReturnValues.PARAMETER_EXCEPTION;
            }

            if (t instanceof StateSerializationException) {
                return AEATKReturnValues.SERIALIZATION_EXCEPTION;
            }

            if (t instanceof TrajectoryDivergenceException) {
                return AEATKReturnValues.TRAJECTORY_DIVERGENCE;
            }

            return AEATKReturnValues.OTHER_EXCEPTION;
        }

    }

    private static String runGroupName = "DEFAULT";

    /**
     * Parsers Command Line Arguments and returns a options object
     *
     * @param args
     * @return
     */
    private static SMACOptions parseCLIOptions(String[] args) throws ParameterException, IOException {
        //DO NOT LOG UNTIL AFTER WE PARSE CONFIG OBJECT

        SMACOptions options = new SMACOptions();
        taeOptions = options.scenarioConfig.algoExecOptions.taeOpts.getAvailableTargetAlgorithmEvaluators();
        JCommander jcom = JCommanderHelper.getJCommanderAndCheckForHelp(args, options, taeOptions);

        jcom.setProgramName("smac");

        try {
            try {
                try {

                    args = processScenarioStateRestore(args);
                    jcom.parse(args);
                } finally {
                    runGroupName = options.runGroupOptions.getFailbackRunGroup();
                }

                if (options.adaptiveCapping == null) {
                    switch (options.scenarioConfig.getRunObjective()) {
                        case RUNTIME:
                            options.adaptiveCapping = true;
                            break;

                        case QUALITY:
                            options.adaptiveCapping = false;
                            break;

                        default:
                            //You need to add something new here
                            throw new IllegalStateException("Not sure what to default too");
                    }
                }

                if (options.randomForestOptions.logModel == null) {
                    switch (options.scenarioConfig.getRunObjective()) {
                        case RUNTIME:
                            options.randomForestOptions.logModel = true;
                            break;
                        case QUALITY:
                            options.randomForestOptions.logModel = false;
                            break;
                        default:
                            throw new IllegalStateException("Unsure what to do with new run objective");
                    }
                }

                runGroupName = options.getRunGroupName(taeOptions.values());
                //File outputDir = new File(options.scenarioConfig.outputDirectory);

                /*
				 * Build the Serializer object used in the model 
                 */
                outputDir = options.getOutputDirectory(runGroupName);

                File outputDirFile = new File(outputDir);

                if (!outputDirFile.exists()) {
                    outputDirFile.mkdirs();
                    //Check again to ensure there isn't a race condition
                    if (!outputDirFile.exists()) {
                        throw new ParameterException("Could not create all folders necessary for output directory: " + outputDir);
                    }

                }

            } finally {

                options.logOptions.initializeLogging(outputDir, options.seedOptions.numRun);
                SMACExecutor.logLocation = options.logOptions.getLogLocation(outputDir, options.seedOptions.numRun);

                log = LoggerFactory.getLogger(SMACExecutor.class);

                exception = MarkerFactory.getMarker("EXCEPTION");
                stackTrace = MarkerFactory.getMarker("STACKTRACE");

                VersionTracker.setClassLoader(SPIClassLoaderHelper.getClassLoader());

                VersionTracker.logVersions();
                SMACVersionInfo s = new SMACVersionInfo();
                JavaVersionInfo j = new JavaVersionInfo();
                OSVersionInfo o = new OSVersionInfo();
                //log.info(CommonMarkers.SKIP_FILE_PRINTING,"Version of {} is {}, running on {} and {} ", s.getProductName(), s.getVersion(), j.getVersion(), o.getVersion());
                log.info("Version of {} is {}, running on {} and {} ");

                for (String name : jcom.getParameterFilesToRead()) {
                    log.debug("Parsing (default) options from file: {} ", name);
                }

            }

            JCommanderHelper.logCallString(args, "smac");

            Map<String, String> env = new TreeMap<String, String>(System.getenv());

            StringBuilder sb = new StringBuilder();
            for (String envName : env.keySet()) {
                sb.append(envName).append("=").append(env.get(envName)).append("\n");

            }

            log.info(CommonMarkers.SKIP_CONSOLE_PRINTING, "********** The next bit of output can be ignored, it is merely useful for debugging **********");
            log.info(CommonMarkers.SKIP_CONSOLE_PRINTING, "==========Enviroment Variables===========\n{}", sb.toString());

            Map<Object, Object> props = new TreeMap<Object, Object>(System.getProperties());
            sb = new StringBuilder();
            for (Entry<Object, Object> ent : props.entrySet()) {

                sb.append(ent.getKey().toString()).append("=").append(ent.getValue().toString()).append("\n");

            }

            String hostname = "[UNABLE TO DETERMINE HOSTNAME]";
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) { //If this fails it's okay we just use it to output to the log

            }

            log.info(CommonMarkers.SKIP_CONSOLE_PRINTING, "Hostname:{}", hostname);
            log.info(CommonMarkers.SKIP_CONSOLE_PRINTING, "==========System Properties==============\n{}", sb.toString());

            JCommanderHelper.logConfigurationInfoToFile(jcom);
            pool = options.seedOptions.getSeedableRandomPool();

            TrainTestInstances tti = options.getTrainingAndTestProblemInstances(pool, new SeedableRandomPool(options.validationSeed + options.seedOptions.seedOffset, pool.getInitialSeeds()));
            trainingILWS = tti.getTrainingInstances();
            testingILWS = tti.getTestInstances();

            try {
                //We don't handle this more gracefully because this seems like a super rare incident.
                if (ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled()) {
                    log.trace("JVM Supports CPU Timing Measurements");
                } else {
                    log.warn("This Java Virtual Machine has CPU Time Measurements disabled, tunerTimeout will not contain any SMAC Execution Time.");
                }
            } catch (UnsupportedOperationException e) {
                log.warn("This Java Virtual Machine does not support CPU Time Measurements, tunerTimeout will not contain any SMAC Execution Time Information (http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/management/ThreadMXBean.html#setThreadCpuTimeEnabled(boolean))");
            }

            if (options.seedOptions.numRun + options.seedOptions.seedOffset < 0) {
                log.warn("NumRun {} plus Seed Offset {} should be positive, things may not seed correctly", options.seedOptions.numRun, options.seedOptions.seedOffset);
            }

            return options;
        } catch (IOException e) {
            throw e;

        } catch (ParameterException e) {

            throw e;
        }
    }

    private static String[] processScenarioStateRestore(String[] args) {
        return StateFactoryOptions.processScenarioStateRestore(args);

    }

}

package ca.ubc.cs.beta.smac.builder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.acquisitionfunctions.AcquisitionFunctions;
import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.eventsystem.EventManager;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.AutomaticConfigurationEnd;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.ChallengeEndEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.ChallengeStartEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.IncumbentPerformanceChangeEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.IterationStartEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.basic.AlgorithmRunCompletedEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.model.ModelBuildEndEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.model.ModelBuildStartEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.events.state.StateRestoredEvent;
import ca.ubc.cs.beta.aeatk.eventsystem.handlers.LogRuntimeStatistics;
import ca.ubc.cs.beta.aeatk.initialization.InitializationMode;
import ca.ubc.cs.beta.aeatk.initialization.InitializationProcedure;
import ca.ubc.cs.beta.aeatk.initialization.classic.ClassicInitializationProcedure;
import ca.ubc.cs.beta.aeatk.initialization.doublingcapping.DoublingCappingInitializationProcedure;
import ca.ubc.cs.beta.aeatk.initialization.table.UnbiasChallengerInitializationProcedure;
import ca.ubc.cs.beta.aeatk.misc.cputime.CPUTime;
import ca.ubc.cs.beta.aeatk.objectives.ObjectiveHelper;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking.ParamConfigurationOriginTracker;
import ca.ubc.cs.beta.aeatk.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPoolConstants;
import ca.ubc.cs.beta.aeatk.runhistory.FileSharingRunHistoryDecorator;
import ca.ubc.cs.beta.aeatk.runhistory.NewRunHistory;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.runhistory.TeeRunHistory;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistory;
import ca.ubc.cs.beta.aeatk.runhistory.ThreadSafeRunHistoryWrapper;
import ca.ubc.cs.beta.aeatk.smac.ExecutionMode;
import ca.ubc.cs.beta.aeatk.smac.SMACOptions;
import ca.ubc.cs.beta.aeatk.state.StateDeserializer;
import ca.ubc.cs.beta.aeatk.state.StateFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.helpers.TargetAlgorithmEvaluatorNotifyTerminationCondition;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource.caching.runhistory.RunHistoryCachingTargetAlgorithmEvaluatorDecorator;
import ca.ubc.cs.beta.aeatk.termination.CompositeTerminationCondition;
import ca.ubc.cs.beta.aeatk.trajectoryfile.TrajectoryFileLogger;
import ca.ubc.cs.beta.smac.configurator.AbstractAlgorithmFramework;
import ca.ubc.cs.beta.smac.configurator.SequentialModelBasedAlgorithmConfiguration;
import ca.ubc.cs.beta.smac.executors.LoggerUtil;
import ca.ubc.cs.beta.smac.handler.ChallengePredictionHandler;
import java.util.ArrayList;
import java.util.Random;

/**
 * Builds an Automatic Configurator
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class SMACBuilder {

	private static transient Logger log = LoggerFactory.getLogger(SMACBuilder.class);
	
	private final EventManager eventManager; 
	
	private volatile TrajectoryFileLogger tLog;

	private volatile LogRuntimeStatistics logRT;
	public SMACBuilder()
	{
		this.eventManager = new EventManager();
	}
	
	
	public EventManager getEventManager()
	{
		return eventManager;
	}	
	
	public volatile TAEWrapper taeWrapper = new TAEWrapper()
	{

		@Override
		public TargetAlgorithmEvaluator wrap(TargetAlgorithmEvaluator tae) {
			return tae;
		}
		
	};
	public AbstractAlgorithmFramework getAutomaticConfigurator(AlgorithmExecutionConfiguration execConfig, InstanceListWithSeeds trainingILWS, List<SMACOptions> listOfSMACOptions,Map<String, AbstractOptions> taeOptions, String outputDir, SeedableRandomPool pool)
	{
		return this.getAutomaticConfigurator(execConfig, trainingILWS, listOfSMACOptions, taeOptions, outputDir, pool, null, null);
	}
	
	public AbstractAlgorithmFramework getAutomaticConfigurator(AlgorithmExecutionConfiguration execConfig, InstanceListWithSeeds trainingILWS, List<SMACOptions> listOfSMACOptions,Map<String, AbstractOptions> taeOptions, String outputDir, SeedableRandomPool pool, TargetAlgorithmEvaluator oTAE, RunHistory oRHModel)
	{	
		CPUTime cpuTime = new CPUTime();
                
                SMACOptions options = listOfSMACOptions.get(0);
		
		StateFactory restoreSF = options.getRestoreStateFactory(outputDir);

		if(options.adaptiveCapping == null)
		{
			switch(options.scenarioConfig.getRunObjective())
			{
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
		
		if(options.randomForestOptions.logModel == null)
		{
			switch(options.scenarioConfig.getRunObjective())
			{
			case RUNTIME:
				options.randomForestOptions.logModel = true;
				break;
			case QUALITY:
				options.randomForestOptions.logModel = false;
			default:
				//You need to add something new here
				throw new IllegalStateException("Not sure what to default too");
			}
		}
		
		
		ParameterConfigurationSpace configSpace = execConfig.getParameterConfigurationSpace();
		
		double configSpaceSize = configSpace.getUpperBoundOnSize();
	
		if(Double.isInfinite(configSpaceSize))
		{
			log.debug("Configuration Space has at least one continuous parameter or is very large (only bound expressible in IEEE 754 format is Infinity)");
		} else
		{
			log.debug("Configuration Space size is at most {}", configSpace.getUpperBoundOnSize());
		}
		
		StateFactory sf = options.getSaveStateFactory(outputDir);
		
		
		List<ProblemInstance> instances = trainingILWS.getInstances();
		InstanceSeedGenerator instanceSeedGen = trainingILWS.getSeedGen();
		
		options.checkProblemInstancesCompatibleWithVerifySAT(instances);
		
           
                LoggerUtil.log("options.seedOptions.numRun: " + String.valueOf(options.seedOptions.numRun));
                LoggerUtil.log("options.seedOptions.seedOffset: " + String.valueOf(options.seedOptions.seedOffset));
                
		ParameterConfiguration initialIncumbent = configSpace.getParameterConfigurationFromString(options.initialIncumbent, ParameterStringFormat.NODB_SYNTAX, pool.getRandom(SeedableRandomPoolConstants.INITIAL_INCUMBENT_SELECTION));

                
                // init n random configurations
                //Random configSpaceRandomInterleave = pool.getRandom(SeedableRandomPoolConstants.INITIAL_INCUMBENT_SELECTION);              
                List<ParameterConfiguration> listOfInititalIncumbent = new ArrayList<>();
                for (SMACOptions sMACOoptions : listOfSMACOptions) {
                    //ParameterConfiguration initialIncumbentI = configSpace.getRandomParameterConfiguration(configSpaceRandomInterleave);
                    ParameterConfiguration initialIncumbentI = configSpace.getParameterConfigurationFromString(options.initialIncumbent, ParameterStringFormat.NODB_SYNTAX, pool.getRandom(SeedableRandomPoolConstants.INITIAL_INCUMBENT_SELECTION));
                    listOfInititalIncumbent.add(initialIncumbentI);
                }
                
                
                
                
		List<ParameterConfiguration> initialChallengers = configSpace.getParameterConfigurationsFromList(options.initialChallengers, ParameterStringFormat.NODB_SYNTAX, pool.getRandom(SeedableRandomPoolConstants.INITIAL_INCUMBENT_SELECTION));
		
		if(!initialIncumbent.equals(configSpace.getDefaultConfiguration()))
		{
			log.debug("Initial Incumbent set to \"{}\" ", initialIncumbent.getFormattedParameterString(ParameterStringFormat.NODB_SYNTAX));
		} else
		{
			log.debug("Initial Incumbent is the default \"{}\" ", initialIncumbent.getFormattedParameterString(ParameterStringFormat.NODB_SYNTAX));
		}
		
		if(initialChallengers.size() > 0)
		{
			log.info("Specified {} initial challengers.", initialChallengers.size());
		}

		validateObjectiveCombinations(options.scenarioConfig, options.adaptiveCapping);
		
		TargetAlgorithmEvaluator tae;
		if(oTAE == null)
		{
			tae = options.scenarioConfig.algoExecOptions.taeOpts.getTargetAlgorithmEvaluator( taeOptions, outputDir, options.seedOptions.numRun);
		} else
		{
			tae = oTAE;
		}
		
		
		
		AbstractAlgorithmFramework smac;



		RunHistory rhROAR = new NewRunHistory(options.scenarioConfig.getIntraInstanceObjective(), options.scenarioConfig.interInstanceObj, options.scenarioConfig.getRunObjective());
		
		
		ThreadSafeRunHistory rhModel;
		
		
		//Make threadsafe
		if(oRHModel == null)
		{
			rhModel= new ThreadSafeRunHistoryWrapper(new NewRunHistory(options.scenarioConfig.getIntraInstanceObjective(), options.scenarioConfig.interInstanceObj, options.scenarioConfig.getRunObjective()));;
		} else
		{
			rhModel = new ThreadSafeRunHistoryWrapper(oRHModel);
		}
		
		
		switch(options.execMode)
		{
			case SMAC: 
				options.warmStartOptions.getWarmStartState(configSpace, instances, execConfig, rhModel);
				break;
			case ROAR:
				break;
			default:
				throw new IllegalStateException("Execution Mode Not Supported at this time");

		}
		
		
		ThreadSafeRunHistory rh ;
		if(rhModel.getAlgorithmRunsExcludingRedundant().size() == 0 && !options.shareModelMode)
		{
			//Don't make a seperate run history 
			
			rh = new ThreadSafeRunHistoryWrapper(new FileSharingRunHistoryDecorator(rhROAR,new File(outputDir), options.seedOptions.numRun, instances, options.shareRunDataFrequency * 1000, options.shareModelMode, options.sharedModeModeAssymetricMode, options.defaultHandler, options.writeRunData));
			rhModel = rh;
			
			log.debug("No warm started data, and shared model mode is false using single run data object");
		} else
		{
			//It's important that the FileSharingRunHistoryDecorator go on the rhModel object, and not the rh object, because some runs may
			//be sent only to the model.
			rhModel = new FileSharingRunHistoryDecorator(rhModel,new File(outputDir), options.seedOptions.numRun, instances, options.shareRunDataFrequency * 1000, options.shareModelMode, options.sharedModeModeAssymetricMode, options.defaultHandler, options.writeRunData);
			
			rh = new ThreadSafeRunHistoryWrapper(new TeeRunHistory(rhROAR, rhModel));

		}

		
		
		if(options.shareModelMode && options.shareModeModeTAE)
		{
			tae = new RunHistoryCachingTargetAlgorithmEvaluatorDecorator(tae, rhModel);
		}
		
		tae = taeWrapper.wrap(tae);
		
		
		
		CompositeTerminationCondition termCond = options.scenarioConfig.limitOptions.getTerminationConditions(cpuTime);
		
		tLog = new TrajectoryFileLogger(rh, termCond, outputDir +  File.separator + "traj-run-" + options.seedOptions.numRun, initialIncumbent, cpuTime);
		eventManager.registerHandler(IncumbentPerformanceChangeEvent.class, tLog);
		eventManager.registerHandler(AutomaticConfigurationEnd.class, tLog);
		eventManager.registerHandler(ModelBuildStartEvent.class, tLog);
		eventManager.registerHandler(ModelBuildEndEvent.class, tLog);
		
		Set onEvents = new HashSet();
		
		onEvents.add(IterationStartEvent.class);
		onEvents.add(AutomaticConfigurationEnd.class);
		
		logRT = new LogRuntimeStatistics(rh, termCond, execConfig.getAlgorithmMaximumCutoffTime(),tae,false, cpuTime, onEvents);
		termCond.registerWithEventManager(eventManager);	
		eventManager.registerHandler(ModelBuildStartEvent.class, logRT);
		eventManager.registerHandler(IncumbentPerformanceChangeEvent.class,logRT);
		eventManager.registerHandler(AlgorithmRunCompletedEvent.class, logRT);
		eventManager.registerHandler(AutomaticConfigurationEnd.class, logRT);
		eventManager.registerHandler(StateRestoredEvent.class, logRT);
		
		eventManager.registerHandler(ChallengeStartEvent.class, logRT);
		eventManager.registerHandler(ChallengeEndEvent.class, logRT);
		eventManager.registerHandler(IterationStartEvent.class, logRT);
		
		ParamConfigurationOriginTracker configTracker = options.trackingOptions.getTracker(eventManager, initialIncumbent, outputDir, rh, execConfig, options.seedOptions.numRun);
		
		
		TargetAlgorithmEvaluator acTae = new TargetAlgorithmEvaluatorNotifyTerminationCondition(tae, eventManager, termCond, true);
		
		
		InitializationProcedure initProc;
		
		ObjectiveHelper objHelper = new ObjectiveHelper(options.scenarioConfig.getRunObjective(), options.scenarioConfig.getIntraInstanceObjective(), options.scenarioConfig.interInstanceObj, execConfig.getAlgorithmMaximumCutoffTime());
		
                
                
                
		switch(options.initializationMode)
		{
			case CLASSIC:
				initProc = new ClassicInitializationProcedure(rh, initialIncumbent, acTae, options.classicInitModeOpts, instanceSeedGen, instances, options.maxIncumbentRuns, termCond, execConfig.getAlgorithmMaximumCutoffTime(), pool, options.deterministicInstanceOrdering, execConfig);
				break;
			
			case ITERATIVE_CAPPING:
				initProc = new DoublingCappingInitializationProcedure(rh, initialIncumbent, acTae, options.dciModeOpts, instanceSeedGen, instances, options.maxIncumbentRuns, termCond, execConfig.getAlgorithmMaximumCutoffTime(), pool, options.deterministicInstanceOrdering, objHelper, execConfig);
				break;
				
			case UNBIASED_TABLE:
				initProc = new UnbiasChallengerInitializationProcedure(rh, initialIncumbent, acTae, execConfig, options.ucip, instanceSeedGen, instances, options.maxIncumbentRuns, termCond, execConfig.getAlgorithmMaximumCutoffTime(), pool, options.deterministicInstanceOrdering, objHelper);
				break;
				
			default:
				throw new IllegalStateException("Not sure what this initialization mode is");
		}
		
		if(options.expFunc == null)
		{
			switch(options.scenarioConfig.getRunObjective())
			{
			case RUNTIME:
				options.expFunc = AcquisitionFunctions.EXPONENTIAL;
				break;
			case QUALITY:
				options.expFunc = AcquisitionFunctions.EI;
				break;
			default:
				//You need to add something new here
				throw new IllegalStateException("Not sure what to default too");
				
			}
		}
		
		
		
		
		switch(options.expFunc)
		{
			case EXPONENTIAL:
				if(!options.randomForestOptions.logModel)
				{
					log.warn("With log model turned off the exponential expected improvement function is not recommended, use: " + AcquisitionFunctions.EI);
				}
			break;
			case EI:
				if(options.randomForestOptions.logModel)
				{
					log.warn("With log model turned on the expected improvement function is not recommended, use: " + AcquisitionFunctions.EXPONENTIAL);
				} 
			break;
			case SIMPLE:
				log.warn("The simple acquisition function is never recommended");
				break;
			case LCB:
				break;
			default:
				throw new IllegalStateException("Not sure what to default too");
		}
		
		
		switch(options.execMode)
		{
			case ROAR:

				smac = new AbstractAlgorithmFramework(listOfInititalIncumbent, options,execConfig, instances,acTae,sf, configSpace, instanceSeedGen, initialIncumbent, initialChallengers, eventManager, rh, pool, termCond, configTracker, initProc,cpuTime);
                            
				break;
			case SMAC:
				
				
				smac = new SequentialModelBasedAlgorithmConfiguration(listOfInititalIncumbent, listOfSMACOptions, options, execConfig, instances, acTae, options.expFunc.getFunction(),sf, configSpace, instanceSeedGen, initialIncumbent, initialChallengers, eventManager, rh,pool, termCond, configTracker, initProc, rhModel, cpuTime);

				break;
			case PSEL:
				throw new ParameterException("This version of SMAC does not support " + options.execMode + " at this time");
			default:
				throw new IllegalArgumentException("Execution Mode Specified is not supported");
		}
		
		if(options.trackingOptions.configTracking && options.execMode.equals(ExecutionMode.SMAC))
		{
			ChallengePredictionHandler cph = new ChallengePredictionHandler(smac, configTracker);
			eventManager.registerHandler(ModelBuildStartEvent.class, cph);
			eventManager.registerHandler(ModelBuildEndEvent.class, cph);
			eventManager.registerHandler(ChallengeStartEvent.class, cph);
			
		}
		
		options.saveContextWithState(configSpace, trainingILWS, sf);
					
		if(options.stateOpts.restoreIteration != null)
		{
			restoreState(options, restoreSF, smac, configSpace, instances, execConfig, rh);
		}
	
		return smac;
		
	}
	
	public TrajectoryFileLogger getTrajectoryFileLogger()
	{
		return tLog;
	}
	
	public LogRuntimeStatistics getLogRuntimeStatistics()
	{
		return logRT;
	}
	
	
	private void restoreState(SMACOptions options, StateFactory sf, AbstractAlgorithmFramework smac,  ParameterConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfiguration execConfig, RunHistory rh) {
		
		if(options.stateOpts.restoreIteration < 0)
		{
			throw new ParameterException("Iteration must be a non-negative integer");
		}
		
		StateDeserializer sd = sf.getStateDeserializer("it", options.stateOpts.restoreIteration, configSpace, instances, execConfig, rh);
		
		smac.restoreState(sd);
		
		
		
	}
		
	/**
	 * Validates the various objective functions and ensures that they are legal together
	 * @param scenarioOptions
	 */
	private static void validateObjectiveCombinations(
			ScenarioOptions scenarioOptions, boolean adaptiveCapping) {

		switch(scenarioOptions.interInstanceObj)
		{
			case MEAN:
				//Okay
				break;
			default:
				throw new ParameterException("Model does not currently support an inter-instance objective other than " +  OverallObjective.MEAN);
				
		}
		
		
		
		
		switch(scenarioOptions.getRunObjective())
		{
			case RUNTIME:
				break;
			
			case QUALITY:
				if(!scenarioOptions.getIntraInstanceObjective().equals(OverallObjective.MEAN))
				{
					throw new ParameterException("To optimize quality you MUST use an intra-instance objective of " + OverallObjective.MEAN);
				}
				
				if(adaptiveCapping)
				{
					throw new ParameterException("You can only use Adaptive Capping when using " + RunObjective.RUNTIME + " as an objective");
				}
				
		}
	}
		
}

package ca.ubc.cs.beta.aeatk.smac;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import ca.ubc.cs.beta.aeatk.acquisitionfunctions.AcquisitionFunctions;
import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.initialization.InitializationMode;
import ca.ubc.cs.beta.aeatk.initialization.classic.ClassicInitializationProcedureOptions;
import ca.ubc.cs.beta.aeatk.initialization.doublingcapping.DoublingCappingInitializationProcedureOptions;
import ca.ubc.cs.beta.aeatk.initialization.table.UnbiasChallengerInitializationProcedureOptions;
import ca.ubc.cs.beta.aeatk.logging.ComplexLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.*;
import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.model.ModelBuildingOptions;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.RandomForestOptions;
import ca.ubc.cs.beta.aeatk.options.RunGroupOptions;
import ca.ubc.cs.beta.aeatk.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking.ParamConfigurationOriginTrackingOptions;
import ca.ubc.cs.beta.aeatk.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions.TrainTestInstances;
import ca.ubc.cs.beta.aeatk.random.SeedOptions;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPoolConstants;
import ca.ubc.cs.beta.aeatk.state.StateFactory;
import ca.ubc.cs.beta.aeatk.state.StateFactoryOptions;
import ca.ubc.cs.beta.aeatk.state.WarmStartOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;


/**
 * Represents the configuration for SMAC, 
 * 
 * @author seramage
 *
 *
 *
 */
@UsageTextField(title="SMAC Options", description="General Options for Running SMAC", claimRequired={"--pcs-file","--instanceFile","--run-obj"}, noarg=SMACNoArgHandler.class)
public class SMACOptions extends AbstractOptions {
	
	@UsageTextField(defaultValues="Defaults to true when --runObj is RUNTIME, false otherwise", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--adaptive-capping","--ac","--adaptiveCapping"}, description="Use Adaptive Capping")
	public Boolean adaptiveCapping = null;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--always-run-initial-config","--alwaysRunInitialConfiguration"}, description="if true we will always run the default and switch back to it if it is better than the incumbent")
	public boolean alwaysRunInitialConfiguration = false;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--ac-add-slack","--capAddSlack"}, description="amount to increase computed adaptive capping value of challengers by (post scaling)", validateWith=ZeroInfinityOpenInterval.class)
	public double capAddSlack = 1;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--ac-mult-slack","--capSlack"}, description="amount to scale computed adaptive capping value of challengers by", validateWith=ZeroInfinityOpenInterval.class)
	public double capSlack = 1.3;

	
	@ParametersDelegate
	public ClassicInitializationProcedureOptions classicInitModeOpts = new ClassicInitializationProcedureOptions();
	
	@ParametersDelegate
	public DoublingCappingInitializationProcedureOptions dciModeOpts = new DoublingCappingInitializationProcedureOptions();
	
	@ParametersDelegate
	public UnbiasChallengerInitializationProcedureOptions ucip = new UnbiasChallengerInitializationProcedureOptions();
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--deterministic-instance-ordering","--deterministicInstanceOrdering"}, description="If true, instances will be selected from the instance list file in the specified order")
	public boolean deterministicInstanceOrdering = false;
	
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.BASIC)
	@Parameter(names={"--validation","--doValidation"}, description="perform validation when SMAC completes")
	public boolean doValidation = true;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--exec-mode","--execution-mode","--executionMode"}, description="execution mode of the automatic configurator")
	public ExecutionMode execMode = ExecutionMode.SMAC;

	@CommandLineOnly
	@UsageTextField(defaultValues="<current working directory>", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--experiment-dir","--experimentDir","-e"}, description="root directory for experiments Folder")
	public String experimentDir = System.getProperty("user.dir") + File.separator + "";
	
	@UsageTextField(level=OptionLevel.ADVANCED, defaultValues="EXPONENTIAL if minimizing runtime, EI otherwise.")
	@Parameter(names={"--acq-func","--acquisition-function", "--ei-func","--expected-improvement-function","--expectedImprovementFunction"}, description="acquisition function to use during local search, NOTE: The LCB acquisition function mu+k*sigma will have k sampled from an exponential distribution with mean 1.")
	public AcquisitionFunctions expFunc = null;
	
	@ParametersDelegate
	public HelpOptions help = new HelpOptions();
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--initial-challenger-runs","--initialN","--initialChallenge"}, description="initial amount of runs to request when intensifying on a challenger", validateWith=FixedPositiveInteger.class)
	public int initialChallengeRuns = 1;

	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--initial-incumbent","--initialIncumbent"}, description="Initial Incumbent to use for configuration (you can use RANDOM, or DEFAULT as a special string to get a RANDOM or the DEFAULT configuration as needed). Other configurations are specified as: -name 'value' -name 'value' ... For instance: --quick-sort 'on' ")
	public String initialIncumbent = "DEFAULT";
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--init-mode","--initialization-mode","--initMode","--initializationMode"}, description="Initialization Mode")
	public InitializationMode initializationMode = InitializationMode.CLASSIC;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--intensification-percentage","--intensificationPercentage","--frac_rawruntime"}, description="percent of time to spend intensifying versus model learning", validateWith=ZeroOneHalfOpenRightDouble.class)
	public double intensificationPercentage = 0.50;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--initial-challengers","--initialChallengers"}, description="Can be specified multiple times. Every item is one additional initial challenger which will be used to challenge the incumbent prior to starting the actual optimization method. For the syntax, please see --initialIncumbent.")
	public List<String> initialChallengers = new ArrayList<String>();
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--initial-challengers-intensification-time","--initialChallengersIntensificationTime"}, description="Time to spend on intensify for the initial challengers.")
	public int initialChallengersIntensificationTime = Integer.MAX_VALUE;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--iterativeCappingBreakOnFirstCompletion"}, description="In Phase 2 of the initialization phase, we will abort the first time something completes and not look at anything else with the same kappa limits")
	public boolean iterativeCappingBreakOnFirstCompletion = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--iterativeCappingK"}, description="Iterative Capping K")
	public int iterativeCappingK = 1;
	
	@ParametersDelegate
	public ComplexLoggingOptions logOptions = new ComplexLoggingOptions();
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--max-incumbent-runs","--maxIncumbentRuns","--maxRunsForIncumbent"}, description="maximum number of incumbent runs allowed", validateWith=FixedPositiveInteger.class)
	public int maxIncumbentRuns = 2000;
	
	@ParametersDelegate
	public ModelBuildingOptions mbOptions = new ModelBuildingOptions();
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--model-hashcode-file","--modelHashCodeFile"}, description="file containing a list of model hashes one per line with the following text per line: \"Preprocessed Forest Built With Hash Code: (n)\" or \"Random Forest Built with Hash Code: (n)\" where (n) is the hashcode", converter=ReadableFileConverter.class, hidden = true)
	public File modelHashCodeFile;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--num-challengers","--numChallengers","--numberOfChallengers"}, description="number of challengers needed for local search", validateWith=FixedPositiveInteger.class)
	public int numberOfChallengers = 10;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--num-ei-random","--numEIRandomConfigs","--numberOfRandomConfigsInEI","--numRandomConfigsInEI","--numberOfEIRandomConfigs"} , description="number of random configurations to evaluate during EI search", validateWith=NonNegativeInteger.class)
	public int numberOfRandomConfigsInEI = 10000;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--num-ls-random","--num-local-search-random"}, description="Number of random configurations that will be used as potential starting points for local search", validateWith=NonNegativeInteger.class)
	public int numberOfRandomConfigsUsedForLocalSearch = 0;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--num-pca","--numPCA"}, description="number of principal components features to use when building the model", validateWith=FixedPositiveInteger.class)
	public int numPCA = 7;

	@UsageTextField(defaultValues="", level=OptionLevel.ADVANCED)
	@ParameterFile
	@Parameter(names={"--option-file","--optionFile"}, description="read options from file")
	public File optionFile;

	@UsageTextField(defaultValues="", level=OptionLevel.ADVANCED)
	@ParameterFile
	@Parameter(names={"--option-file2","--optionFile2","--secondaryOptionsFile"}, description="read options from file")
	public File optionFile2;

	@ParametersDelegate
	public RandomForestOptions randomForestOptions = new RandomForestOptions();


	@ParametersDelegate
	public RunGroupOptions runGroupOptions = new RunGroupOptions("%SCENARIO_NAME"); 

	@ParametersDelegate
	public ScenarioOptions scenarioConfig = new ScenarioOptions();

	
	@ParametersDelegate
	public SeedOptions seedOptions = new SeedOptions();
	
	@UsageTextField(defaultValues="~/.aeatk/smac.opt", level=OptionLevel.ADVANCED)
	@Parameter(names={"--smac-default-file","--smacDefaultsFile"}, description="file that contains default settings for SMAC")
	@ParameterFile(ignoreFileNotExists = true) 
	public File smacDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "smac.opt");
	
	@ParametersDelegate
	public StateFactoryOptions stateOpts = new StateFactoryOptions();

	@ParametersDelegate
	public ParamConfigurationOriginTrackingOptions trackingOptions= new ParamConfigurationOriginTrackingOptions();

	@ParametersDelegate
	public ValidationOptions validationOptions = new ValidationOptions();
	
	@ParametersDelegate
	public WarmStartOptions warmStartOptions = new WarmStartOptions();
	
	
	@UsageTextField(defaultValues="0 which should cause it to run exactly the same as the stand-alone utility.", level=OptionLevel.ADVANCED)
	@Parameter(names="--validation-seed", description="Seed to use for validating SMAC")
	public int validationSeed = 0;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--save-runs-every-iteration"}, description="if true will save the runs and results file to disk every iteration. Useful if your runs are expensive and your cluster unreliable, not recommended if your runs are short as this may add an unacceptable amount of overhead")
	public boolean saveRunsEveryIteration = false;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--quick-saves", description="determines whether to make quick saves or not")
	public boolean stateQuickSaves = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names="--intermediary-saves", description="determines whether to make any intermediary-saves or not (if false, no quick saves will be made either). The state will still be saved at the end of the run however")

	public boolean shutdownTAEWhenDone = true;
	public boolean intermediarySaves = true; 

	@UsageTextField(defaultValues="The value of --cores", level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--validation-cores", description="Number of cores to use when validating (only applicable when using local command line cores). Essentially this changes the value of --cli-cores and --cores after SMAC has run. The use of this parameter is undefined if the TargetAlgorithmEvaluator being used is not the CLI", validateWith=FixedPositiveInteger.class)
	public Integer validationCores = null;

	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--shared-model-mode","--share-model-mode","--shared-run-data","--share-run-data"}, description="If true the run data will be read from other runs in the output dir periodically (the runs need have a specific filename)")
	public boolean shareModelMode = false;
	
	
	@UsageTextField(defaultValues="300 seconds", level=OptionLevel.ADVANCED)
	@Parameter(names={"--shared-model-mode-frequency","--share-model-mode-frequency","--shared-run-data-frequency","--share-run-data-frequency"}, description="How often to poll for new run data (in seconds) ", validateWith=FixedPositiveInteger.class)
	public int shareRunDataFrequency = 300;

	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--shared-model-mode-tae"}, description="If true and shared model mode is enabled, then we will also try and share run data at the TAE level")
	public boolean shareModeModeTAE = true;

	public enum SharedModelModeDefaultHandling{
		USE_ALL,
		SKIP_FIRST_TWO,
		IGNORE_ALL
	}
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--shared-model-mode-write-data","--write-json-data"}, description="If true we will write run data to a JSON file")
	public boolean writeRunData = true;
	
	
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--shared-model-mode-default-handling"}, description="If set to USE_ALL then all runs of the default configuration will be used, If set to SKIP_FIRST_TWO then then first two runs (presumably the default) will not be read, If set to IGNORE_ALL then we will always ignore runs with the default configuration")
	public SharedModelModeDefaultHandling defaultHandler = SharedModelModeDefaultHandling.USE_ALL;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--shared-model-mode-asymetric"}, description="If set to true, then (based on the order of the file names) we will only read from runs that are transitively 2N and 2N+1 from our ID. So for instance if there were 16 runs, 0-15, runs 8-15 would be independent. Run 4 would read from 8,9. Run 5 would read from 10,11. Run 2 would read from 4,5,8,9,10,11, etc...")
	public boolean sharedModeModeAssymetricMode = false;

	
	/**
	 * Checks if the verify sat option is compatible with this set of probelm instances
	 * @param instances 	The problem instances
	 */
	public void checkProblemInstancesCompatibleWithVerifySAT(List<ProblemInstance> instances)
	{
		this.scenarioConfig.algoExecOptions.taeOpts.checkProblemInstancesCompatibleWithVerifySAT(instances);
	}
	
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig() {
		return this.scenarioConfig.getAlgorithmExecutionConfig(experimentDir);
	}
	
	public String getOutputDirectory(String runGroupName)
	{
		File outputDir = new File(this.scenarioConfig.outputDirectory + File.separator + runGroupName);
		if(!outputDir.isAbsolute())
		{
			outputDir = new File(experimentDir + File.separator + this.scenarioConfig.outputDirectory + File.separator + runGroupName);
		}
		
		return outputDir.getAbsolutePath();
	}

	/**
	 * Returns a state factory
	 * @param outputDir	output directory
	 * @return
	 */
	public StateFactory getRestoreStateFactory(String outputDir) {
		return stateOpts.getRestoreStateFactory(outputDir, this.seedOptions.numRun);
	}

	public String getRunGroupName(Collection<AbstractOptions> opts)
	{	
		opts = new HashSet<AbstractOptions>(opts);
		opts.add(this);
		return runGroupOptions.getRunGroupName(opts);	
	}
	
	public StateFactory getSaveStateFactory(String outputDir) {
		return stateOpts.getSaveStateFactory(outputDir, this.seedOptions.numRun);
	}

	/**
	 * Gets both the training and the test problem instances
	 * 
	 * @param experimentDirectory	Directory to search for instance files
	 * @param trainingSeed			Seed to use for the training instances
	 * @param testingSeed			Seed to use for the testing instances
	 * @param trainingRequired		Whether the training instance file is required
	 * @param testRequired			Whether the test instance file is required
	 * @return
	 * @throws IOException
	 */
	public TrainTestInstances getTrainingAndTestProblemInstances(SeedableRandomPool instancePool, SeedableRandomPool testInstancePool) throws IOException
	{
			return this.scenarioConfig.getTrainingAndTestProblemInstances(this.experimentDir, instancePool.getRandom(SeedableRandomPoolConstants.INSTANCE_SEEDS).nextInt(), testInstancePool.getRandom(SeedableRandomPoolConstants.TEST_SEED_INSTANCES).nextInt(), true, this.doValidation, false, false);
	}

	public void saveContextWithState(ParameterConfigurationSpace configSpace, InstanceListWithSeeds trainingILWS,	StateFactory sf)
	{
		this.stateOpts.saveContextWithState(configSpace, trainingILWS, this.scenarioConfig.scenarioFile, sf);
	}
}

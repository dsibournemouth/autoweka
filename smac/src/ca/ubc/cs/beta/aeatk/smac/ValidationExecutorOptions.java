package ca.ubc.cs.beta.aeatk.smac;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.help.HelpOptions;
import ca.ubc.cs.beta.aeatk.logging.SingleLogFileLoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.file.HomeFileUtils;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.FixedPositiveInteger;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.scenario.ScenarioOptions;
import ca.ubc.cs.beta.aeatk.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions.TrainTestInstances;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPool;
import ca.ubc.cs.beta.aeatk.random.SeedableRandomPoolConstants;
import ca.ubc.cs.beta.aeatk.random.SeperateSeedNumRunOptions;
import ca.ubc.cs.beta.aeatk.trajectoryfile.TrajectoryFileOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;

/**
 * Options controlling the Stand Alone Validation Utility
 * 
 * @see ValidationOptions 
 */
@UsageTextField(title="Validation Executor Options", description="Options that control the stand-alone validator",claimRequired={"--pcs-file","--run-obj"})
public class ValidationExecutorOptions extends AbstractOptions {
	
	@ParametersDelegate
	public ScenarioOptions scenarioConfig = new ScenarioOptions();
	
	@UsageTextField(defaultValues="~/.aeatk/smac-validate.opt")
	@Parameter(names={"--validation-defaults-file","--validationDefaultsFile"}, description="file that contains default settings for SMAC-Validate")
	@ParameterFile(ignoreFileNotExists = true) 
	public File smacValidateDefaults = HomeFileUtils.getHomeFile(".aeatk" + File.separator  + "smac-validate.opt");
	
	@Parameter(names={"--experiment-dir","--experimentDir","-e"}, description="Root Directory for Experiments Folder")
	public String experimentDir = System.getProperty("user.dir") + File.separator + "";
	
	
	@ParametersDelegate
	public SeperateSeedNumRunOptions seedOptions = new SeperateSeedNumRunOptions();
	
	
	@ParametersDelegate
	public HelpOptions helpOptions = new HelpOptions();
	/*
	@Parameter(names="--seed", description="Seed for Random Number Generator")
	public long seed = 0;
	
	@Parameter(names="--numRun", description="Number of Run the Run", required=true)
	public long numRun = 0;
	*/
	
	@ParametersDelegate
	public SingleLogFileLoggingOptions logOptions = new SingleLogFileLoggingOptions("val");
	
	@Parameter(names="--configuration", description="Parameter configuration to validate (In the same format calls are made to the algorithm) [Use 'DEFAULT' to validate the default]")
	public String incumbent;
	
	
	@ParametersDelegate
	public ValidationOptions validationOptions = new ValidationOptions();

	@Parameter(names={"--validation-tunertime","--tunerTime"}, description="Tuner Time when Validation occured (when specifying the configuration this is simply reported in the output file, when using a trajectory file we use the incumbent at this time, if you set this to -1 we use the tuner time from the scenario file or 0 if reading configuration from command line)")
	public double tunerTime = -1; 
	
	@Parameter(names={"--use-scenario-outdir","--useScenarioOutDir"}, description="Use the scenarios output directory")
	public boolean useScenarioOutDir = false;

	@Parameter(names={"--empirical-performance","--empiricalPerformance"}, description="Estimated performance of configuration on training set (-1 means use the trajectory file value or 0 if not trajectory file)")
	public double empiricalPerformance = -1; 
	
	@Parameter(names={"--tuner-overhead-time","--tunerOverheadTime"}, description="Amount of Tuner Overhead time to report in the output (-1 means use trajectory file overhead or 0 if no trajectory file)")
	public double tunerOverheadTime = -1;
	
	@Parameter(names={"--validate-test-instances","--validateTestInstances"}, description="Use the test instances for validation")
	public boolean validateTestInstances = true;
	
	@Parameter(names={"--wait-for-persistent-run-completion","--waitForPersistedRunCompletion"}, description="If the Target Algorithm Evaluator is persistent, then you can optionally not wait for it to finish, and come back later")
	public boolean waitForPersistedRunCompletion = true;

	@Parameter(names={"--random-configurations","--randomConfigurations","--random"}, description="Number of random configurations to validate", validateWith=FixedPositiveInteger.class)
	public int randomConfigurations = 0;
	
	@Parameter(names="--includeDefaultAsFirstRandom", description="Use the default as the first random default configuration")
	public boolean includeRandomAsFirstDefault = false;


	@Parameter(names={"--configuration-list","--configurationList"}, description="Listing of configurations to validate against (Can use DEFAULT for a default configuration or a RANDOM for a random one")
	public File configurationList;
	
	@Parameter(names={"--auto-increment-tunertime","--autoIncrementTunerTime"}, description="Auto Increment Tuner Time (each configuration in the list will have a different tunertime)")
	public boolean autoIncrementTunerTime = true;

	@Parameter(names={"--wall-time","--wallTime"}, description="Wall Time when Validation occured (when specifying the configuration this is simply reported in the output file, when using a trajectory file we use the incumbent at this time, if you set this to -1 we use the wall time from the scenario file or 0 if reading configuration from command line)")
	public double wallTime;

	@ParametersDelegate
	public TrajectoryFileOptions trajectoryFileOptions = new TrajectoryFileOptions();
	

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
	public InstanceListWithSeeds getTrainingAndTestProblemInstances(SeedableRandomPool pool) throws IOException
	{
			TrainTestInstances tti = this.scenarioConfig.getTrainingAndTestProblemInstances(this.experimentDir, pool.getRandom(SeedableRandomPoolConstants.INSTANCE_SEEDS).nextInt(), pool.getRandom(SeedableRandomPoolConstants.TEST_SEED_INSTANCES).nextInt(), true, false, false, false);
			
			Logger log = LoggerFactory.getLogger(getClass());
			if(this.validateTestInstances)
			{
				log.debug("Validating using test instances");
				return tti.getTestInstances();
			} else
			{
				log.debug("Validating using training instances");
				return tti.getTrainingInstances();
			}
	}
	
	/**
	 * Checks if the verify sat option is compatible with this set of probelm instances
	 * @param instances 	The problem instances
	 */
	public void checkProblemInstancesCompatibleWithVerifySAT(List<ProblemInstance> instances)
	{
		this.scenarioConfig.algoExecOptions.taeOpts.checkProblemInstancesCompatibleWithVerifySAT(instances);
	}

	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig() {
		return this.scenarioConfig.algoExecOptions.getAlgorithmExecutionConfig(experimentDir);
	}
	
}

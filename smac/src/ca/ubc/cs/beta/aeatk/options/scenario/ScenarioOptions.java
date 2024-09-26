package ca.ubc.cs.beta.aeatk.options.scenario;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aeatk.misc.jcommander.converter.OverallObjectiveConverter;
import ca.ubc.cs.beta.aeatk.misc.jcommander.converter.RunObjectiveConverter;
import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions.TrainTestInstances;
import ca.ubc.cs.beta.aeatk.termination.TerminationCriteriaOptions;
import ca.ubc.cs.beta.smac.executors.LoggerUtil;

/**
 * Object which contains all information about a scenario
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@UsageTextField(title="Scenario Options", description="Standard Scenario Options for use with SMAC. In general consider using the --scenarioFile directive to specify these parameters and Algorithm Execution Options")
public class ScenarioOptions extends AbstractOptions{
	
	
	@CommandLineOnly
	
	@Parameter(names={"--run-obj","--run-objective","--runObj","--run_obj"}, description="per target algorithm run objective type that we are minimizing", converter=RunObjectiveConverter.class)
	public RunObjective _runObj = null;
	
	
	
	public RunObjective getRunObjective()
	{
		if(_runObj == null)
		{
			throw new ParameterException("--run-obj must be set");
		} else
		{
			return _runObj;
		}
	}
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE, defaultValues="MEAN if --run-obj is QUALITY and MEAN10 if it is runtime")
	@Parameter(names={"--intra-obj","--intra-instance-obj","--overall-obj","--intraInstanceObj","--overallObj", "--overall_obj","--intra_instance_obj"}, description="objective function used to aggregate multiple runs for a single instance", converter=OverallObjectiveConverter.class)
	/**
	 * This deprecated warning is only to flag a compile error, this field is not really deprecated per say.
	 * @deprecated please use the getIntraInstanceObjective as it will resolve this if no option is specified 
	 * 
	 * 
	 */
	public OverallObjective intraInstanceObj = null;
	
	public OverallObjective getIntraInstanceObjective()
	{
		if(intraInstanceObj != null)
		{
			
			if(getRunObjective().equals(RunObjective.QUALITY) && !intraInstanceObj.equals(OverallObjective.MEAN))
			{
				//LoggerFactory.getLogger(getClass()).warn("Using a run objective of {} and an overall of objective of {} may not work correctly. You should probably only use {}", getRunObjective(), intraInstanceObj, OverallObjective.MEAN);
                            LoggerUtil.log("Using a run objective of {} and an overall of objective of {} may not work correctly. You should probably only use {}");
			}
			return intraInstanceObj;
		} else
		{
			switch(getRunObjective())
			{
			case RUNTIME:
				return OverallObjective.MEAN10;
			case QUALITY:
				return OverallObjective.MEAN;
			default:
				throw new IllegalStateException("Unknown run objective: " + getRunObjective());
			}
		}
		
	}
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--inter-obj","--inter-instance-obj","--interInstanceObj","--inter_instance_obj"}, description="objective function used to aggregate over multiple instances (that have already been aggregated under the Intra-Instance Objective)", converter=OverallObjectiveConverter.class)
	public OverallObjective interInstanceObj = OverallObjective.MEAN;
	
	@ParametersDelegate
	public TerminationCriteriaOptions limitOptions = new TerminationCriteriaOptions();
	
	@ParametersDelegate
	public ProblemInstanceOptions instanceOptions = new ProblemInstanceOptions();
	
	@UsageTextField(defaultValues="")
	@Parameter(names={"--scenario-file","--scenarioFile","--scenario"}, description="scenario file")
	@ParameterFile
	public File scenarioFile = null;
	
	@UsageTextField(defaultValues="<current working directory>/____-output", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--output-dir","--outputDirectory","--outdir"}, required=false, description="Output Directory")
	public String outputDirectory = System.getProperty("user.dir") + File.separator + "smac-output";

	
	public static final String invalidScenarioKey = "invalid-scenario-reason";
	@Parameter(names="--" + invalidScenarioKey, description="If this scenario file is invalid this a little notice that says why. This field is only used internally by verify-scenario", hidden= true)
	public String invalidScenarioReason;
	
	@ParametersDelegate
	public AlgorithmExecutionOptions algoExecOptions = new AlgorithmExecutionOptions();

	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig(String experimentDir)
	{
		ArrayList<String> arrList = new ArrayList<String>();
		arrList.add(experimentDir);
		if(scenarioFile!=null)
		{
			arrList.add(scenarioFile.getAbsoluteFile().getParentFile().getAbsolutePath() + File.separator);
		}
		
		return algoExecOptions.getAlgorithmExecutionConfig(arrList, true);
	}
	
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfigSkipExecDirCheck(String experimentDir)
	{
		ArrayList<String> arrList = new ArrayList<String>();
		arrList.add(experimentDir);
		if(scenarioFile!=null)
		{
			arrList.add(scenarioFile.getAbsoluteFile().getAbsolutePath() + File.separator);
		}
		
		return algoExecOptions.getAlgorithmExecutionConfig(arrList, false);
	}
	/**
	 * Gets both the training and the test problem instances
	 * 
	 * @param experimentDirectory			Directory to search for instance files
	 * @param trainingSeed					Seed to use for the training instances
	 * @param testingSeed					Seed to use for the testing instances
	 * @param trainingRequired				Whether the training instance file is required
	 * @param testRequired					Whether the test instance file is required
	 * @param trainingFeaturesRequired		Whether the training instance file is required
	 * @param testingFeaturesRequired		Whether the test instance file is required
	 * @return
	 * @throws IOException
	 */
	public TrainTestInstances getTrainingAndTestProblemInstances(String experimentDirectory, long trainingSeed, long testingSeed, boolean trainingRequired, boolean testRequired, boolean trainingFeaturesRequired, boolean testingFeaturesRequired) throws IOException
	{
		List<String> dirsToSearch = new ArrayList<String>();
		dirsToSearch.add(experimentDirectory);
		
		if(scenarioFile!=null)
		{
			dirsToSearch.add(scenarioFile.getAbsoluteFile().getParentFile().getAbsolutePath() + File.separator);
		}
		
		return this.instanceOptions.getTrainingAndTestProblemInstances(dirsToSearch, trainingSeed, testingSeed, this.algoExecOptions.deterministic, trainingRequired, testRequired, trainingFeaturesRequired, testingFeaturesRequired);
	}

	public void makeOutputDirectory(String runGroupName) {
		
		
		File outputDir = (new File(outputDirectory + File.separator + runGroupName));
		outputDir.mkdirs();
		
		if(outputDir.exists() && outputDir.isDirectory())
		{
			return;
		} else
		{
			throw new ParameterException("Output directory " + outputDir.getPath() + " does not exist and could not be created. Try executing the following command: mkdir " + outputDir.getPath());
		}
	}

	
}

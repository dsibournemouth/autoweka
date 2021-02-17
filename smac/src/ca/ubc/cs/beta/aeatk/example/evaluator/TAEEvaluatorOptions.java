package ca.ubc.cs.beta.aeatk.example.evaluator;

import java.io.File;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionOptions;
import ca.ubc.cs.beta.aeatk.logging.ConsoleOnlyLoggingOptions;
import ca.ubc.cs.beta.aeatk.logging.LoggingOptions;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorBuilder;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterFile;
import com.beust.jcommander.ParametersDelegate;

/**
 * Options needed for TAE evaluation. 
 * @author afrechet
 */
@UsageTextField(title="TAE Evaluator Options",description="Parameter defining a TAE and the instances to be executed with said TAE.",noarg=TAEEvaluatorNoArgumentHandler.class)
public class TAEEvaluatorOptions extends AbstractOptions{

    private static final long serialVersionUID = 1L;

    /**
	 * Scenario file.
	 */
	@ParameterFile
	@Parameter(names={"--scenario"}, description="file containing options to run a TAE evaluation.")
	public File ScenarioFile;
	
    @ParametersDelegate
    public LoggingOptions fLoggingOptions = new ConsoleOnlyLoggingOptions();
	
	/**
	 * Experiment directory.
	 */
	@UsageTextField(defaultValues="<current working directory>", level=OptionLevel.INTERMEDIATE)
    @Parameter(names={"--experiment-dir","--experimentDir","-e"}, description="root directory for experiments Folder")
    public String fExperimentDir = System.getProperty("user.dir") + File.separator + "";
	
	/**
	 * Instance options
	 */
	@ParametersDelegate
	public ProblemInstanceOptions fProblemInstanceOptions = new ProblemInstanceOptions();
	
	/**
	 * Available TAE options.
	 */
	/*DON'T MAKE THIS A PARAMETER.*/
	public final Map<String,AbstractOptions> fAvailableTAEOptions = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
	
	/**
	 * Algorithm execution options.
	 */
	@ParametersDelegate
	public AlgorithmExecutionOptions fAlgorithmExecutionOptions = new AlgorithmExecutionOptions();
	
	/**
	 * @return the TAE to evaluate.
	 */
	public TargetAlgorithmEvaluator getTAE()
	{
		return TargetAlgorithmEvaluatorBuilder.getTargetAlgorithmEvaluator(fAlgorithmExecutionOptions.taeOpts, true, false, fAvailableTAEOptions, null);
	}
	
	/**
	 * The name of the TAE evaluated.
	 */
	@Parameter(names={"--name","--algorithm-name"}, description = "TAE name (used for identification only)",required = true)
	public String fTAEName;
	
	/**
	 * The configuration of the TAE evaluated.
	 */
	@UsageTextField(defaultValues="DEFAULT")
	@Parameter(names={"--configuration","--config"}, description = "configuration to execute the TAE with.")
	public String fConfig = "DEFAULT";
	
	/**
	 * @return the configuration to use for the TAE.
	 */
	public ParameterConfiguration getConfiguration()
	{
		return fAlgorithmExecutionOptions.getAlgorithmExecutionConfig().getParameterConfigurationSpace().getParameterConfigurationFromString(fConfig, ParameterStringFormat.NODB_SYNTAX);
	}
	
	/**
	 * The output file's name.
	 */
	@Parameter(names={"--output-filename"}, description="name of a file to write output to (no extension)")
	private String fOutputFilename = null;
	
	
    public enum ReportType
    {
        JSON,
        CSV;
    }
    @Parameter(names={"--output-type"}, description = "type of output file to write")
	public ReportType fReportType = ReportType.JSON;
	
    @Parameter(names={"--overwrite-report"}, description = "whether to automatically overwrite output report if already present")
    public boolean fOverwriteReport = false;
    
	/**
	 * @return the name of the output file.
	 */
	public String getOuputFileName()
	{
		if(fOutputFilename==null)
		{
			return "TAEEvaluationOutput-"+fTAEName;
		}
		else
		{
			return fOutputFilename;
		}
	}
	
	/**
	 * The cutoff for the TAE evaluations.
	 */
	@Parameter(names={"--cutoff"}, description="cutoff time for TAE execution.", required = true)
	public double fCutoff;
	
	/**
	 * The seed for the TAE evaluations.
	 */
	@Parameter(names={"--seed"}, description="seed to use in evaluation.",required = true)
	public long fSeed;
	
	/**
	 * Whether to exit after submission or wait for evaluation completions.
	 */
	@Parameter(names={"--exit-after-submission"}, description="whether to exit right after submission; useful when submitting many runs to asynchronously")
	public boolean fExitAfterSubmission = false;
}
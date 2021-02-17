package ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ca.ubc.cs.beta.aeatk.misc.jcommander.converter.BinaryDigitBooleanConverter;
import ca.ubc.cs.beta.aeatk.misc.jcommander.converter.StringToDoubleConverterWithMax;
import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ZeroInfinityOpenInterval;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.Semantics;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParamConfigurationSpaceOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;


/**
 * Options object that defines arguments for Target Algorithm Execution
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */

@UsageTextField(title="Algorithm Execution Options", description="Options related to invoking the target algorithm")
public class AlgorithmExecutionOptions extends AbstractOptions {
	
	
	
	
	
	public AlgorithmExecutionOptions()
	{
		try {
			algoExecDir = new File(".").getCanonicalPath();
		} catch (IOException e) {
			System.out.flush();
			
			System.err.println("\n\n\nPlease report this error it occurred when trying to get the canonical path of the current working directory.");
			e.printStackTrace();
			System.err.println("Everything should work fine if you specify an exec-dir");
			
			System.err.flush();
			
		}
	}
	@Parameter(names={"--algo-exec","--algoExec", "--algo"}, description="command string to execute algorithm with", required=true)
	public String algoExec;
	
	
	@UsageTextField(defaultValues = "current working directory", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--algo-exec-dir","--exec-dir","--execDir","--execdir"}, description="working directory to execute algorithm in", required=false)
	public String algoExecDir; 
	
	@Parameter(names={"--algo-deterministic","--deterministic"}, description="treat the target algorithm as deterministic", converter=BinaryDigitBooleanConverter.class)
	public boolean deterministic = true;

	@Semantics(name="MAX_SUBRUN_CPUTIME", domain="OPT")
	@Parameter(names={"--algo-cutoff-time","--target-run-cputime-limit","--target_run_cputime_limit","--cutoff-time","--cutoffTime","--cutoff_time"}, description="CPU time limit for an individual target algorithm run", validateWith=ZeroInfinityOpenInterval.class)
	public double cutoffTime = Double.MAX_VALUE;
	
	@Semantics(name="MAX_SUBRUN_RUNLENGTH", domain="OPT")
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--algo-cutoff-length","--cutoffLength","--cutoff_length"}, description="cap limit for an individual run [not implemented currently]", converter=StringToDoubleConverterWithMax.class, hidden=true)
	public double cutoffLength = -1.0;
	
	
	@Parameter(names="-T", description="additional context needed for target algorithm execution (see TAE documentation for possible values, generally rare)", variableArity = true)
	public Map<String, String> additionalContext = new TreeMap<String, String>();
	
	@ParametersDelegate
	public TargetAlgorithmEvaluatorOptions taeOpts = new TargetAlgorithmEvaluatorOptions();
	
	@ParametersDelegate
	public ParamConfigurationSpaceOptions paramFileDelegate = new ParamConfigurationSpaceOptions();
	
	
	/**
	 * Gets an algorithm execution configuration
	 * 
	 * @return configured object based on the options
	 */
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig()
	{
		return getAlgorithmExecutionConfig(null);
	}
	
	/**
	 * Gets an algorithm execution configuration
	 * 
	 * @return configured object based on the options
	 */
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfigSkipDirCheck()
	{
		return getAlgorithmExecutionConfig(Collections.<String> emptyList(), false);
	}
	
	
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig(String experimentDir)
	{
		return getAlgorithmExecutionConfig(experimentDir, true);
	}
	
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig(String experimentDir, boolean checkExecDir)
	{
		if(experimentDir == null)
		{
			return getAlgorithmExecutionConfig(Collections.<String> emptyList(), checkExecDir);
		} else
		{
			return getAlgorithmExecutionConfig(Collections.singletonList(experimentDir), checkExecDir);
		}
		
	}
	/**
	 * Gets an algorithm execution configuration
	 * 
	 * @param inputDirs the experiment directory to search for parameter configurations (it is expected that the first one will be the experiment directory)
	 * @param checkExecDir  if <code>true</code> we will check that the execution directory exists
	 * @return configured object based on the options
	 */
	public AlgorithmExecutionConfiguration getAlgorithmExecutionConfig(List<String> inputDirs, boolean checkExecDir)
	{
		List<String> dirToSearch = new ArrayList<String>();
		if(inputDirs != null)
		{
			dirToSearch.addAll(inputDirs);
		}
		

		File execDir = new File(algoExecDir);
		if(checkExecDir)
		{
			if(!execDir.exists())
			{
				if (inputDirs.size()> 0)
				{
					//Only check if there is another place to look
					execDir = new File(inputDirs.get(0) + File.separator + algoExecDir);
				}
				
				if(!execDir.exists())
				{
					throw new ParameterException("Cannot find execution algorithm execution directory: " + algoExecDir +  "  in context:" + dirToSearch);
				}
			}
			
			dirToSearch.add(execDir.getAbsolutePath());
		}
		
		return new AlgorithmExecutionConfiguration(algoExec, execDir.getAbsolutePath(), paramFileDelegate.getParamConfigurationSpace(dirToSearch),  deterministic, this.cutoffTime,this.additionalContext );
	}
}

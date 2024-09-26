package ca.ubc.cs.beta.aeatk.state;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.state.converter.AutoAsMaxConverter;
import ca.ubc.cs.beta.aeatk.state.legacy.LegacyStateFactory;

import com.beust.jcommander.Parameter;

@UsageTextField(hiddenSection=true)
public class WarmStartOptions extends AbstractOptions {
	
	@CommandLineOnly
	@UsageTextField(defaultValues="N/A (No state is being warmstarted)", level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--warmstart", "--warmstart-from"}, description="location of state to use for warm-starting")
	public String warmStartStateFrom = null;
	
	@CommandLineOnly
	@UsageTextField(defaultValues="AUTO (if being restored)", level=OptionLevel.ADVANCED)
	@Parameter(names={"--warmstart-iteration"}, description="iteration of the state to use for warm-starting, use \"AUTO\" to automatically pick the last iteration", converter=AutoAsMaxConverter.class)
	public Integer restoreIteration = Integer.MAX_VALUE;
	
	public void getWarmStartState(ParameterConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfiguration execConfig, RunHistory rhToPopulate)
	{
		
		Logger log = LoggerFactory.getLogger(getClass());
		
		if(warmStartStateFrom != null)
		{
			log.debug("Warm-starting from folder {} " ,warmStartStateFrom);
			StateFactory sf = new LegacyStateFactory(null, warmStartStateFrom);
			sf.getStateDeserializer("it", restoreIteration, configSpace, instances, execConfig, rhToPopulate);
		}
		
	}

	

}

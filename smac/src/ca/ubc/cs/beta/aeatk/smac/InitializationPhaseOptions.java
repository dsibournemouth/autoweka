package ca.ubc.cs.beta.aeatk.smac;

import com.beust.jcommander.Parameter;

import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.options.enums.InitializationOptions;

@UsageTextField(title="Initalization Phase Options", description="Options related to how we warm up the automatic configurator [WARNING]: This probably has no effect in anything other than dSMAC at this moment")
public class InitializationPhaseOptions extends AbstractOptions {

	@Parameter(names={"--initType","--initializationType"},description="Describes how we will warm up the automatic configurator")
	public InitializationOptions initType = InitializationOptions.INCREMENTAL_CAPPING_K_CONFIGS;
	
	@Parameter(names={"--initialConfigs","--kConfigs"}, description="Number of initial configurations (Default will result in the number of cores)")
	public Integer kConfigs =  Runtime.getRuntime().availableProcessors();
	
	
	
	
}

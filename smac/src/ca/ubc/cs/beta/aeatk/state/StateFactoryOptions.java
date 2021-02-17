package ca.ubc.cs.beta.aeatk.state;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aeatk.state.converter.AutoAsMaxConverter;
import ca.ubc.cs.beta.aeatk.state.legacy.LegacyStateFactory;
import ca.ubc.cs.beta.aeatk.state.nullFactory.NullStateFactory;

@UsageTextField(hiddenSection = true)
public class StateFactoryOptions extends AbstractOptions{

	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--state-serializer","--stateSerializer"}, description="determines the format of the files to save the state in")
	public StateSerializers stateSerializer = StateSerializers.LEGACY;
	
	@CommandLineOnly
	@UsageTextField( level=OptionLevel.ADVANCED)
	@Parameter(names={"--state-deserializer","--stateDeserializer"}, description="determines the format of the files that store the saved state to restore")
	public StateSerializers statedeSerializer = StateSerializers.LEGACY;
	
	@CommandLineOnly
	@UsageTextField(defaultValues="N/A (No state is being restored)", level=OptionLevel.ADVANCED)
	@Parameter(names={"--restore-state-from","--restoreStateFrom"}, description="location of state to restore")
	public String restoreStateFrom = null;
	
	@CommandLineOnly
	@UsageTextField(defaultValues="N/A (No state is being restored)", level=OptionLevel.ADVANCED)
	@Parameter(names={"--restore-iteration","--restoreStateIteration","--restoreIteration"}, description="iteration of the state to restore, use \"AUTO\" to automatically pick the last iteration", converter=AutoAsMaxConverter.class)
	public Integer restoreIteration = null;
	
	/**
	 * Restore scenario is done before we parse the configuration and fixes input args
	 * in the input string to jcommander 
	 */
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--restore-scenario","--restoreScenario"}, description="Restore the scenario & state in the state folder")
	public File restoreScenario=null; 
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--clean-old-state-on-success","--cleanOldStateOnSuccess"}, description="will clean up much of the useless state files if smac completes successfully")
	public boolean cleanOldStatesOnSuccess = true;
	
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--save-context","--saveContext","--saveContextWithState" }, description="saves some context with the state folder so that the data is mostly self-describing (Scenario, Instance File, Feature File, Param File are saved)")
	public boolean saveContextWithState = true;
	
	
	public StateFactory getRestoreStateFactory(String outputDirectory, int numRun)
	{
	/*
	 * Build the Serializer object used in the model 
	 */
		StateFactory restoreSF;
		switch(statedeSerializer)
		{
			case NULL:
				restoreSF = new NullStateFactory();
				break;
			case LEGACY:
				restoreSF = new LegacyStateFactory(outputDirectory +  File.separator + "state-run" + numRun + File.separator, restoreStateFrom);
				break;
			default:
				throw new IllegalArgumentException("State Serializer specified is not supported");
		}
		
		return restoreSF;
	}


	public StateFactory getSaveStateFactory(String outputDir, int numRun) {

		StateFactory sf;
		switch(stateSerializer)
		{
			case NULL:
				sf = new NullStateFactory();
				break;
			case LEGACY:
				String savePath = outputDir + File.separator + "state-run" + numRun + File.separator;
				sf = new LegacyStateFactory(savePath, restoreStateFrom);
				break;
			default:
				throw new IllegalArgumentException("State Serializer specified is not supported");
		}
		
		return sf;
	}
	
	public void saveContextWithState(ParameterConfigurationSpace configSpace, InstanceListWithSeeds trainingILWS, File scenarioFile, StateFactory sf)
	{
		if(saveContextWithState)
		{
			sf.copyFileToStateDir(LegacyStateFactory.PARAM_FILE, new File(configSpace.getParamFileName()));
			
			String instanceFileAbsolutePath = trainingILWS.getInstanceFileAbsolutePath();
			if(instanceFileAbsolutePath != null)
			{
				sf.copyFileToStateDir("instances.txt", new File(instanceFileAbsolutePath));
			}
			
			String instanceFeatureFileAbsolutePath = trainingILWS.getInstanceFeatureFileAbsolutePath();
			
			if(instanceFeatureFileAbsolutePath != null)
			{
				sf.copyFileToStateDir("instance-features.txt", new File(instanceFeatureFileAbsolutePath));
			}
	
			File scenFile = scenarioFile;
			
			if ((scenFile != null) && (scenFile.exists()))
			{
				sf.copyFileToStateDir(LegacyStateFactory.SCENARIO_FILE, scenFile);
			}

		}
		
	}
	
	
	private static String[] getNamesForField(String field)
	{
		Field f;
		try {
			f = StateFactoryOptions.class.getField(field);
		} catch (SecurityException e) {
			throw new IllegalStateException("No permissions to read field", e);
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException("Field " + field + " doesn't exist");
		}
		
		
		Parameter p = f.getAnnotation(Parameter.class);
		
		if(p == null)
		{
			throw new IllegalStateException("Field " + field + " doesn't have correct annotation @Parameter");
		}
		
		return p.names();
		
	}
	public static String[] processScenarioStateRestore(String[] args) {
		
		
		ArrayList<String> inputArgs = new ArrayList<String>(Arrays.asList(args));
		
		
		ListIterator<String> inputIt =  inputArgs.listIterator();
		
		
		Iterator<String> firstPass = inputArgs.iterator();
		
		
		
		List<String> restoreIterationTags = Arrays.asList(getNamesForField("restoreIteration"));
		
		List<String> restoreScenarioTags = Arrays.asList(getNamesForField("restoreScenario"));
		
		
		
		
		boolean foundIteration = false;
		while(firstPass.hasNext())
		{
			String arg = firstPass.next();
			
			
			if(restoreIterationTags.contains(arg))
			{
				if(firstPass.hasNext())
				{
					foundIteration= true;
				}
			}
		}
		while(inputIt.hasNext())
		{
			String input = inputIt.next();
			
			if(restoreScenarioTags.contains(input))
			{
				if(!inputIt.hasNext())
				{
					throw new ParameterException("Failed to parse argument --restoreScenario expected 1 more argument");
				} else
				{
					String dir = inputIt.next();
					
					
					inputIt.add("--restoreStateFrom");
					inputIt.add(dir);
					if(!foundIteration)
					{
						inputIt.add("--restoreIteration");
						inputIt.add(String.valueOf(Integer.MAX_VALUE));
					}
					inputIt.add("--scenarioFile");
					inputIt.add(dir + File.separator + LegacyStateFactory.SCENARIO_FILE);
					
					
					if(new File(dir + File.separator + LegacyStateFactory.FEATURE_FILE).exists())
					{
						inputIt.add("--instanceFeatureFile");
						inputIt.add(dir + File.separator + LegacyStateFactory.FEATURE_FILE);
					} else if(new File(dir + File.separator + "instance-features.txt").exists())
					{
						inputIt.add("--instanceFeatureFile");
						inputIt.add(dir + File.separator + "instance-features.txt");
					}
					
					inputIt.add("--instanceFile");
					inputIt.add(dir + File.separator + LegacyStateFactory.INSTANCE_FILE);
					inputIt.add("--paramFile");
					//Old version of the file
					if(new File(dir + File.separator + "param-file.txt").exists())
					{
						inputIt.add(dir + File.separator + "param-file.txt");
					} else if(new File(dir + File.separator + LegacyStateFactory.PARAM_FILE).exists())
					{
						inputIt.add(dir + File.separator + LegacyStateFactory.PARAM_FILE);
					} else
					{
						throw new ParameterException("Couldn't find parameter file to restore scenario");
					}
					
					
					inputIt.add("--testInstanceFile");
					inputIt.add(dir + File.separator + LegacyStateFactory.INSTANCE_FILE);
					
				}
				
				
			}
			
		}
		
		return inputArgs.toArray(new String[0]);
	}
}

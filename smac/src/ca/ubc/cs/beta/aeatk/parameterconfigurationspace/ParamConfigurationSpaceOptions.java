package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.ReadableFileConverter;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Delegate for ParamConfigurationSpace objects
 * 
 */
@UsageTextField(hiddenSection = true)
public class ParamConfigurationSpaceOptions extends AbstractOptions{
	
	
	@Parameter(names={"--pcs-file","--param-file","-p", "--paramFile","--paramfile"}, description="File containing algorithm parameter space information in PCS format (see Algorithm Parameter File in the Manual). You can specify \"SINGLETON\" to get a singleton configuration space or \"NULL\" to get a null one.")
	public String paramFile;

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--search-subspace","--searchSubspace"}, description="Only generate random and neighbouring configurations with these values. Specified in a \"name=value,name=value,...\" format (Overrides those set in file)", required=false)
	public String searchSubspace;
	
	@UsageTextField( level=OptionLevel.DEVELOPER)
	@Parameter(names={"--search-subspace-file","--searchSubspaceFile"}, description="Only generate random and neighbouring configurations with these values. Specified each parameter on each own line with individual value", required=false, converter=ReadableFileConverter.class)
	public File searchSubspaceFile;
	
	@UsageTextField( level=OptionLevel.ADVANCED)
	@Parameter(names={"--continous-neighbours","--continuous-neighbors","--continuousNeighbours"}, description="Number of neighbours for continuous parameters")
	public int continuousNeighbours = 4;
	
	public Map<String,String> getSubspaceMap()
	{
		Map<String, String> map = new HashMap<String, String>();
		if(searchSubspaceFile != null)
		{
			BufferedReader reader = null;
			try {
				try {
					reader = new BufferedReader(new FileReader(searchSubspaceFile));
					
					String line;
					while((line = reader.readLine()) != null)
					{
						if(line.trim().length() == 0) continue;
						
						String[] args = line.split("=");
						
						if(args.length != 2)
						{
							throw new IllegalArgumentException("Invalid line specified in subspace file (--searchSubspaceFile), expected to have a name=value format: " + line);
						}
						
						String name = args[0].trim();
						String value = args[1].trim();
						
						map.put(name, value);
					}
					
					
					
				} catch (IOException e) {
					throw new IllegalStateException("Couldn't open search subspace file, even though it validated",e);
				}
			} finally
			{
				if(reader != null)
					try {
						reader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
		
		
		}
		
		
		if(searchSubspace != null)
		{
			String mysubSpace = searchSubspace.trim();
			String[] spaces = mysubSpace.split(",");
			
			for(String space : spaces)
			{
				if(space.trim().length() == 0) continue;
				String[] args = space.split("=");
				if(args.length != 2)
				{
					throw new IllegalArgumentException("Invalid parameter space declaration (--searchSubspace), something around here was the problem: " + space);
				}
				
				String name = args[0].trim();
				String value = args[1].trim();
				
				map.put(name,value);
			}
			
		}
		return map;
		
	}
	
	/**
	 * Creates a ParamConfigurationSpace based on the setting of the options in this object.
	 * @return ParamConfigurationSpace object
	 */
	public ParameterConfigurationSpace getParamConfigurationSpace()
	{
		return getParamConfigurationSpace(Collections.<String> emptyList());
	}
	
	/**
	 * Creates a ParamConfigurationSpace object based on the setting of the options in this object, searching for the file in the directories given in the list.
	 * 
	 * @param searchDirectories  directories to search for path (you do not need to include the current one)
	 * @return ParamConfigurationSpace object
	 */
	public ParameterConfigurationSpace getParamConfigurationSpace(List<String> searchDirectories)
	{
		
		if(this.paramFile == null)
		{
			throw new ParameterException("No PCS file specified, please check your command line options / scenario file");
		} else if(this.paramFile.trim().equals("SINGLETON"))
		{
			return ParameterConfigurationSpace.getSingletonConfigurationSpace();
		} else if(this.paramFile.trim().equals("NULL"))
		{
			return ParameterConfigurationSpace.getNullConfigurationSpace();
		}
		
		Logger log = LoggerFactory.getLogger(this.getClass());
		List<String> searchPaths = new ArrayList<String>();
		
		//==This will check the current working directory
		searchPaths.add("");
		
		
		ParameterConfigurationSpace configSpace = null;
		
		for(String searchDir : searchDirectories)
		{
			
			File f = new File(searchDir).getAbsoluteFile();
			searchPaths.add(f.getPath());
			while(f.getParent() != null)
			{
				f = f.getParentFile();
				searchPaths.add(f.getPath());
			}
		}

		for(String path : searchPaths)
		{
			try {
				
				if(path.trim().length() > 0)
				{
					path = path + File.separator;
				} 
				
				path += this.paramFile;
				
		
				configSpace = ParamFileHelper.getParamFileParser(path);
				log.trace("Configuration space found in " + path);
			} catch(IllegalStateException e)
			{ 
				if(e.getCause() instanceof FileNotFoundException)
				{
					//We don't care about this because we will just toss an exception if we don't find it
				} else
				{
					log.warn("Error occured while trying to parse {} is {}", path ,  e.getMessage() );
				}
				

			}
		}
			
		
		if(configSpace == null)
		{
			throw new ParameterException("Could not find a valid PCS file named " + this.paramFile  +  "  in any of the following locations: (" + searchPaths.toString()+ ") please check the file exists or for a previous error");
		} else
		{
			return configSpace;
		}
		
	}

}

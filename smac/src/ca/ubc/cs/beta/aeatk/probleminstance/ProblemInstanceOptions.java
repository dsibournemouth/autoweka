package ca.ubc.cs.beta.aeatk.probleminstance;



import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ca.ubc.cs.beta.aeatk.exceptions.FeatureNotFoundException;
import ca.ubc.cs.beta.aeatk.misc.options.CommandLineOnly;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.smac.executors.LoggerUtil;

@UsageTextField(hiddenSection = true)
public class ProblemInstanceOptions extends AbstractOptions{

	@CommandLineOnly
	@Parameter(names={"--instances","--instance-file","--instance-dir","--instanceFile","-i","--instance_file","--instance_seed_file"}, description="File or directory containing the instances to use for the scenario. If it's a file it must coform a specific format (see Instance File Format section of the manual), if it's a directory it you must also use the --instance-suffix option to restrict the match (unless all files have the same extension), and the instance list will be in sorted order.", required=false)
	public String instanceFile;

	@CommandLineOnly
	@UsageTextField(defaultValues="")
	@Parameter(names={"--feature-file","--instanceFeatureFile", "--feature_file"}, description="file that contains the all the instances features")
	public String instanceFeatureFile;
	
	@CommandLineOnly
	@Parameter(names={"--test-instances","--test-instance-file","--test-instance-dir","--testInstanceFile","--test_instance_file","--test_instance_seed_file"}, description="File or directory containing the instances to use for the scenario. If it's a file it must coform a specific format (see Instance File Format section of the manual), if it's directory you must also use the --test-instance-suffix option to restrict the match (unless all files have the same extension), , and the instance list will be in sorted order", required=false)
	public String testInstanceFile;

	@CommandLineOnly
	@UsageTextField(level=OptionLevel.ADVANCED)
	@Parameter(names={"--check-instances-exist","--checkInstanceFilesExist"}, description="check if instances files exist on disk")
	public boolean checkInstanceFilesExist = false;
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names="--use-instances", description="If false skips reading the instances and just uses a dummy instance")
	public boolean useInstances = true;
	
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--instance-suffix","--instance-regex"}, description="A suffix that all instances must match when reading instances from a directory. You can optionally specify a (java) regular expression but be aware that it is suffix matched (internally we take this string and append a $ on it)")
	public String instanceSuffix = null;
	
	@CommandLineOnly
	@UsageTextField(level=OptionLevel.INTERMEDIATE)
	@Parameter(names={"--test-instance-suffix","--test-instance-regex"}, description="A suffix that all instances must match when reading instances from a directory. You can optionally specify a (java) regular expression but be aware that it is suffix matched (internally we take this string and append a $ on it)")
	public String testInstanceSuffix = null;
	
	
	@CommandLineOnly
	@Parameter(names={"--skip-features","--ignore-features"}, description="If true the feature file will be ignored (if the feature file is required, this will cause an error, as if it was not supplied")
	public boolean ignoreFeatures = false;
	
	/**
	 * Gets the training problem instances
	 * @param experimentDirectory	Directory to search for instance files
	 * @param seed					Seed to use for the instances
	 * @param deterministic			Whether or not the instances should be generated with deterministic (-1) seeds
	 * @param required				Whether the instance file is required
	 */
	public InstanceListWithSeeds getTrainingProblemInstances(String experimentDirectory, long seed, boolean deterministic, boolean required, boolean featuresRequired) throws IOException
	{
		
		Logger log = LoggerFactory.getLogger(getClass());
		
		if(ignoreFeatures)
		{
			log.trace("Ignoring features as per command line option");
			this.instanceFeatureFile = null;
		}
		
		String instancesString = this.instanceFile;

	
		String instanceFeatureFile = this.instanceFeatureFile;

		
		if(!this.useInstances)
		{
			instancesString = getNoInstanceFile();
			instanceFeatureFile = null;
		} 
		
		if(instancesString == null)
		{
			if(required)
			{			
				throw new ParameterException("The instance file option --instances must be set");
			} else
			{
				return null;
			}
		}
	
		if(new File(instancesString).isDirectory())
		{
			instancesString = this.getInstanceDirectory(instancesString, this.instanceSuffix, "for training");
		}
		
		InstanceListWithSeeds ilws;
		
		
				
		try {
			ilws = ProblemInstanceHelper.getInstances(instancesString,experimentDirectory, instanceFeatureFile, checkInstanceFilesExist, seed, deterministic);
			
			
		} catch(FeatureNotFoundException e)
		{
			ProblemInstanceHelper.clearCache();
			if(featuresRequired || (instanceFeatureFile != null))
			{
				throw new ParameterException("Training instances require features and there was a problem loading features for all instances: " + e.getMessage());
			} else
			{
				ilws = ProblemInstanceHelper.getInstances(instancesString,experimentDirectory, null, checkInstanceFilesExist, seed, deterministic);
			}
			
			
			
		}
		
		
		
		
		//ilws = ProblemInstanceHelper.getInstances(options.scenarioConfig.instanceFile,options.experimentDir, options.scenarioConfig.instanceFeatureFile, options.scenarioConfig.checkInstanceFilesExist, pool.getRandom(SeedableRandomPoolConstants.INSTANCE_SEEDS).nextInt(), (options.scenarioConfig.algoExecOptions.deterministic));
		
		//instanceFileAbsolutePath = ilws.getInstanceFileAbsolutePath();
		//instanceFeatureFileAbsolutePath = ilws.getInstanceFeatureFileAbsolutePath();
	
		log.trace("Training Instance Seed Generator reports {} seeds ",  ilws.getSeedGen().getInitialInstanceSeedCount());
		if(ilws.getSeedGen().allInstancesHaveSameNumberOfSeeds())
		{
			log.trace("Training Instance Seed Generator reports that all instances have the same number of available seeds");
		} else
		{
			log.error("Training Instance Seed Generator reports that some instances have a different number of seeds than others");
			throw new ParameterException("All Training instances must have the same number of seeds in this version of SMAC");
		}
		
		return ilws;
		
		
		
	}
	/**
	 * Gets the testing problem instances
	 * @param experimentDirectory	Directory to search for instance files
	 * @param seed					Seed to use for the instances
	 * @param deterministic			Whether or not the instances should be generated with deterministic (-1) seeds
	 * @param required				Whether the instance file is required
	 */
	public InstanceListWithSeeds getTestingProblemInstances(String experimentDirectory, long seed, boolean deterministic, boolean required, boolean featuresRequired) throws IOException
	{
		

		Logger log = LoggerFactory.getLogger(getClass());
		if(ignoreFeatures)
		{
			log.trace("Ignoring features as per command line option");
			this.instanceFeatureFile = null;
		}
		
		String testInstancesString = this.testInstanceFile;
		
		
		String instanceFeatureFile = this.instanceFeatureFile;
		
		if(!this.useInstances)
		{
			testInstancesString = getNoInstanceFile();
			instanceFeatureFile = null;
		} 
		
		if(testInstancesString == null)
		{
			if(required)
			{			
				throw new ParameterException("The instance file option --test-instances must be set");
			} else
			{
				return null;
			}
		}
		
		
		
		if(new File(testInstancesString).isDirectory())
		{
			testInstancesString = this.getInstanceDirectory(testInstancesString, this.instanceSuffix,"for testing");
		}
		
	
		InstanceListWithSeeds ilws;
		
	
		try {
			ilws = ProblemInstanceHelper.getInstances(testInstancesString,experimentDirectory, instanceFeatureFile, checkInstanceFilesExist, seed, deterministic);
			
			
		} catch(FeatureNotFoundException e)
		{
			ProblemInstanceHelper.clearCache();
			if(featuresRequired)
			{
				throw new ParameterException("Testing instances require features and there was a problem loading features for all instances: " + e.getMessage());
			} else
			{
				ilws = ProblemInstanceHelper.getInstances(testInstancesString,experimentDirectory, null, checkInstanceFilesExist, seed, deterministic);
			}
			
			
			
		}
		
		
		//ilws = ProblemInstanceHelper.getInstances(options.scenarioConfig.instanceFile,options.experimentDir, options.scenarioConfig.instanceFeatureFile, options.scenarioConfig.checkInstanceFilesExist, pool.getRandom(SeedableRandomPoolConstants.INSTANCE_SEEDS).nextInt(), (options.scenarioConfig.algoExecOptions.deterministic));
		
		//instanceFileAbsolutePath = ilws.getInstanceFileAbsolutePath();
		//instanceFeatureFileAbsolutePath = ilws.getInstanceFeatureFileAbsolutePath();
	
		log.trace("Test Instance Seed Generator reports {} seeds ",  ilws.getSeedGen().getInitialInstanceSeedCount());
		if(ilws.getSeedGen().allInstancesHaveSameNumberOfSeeds())
		{
			log.trace("Test Instance Seed Generator reports that all instances have the same number of available seeds");
		} else
		{
			log.trace("Test Instance Seed Generator reports that some instances have a different number of seeds than others");
			throw new ParameterException("All Testing instances must have the same number of seeds in this version of SMAC");
		}
		
		return ilws;
	
		
		
	}
	
	/**
	 * Gets both the training and the test problem instances
	 * 
	 * @param experimentDirectory	Directory to search for instance files
	 * @param trainingSeed			Seed to use for the training instances
	 * @param testingSeed			Seed to use for the testing instances
	 * @param deterministic			Whether or not the instances should be generated with deterministic (-1) seeds
	 * @param trainingRequired		Whether the training instance file is required
	 * @param testRequired			Whether the test instance file is required
	 * @return
	 * @throws IOException
	 */
	public TrainTestInstances getTrainingAndTestProblemInstances(String experimentDirectory, long trainingSeed, long testingSeed, boolean deterministic, boolean trainingRequired, boolean testRequired, boolean trainingFeaturesRequired, boolean testingFeaturesRequired) throws IOException
	{
		
		InstanceListWithSeeds training = getTrainingProblemInstances(experimentDirectory, trainingSeed, deterministic, trainingRequired, trainingFeaturesRequired);
		InstanceListWithSeeds testing = getTestingProblemInstances(experimentDirectory, testingSeed, deterministic, testRequired, testingFeaturesRequired);

		return new TrainTestInstances(training, testing); 
	}
	
	/**
	 * Gets both the training and the test problem instances
	 * 
	 * @param experimentDirectory	Directory to search for instance files
	 * @param trainingSeed			Seed to use for the training instances
	 * @param testingSeed			Seed to use for the testing instances
	 * @param deterministic			Whether or not the instances should be generated with deterministic (-1) seeds
	 * @param trainingRequired		Whether the training instance file is required
	 * @param testRequired			Whether the test instance file is required
	 * @return
	 * @throws IOException
	 */
	public TrainTestInstances getTrainingAndTestProblemInstances(List<String> directories, long trainingSeed, long testingSeed, boolean deterministic, boolean trainingRequired, boolean testRequired, boolean trainingFeaturesRequired, boolean testingFeaturesRequired) throws IOException
	{
		
		directories.add((new File(".")).getAbsolutePath());
		
		Map<String, String> exceptionMessages = new LinkedHashMap<String, String>();
		Logger log = LoggerFactory.getLogger(getClass());
		
		Set<File> checkedDirectories = new HashSet<File>();
		
		Set<File> canonicalDirectories = new HashSet<File>();
		
		for(String dir : directories)
		{
			canonicalDirectories.add(new File(dir).getCanonicalFile());
		}
		
		for(String dir : directories)
		{
			try {
				
			if(checkedDirectories.contains(new File(dir).getCanonicalFile()))
			{
				continue;
			} else
			{
				checkedDirectories.add(new File(dir).getCanonicalFile());
			}
			InstanceListWithSeeds training = getTrainingProblemInstances(dir, trainingSeed, deterministic, trainingRequired, trainingFeaturesRequired);
			InstanceListWithSeeds testing = getTestingProblemInstances(dir, testingSeed, deterministic, testRequired, testingFeaturesRequired);
	
			
			return new TrainTestInstances(training, testing);
			} catch(ParameterException e)
			{
				
				
				log.trace("Ignore this exception for now: ", e);
				exceptionMessages.put(dir,  e.getMessage());
				
				if(canonicalDirectories.size() == 1)
				{
					throw e;
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for(Entry<String, String> ent : exceptionMessages.entrySet())
		{
			sb.append(ent.getKey() + "==>" + ent.getValue()).append("\n");
		}
		
		
		throw new ParameterException("Couldn't retrieve instances after searching several locations, errors for each location as follows: \n" + sb.toString());
		
		
	}
	
	

	public static class TrainTestInstances
	{
		private final InstanceListWithSeeds trainingInstances;
		private final InstanceListWithSeeds testInstances;
		public TrainTestInstances(InstanceListWithSeeds training,
				InstanceListWithSeeds testing) {
			// 
			this.trainingInstances = training;
			this.testInstances = testing;
		}
		public InstanceListWithSeeds getTrainingInstances() {
			return trainingInstances;
		}
		public InstanceListWithSeeds getTestInstances() {
			return testInstances;
		}
		
	}
	
	
	private String getNoInstanceFile()
	{
		
		
		try {
			
			
			File instanceFile = File.createTempFile("aeatoolkit-no-instance-", ".txt");
			FileWriter fWrite = new FileWriter(instanceFile);
	
			fWrite.append("no_instance\n");
			fWrite.close();
			instanceFile.deleteOnExit();
			
			
	
			
			
			return instanceFile.getAbsolutePath();
		} catch(IOException e)
		{
			throw new IllegalStateException("Couldn't create an instance file ");
		}
		
	}
	
	private String getInstanceDirectory(String instanceDirName, String instanceSuffix, String motive)
	{
		
		File f = new File(instanceDirName);
		
		Logger log = LoggerFactory.getLogger(ProblemInstanceOptions.class);
		if(!f.isDirectory())
		{
			throw new IllegalArgumentException("This function can only be called with a directory");
		}
		
		Pattern p = Pattern.compile("\\.([^\\.]*)$");
		

		if(instanceSuffix == null)
		{
			String foundExtension = null;
			for(String file : f.list())
			{
				Matcher match = p.matcher(file);
				if(match.find())
				{
					if(foundExtension == null)
					{
						foundExtension = match.group(1);
						log.trace("Using extension {} for instance directory", foundExtension );
					} else if (!foundExtension.equals(match.group(1)))
					{
						throw new ParameterException("You must specify an instance suffix for directory (using --instance-suffix) unless all files have the same extension, detected extensions in the specified directory: ." + foundExtension + " and ." + match.group(1));
					}
				}
			}
			
			instanceSuffix = foundExtension;
			
		}
		
		
		p = Pattern.compile(instanceSuffix + "$");
		
		List<String> foundInstances = new ArrayList<String>();
		
		String instanceDirPrefix = instanceDirName;
		if(!instanceDirPrefix.endsWith(File.separator))
		{
			instanceDirPrefix += File.separator;
		}
		for(String file : f.list())
		{
			Matcher match = p.matcher(file);
			if(match.find())
			{
				foundInstances.add(instanceDirPrefix+file);
			}
		}
			
	
		if(foundInstances.size() == 0)
		{
			throw new ParameterException("Instance directory specified " + instanceDirPrefix +  " and suffix \"" + instanceSuffix + "\" do not match any instances");
		}
		Collections.sort(foundInstances);
		
		try {
			
			
			File instanceFile = File.createTempFile("aeatoolkit-instances-", ".txt");
			FileWriter fWrite = new FileWriter(instanceFile);
			
			
			
			
			StringBuilder sb = new StringBuilder();
			
			
			for(String fString : foundInstances)
			{
				String line = fString.replaceAll("\\\\",Matcher.quoteReplacement("\\\\")) + "\n";
				fWrite.append(line);
				
				if(log.isTraceEnabled())
				{
					sb.append(line);
				}
				
			}
			fWrite.close();
			instanceFile.deleteOnExit();
			
			
			if(log.isDebugEnabled())
			{
				//log.debug("Detected {} instances {} with suffix {}, file created and written to {} ",foundInstances.size(),motive,instanceSuffix,  instanceFile.getAbsolutePath()) ;
                            LoggerUtil.log("Detected {} instances {} with suffix {}, file created and written to {} ");
			} else
			{
				//log.info("Detected {} instances {} with suffix {} ",foundInstances.size(),motive,instanceSuffix) ;
                                LoggerUtil.log("Detected {} instances {} with suffix {} ");
			}
			
			log.trace("Auto generated instance file {} has content:\n{}", instanceFile.getAbsolutePath(), sb.toString());
			
			return instanceFile.getAbsolutePath();
		} catch(IOException e)
		{
			throw new IllegalStateException("Couldn't create an instance file ");
		}
		
		
		
	
		
		
		
		
	}
}

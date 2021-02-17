package ca.ubc.cs.beta.aeatk.probleminstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.aeatk.exceptions.FeatureNotFoundException;
import ca.ubc.cs.beta.aeatk.misc.csvhelpers.ConfigCSVFileHelper;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.RandomInstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.SetInstanceSeedGenerator;

import com.beust.jcommander.ParameterException;

/**
 * Helper class to get Parse Instance files and get the corresponding instances
 * @author sjr
 *
 */
public class ProblemInstanceHelper {
	
	/**
	 * Returns a file for the given context  & path
	 * @param context string specifying location on disk to look for file (Used if the path is relative)
	 * @param path 	  string specifying the path name of the file
	 * @return	File representing the path
	 */
	private static File getFileForPath(String context, String path)
	{
		File f;
		
		File f2 = new File(path);
		
		if(path.length() > 0 && f2.isAbsolute())
		{
		
			f = new File(path);
		} else
		{
			f = new File(context + File.separator + path);
		}
		
		if(!f.exists())
		{
			//TODO take a full path c/d/e and a context a/b/c and somehow get a/b/c/d/e 		
			f = new File(context + File.separator + new File(path).getName());
			if(!f.exists())
			{
				throw new ParameterException("Could not find needed file:" + path + " Context:" + context);
			}
		}
		
		return f;
	}
	

	private static Logger logger = LoggerFactory.getLogger(ProblemInstanceHelper.class);
	
	
	/**
	 * Stores a list of cached instances
	 */
	private static final Map<String, ProblemInstance> cachedProblemInstances = new HashMap<String, ProblemInstance>();

	/**
	 * Clears the list of cached instances
	 * 
	 */
	public static void clearCache()
	{
		cachedProblemInstances.clear();
		
	}
	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename  				string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk 
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, boolean checkFileExistsOnDisk) throws IOException
	{
		return getInstances(filename, experimentDir, null, checkFileExistsOnDisk);
	}
	
	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename  				string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk
	 * @param deterministic				boolean specifying whether the instances should be loaded with a deterministic seed generator 
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, boolean checkFileExistsOnDisk, boolean deterministic) throws IOException
	{
		return getInstances(filename, experimentDir, null, checkFileExistsOnDisk, deterministic);
	}
	
	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename  				string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param featureFileName			string with instance feature filename filename
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk) throws IOException	{
	
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, 0, Integer.MAX_VALUE);
	}

	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename  				string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param featureFileName			string with instance feature filename filename
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk
	 * @param seed						long with the seed we should populate the InstanceSeedGenerator with
	 * @param deterministic				boolean specifying whether the instances should be loaded with a deterministic seed generator 
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, long seed, boolean deterministic) throws IOException
	{
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, seed, Integer.MAX_VALUE, deterministic);
	}
	
	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename  				string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param featureFileName			string with instance feature filename filename
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk
	 * @param seed						long with the seed we should populate the InstanceSeedGenerator with
	 * @param maxSeedsPerInstance		maximum number of seeds to allow for any instance
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, long seed, int maxSeedsPerInstance) throws IOException
	{
		
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, seed, Integer.MAX_VALUE, false);
	}
	
	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename  				string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param featureFileName			string with instance feature filename filename
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk
	 * @param deterministic				boolean specifying whether the instances should be loaded with a deterministic seed generator 
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, boolean deterministic) throws IOException	{
		
		return getInstances(filename, experimentDir, featureFileName, checkFileExistsOnDisk, 0, Integer.MAX_VALUE, deterministic);
	}

	/**
	 * Returns the InstanceList and Seed Generator for the given parameters 
	 * 
	 * <b>NOTE:</b> If checkFileExistsOnDisk is <code>true</code> all instances will be transformed to use absolute path names
	 * 
	 * @param filename					string with instancelist filename
	 * @param experimentDir				string with experiment directory
	 * @param featureFileName			string with instance feature filename filename
	 * @param checkFileExistsOnDisk		boolean specifying if we should check if the instance name exists on disk
	 * @param seed						long with the seed we should populate the InstanceSeedGenerator with
	 * @param maxSeedsPerInstance		maximum number of seeds to allow for any instance
	 * @param deterministic				boolean specifying whether the instances should be loaded with a deterministic seed generator 
	 * @return InstanceListWithSeeds 	representing the instance file
	 * @throws IOException				when a problem prevents us from accessing the file
	 * @throws ParameterException		when a logical error in the file exists
	 */
	public static InstanceListWithSeeds getInstances(String filename, String experimentDir, String featureFileName, boolean checkFileExistsOnDisk, long seed, int maxSeedsPerInstance, boolean deterministic) throws IOException {
		
		
		if(experimentDir == null)
		{
			throw new ParameterException("Experiment directory cannot be null");
		}
		
		
		
		logger.trace("Loading instances from file: {} and experiment dir {}", filename, experimentDir);
		
		
		List<ProblemInstance> instances = new ArrayList<ProblemInstance>();
		Set<ProblemInstance> instancesSet = new HashSet<ProblemInstance>();
		
		String instanceFileAbsolutePath = null;
		
		String instanceFeatureFileAbsolutePath = null;
		
		String line = "";
		
		
		int instID=1; 
		
		
		/*
		 * Maps from an instance name to a name/value pair of features
		 */
		Map<String, Map<String, Double>> featuresMap = new LinkedHashMap<String, Map<String, Double>>();
		
		int numberOfFeatures = 0;
		boolean numericFeatureNames = false;
		if(featureFileName != null)
		{
			//=======Parse Features=====
			logger.trace("Feature File specified reading features from: {} ", new File(featureFileName).getAbsolutePath());
			File featureFile = getFileForPath(experimentDir, featureFileName);
			
			if(!featureFile.exists())
			{
				throw new ParameterException("Feature file given does not exist " + featureFile);
			}
			
			instanceFeatureFileAbsolutePath = featureFile.getAbsolutePath();
			
			CSVReader featureCSV = new CSVReader(new InputStreamReader(new FileInputStream(featureFile)));
			
			ConfigCSVFileHelper features = new ConfigCSVFileHelper(featureCSV.readAll(),1,1);
			
			featureCSV.close();
			
			numberOfFeatures = features.getNumberOfDataColumns();
			
			int column=2;
			for(String key : features.getDataKeyList())
			{
				try {
					Double.valueOf(key);
					numericFeatureNames = true;
					
					throw new ParameterException("Column " + column + " of feature file seems to have a numeric name:  " + key + " this is forbidden. All Feature Files must have a header row that identifies for each feature a non-numeric name");
					
				} catch(NumberFormatException e)
				{
					//This is what we want
				}
				column++;
			}
			logger.trace("Feature File specifies: {} features for {} instances", numberOfFeatures, features.getNumberOfDataRows() );
			
			
			for(int i=0; i  < features.getNumberOfDataRows(); i++)
			{
				TreeMap<String, Double> instFeatMap = new TreeMap<String, Double>();
				
				featuresMap.put(features.getKeyForDataRow(i), Collections.unmodifiableMap(instFeatMap));
				
				String lastValue = "";
				try {
					for (int j=0; j < features.getNumberOfDataColumns(); j++)
					{		
							String key = features.getDataKeyByIndex(j);
							lastValue = features.getStringDataValue(i, j);
							Double value = features.getDoubleDataValue(i, j);
							instFeatMap.put(key, value);
					}
				} catch(NumberFormatException e)
				{
					e.printStackTrace();
					for(int j=0; j < lastValue.length(); j++)
					{
						
						System.out.println(j +":" + lastValue.charAt(j) + ":" + Integer.toHexString(lastValue.getBytes()[j]));
					}
					throw new ParameterException("Could not parse feature file "+ featureFileName + " , error on line " + i + " expected double value but got string:" + e.getMessage() + " value " + lastValue );
				}
			}
		}
		
		
		/*
		 * Stores our list of instances that we will return 
		 */
		List<String> instanceList = new ArrayList<String>(featuresMap.size());
		
		/*
		 * Stores the instance seed generator we will return
		 */
		InstanceSeedGenerator gen;
		
		
		/*
		 * Map containing the instance names to instance specific information
		 */
		Map<String, String> instanceSpecificInfo = Collections.emptyMap();
		if(filename != null)
		{	
			//====Parse Instance File=====
			if(filename.trim().equals(""))
			{
				throw new ParameterException("File name is specified but empty");
			}
			File instanceListFile = getFileForPath(experimentDir, filename);
			instanceFileAbsolutePath = instanceListFile.getAbsolutePath();
			logger.trace("Reading instances from file {}", instanceFileAbsolutePath);
			InstanceListWithSeeds insc = getListAndSeedGen(instanceListFile,seed, maxSeedsPerInstance);
			instanceList = insc.getInstancesByName();
			gen = insc.getSeedGen();
			instanceSpecificInfo = insc.getInstanceSpecificInfo();
			
		} else
		{   
			//====Just use Instances specified in Feature File====
			instanceList.addAll(featuresMap.keySet());
			logger.trace("Reading instances from feature file");
			gen = new RandomInstanceSeedGenerator(instanceList.size(), seed, maxSeedsPerInstance);
			
		}
		
		/*
		 * Stores instance names from the features map that we haven't matched to features 
		 */
		Set<String> unMappedFeatureMapEntries = new HashSet<String>();
		unMappedFeatureMapEntries.addAll(featuresMap.keySet());
		
		
		for(String instanceFile : instanceList)
		{
			
			String originalInstanceFilename = instanceFile;
			if(checkFileExistsOnDisk)
			{
				File f = getFileForPath(experimentDir, instanceFile);
				
				//Should store the absolute file name if the file exists on disk
				//If we don't check if the file exists on disks we don't know whether to add experimentDir to it

				
				instanceFile = f.getAbsolutePath();
				if(!f.exists())
				{					
					throw new ParameterException("Instance does not exist on disk "+ f.getAbsolutePath());
				}
			
				
			}
			
			
			/*
			 * Map of features for instance
			 */
			Map<String, Double> features;
			
			
			if(featureFileName != null)
			{
				
				features = featuresMap.get(instanceFile.trim());
				
				if(features == null)
				{
					throw new FeatureNotFoundException("Feature file : " + featureFileName + " does not contain feature data for instance: " + instanceFile + " previous versions were much more forgiving about this but please make sure the feature file has an entry that matches that exactly.");
				}
				
				
				if(features.size() != numberOfFeatures)
				{
					if(numericFeatureNames)
					{
						throw new ParameterException("Feature file : " + featureFileName + " is almost certainly missing a header row identifying each of the features in it. Otherwise there is a mismatch between the number of features we found for this instance and the number expected: " +  instanceFile + " found: " + features.size() + " but expected: " + numberOfFeatures );
					} else
					{
						throw new ParameterException("Feature file : " + featureFileName + " contains " + features.size() + " for instance: " + instanceFile +  " but expected " + numberOfFeatures );
					}
					
				}
			} else
			{	//No features loaded
				features = Collections.emptyMap();
			}
			
			//Removes // from filenames as some files had this for some reason
			//We don't use Absolute File Name, because they may not actually exist
			instanceFile = instanceFile.replaceAll("//", "/");
			ProblemInstance ai;
			
			if(cachedProblemInstances.containsKey(instanceFile))
			{
				
				
				ai = cachedProblemInstances.get(instanceFile);
				
				if(ai.getFeatures().size() > 0 && features.size() > 0)
				{
					if(!ai.getFeatures().equals(features))
					{
						logger.warn("We previously loaded an instance for filename {} but the instance Features don't match", instanceFile);
					}
				}
				
				
			} else
			{
				ai = new ProblemInstance(instanceFile, instID++, features, instanceSpecificInfo.get(originalInstanceFilename));
				cachedProblemInstances.put(instanceFile, ai);
			}
			
			

			if(instancesSet.contains(ai))
			{
				logger.warn("Instance file seems to contain duplicate entries for the following filename {}", line);
			}
			
			instances.add(ai);
			instancesSet.add(ai);
			
		}
		
		
		/*
		 * Stores all the instances we got from the feature file. Typically we use 50/50 split of test and training set
		 * so we use 2*instance.size() for this.
		 */
		List<ProblemInstance> instancesFromFeatures = new ArrayList<ProblemInstance>(instances.size() * 2);
		
		instancesFromFeatures.addAll(instances);
		
		for(String instanceFromFeatureFile : unMappedFeatureMapEntries)
		{
			instancesFromFeatures.add(new ProblemInstance(instanceFromFeatureFile, instID++, featuresMap.get(instanceFromFeatureFile)));
		}
		
	
		
		if(deterministic)
		{
			if(gen instanceof SetInstanceSeedGenerator)
			{
				logger.warn("Detected that seeds have been preloaded, yet the algorithm is listed as deterministic, generally this means we should use -1 as a seed");
			} else
			{
				logger.trace("Deterministic algorithm, selecting hard coded instance seed generator");
				
				LinkedHashMap<String, List<Long>> instanceSeedMap = new LinkedHashMap<String, List<Long>>(); 
				
				for(ProblemInstance pi : instances)
				{
					List<Long> l = new ArrayList<Long>(1);
					l.add(-1L);
					
					instanceSeedMap.put(pi.getInstanceName(),l);
				}
				
				gen = new SetInstanceSeedGenerator(instanceSeedMap,instanceList, 1);

			}
			
		}
		
		logger.debug("Instances loaded from file named: {}", filename);
		return new InstanceListWithSeeds(gen, instances, instancesFromFeatures, instanceFileAbsolutePath, instanceFeatureFileAbsolutePath);
		
		
	}

	enum InstanceFileFormat
	{
		NEW_CSV_SEED_INSTANCE_PER_ROW,
		NEW_CSV_INSTANCE_PER_ROW,
		NEW_INSTANCE_SPECIFIC_PER_ROW,
		NEW_SEED_INSTANCE_SPECIFIC_PER_ROW,
		LEGACY_INSTANCE_PER_ROW,
		LEGACY_SEED_INSTANCE_PER_ROW, 
		LEGACY_INSTANCE_SPECIFIC_PER_ROW,
		LEGACY_SEED_INSTANCE_SPECIFIC_PER_ROW
	}
	/**
	 * Parses an instance file with the given parameter
	 * @param instanceListFile 		file to parse
	 * @param seed					seed to use for InstanceSeedGenerators
	 * @param maxSeedsPerConfig		max allowed seeds per instance for InstanceSeedGenerators
	 * @return InstanceListwithSeeds containing the parsed information in string formats
	 * @throws IOException		if an error occurs while parsing the file
	 */
	private static InstanceListWithSeeds getListAndSeedGen(File instanceListFile, long seed, int maxSeedsPerConfig) throws IOException {
		
		String line;
		
		List<String> instanceList = new LinkedList<String>();
		
		
		logger.trace("Reading instance file detecting format");
		
		LinkedHashMap<String, List<Long>> instances;
		LinkedHashMap<String, String> instanceSpecificInfo;
		List<String> declaredInstanceOrderForSeeds = null;
		try
		{
			CSVReader reader = new CSVReader(new FileReader(instanceListFile),',','"',true);
			List<String[]> csvContents = reader.readAll();
			ValueObject v = parseCSVContents(csvContents, InstanceFileFormat.NEW_CSV_INSTANCE_PER_ROW, InstanceFileFormat.NEW_CSV_SEED_INSTANCE_PER_ROW, InstanceFileFormat.NEW_INSTANCE_SPECIFIC_PER_ROW, InstanceFileFormat.NEW_SEED_INSTANCE_SPECIFIC_PER_ROW);
			instances = v.instanceSeedMap;
			instanceSpecificInfo = v.instanceSpecificInfoMap;
			declaredInstanceOrderForSeeds = v.declaredInstanceOrderForSeeds;
			reader.close();
		} catch(IllegalArgumentException e)
		{
			try { 
			
			/**
			 * For the old format we trim each line to get rid of spurious whitespace
			 */
			BufferedReader bufferedReader = new BufferedReader(new FileReader(instanceListFile));
			
			StringBuilder sb = new StringBuilder();
			while((line = bufferedReader.readLine()) != null)
			{
				sb.append(line.trim()).append("\n");
			}
			
			bufferedReader.close();
			
			
				
			CSVReader reader = new CSVReader(new StringReader(sb.toString().trim()),' ');
			List<String[]> csvContents = reader.readAll();
			ValueObject v = parseCSVContents(csvContents, InstanceFileFormat.LEGACY_INSTANCE_PER_ROW, InstanceFileFormat.LEGACY_SEED_INSTANCE_PER_ROW, InstanceFileFormat.LEGACY_INSTANCE_SPECIFIC_PER_ROW, InstanceFileFormat.LEGACY_SEED_INSTANCE_SPECIFIC_PER_ROW);
			instances = v.instanceSeedMap;
			instanceSpecificInfo = v.instanceSpecificInfoMap;
			declaredInstanceOrderForSeeds = v.declaredInstanceOrderForSeeds;
			reader.close();
			} catch(IllegalArgumentException e2)
			{
				throw new ParameterException("Could not parse instanceFile " + instanceListFile.getAbsolutePath());
			}
		}
		InstanceSeedGenerator gen;
		//We check if some entry has a non zero amount of seeds (if we are in an instance seed pair file all entries must have atleast one)
		//Then we use our manual instance seed generator
		if(instances.entrySet().iterator().next().getValue().size() > 0)
		{
			if(declaredInstanceOrderForSeeds == null)
			{
				throw new IllegalStateException("Expected instanceOrder to be specified, got null.");
			}
			gen = new SetInstanceSeedGenerator(instances,declaredInstanceOrderForSeeds, maxSeedsPerConfig);
		} else
		{
			gen = new RandomInstanceSeedGenerator(instances.size(),seed, maxSeedsPerConfig);
		}
		
		
		instanceList.addAll(instances.keySet());
		return new InstanceListWithSeeds(gen, null, instanceList, instanceSpecificInfo);
	}
	
	static class ValueObject
	{
		public List<String> declaredInstanceOrderForSeeds;
		public LinkedHashMap<String, List<Long>> instanceSeedMap;
		public LinkedHashMap<String, String> instanceSpecificInfoMap;
	}
	
	/**
	 * Parses the CSV File Contents and tries to auto detect the format of the given arguments
	 * @param csvContents			list of string arrays containing the instance file
	 * @param instanceOnly			flag for file format if it only contains instances
	 * @param seedPair				flag for file format if it contains seeds and instances
	 * @param instanceSpecific		flag for file format if it contains instance specific information and instances
	 * @param instanceSpecificSeed	flag for file format if it contains seeds, instances, and instance specific information.
	 * @return
	 */
	private static ValueObject parseCSVContents(List<String[]> csvContents, InstanceFileFormat instanceOnly, InstanceFileFormat seedPair, InstanceFileFormat instanceSpecific, InstanceFileFormat instanceSpecificSeed )
	{
		InstanceFileFormat possibleFormat = null;
	
		/**
		 * Note we make the determination of which instanceSeedGenerator to use based on the first entries list size()
		 */
		LinkedHashMap<String, List<Long>> instanceSeedMap = new LinkedHashMap<String, List<Long>>();
		
		/**
		 * Note we make the determination of which instanceSeedGenerator to use based on the first entries list size()
		 */
		LinkedHashMap<String, String> instanceSpecificInfoMap = new LinkedHashMap<String, String>();
		
		List<String> problemInstanceDeclaredOrder = new ArrayList<String>();
		
		for(String[] s : csvContents)
		{
			
			if(s.length == 1)
			{
				if(s[0].trim().equals("")) throw new IllegalArgumentException();
				
				if(possibleFormat == null)
				{
					possibleFormat = instanceOnly;
					logger.trace("Line with only 1 entry found, trying {}", possibleFormat);
				}
				if(possibleFormat == instanceOnly)
				{
					
					instanceSeedMap.put(s[0], new LinkedList<Long>());
				} else
				{
					logger.trace("Line with only 1 entry found, we are not {}",possibleFormat);
					throw new IllegalArgumentException();
				}
			} else if(s.length == 2)
			{
			
				if(possibleFormat == null)
				{
					try {
						possibleFormat = seedPair;
						logger.trace("Line with only 2 entries found, trying {}", possibleFormat);
						Long.valueOf(s[0]);
						possibleFormat = seedPair;
					} catch(NumberFormatException e)
					{
						possibleFormat = instanceSpecific;
						logger.trace("First entry on line 1 not a long value, trying {}", possibleFormat);
					}
					
					
				}
				
				
				if(possibleFormat.equals(seedPair))
				{
					String instanceName = s[1];
					try {
						if(instanceSeedMap.get(instanceName) == null)
						{
							instanceSeedMap.put(instanceName, new LinkedList<Long>());
						}
						
					
					instanceSeedMap.get(instanceName).add(Long.valueOf(s[0]));
					} catch(NumberFormatException e)
					{
						logger.trace("{} is not a valid long value", s[0]);
						
						throw new IllegalArgumentException();
					}
					
					problemInstanceDeclaredOrder.add(instanceName);
				} else if(possibleFormat.equals(instanceSpecific))
				{
					String instanceName = s[0];
					String instanceSpecificInfo = s[1];
					
					instanceSpecificInfoMap.put(instanceName, instanceSpecificInfo);
					instanceSeedMap.put(instanceName, new LinkedList<Long>());
				} else
				{
					logger.trace("Line with 2 entries found, we are not {}",possibleFormat);
					throw new IllegalArgumentException();
				}
			
			} else if(s.length == 3)
			{
				if(possibleFormat == null)
				{
					possibleFormat = instanceSpecificSeed;
				}
			
				if(possibleFormat == instanceSpecificSeed)
				{
					
					String instanceName = s[1];
					if(s[1].trim().length() == 0)
					{
						logger.trace("\"{}\" is not a valid instance name (All Whitespace)", s[1]);
						throw new IllegalArgumentException();
					}
					
					if(instanceSeedMap.get(instanceName) == null)
					{
						instanceSeedMap.put(instanceName, new LinkedList<Long>());
					}
					
					try
					{
						instanceSeedMap.get(instanceName).add(Long.valueOf(s[0]));
					
					} catch(NumberFormatException e)
					{
						logger.trace("{} is not a valid long value", s[0]);
						
						throw new IllegalArgumentException();
					}
					
					s[2] = s[2].trim();
					if(instanceSpecificInfoMap.get(instanceName) != null)
					{
						if(!s[2].equals(instanceSpecificInfoMap.get(instanceName)))
						{
							Object[] args = {instanceName, s[2], instanceSpecificInfoMap.get(instanceName)};
							logger.trace("Discrepancy detected in instance specific information {} had {} vs. {}  (This is not permitted)", args );
							throw new IllegalArgumentException();
						}
					} else
					{
						instanceSpecificInfoMap.put(instanceName, s[2]);
					}
					
					problemInstanceDeclaredOrder.add(instanceName);
					
				} else
				{
					logger.trace("Line with 3 entries found, we are not {}", possibleFormat);
					throw new IllegalArgumentException();
				}
				
			} else
			{
				logger.trace("Line with {} entries found unknown format", s.length);
				possibleFormat = null;
				throw new IllegalArgumentException();
			}
	}
			if(instanceSeedMap.size() == 0) throw new IllegalArgumentException("No Instances Found");
			ValueObject v = new ValueObject();
			v.instanceSeedMap = instanceSeedMap;
			v.instanceSpecificInfoMap = instanceSpecificInfoMap;
			v.declaredInstanceOrderForSeeds = problemInstanceDeclaredOrder;
			return v;
	}
	
	public static boolean isVerifySATCompatible(Collection<ProblemInstance> pis)
	{
		HashSet<String> validValues = new HashSet<String>();
		
		validValues.add("SAT");
		validValues.add("SATISFIABLE");
		validValues.add("UNKNOWN");
		validValues.add("UNSAT");
		validValues.add("UNSATISFIABLE");
		
		for(ProblemInstance pi : pis)
		{
			if(!validValues.contains(pi.getInstanceSpecificInformation()))
			{
				return false;
			
			}
		}
		
		return true;
	}
	
	
}

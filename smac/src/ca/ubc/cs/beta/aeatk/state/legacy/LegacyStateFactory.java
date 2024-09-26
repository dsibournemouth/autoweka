package ca.ubc.cs.beta.aeatk.state.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.exceptions.StateSerializationException;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.state.StateDeserializer;
import ca.ubc.cs.beta.aeatk.state.StateFactory;
import ca.ubc.cs.beta.aeatk.state.StateSerializer;

/**
 * Supports saving and restoring files from disk in a way that is mostly compatible with MATLAB
 * 
 * 
 * @author seramage
 */
public class LegacyStateFactory implements StateFactory{

	
	

	public static final String RUNS_AND_RESULTS_FILENAME = "runs_and_results";
	public static final String PARAMSTRINGS_FILENAME = "paramstrings";
	public static final String UNIQ_CONFIGURATIONS_FILENAME = "uniq_configurations";
	
	static final String OBJECT_MAP_KEY = "OBJECT_MAP_KEY";
	static final String ITERATION_KEY = "ITERATION_KEY";
	static final String INCUMBENT_TEXT_KEY = "INCUMBENT_TEXT_KEY";
	
	

	public static final String SCENARIO_FILE = "scenario.txt";
	public static final String PARAM_FILE = "param.pcs";
	public static final String FEATURE_FILE = "instance-features.csv";
	public static final String INSTANCE_FILE = "instances.txt";
	
	private final String saveStatePath;
	private final String restoreFromPath;
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	
	
	
	/**
	 * Stores for each iteration a set of files written
	 */
	private final ConcurrentSkipListMap<Integer, Set<String>> savedFilesPerIteration = new ConcurrentSkipListMap<Integer, Set<String>>();
	
	
	/**
	 * Constructs the LegacyStateFactory
	 * @param saveStatePath 	Where we should save files to
	 * @param restoreFromPath	Where we should restore from
	 */
	public LegacyStateFactory(String saveStatePath, String restoreFromPath)
	{
		
		this.saveStatePath = saveStatePath;
		this.restoreFromPath = restoreFromPath;
		
		if(saveStatePath != null)
		{
			File f = new File(this.saveStatePath);
			
			
			
			if(!f.exists())
			{
				if(!f.mkdirs())
				{
					log.error("Could not create directory to save states: {} " + f.getAbsolutePath());
					throw new IllegalArgumentException("Could not create directory" + f.getAbsolutePath());
				} else
				{
					log.trace("Directory created for states: {}",  f.getAbsolutePath());
				}
				
			} else
			{
				if(f.isDirectory() && f.listFiles().length > 0)
				{
					File newFileName = new File(f.getParent() + File.separator + "/" + "old-state-" + f.getName()+  "-" + System.currentTimeMillis() + "/");
					f.renameTo(newFileName);
					log.info("Found previous run data in state output folder: {} , renamed to: {}", newFileName.getParent() ,newFileName.getName());  
					f = new File(this.saveStatePath);
					f.mkdir();
				}
			}
			
			if(!f.isDirectory())
			{
				throw new IllegalArgumentException("Not a directory: " + f.getAbsolutePath());
			}
			
			if(!f.canWrite())
			{
				throw new IllegalArgumentException("Can't write to state saving directory: " + f.getAbsolutePath());
		}
		
		}
		
	}
	@Override
	public StateDeserializer getStateDeserializer(String id, int iteration, ParameterConfigurationSpace configSpace, List<ProblemInstance> instances, AlgorithmExecutionConfiguration execConfig, RunHistory rh) throws StateSerializationException
	{
		if(restoreFromPath == null) 
		{
			throw new IllegalArgumentException("This Serializer does not support restoring state");
		}
		return new LegacyStateDeserializer(restoreFromPath, id, iteration, configSpace, instances, execConfig, rh);
	}

	@Override
	public StateSerializer getStateSerializer(String id, int iteration)	throws StateSerializationException 
	{
		if(saveStatePath == null) 
		{
			throw new IllegalArgumentException("This Serializer does not support saving State");
		}
		return new LegacyStateSerializer(saveStatePath, id, iteration, this);
	}

	
	/**
	 * Copies the file to the State Dir
	 * @param name name of the file to write
	 * @param f source file
	 */
	@Override
	public void copyFileToStateDir(String name, File f)
	{
		
		
		if(!f.isFile())
		{
			throw new IllegalArgumentException("Input file f is not a file :" + f.getAbsolutePath());
		}
		
		if(!f.exists())
		{
			throw new IllegalArgumentException("Input file f does not exist :" + f.getAbsolutePath());
		}
		
		try {
			copyFileToStateDir(name, new FileInputStream(f));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("IOException occured :",e);
		}
		
		
	}
	
	/**
	 * Copies the file to the State Dir
	 * @param name name of the file to write
	 * @param f source file
	 */
	public void copyFileToStateDir(String name, InputStream in)
	{
		if(saveStatePath == null)
		{
			throw new IllegalArgumentException("This Serializer does not support saving State");
		}
	
		
		File outputFile = new File(saveStatePath + File.separator + name);
		
		try {
			OutputStream out = new FileOutputStream(outputFile);
			
			
			byte[] buf = new byte[8172];
			int len;
			
			
			while((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			log.trace("File copied to {} ", outputFile.getAbsolutePath());
			
		} catch(IOException e)
		{
			throw new IllegalStateException("IOException occured :",e);
		}
		
		
	}
	

	/**
	 * Generates the filename on disk that we should use to store uniq_configurations (array format of configurations)
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @return string representing the file name
	 */
	static String getUniqConfigurationsFilename(String path, String id, int iteration)
	{
		return getUniqConfigurationsFilename(path, id, String.valueOf(iteration), "-");
	}
	
	/**
	 * Generates the filename on disk that we should use to store uniq_configurations (array format of configurations)
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @param dash			character we use as a dash (this is poorly named).
	 * @return string representing the file name
	 */
	static String getUniqConfigurationsFilename(String path, String id, String iteration, String dash)
	{
		if(path.equals(""))
		{
			return UNIQ_CONFIGURATIONS_FILENAME + dash + id + iteration + ".csv";
		} else
		{
			return path + File.separator + UNIQ_CONFIGURATIONS_FILENAME + dash + id + iteration + ".csv";
		}
	}
	
	/**
	 * Generates the filename on disk that we should use to store paramstrings (String format of configurations)
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @return string representing the file name
	 */
	static String getParamStringsFilename(String path, String id, int iteration) {
		return getParamStringsFilename(path, id, String.valueOf(iteration), "-");
			
	}
	
	/**
	 * Generates the filename on disk that we should use to store paramstrings (String format of configurations)
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @param dash			character we use as a dash (this is poorly named).
	 * @return string representing the file name
	 */
	static String getParamStringsFilename(String path, String id, String iteration, String dash) {
		
		if(path.equals(""))
		{ 
			return PARAMSTRINGS_FILENAME + dash + id + iteration + ".txt";
		} else
		{
			return path + File.separator + PARAMSTRINGS_FILENAME + dash + id + iteration + ".txt";
		}
			
	}
	
	/**
	 * Generates the filename on disk that we should use to store uniq_configurations (array format of configurations)
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @return string representing the file name
	 */
	static String getRunAndResultsFilename(String path, String id, int iteration) 
	{	
		return getRunAndResultsFilename(path, id, String.valueOf(iteration));
	}
	
	/**
	 * Generates the filename on disk that we should use to store run_and_results 
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @return string representing the file name
	 */
	public static String getRunAndResultsFilename(String path, String id, String iteration)
	{
		
		return getRunAndResultsFilename(path, id, iteration, "-");
		
	}
	
	/**
	 * Generates the filename on disk that we should use to store run_and_results
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @param dash			character we use as a dash (this is poorly named).
	 * @return string representing the file name
	 */
	public static String getRunAndResultsFilename(String path, String id, String iteration, String dash)
	{
		
		if(!path.equals(""))
		{
			return path + File.separator + RUNS_AND_RESULTS_FILENAME + dash + id + iteration + ".csv";
		} else
		{
			return  RUNS_AND_RESULTS_FILENAME+dash + id + iteration + ".csv";
		}
		
	}
	
	/**
	 * Generates the filename on disk that we should use to store the java object dump
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file
	 * @return string representing the file name
	 */
	static String getJavaObjectDumpFilename(String path, String id, int iteration)
	{
	 	return path + File.separator + "java_obj_dump-v2-"+id + iteration +".obj";
	}
	
	/**
	 * Generates the filename on disk that we should use to store run_and_results 
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file (not used, probably should be deleted)
	 * @return string representing the file name
	 */
	static String getJavaQuickObjectDumpFilename(String path, String id,
			int iteration) {
		return path + File.separator + "java_obj_dump-v2-"+id + "quick.obj";
	}
	
	/**
	 * Generates the filename on disk that we should use to store run_and_results 
	 * @param path 			directory to look into
	 * @param id 			id of the save (generally "it" or "CRASH")
	 * @param iteration		iteration of the file (not used, probably should be deleted)
	 * @return string representing the file name
	 */
	public static String getJavaQuickBackObjectDumpFilename(String path, String id,
			int iteration) {
		return path + File.separator + "java_obj_dump-v2-"+id + "quick-bak.obj";
	}
	
	/**
	 * Reads the iteration that this java object file contains
	 * @param javaObjDumpFile file to read
	 * @return iteration stored in it
	 */
	public static int readIterationFromObjectFile(File javaObjDumpFile) {
		ObjectInputStream oReader =  null;
		try{
			try {
				
				oReader =  new ObjectInputStream(new FileInputStream(javaObjDumpFile));
				Object o = oReader.readObject();
				@SuppressWarnings("unchecked")
				Map<String, Serializable> map = (Map<String, Serializable>) o;
				return Integer.valueOf(map.get(ITERATION_KEY).toString());
				//if(true) throw new IllegalStateException();
				
		
			} finally
			{
				if(oReader != null) oReader.close();
			}
		} catch(Exception e)
		{
			
			return -1;
		}	
	}

	
	
	static final String RUN_NUMBER_HEADING = "Run Number";
	
	
	
	@Override
	public void purgePreviousStates() {
		
		
		if(savedFilesPerIteration.size() == 0)
		{ //No iterations
			return;
		}
		
		Set<String> filesToDelete = new HashSet<String>();
		
		for(Set<String> files : savedFilesPerIteration.values())
		{
			filesToDelete.addAll(files);
		}
		
		
		filesToDelete.removeAll(savedFilesPerIteration.lastEntry().getValue());
		
		Integer lastIteration = savedFilesPerIteration.lastKey();
		
		log.trace("Deleting all saved state files except those applicable to iteration {} ", lastIteration);
		
		
		
		for(String filename : filesToDelete)
		{
			log.trace("Deleting file {}", filename);
			if(!(new File(filename)).delete())
			{
				if(log.isDebugEnabled())
				{
					log.warn("Could not delete file {} ", filename);
				}
			}
			
		}
	
		
		
		
	}
	
	void addWrittenFilesForIteration(int iteration,
			Set<String> savedFiles) {
		
			 this.savedFilesPerIteration.putIfAbsent(iteration, new HashSet<String>());
			 Set<String> iterationFiles = this.savedFilesPerIteration.get(iteration);
			iterationFiles.addAll(savedFiles);
	}
	

	
}

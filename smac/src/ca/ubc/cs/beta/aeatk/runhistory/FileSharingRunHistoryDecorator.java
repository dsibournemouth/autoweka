package ca.ubc.cs.beta.aeatk.runhistory;



import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.smac.SMACOptions.SharedModelModeDefaultHandling;

public class FileSharingRunHistoryDecorator implements ThreadSafeRunHistory {

	private final RunHistory runHistory;
	
	ReadWriteLockThreadTracker rwltt = new ReadWriteLockThreadTracker();
	
	private final static Logger log = LoggerFactory.getLogger(FileSharingRunHistoryDecorator.class);
	
	private final String sharedFileName;
	private final FileOutputStream fout;
	private final ObjectMapper map = new ObjectMapper();
	
	private final static String JSON_FILE_PREFIX = "live-rundata-";
	private final static String JSON_FILE_SUFFIX = ".json";
	
	private final File outputDir;
	private final int MSBetweenUpdates;
	
	private final  JsonFactory factory = new JsonFactory();

	private final JsonGenerator g;
	
	private final List<ProblemInstance> pis;

	private final ConcurrentMap<File, Integer> importedRuns = new ConcurrentSkipListMap<>();
	
	private final SharedModelModeDefaultHandling srdh;
	private final boolean writeData;
	private final boolean assymetricMode;
	/**
	 * Actually read the data in 
	 */
	private final boolean readData;
	
	
	public FileSharingRunHistoryDecorator(RunHistory runHistory, File directory, final int outputID, List<ProblemInstance> pis, int MSecondsBetweenUpdates, final boolean readData)
	{
		
		this(runHistory, directory, outputID, pis, MSecondsBetweenUpdates, readData, false, SharedModelModeDefaultHandling.USE_ALL, true);
	}
	public FileSharingRunHistoryDecorator(RunHistory runHistory, File directory, final int outputID, List<ProblemInstance> pis, int MSecondsBetweenUpdates, final boolean readData, boolean sharedModeModeAssymetricMode, SharedModelModeDefaultHandling defaultHandler, boolean writeRunData)
	{
		this.runHistory = runHistory;
		this.outputDir = directory;
		this.readData = readData;
		this.srdh = defaultHandler;
		this.writeData = writeRunData;
		this.assymetricMode = sharedModeModeAssymetricMode;
		
		if(MSecondsBetweenUpdates < 0)
		{
			throw new IllegalArgumentException("Seconds between updates must be positive, not:" + MSecondsBetweenUpdates);
		}
		
		this.MSBetweenUpdates = MSecondsBetweenUpdates;
		
		sharedFileName = new File(directory + File.separator + JSON_FILE_PREFIX+outputID + JSON_FILE_SUFFIX).getAbsolutePath();
		
		String filename = sharedFileName;
		
		if(!writeData)
		{
			try {
				//This is really a hacky way to deal with not writing a file (creating a temp file)
				File tmp =File.createTempFile("DummyFile", "AEATK");
				tmp.deleteOnExit();
				filename = tmp.getAbsolutePath();
			} catch (IOException e) {
				throw new IllegalStateException("Couldn't create temp file");
			}
		}
		final File f = new File(filename);
		
		try {
			
			fout = new FileOutputStream(f);
			
			factory.setCodec(map);
			

			g = factory.createGenerator(fout);
			
			SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
			map.configure(SerializationFeature.INDENT_OUTPUT, true);
			  
			map.registerModule(sModule);
			
			List<ProblemInstance> myPis = new ArrayList<>(pis);
			if(writeData)
			{
				g.writeObject(myPis);
				g.flush();
			}
			this.pis = Collections.unmodifiableList(myPis);
			//map.writeValue(fout, pis);
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't create shared model output file :" + sharedFileName);
		}
		
		
		if(log.isInfoEnabled())
		{
			Thread t = new Thread(new Runnable(){

				@Override
				public void run() {
					
					
					
					Thread.currentThread().setName("FileSharingRunHistory Logger ( outputID:" + outputID + ")");
					List<String> addedRunsStr = new ArrayList<String>();
					int total = 0;
					
					
					
					for(Entry<File, Integer> ent : importedRuns.entrySet())
					{
						addedRunsStr.add(ent.getKey().getName() + "=>" + ent.getValue());
						int values = ent.getValue();
						if(values > 0)
						{
							total += values;
						}
					}
					
					if(readData)
					{
						log.info("At shutdown: {} had {} runs added to it", f, locallyAddedRuns.get());
						//log.info("At shutdown: we retrieved atleast {} runs and added them to our current data set {} which now has {} runs ({} local)",  total, addedRunsStr , FileSharingRunHistoryDecorator.this.getAlgorithmRunDataIncludingRedundant().size() , locallyAddedRuns.get() );
					} else
					{
						log.debug("At shutdown: {} had {} runs added to it", f, locallyAddedRuns.get());
					}
					
					
				
				}
				
			});
			
			Runtime.getRuntime().addShutdownHook(t);
		}
		
		
		
	}
	
	private final AtomicInteger locallyAddedRuns = new AtomicInteger(0);

	@Override
	public void append(Collection<AlgorithmRunResult> runs)	 {

	
		lockWrite();
		
		try {
			for(AlgorithmRunResult run : runs)
			{
				
				try {
					
					runHistory.append(run);
				} catch (DuplicateRunException e1) {
					//We will drop this silently because
					//The client can't realistically be expected to know if
					//the run already exists or not.
					continue;
				}

				
				try {
					if(writeData)
					{
						g.writeObject(run);
						g.flush();
					}
					
					
					
					locallyAddedRuns.incrementAndGet();
					
					//map.writeValue(fout, run);
					//fout.flush();
				} catch (IOException e) {
					throw new IllegalStateException("Couldn't save data as JSON", e);
				}
				
				if(this.readData)
				{
					reReadFiles();
				}
			
				
			}
			
		} finally
		{
			unlockWrite();
		}
		
	}
	

	
	private long lastUpdateTime = 0;
	/**
	 * Rereads matching files in the directory every so often.
	 */
	private final void reReadFiles()
	{
		
		try 
		{
			lockWrite();
			if (System.currentTimeMillis() - lastUpdateTime < MSBetweenUpdates)
			{
				return;
			}
			try 
			{
				
				File[] matchingFiles = this.outputDir.listFiles(new FileFilter()
				{
					@Override
					public boolean accept(File pathname) {
						if(pathname.getName().startsWith(JSON_FILE_PREFIX) && pathname.getName().endsWith(JSON_FILE_SUFFIX))
						{
							if (pathname.getAbsolutePath().equals(sharedFileName))
							{
								return false;
							} else
							{
								return true;
							}
						} else
						{
							return false;
						}
					}
				});
				
				
				
				if(this.assymetricMode)
				{
					
					
					Set<File> allFiles = new TreeSet<File>();
					allFiles.addAll(Arrays.asList(matchingFiles));
					
					File me = new File(sharedFileName);
					allFiles.add(me);
					
					List<File> filesInOrder = new ArrayList<File>(allFiles);
					
					//Set<File>
					int startingFile = filesInOrder.indexOf(me);
					
					
					
					//Don't really read these files, it's just short hand
					Set<File> myFilesToReadSet = new TreeSet<File>();
					myFilesToReadSet.add(me);
					for(int i=startingFile; i < filesInOrder.size(); i++)
					{
						if(myFilesToReadSet.contains(filesInOrder.get(i)))
						{
							if(2*i < filesInOrder.size())
							{
								myFilesToReadSet.add(filesInOrder.get(2*i));
							}
							
							if(2*i+1 <filesInOrder.size())
							{
								myFilesToReadSet.add(filesInOrder.get(2*i+1));
							}
						}
					}
					
					myFilesToReadSet.remove(me);
					ArrayList<File> myFilesToRead = new ArrayList<File>(myFilesToReadSet);
					matchingFiles = myFilesToRead.toArray(new File[0]);
				}
				Set<String> newReads = new TreeSet<String>();
				
				for(File match : matchingFiles)
				{
					log.trace("Matching files: {} my file: {} ", match.getAbsolutePath(), sharedFileName);
					
					boolean newFileRead = readRunsFromFile(match);
					
					if(newFileRead)
					{
						newReads.add(match.getName());
					}
			
				}
				
				if(newReads.size() > 0)
				{
					log.info("Detected new source(s) of run data which we will read from : {}", newReads);
				}
			} finally
			{
				lastUpdateTime = System.currentTimeMillis();
			}
		} finally
		{
			unlockWrite();
		}
		
	}
	
	
	
	private final Set<File> filesWithErrors = Collections.synchronizedSet(new HashSet<File>());
	
	/**
	 * 
	 * @param match
	 * @return true if we successfully read a new file (which means we imported run 0), false otherwise
	 */
	private boolean readRunsFromFile(File match) {
		
		importedRuns.putIfAbsent(match, 0);
		
		int previousRuns = importedRuns.get(match);
		
		if(previousRuns == -1)
		{
			//Blacklisted
			return false;
		}
		JsonFactory jfactory = new JsonFactory();
		
		
		//System.err.println("Starting...");
		
		boolean readNewFile = false;
		try {
			
			ObjectMapper map = new ObjectMapper(jfactory);
			SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
			map.registerModule(sModule);

			
			JsonParser jParser = jfactory.createParser(match);
			
			
			List<ProblemInstance> pis = new ArrayList<ProblemInstance>(Arrays.asList(map.readValue(jParser, ProblemInstance[].class)));

			if(!pis.equals(this.pis))
			{
				//log.warn("Instances in file {} do match our instances, ignoring file.\nMine   : {}\nTheirs : {}", match,this.pis, pis);
				
				//importedRuns.put(match, -1);
				return false;
			}
			
			
			
			
		
			List<AlgorithmRunResult> runResult = new ArrayList<AlgorithmRunResult>();
		
			
			MappingIterator<AlgorithmRunResult> it =  map.readValues(jParser, new TypeReference<AlgorithmRunResult>() {/*Who Cares*/} );
			
			while(it.hasNext())
			{					
		
				runResult.add(it.nextValue());
			}
			
			int newValue = previousRuns; 
			
			try {
				
				
				for(AlgorithmRunResult run : runResult.subList(previousRuns, runResult.size()))
				{
					if(previousRuns == 0)
					{
						readNewFile = true;
					}
					
					try {
						newValue++; //Always count this, if it's a duplicate it counts as a success.
						if(run.getParameterConfiguration().equals(run.getParameterConfiguration().getParameterConfigurationSpace().getDefaultConfiguration()))
						{
							switch(this.srdh)
							{
								case IGNORE_ALL:
									//System.err.println("Ignoring Run");
									break;
								case SKIP_FIRST_TWO:
									if(newValue <= 2)
									{
										//System.err.println("Ignoring Run: #" + newValue);
										break;
									}
								case USE_ALL:
									runHistory.append(run);
									break;
								default:
									throw new IllegalStateException("Not sure how to deal with this");
							}
							
							
						} else
						{
							runHistory.append(run);
						}
					} catch (DuplicateRunException e) {
						//Doesn't matter here
					}
					 
				}
				
			} finally
			{
				if(previousRuns != newValue)
				{
					//log.debug("Successfully read {} new runs (out of {} total) from file {} ", newValue - previousRuns , newValue, match); 
				}
				importedRuns.put(match,newValue);
			}

			
			if(this.filesWithErrors.remove(match))
			{
				log.info("Successfully read file: {} after previously logged error", match.getAbsolutePath());
			}
		
		} catch (RuntimeException | IOException e) {
			
			//We will just retry later
			
			if(this.filesWithErrors.add(match))
			{
				log.warn("Error occurred reading file in shared run history " + match.getAbsolutePath() + ". We will keep trying to read this file, but will only log another error after it succeeds once. We may not be able to get it's run data but we should be able to continue", e);
				
			}
		} 
		
		
		return readNewFile;
		
	}

	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException {
		this.append(Collections.singleton(run));
	}

	@Override
	public RunObjective getRunObjective() {
		lockRead();
		
		try {
			return runHistory.getRunObjective();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public OverallObjective getOverallObjective() {
		lockRead();
		try {
			return runHistory.getOverallObjective();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public void incrementIteration() {
		lockWrite();
		
		try {
			 runHistory.incrementIteration();
		} finally
		{
			unlockWrite();
		}
	}

	@Override
	public int getIteration() {

		lockRead();
		try {
			return runHistory.getIteration();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ProblemInstance> getProblemInstancesRan(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getProblemInstancesRan(config);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ProblemInstanceSeedPair> getProblemInstanceSeedPairsRan(
			ParameterConfiguration config) {
		
		lockRead();
		try {
			return runHistory.getProblemInstanceSeedPairsRan(config);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues,
			double minimumResponseValue) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues, minimumResponseValue);
		} finally
		{
			unlockRead();
		}
	}




	@Override
	public double getTotalRunCost() {
		lockRead();
		try {
			return runHistory.getTotalRunCost();
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public Set<ProblemInstance> getUniqueInstancesRan() {
		lockRead();
		try {
			return runHistory.getUniqueInstancesRan();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public Set<ParameterConfiguration> getUniqueParamConfigurations() {
		lockRead();
		try {
			return runHistory.getUniqueParamConfigurations();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public int[][] getParameterConfigurationInstancesRanByIndexExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getParameterConfigurationInstancesRanByIndexExcludingRedundant();
		} finally
		{
			unlockRead();
		}
	}


	
	@Override
	public List<ParameterConfiguration> getAllParameterConfigurationsRan() {
		lockRead();
		try {
			return runHistory.getAllParameterConfigurationsRan();
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public double[][] getAllConfigurationsRanInValueArrayForm() {
		lockRead();
		try {
			return runHistory.getAllConfigurationsRanInValueArrayForm();
		} finally
		{
			unlockRead();
		}
	}


	@Override
	public List<RunData> getAlgorithmRunDataIncludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunDataIncludingRedundant();
		} finally
		{
			unlockRead();
	
		}
		
	
	}


	@Override
	public List<RunData> getAlgorithmRunDataExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunDataExcludingRedundant();
		} finally
		{
			unlockRead();
	
		}
		
	
	}
	

	@Override
	public Set<ProblemInstanceSeedPair> getEarlyCensoredProblemInstanceSeedPairs(
			ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getEarlyCensoredProblemInstanceSeedPairs(config);
		} finally
		{
			unlockRead();
	
		}
	}



	@Override
	public int getThetaIdx(ParameterConfiguration configuration) {
		lockRead();
		try {
			return runHistory.getThetaIdx(configuration);
		} finally
		{
			unlockRead();
	
		}
	}

	@Override
	public double getEmpiricalCost(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime,
			double minimumResponseValue) {
		lockRead();
		try {
			return runHistory.getEmpiricalCost(config, instanceSet, cutoffTime);
		} finally
		{
			unlockRead();
	
		}
	}

	@Override
	public int getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(
			ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getNumberOfUniqueProblemInstanceSeedPairsForConfiguration(config);
		} finally
		{
			unlockRead();
	
		}
	}


	@Override
	public void readLock() {
		lockRead();
	}


	@Override
	public void releaseReadLock() {
		unlockRead();
		
	}

	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsExcludingRedundant(config);
		} finally
		{
			unlockRead();
	
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigExcludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getTotalNumRunsOfConfigExcludingRedundant(config);
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsExcludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsExcludingRedundant();
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsIncludingRedundant(config);
		} finally
		{
			unlockRead();
	
		}
	}
	

	@Override
	public int getTotalNumRunsOfConfigIncludingRedundant(ParameterConfiguration config) {
		lockRead();
		try {
			return runHistory.getTotalNumRunsOfConfigIncludingRedundant(config);
		} finally
		{
			unlockRead();
		}
	}
	
	@Override
	public List<AlgorithmRunResult> getAlgorithmRunsIncludingRedundant() {
		lockRead();
		try {
			return runHistory.getAlgorithmRunsIncludingRedundant();
		} finally
		{
			unlockRead();
		}
	}
	
	

	@Override
	public Map<ProblemInstance, LinkedHashMap<Long, Double>> getPerformanceForConfig(
			ParameterConfiguration configuration) {
		lockRead();
		try {
			return runHistory.getPerformanceForConfig(configuration);
		} finally
		{
			unlockRead();
		}
	}

	@Override
	public List<Long> getSeedsUsedByInstance(ProblemInstance pi) {
		lockRead();
		try {
			return runHistory.getSeedsUsedByInstance(pi);
		} finally
		{
			unlockRead();
		}
	}
	
	
	public void lockRead()
	{
		this.rwltt.lockRead();
	
	}
	
	private void unlockRead()
	{
		this.rwltt.unlockRead();
	}
	
	private void lockWrite()
	{
		this.rwltt.lockWrite();
	}
	
	private void unlockWrite()
	{
		this.rwltt.unlockWrite();
		
	}

	@Override
	public int getOrCreateThetaIdx(ParameterConfiguration config) {
		lockWrite();
		try {
			return this.runHistory.getOrCreateThetaIdx(config);
		} finally
		{
			unlockWrite();
		}
	
		
	}

	@Override
	public double getEmpiricalCostLowerBound(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return this.runHistory.getEmpiricalCostLowerBound(config, instanceSet, cutoffTime);
	}

	@Override
	public double getEmpiricalCostUpperBound(ParameterConfiguration config,
			Set<ProblemInstance> instanceSet, double cutoffTime) {
		return this.runHistory.getEmpiricalCostUpperBound(config, instanceSet, cutoffTime);
	}

	@Override
	public AlgorithmRunResult getAlgorithmRunResultForAlgorithmRunConfiguration(
			AlgorithmRunConfiguration runConfig) {
		lockRead();
		try
		{
			return this.runHistory.getAlgorithmRunResultForAlgorithmRunConfiguration(runConfig);
		} finally
		{
			unlockRead();
		}
	}


	

	

}

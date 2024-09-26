package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import net.jcip.annotations.ThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorHelper;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorRunObserver;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.decorators.AbstractTargetAlgorithmEvaluatorDecorator;


/**
 * This Target Algorithm Evaluator reads all runs from a directory or file and answers queries from this cache.
 * 
 * It does <b>NOT</b> perform in memory caching.
 *
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@ThreadSafe
public class FileCacheTargetAlgorithmEvaluatorDecorator extends	AbstractTargetAlgorithmEvaluatorDecorator {

	
	/**
	 * Stores the cache of runs we have
	 */
	private final Map<AlgorithmRunConfiguration, AlgorithmRunResult> cache; 
	
	
	/**
	 * Stores all the runs we have learnt
	 */
	private final ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> learnedRuns = new ConcurrentHashMap<AlgorithmRunConfiguration,AlgorithmRunResult>();

	/**
	 * File or directory we will write output to
	 */

	private final File output;


	/**
	 * Number of our run
	 * 
	 */
	private final int numRun;
	
	
	/**
	 * Number of collisions
	 */
	private final AtomicInteger collisions = new AtomicInteger(0);
	
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Learnt Runs Written
	 */
	private final AtomicBoolean learntRunsWritten = new AtomicBoolean(); 
	
	private final AtomicInteger cacheHits = new AtomicInteger(0);
	
	private final AtomicInteger runRequests = new AtomicInteger(0);


	private final boolean crashOnCacheMiss;
	
	public FileCacheTargetAlgorithmEvaluatorDecorator(
			TargetAlgorithmEvaluator tae, File source, File output, int numRun, boolean crashOnCacheMiss) {
		super(tae);
		this.output = output;
		this.numRun = numRun;
		this.crashOnCacheMiss = crashOnCacheMiss;
		
		if(output == null && source == null)
		{
			throw new ParameterException("Both source and output options for the file cache are null, you must set at least one of them.");
		}
		
		if(output != null)
		{
			if(output.exists() && !output.canWrite())
			{
				throw new ParameterException("Cannot write to output, check permissions: " + output.getAbsolutePath());
			} else
			{
				if(!output.exists())
				{
					if(!output.getParentFile().mkdirs())
					{
						throw new ParameterException("Could not create directory: " + output.getAbsolutePath());
					}
				}
			}
		
			Runnable run = new Runnable()
			{

				@Override
				public void run() {
					String name = Thread.currentThread().getName();
					try 
					{
						Thread.currentThread().setName("FileCacheTargetAlgorithmEvaluator Fallback writing thread");
						writeLearnedRuns();
					} finally
					{
						Thread.currentThread().setName(name);
					}
					
				}
				
			};
			
			Runtime.getRuntime().addShutdownHook(new Thread(run));
			
			
		}
		
		ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> cacheLoading = new ConcurrentHashMap<AlgorithmRunConfiguration,AlgorithmRunResult>();
		
		this.cache = Collections.unmodifiableMap(cacheLoading);
		if(source != null)
		{
			if(!source.exists())
			{
				throw new ParameterException("Could not read source:" + source.getAbsolutePath());
			} else 
			{
				if(source.isDirectory())
				{
				
				
					//Sort the file list in order first so that we always read files in the same order as best as possible.
					TreeMap<Long, Map<String, File>> files = new TreeMap<Long, Map<String, File>>();
					
				
					for(File f : source.listFiles())
					{
						
						long lastModified = f.lastModified();
						if(files.get(lastModified) == null)
						{
							files.put(lastModified, new TreeMap<String, File>());
						}
						
						files.get(lastModified).put(f.getAbsolutePath(), f);
						
					}
					
					List<File> fileList = new ArrayList<File>();
					
					for(Map<String, File> val : files.values())
					{
						
						for(File valFile : val.values())
						{
							fileList.add(valFile);
						}
					}
					
					for(File f : fileList)
					{
					
						readFile(f, cacheLoading);
					}
				} else 
				{
					readFile(source, cacheLoading);
				}
				
				
				
			}
			//log.info("File Cache of Runs has been initialized from {} ; {} entries prepopulated in cache; {} duplicates", source.getAbsolutePath(), this.cache.size(), this.collisions.get());
		}
			
		
		
	
	}

	
	
	@Override
	public void evaluateRunsAsync(final List<AlgorithmRunConfiguration> AlgorithmRunConfigurations, final TargetAlgorithmEvaluatorCallback handler, final TargetAlgorithmEvaluatorRunObserver obs) {
		
		
		final List<AlgorithmRunConfiguration> runsToSubmit = new ArrayList<AlgorithmRunConfiguration>(AlgorithmRunConfigurations.size());
		
		final ConcurrentSkipListMap<Integer, AlgorithmRunConfiguration> AlgorithmRunConfigurationOrder = new ConcurrentSkipListMap<Integer,AlgorithmRunConfiguration>();
		
		final ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> solvedRuns = new ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
		
		for(int i=0; i < AlgorithmRunConfigurations.size(); i++)
		{
			AlgorithmRunConfiguration rc = AlgorithmRunConfigurations.get(i);
			
			AlgorithmRunConfigurationOrder.put(i, rc);
			this.runRequests.incrementAndGet();
			if(this.cache.get(rc) != null)
			{
				this.cacheHits.incrementAndGet();
				solvedRuns.put(rc, this.cache.get(rc));
			} else
			{
				
				runsToSubmit.add(rc);
			}
		}
		
		
		
		TargetAlgorithmEvaluatorRunObserver myObs = new TargetAlgorithmEvaluatorRunObserver()
		{

			Map<AlgorithmRunConfiguration, AlgorithmRunResult> myRuns = new HashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
			@Override
			public void currentStatus(List<? extends AlgorithmRunResult> runs) {
				
				
				if(obs == null)
				{
					return;
				}
				
				List<AlgorithmRunResult> runsToReport = new ArrayList<AlgorithmRunResult>(AlgorithmRunConfigurations.size());
				
				myRuns.clear();
				for(AlgorithmRunResult run : runs)
				{
					myRuns.put(run.getAlgorithmRunConfiguration(), run);
				}
				
				for(AlgorithmRunConfiguration rc : AlgorithmRunConfigurationOrder.values())
				{
					if(solvedRuns.contains(rc))
					{
						runsToReport.add(solvedRuns.get(rc));
					} else
					{
						runsToReport.add(myRuns.get(rc));
					}
				}
				
				obs.currentStatus(runsToReport);
			}
		
		};
		
		
		final TargetAlgorithmEvaluatorCallback myHandler = new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs)
			{
			
					
				Map<AlgorithmRunConfiguration, AlgorithmRunResult> myRuns = new HashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
	
				
				List<AlgorithmRunResult> runsToReport = new ArrayList<AlgorithmRunResult>(AlgorithmRunConfigurations.size());
				
			
				for(AlgorithmRunResult run : runs)
				{
					myRuns.put(run.getAlgorithmRunConfiguration(), run);
				}
				
				
				for(AlgorithmRunConfiguration rc : AlgorithmRunConfigurationOrder.values())
				{
					if(solvedRuns.containsKey(rc))
					{
						runsToReport.add(solvedRuns.get(rc));
					} else
					{
						runsToReport.add(myRuns.get(rc));
						
						learnedRuns.put(rc, myRuns.get(rc));
					}
				}
				
				
				handler.onSuccess(runsToReport);
				
			
				
			}

			@Override
			public void onFailure(RuntimeException e) {
				handler.onFailure(e);
				
			}
			
		};
		
		
		if(runsToSubmit.size() > 0)
		{
			
			
			if(this.crashOnCacheMiss)
			{
				throw new IllegalStateException("We don't have a cache entry for the following runs: " + runsToSubmit);
			}
			
			
			
			this.tae.evaluateRunsAsync(runsToSubmit, myHandler, myObs);
		} else
		{
			try
			{
				List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>();
				
				for(AlgorithmRunConfiguration rc : AlgorithmRunConfigurations)
				{
					runs.add(solvedRuns.get(rc));
				}
				myHandler.onSuccess(runs);
			} catch(RuntimeException e)
			{
				myHandler.onFailure(e);
			}
		}
		

	}
	@Override
	public List<AlgorithmRunResult> evaluateRun(List<AlgorithmRunConfiguration> AlgorithmRunConfigurations, TargetAlgorithmEvaluatorRunObserver obs) {
		return TargetAlgorithmEvaluatorHelper.evaluateRunSyncToAsync(AlgorithmRunConfigurations, this, obs);
	}
	
	private void readFile(File f, ConcurrentHashMap<AlgorithmRunConfiguration, AlgorithmRunResult> cacheLoading) {
		
		boolean logError = false;
		if(f.getAbsolutePath().endsWith(".bin") || f.getAbsolutePath().endsWith(".json"))
		{ 
			logError = true;
		}
		try 
		{
			List<AlgorithmRunResult> runs = getListFromFile(f);
			
			for(AlgorithmRunResult run : runs)
			{
				if(cacheLoading.putIfAbsent(run.getAlgorithmRunConfiguration(), run) != null)
				{
					this.collisions.incrementAndGet();
				}
			}
		} catch(IOException e)
		{
			
		
			if(logError)
			{
				log.error("Coludn't read file {} ", f);
				log.error("Couldn't read data from file", e);
			}
		}
		
		
	}
	
	private List<AlgorithmRunResult> getListFromFile(File f) throws IOException
	{
		
		try
		{
			return getListFromJSONFile(f);
		} catch(IOException  e)
		{
			try
			{
				return getListFromJavaSerializedFile(f);
			} catch(Exception e2)
			{
				log.trace("Trying to read from JSON file as binary file, exception:", e2);
				//Ignore the internal exception
				throw e;
			}
		}
	}
	
	private List<AlgorithmRunResult> getListFromJSONFile(File f) throws JsonParseException, IOException
	{
		JsonFactory jfactory = new JsonFactory();
		
		
		//System.err.println("Starting...");
		
		
			ObjectMapper map = new ObjectMapper(jfactory);
			SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
			map.registerModule(sModule);

		
			JsonParser jParser = jfactory.createParser(f);
			
			
			List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>(Arrays.asList(map.readValue(jParser, AlgorithmRunResult[].class)));

			return runs;
	}
	
	private List<AlgorithmRunResult> getListFromJavaSerializedFile(File f) throws IOException, ClassNotFoundException
	{
		FileInputStream fin = new FileInputStream(f);
		
		
		
		ObjectInputStream in = new ObjectInputStream(fin);
		
		return  (List<AlgorithmRunResult>) in.readObject();
		
	}

	@Override
	public void postDecorateeNotifyShutdown()
	{
	
		try 
		{
			this.log.info(this.getClass().getSimpleName() + ": Total Requests: {}, Cache Hits {}",this.runRequests.get(), this.cacheHits.get());
		} finally
		{
			writeLearnedRuns();
		}
	}
	

	
	private synchronized void writeLearnedRuns()
	{
		if(this.learntRunsWritten.compareAndSet(false, true))
		{
			if(output != null)
			{
				
				if(this.learnedRuns.size() > 0)
				{
					File outFile;
					UUID uuid = UUID.randomUUID();
					if(output.isDirectory())
					{
						outFile = new File(output + File.separator + "rundata-"+this.numRun + "-" + uuid.toString()  + ".json");
					} else
					{
						outFile = output;
					}
					
					
					
					
					try {
						outFile.createNewFile();
					} catch (IOException e) {
						//Who cares we will check this in a second
					}
					
					if(!outFile.canWrite())
					{
						log.error("Cannot write to output file : {} ", outFile.getAbsolutePath());
					}
						
					
					try {
						
						
						
						try(FileWriter fWrite = new FileWriter(outFile))
						{
						
						/*ObjectOutputStream tout = new ObjectOutputStream(fout);
						*/
							List<AlgorithmRunResult> runs = new ArrayList<AlgorithmRunResult>(this.learnedRuns.size());
							for(AlgorithmRunResult run : this.learnedRuns.values())
							{
								runs.add(run);
							}
							
							ObjectMapper map = new ObjectMapper();
							SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
							map.registerModule(sModule);
							
							
							map.writeValue(fWrite, runs);
							
						}
						
						/*
						tout.writeObject(runs);
						
						tout.close();
						*/
					} catch (IOException e) {
						log.error("Couldn't write data to file: {}", outFile);
						log.error("Encountered Error while writing data {}",e);
					}
				}
					
			}
			
			
		} else
		{
			return;
		}
	}
	

}

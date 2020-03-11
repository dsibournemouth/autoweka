package ca.ubc.cs.beta.smac.validation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;
import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DeveloperMadeABooBooException;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.objectives.OverallObjective;
import ca.ubc.cs.beta.aeatk.objectives.RunObjective;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.RandomInstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.SetInstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.runhistory.NewRunHistory;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.smac.ValidationOptions;
import ca.ubc.cs.beta.aeatk.smac.ValidationRoundingMode;
import ca.ubc.cs.beta.aeatk.state.StateFactory;
import ca.ubc.cs.beta.aeatk.state.StateSerializer;
import ca.ubc.cs.beta.aeatk.state.legacy.LegacyStateFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorCallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.WaitableTAECallback;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineAlgorithmRun;
import ca.ubc.cs.beta.aeatk.trajectoryfile.TrajectoryFile;
import ca.ubc.cs.beta.aeatk.trajectoryfile.TrajectoryFileEntry;



public class Validator {

	
	private static Logger log = LoggerFactory.getLogger(Validator.class);
	/*
	public double validate(List<ProblemInstance> testInstances, ValidationOptions config,double cutoffTime,InstanceSeedGenerator testInstGen, TargetAlgorithmEvaluator validatingTae, String outputDir,RunObjective runObj,OverallObjective intraInstanceObjective, OverallObjective interInstanceObjective, TrajectoryFileEntry) {
		
		return validate(testInstances, incumbent, config, cutoffTime, testInstGen, validatingTae, outputDir, runObj, intraInstanceObjective, interInstanceObjective,tunerTime, 0,0, numRun);
	}
		*/

	public SortedMap<TrajectoryFileEntry, ValidationResult>  simpleValidate(List<ProblemInstance> testInstances, final ValidationOptions options,final double cutoffTime,final InstanceSeedGenerator testInstGen,final TargetAlgorithmEvaluator validatingTae, 
			final String outputDir,
			final RunObjective runObj,
			final OverallObjective intraInstanceObjective, final OverallObjective interInstanceObjective,  TrajectoryFile trajFile, boolean waitForRuns, int cores, AlgorithmExecutionConfiguration execConfig)
			{

				if(options.useWallClockTime)
				{
					options.outputFileSuffix = "walltime" + (options.outputFileSuffix.trim().length() > 0 ?"-" + options.outputFileSuffix:"");
				} else
				{
					options.outputFileSuffix = "tunertime" + (options.outputFileSuffix.trim().length() > 0 ?"-" + options.outputFileSuffix:"");
				}
	
				List<ProblemInstanceSeedPair> pisps = getProblemInstanceSeedPairsToRun(testInstances, options, testInstGen);
				ValidationRuns runs = validateStage(pisps, options, cutoffTime, runObj, intraInstanceObjective, interInstanceObjective, trajFile, execConfig, outputDir);
				
				
				if(runs.done)
				{
					return runs.result;
				} else
				{
					
					Set<ParameterConfiguration> configs = new HashSet<ParameterConfiguration>();
					
					for(AlgorithmRunConfiguration rc : runs.runConfigs)
					{
						configs.add(rc.getParameterConfiguration());
					}
					//log.info("Validation needs {} algorithm runs to validate {} configurations found, each on {} problem instance seed pairs", runs.runConfigs.size(), configs.size(),pisps.size());
					
                                        log.info("Validation needs {} algorithm runs to validate {} configurations found, each on {} problem instance seed pairs");
                                        
					Date d = new Date(System.currentTimeMillis());
					DateFormat df = DateFormat.getDateTimeInstance();	
					
				
					if(cutoffTime > Math.pow(10, 10))
					{
						log.info("Validation start time: {}. Approximate worst-case end time is unknown.",df.format(d));
					} else
					{
						Date endTime = new Date(System.currentTimeMillis() + (long) (1.1*(cutoffTime * runs.runConfigs.size()  * 1000/ cores)));
						log.info("Validation start time: {}. Approximate worst-case end time: {}",df.format(d), df.format(endTime));
					}
					
					
					validatingTae.evaluateRunsAsync(runs.runConfigs, runs.callback);
					
					
					if(!validatingTae.areRunsPersisted() || waitForRuns)
					{
						log.debug("Waiting until validation completion");
						runs.callback.waitForCompletion();
					}
					
				
					if(runs.exception.get() != null)
					{
						throw runs.exception.get();
					}
					
					return runs.result;
				}
			}
	
	
	

	public void  multiValidate(List<ProblemInstance> testInstances, final ValidationOptions options,final double cutoffTime,final InstanceSeedGenerator testInstGen,final TargetAlgorithmEvaluator validatingTae,
			final RunObjective runObj,
			final OverallObjective intraInstanceObjective, final OverallObjective interInstanceObjective,  final Set<TrajectoryFile> tfes, boolean waitForRuns, int cores, AlgorithmExecutionConfiguration execConfig, String outputDir)
			{

				if(options.useWallClockTime)
				{
					options.outputFileSuffix = "walltime" + options.outputFileSuffix;
				} else
				{
					options.outputFileSuffix = "tunertime" + options.outputFileSuffix;
				}

				List<ProblemInstanceSeedPair> pisps = getProblemInstanceSeedPairsToRun(testInstances, options, testInstGen);
				List<ValidationRuns> runs = new ArrayList<ValidationRuns>();
				
				if(tfes.size() > 1)
				{
					log.debug("Beginning multi-validation on {} files ", tfes.size() );
				}
				for(TrajectoryFile tfe: tfes)
				{
					runs.add(validateStage(pisps, options, cutoffTime, runObj, intraInstanceObjective, interInstanceObjective, tfe, execConfig, outputDir));
				}
				
				Set<AlgorithmRunConfiguration> runConfigs = new LinkedHashSet<AlgorithmRunConfiguration>();
				Set<ParameterConfiguration> configs = new HashSet<ParameterConfiguration>();
				
				final Map<WaitableTAECallback, List<AlgorithmRunConfiguration>> callbacksToSchedule = new LinkedHashMap<WaitableTAECallback, List<AlgorithmRunConfiguration>>();
				for(ValidationRuns run : runs)
				{
					if(run.done)
					{
						continue;
					}
					
					for(AlgorithmRunConfiguration rc : run.runConfigs)
					{
						runConfigs.add(rc);
						configs.add(rc.getParameterConfiguration());
					}
					
					callbacksToSchedule.put(run.callback, run.runConfigs);
					
				}
				
				List<AlgorithmRunConfiguration> runConfigsToRun = new ArrayList<AlgorithmRunConfiguration>(runConfigs);

				//log.info("Validation needs {} algorithm runs to validate {} configurations found, each on {} problem instance seed pairs", runConfigsToRun.size(), configs.size(),pisps.size());
				log.info("Validation needs {} algorithm runs to validate {} configurations found, each on {} problem instance seed pairs");
                                
                                
				Date d = new Date(System.currentTimeMillis());
				DateFormat df = DateFormat.getDateTimeInstance();	
				
			
				if(cutoffTime > Math.pow(10, 10))
				{
					log.info("Validation start time: {}. Approximate worst-case end time is unknown.",df.format(d));
				} else
				{
					Date endTime = new Date(System.currentTimeMillis() + (long) (1.1*(cutoffTime * runConfigsToRun.size()  * 1000/ cores)));
					log.info("Validation start time: {}. Approximate worst-case end time: {}",df.format(d), df.format(endTime));
				}
				
				final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
				
				
				WaitableTAECallback taeCallback = new WaitableTAECallback(new TargetAlgorithmEvaluatorCallback()
				{

					@Override
					public void onSuccess(List<AlgorithmRunResult> runs) {
						
						Map<AlgorithmRunConfiguration, AlgorithmRunResult> runsToRunConfig = new HashMap<AlgorithmRunConfiguration, AlgorithmRunResult>();
						
						for(AlgorithmRunResult run : runs)
						{
							runsToRunConfig.put(run.getAlgorithmRunConfiguration(),run);
						}
						
						for(Entry<WaitableTAECallback, List<AlgorithmRunConfiguration>> ent : callbacksToSchedule.entrySet())
						{
						
							List<AlgorithmRunResult> runsToNotify = new ArrayList<AlgorithmRunResult>();
							for(AlgorithmRunConfiguration rc : ent.getValue())
							{
								runsToNotify.add(runsToRunConfig.get(rc));
							}
							
							
							try 
							{
								ent.getKey().onSuccess(runsToNotify);
							} catch(RuntimeException e)
							{
								log.error("Error occurred while saving runs:" ,e);
							}
							
						}
						
						
						
					}

					@Override
					public void onFailure(RuntimeException e) {
						exception.set(e);
					}
					
				});
				
				
				validatingTae.evaluateRunsAsync(runConfigsToRun, taeCallback);
				
				
				if(!validatingTae.areRunsPersisted() || waitForRuns)
				{
					log.debug("Waiting until validation completion");
					taeCallback.waitForCompletion();
				}
				
				if(exception.get() != null)
				{
					throw exception.get();
				}

			}
	
	
	
	
	private ValidationRuns  validateStage(final List<ProblemInstanceSeedPair> pisps, final ValidationOptions options,final double cutoffTime, 
		final RunObjective runObj,

		final OverallObjective intraInstanceObjective, final OverallObjective interInstanceObjective,  final TrajectoryFile trajFile, AlgorithmExecutionConfiguration execConfig, final String outputDir)

		{

	
		
		double maxWallTimeStamp = 0;
		double maxTunerTimeStamp = 0;
		final List<TrajectoryFileEntry> tfes =  trajFile.getTrajectoryFileEntries();
		for(TrajectoryFileEntry tfe : tfes)
		{
			
			maxWallTimeStamp = Math.max(tfe.getWallTime(), maxWallTimeStamp);
			maxTunerTimeStamp = Math.max(tfe.getTunerTime(),maxTunerTimeStamp);
		}
		
		if(options.validateOnlyIfWallTimeReached > maxWallTimeStamp)
		{
			log.info("Maximum walltime was {} but we required {} seconds to have passed validating ", maxWallTimeStamp, options.validateOnlyIfWallTimeReached );
			return new ValidationRuns(new TreeMap<TrajectoryFileEntry,ValidationResult>());
		}
		
		if(options.validateOnlyIfTunerTimeReached > maxTunerTimeStamp)
		{
			log.info("Maximum Tuner Time was {} but we required {} seconds to have passed before validating ", maxTunerTimeStamp, options.validateOnlyIfTunerTimeReached );
			return new ValidationRuns(new TreeMap<TrajectoryFileEntry,ValidationResult>());
		}

		Set<TrajectoryFileEntry> tfesToUse = new TreeSet<TrajectoryFileEntry>();
		
		if(options.validateAll)
		{
			tfesToUse.addAll(tfes);
		} else if(options.validateOnlyLastIncumbent)
		{
			
			log.trace("Validating only the last incumbent");
			if(options.useWallClockTime)
			{
				TrajectoryFileEntry tfe = tfes.get(tfes.size() - 1);
				
				
				double wallTime = (tfe.getWallTime() > options.maxTimestamp) ? options.maxTimestamp : tfe.getWallTime();
				
				TrajectoryFileEntry newTfe = new TrajectoryFileEntry(tfe.getConfiguration(), tfe.getTunerTime(), wallTime, tfe.getEmpericalPerformance(), tfe.getACOverhead());
				
				tfesToUse.add(newTfe);
			} else
			{
				TrajectoryFileEntry tfe = tfes.get(tfes.size() - 1);
				
				double tunerTime = (tfe.getTunerTime() > options.maxTimestamp) ? options.maxTimestamp : tfe.getTunerTime();
				
				TrajectoryFileEntry newTfe = new TrajectoryFileEntry(tfe.getConfiguration(), tunerTime, tfe.getWallTime(), tfe.getEmpericalPerformance(), tfe.getACOverhead());
				
				tfesToUse.add(newTfe);
			}
			
		}  else 
		{
			ConcurrentSkipListMap<Double,TrajectoryFileEntry> skipList = new ConcurrentSkipListMap<Double, TrajectoryFileEntry>();
			
			for(TrajectoryFileEntry tfe : tfes)
			{
				if(options.useWallClockTime)
				{
					skipList.put(tfe.getWallTime(), tfe);
				} else
				{
					skipList.put(tfe.getTunerTime(), tfe);
				}
			}
			double maxTimestamp = options.maxTimestamp;
			if(maxTimestamp == -1)
			{
				maxTimestamp = skipList.floorKey(Double.MAX_VALUE);
			}
			ParameterConfiguration lastConfig = null;
			Double lastPerformance = Double.MAX_VALUE;
			for(double x = maxTimestamp; x > options.minTimestamp ; x /= options.multFactor)
			{
				
				Entry<Double, TrajectoryFileEntry> tfeEntry = skipList.floorEntry(x);
				
				TrajectoryFileEntry tfe;
				
				if(tfeEntry != null) 
				{
					tfe = tfeEntry.getValue();
				} else
				{
					tfe = new TrajectoryFileEntry(lastConfig, 0,0,lastPerformance,0);
				}
				/*if(options.useWallClockTime && tfeEntry.getKey() < x/options.multFactor)
				{	export MYSQL_CREATE_TABLES = "false"
					continue;
				}*/
				
				
				if(options.useWallClockTime)
				{
					tfesToUse.add(new TrajectoryFileEntry(tfe.getConfiguration(), x, x, tfe.getEmpericalPerformance(), tfe.getACOverhead()));
				} else
				{
					tfesToUse.add(new TrajectoryFileEntry(tfe.getConfiguration(),x, tfe.getWallTime(), tfe.getEmpericalPerformance(), tfe.getACOverhead()));
					
				}
				lastConfig = tfe.getConfiguration();
				lastPerformance = tfe.getEmpericalPerformance();
				//If minTimestamp is zero, then we would never get there, but we will stop after 0.25
				//We don't put this in the loop condition, because we do always want atleast one run
				if( x  < 0.01) break; 
				
			}
		}
		
		
		final List<TrajectoryFileEntry> tfesToRun = new ArrayList<TrajectoryFileEntry>(tfesToUse.size());
		tfesToRun.addAll(tfesToUse);
				
		
		Set<ParameterConfiguration> configs = new LinkedHashSet<ParameterConfiguration>();
		
		final Map<ParameterConfiguration, Integer> configToID = new LinkedHashMap<ParameterConfiguration, Integer>();
		
		int id=1;
		for(TrajectoryFileEntry tfe : tfesToRun)
		{
			if(configs.add(tfe.getConfiguration()))
			{
				configToID.put(tfe.getConfiguration(), id++);
			}
		}
		
		List<AlgorithmRunConfiguration> runConfigs = getRunConfigs(configs, pisps, cutoffTime,execConfig);
		
		final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
		
		final SortedMap<TrajectoryFileEntry, ValidationResult> finalPerformance = new ConcurrentSkipListMap<TrajectoryFileEntry, ValidationResult>();
		
		WaitableTAECallback callback = new WaitableTAECallback(new TargetAlgorithmEvaluatorCallback()
		{

			@Override
			public void onSuccess(List<AlgorithmRunResult> runs) {

				
				
				try {
					writeConfigurationMapFile(trajFile, options, configToID,  runs, outputDir);
				} catch(IOException e)
				{
					log.error("Could not write results file", e);
				}
				
				try
				{
					Map<ParameterConfiguration, Double> testSetPerformance = writeInstanceResultFile(runs, options, cutoffTime, runObj, intraInstanceObjective, interInstanceObjective, trajFile, configToID, outputDir);
					
					
					for(TrajectoryFileEntry tfe : tfesToRun)
					{
						finalPerformance.put(tfe, new ValidationResult(testSetPerformance.get(tfe.getConfiguration()), pisps));
					}
					
					if(runs.size() > 0)
					{
						appendInstanceResultFile( finalPerformance,  trajFile,options, options.useWallClockTime, configToID, outputDir);
					}
					
					
					
				} catch(IOException e)
				{
					log.error("Could not write results file:", e);
				}
				
				
				try {
					ConcurrentHashMap<ParameterConfiguration, Map<ProblemInstanceSeedPair, AlgorithmRunResult>> matrixRuns = new ConcurrentHashMap<ParameterConfiguration, Map<ProblemInstanceSeedPair, AlgorithmRunResult>>();
					
					for(AlgorithmRunResult run : runs)
					{
						matrixRuns.putIfAbsent(run.getAlgorithmRunConfiguration().getParameterConfiguration(), new TreeMap<ProblemInstanceSeedPair, AlgorithmRunResult>());
						
						Map<ProblemInstanceSeedPair, AlgorithmRunResult> configRuns = matrixRuns.get(run.getAlgorithmRunConfiguration().getParameterConfiguration());
						
						configRuns.put(run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair(), run);
					}
					
					
					List<ParameterConfiguration> configs = new ArrayList<ParameterConfiguration>();
					
					for(AlgorithmRunResult run : runs)
					{
						if(configs.contains(run.getAlgorithmRunConfiguration().getParameterConfiguration())) continue;
						configs.add(run.getAlgorithmRunConfiguration().getParameterConfiguration());
					}
					
					
					writeConfigurationResultsMatrix(configs,matrixRuns, tfes, options, trajFile, runObj, configToID, outputDir);
					
				} catch(IOException e)
				{
					log.error("Could not write matrix file:",e);
				}
			 
				
				
				
				if(options.saveStateFile)
				{
					try {
						
						//log.info("Writing state file  into ");
						writeLegacyStateFile(options.outputFileSuffix,runs, trajFile, interInstanceObjective, interInstanceObjective, runObj, outputDir);
					} catch(RuntimeException e)
					{
						log.error("Couldn't write state file:",e);
					}
				}
				
			}

			

			



			@Override
			public void onFailure(RuntimeException t) {
				exception.set(t);
			}
			
		});

		
		
		
		
		return new ValidationRuns(runConfigs,callback, exception, finalPerformance);
		//return finalPerformance;

		
		
		
		
	}




	/**
	 * @param testInstances
	 * @param options
	 * @param testInstGen
	 * @return
	 */
	private List<ProblemInstanceSeedPair> getProblemInstanceSeedPairsToRun(
			List<ProblemInstance> testInstances,
			final ValidationOptions options,
			final InstanceSeedGenerator testInstGen) {
		int testInstancesCount = Math.min(options.numberOfTestInstances, testInstances.size());
		int testSeedsPerInstance = options.numberOfTestSeedsPerInstance;
		int validationRunsCount = options.numberOfValidationRuns;
		
		ValidationRoundingMode mode = options.validationRoundingMode;		
		
		List<ProblemInstanceSeedPair> pisps = new ArrayList<ProblemInstanceSeedPair>();
		
		if(testInstGen instanceof SetInstanceSeedGenerator)
		{
			if(validationRunsCount > testInstGen.getInitialInstanceSeedCount())
			{
				log.debug("Clamping number of validation runs from {} to {} due to seed limit", validationRunsCount, testInstGen.getInitialInstanceSeedCount());
				validationRunsCount = testInstGen.getInitialInstanceSeedCount();
			}
			pisps = getValidationRuns(testInstances, (SetInstanceSeedGenerator) testInstGen, mode, validationRunsCount);
		} else if(testInstGen instanceof RandomInstanceSeedGenerator)
		{
			pisps = getValidationRuns(testInstances, (RandomInstanceSeedGenerator) testInstGen,mode, validationRunsCount, testSeedsPerInstance, testInstancesCount);
		} else
		{
			throw new IllegalStateException("Unknown Instance Seed Generator specified");
		}
		return Collections.unmodifiableList(pisps);
	}




	
	

	
	
	
	
	
	public static class ValidationRuns
	{
		List<AlgorithmRunConfiguration> runConfigs;
		WaitableTAECallback callback;
		private SortedMap<TrajectoryFileEntry, ValidationResult> result;
		private AtomicReference<RuntimeException> exception;
	
		
		
		
		public boolean done;
		
		public ValidationRuns(SortedMap<TrajectoryFileEntry, ValidationResult> result)
		{
			this.result = result; 
			this.runConfigs = Collections.emptyList();
			done = true;
		}
		
		public ValidationRuns(List<AlgorithmRunConfiguration> runConfigs, WaitableTAECallback callback, AtomicReference<RuntimeException> exception,SortedMap<TrajectoryFileEntry, ValidationResult> result)
		{
			this.runConfigs = runConfigs; 
			this.callback = callback;
			this.exception = exception;
			this.result = result;
			done = false;
		}
	}
	


private List<AlgorithmRunConfiguration> getRunConfigs(Set<ParameterConfiguration> configs, List<ProblemInstanceSeedPair> pisps, double cutoffTime,AlgorithmExecutionConfiguration execConfig) 
{
	
	List<AlgorithmRunConfiguration> runConfigs  = new ArrayList<AlgorithmRunConfiguration>(pisps.size()*configs.size());
	for(ParameterConfiguration config: configs)
	{
		for(ProblemInstanceSeedPair pisp : pisps)
		{
			runConfigs.add(new AlgorithmRunConfiguration(pisp, cutoffTime,config,execConfig));
		}
	}
	
	return runConfigs;
}






	private static List<ProblemInstanceSeedPair> getValidationRuns(List<ProblemInstance> pis,RandomInstanceSeedGenerator testInstGen, ValidationRoundingMode mode,int validationRunsCount, int testSeedsPerInstance, int testInstancesCount) {
		
		int numRuns = 0;
		
		switch(mode)
		{
		case UP:			
			numRuns = Math.round( (float) (Math.ceil( validationRunsCount / (float) testInstancesCount ) * testInstancesCount));
			break;
		case NONE:
			numRuns = Math.min(validationRunsCount, testSeedsPerInstance*testInstancesCount);
			break;
		default:
			throw new IllegalStateException("Unknown Rounding Mode");
		}
		
		
		List<ProblemInstance> pisToUse = testInstGen.getProblemInstanceOrder(pis);
		int runsScheduled = 0;
		List<ProblemInstanceSeedPair> pisps = new ArrayList<ProblemInstanceSeedPair>(numRuns);
		
		
endloop:
		while(true)
		{
			for(int i=0; i < testInstancesCount; i++)
			{
				ProblemInstance pi = pisToUse.get(i);
				pisps.add(new ProblemInstanceSeedPair(pi,testInstGen.getNextSeed(pi)));
				
				runsScheduled++;
				
				if(runsScheduled >= numRuns) break endloop;
			}
		}
		
		

		return pisps;
	}
	
	
	
	private static List<ProblemInstanceSeedPair> getValidationRuns( List<ProblemInstance> pis,	SetInstanceSeedGenerator testInstGen, ValidationRoundingMode mode,  int validationRunsCount)
	{

		List<ProblemInstance> instances = testInstGen.getProblemInstanceOrder(pis);
		
		int numRuns = 0;
		switch(mode)
		{
			case UP:
				numRuns = Math.round( (float) (Math.ceil( validationRunsCount / (float) instances.size() ) * instances.size()));
				break;
			case NONE:
				numRuns = Math.min(instances.size(), validationRunsCount);
				break;
			default:
				throw new IllegalStateException("Unknown mode: " + mode);
		}
		
		List<ProblemInstanceSeedPair> runs = new ArrayList<ProblemInstanceSeedPair>(numRuns);
		for( int i=0; i < numRuns; i++)
		{
			ProblemInstance pi = instances.get(i);
			runs.add(new ProblemInstanceSeedPair(pi,testInstGen.getNextSeed(pi)));
		}
		
		return runs;
	}

	
	private static void writeConfigurationMapFile(TrajectoryFile trajFile, ValidationOptions validationOptions,	Map<ParameterConfiguration, Integer> configToID, List<AlgorithmRunResult> runs, String outputDir) throws IOException {

		File f = getFile(trajFile, "validationCallStrings",validationOptions.outputFileSuffix,"csv", outputDir);

		log.info("Validation Call Strings written to: {}", f.getAbsolutePath());
		CSVWriter writer = new CSVWriter(new FileWriter(f));

		if(validationOptions.validationHeaders)
		{
			String[] args = {"Validation Configuration ID","Configuration ","Sample Call String"};
			writer.writeNext(args);
		}

		for(Entry<ParameterConfiguration, Integer> ent : configToID.entrySet())
		{
			AlgorithmRunConfiguration rc = null;
			
			for(AlgorithmRunResult run : runs)
			{
				if (run.getAlgorithmRunConfiguration().getParameterConfiguration().equals(ent.getKey()))
				{
					rc = run.getAlgorithmRunConfiguration();
					break;
				}
			}
			String[] args = {String.valueOf(ent.getValue()), ent.getKey().getFormattedParameterString(ParameterStringFormat.NODB_SYNTAX), CommandLineAlgorithmRun.getTargetAlgorithmExecutionCommandAsString( rc) };
			writer.writeNext(args);
		}
		
		writer.close();
	}
	
	/**
	 * Writes a matrix of the runs to the given file
	 * @param matrixRuns
	 * @param tfes
	 * @throws IOException 
	 */
	private static void writeConfigurationResultsMatrix( List<ParameterConfiguration> inOrderConfigs, 
			ConcurrentHashMap<ParameterConfiguration, Map<ProblemInstanceSeedPair, AlgorithmRunResult>> matrixRuns,
			List<TrajectoryFileEntry> tfes, ValidationOptions validationOptions,TrajectoryFile trajFile, RunObjective runObj, Map<ParameterConfiguration, Integer> idMap, String outputDir) throws IOException {

		File configurationObjective =  getFile(trajFile, "validationObjectiveMatrix",validationOptions.outputFileSuffix,"csv", outputDir);
		File configurationRun =  getFile(trajFile, "validationRunResultLineMatrix",validationOptions.outputFileSuffix,"csv", outputDir);
		
		log.info("Validation matrix of objectives for cross-product of configurations and (instance,seed) pairs written to: {}", configurationObjective.getAbsolutePath());
		log.info("Validation matrix of complete run result line for cross-product of configurations and (instance,seed) pairs written to: {}", configurationRun.getAbsolutePath());
		
		CSVWriter objectiveMatrixCSV = new CSVWriter(new FileWriter(configurationObjective));
		CSVWriter runMatrixCSV = new CSVWriter(new FileWriter(configurationRun));
		
		
		try {
			TreeMap<ProblemInstanceSeedPair, Map<ParameterConfiguration, AlgorithmRunResult>> transposedMap = new TreeMap<>();
			
			
			for(Map<ProblemInstanceSeedPair, AlgorithmRunResult> ent : matrixRuns.values())
			{
				
				for(AlgorithmRunResult run : ent.values())
				{
					if(transposedMap.get(run.getProblemInstanceSeedPair()) == null)
					{
						transposedMap.put(run.getProblemInstanceSeedPair(), new ConcurrentHashMap<ParameterConfiguration, AlgorithmRunResult>());
					}
					
					
					
					transposedMap.get(run.getProblemInstanceSeedPair()).put(run.getParameterConfiguration(), run);
				}
			}
			
			ArrayList<String> runMatrixHeaderRow = new ArrayList<String>();
			ArrayList<String> objectiveMatrixHeaderRow = new ArrayList<String>();
			

			runMatrixHeaderRow.add("Problem Instance");
			runMatrixHeaderRow.add("Seed");
			objectiveMatrixHeaderRow.add("Problem Instance");
			objectiveMatrixHeaderRow.add("Seed");
			
			for(ParameterConfiguration config : inOrderConfigs)
			{
				runMatrixHeaderRow.add("Run result line of validation config #" + String.valueOf(idMap.get(config)));
				objectiveMatrixHeaderRow.add("Objective of validation config #" + String.valueOf(idMap.get(config)));
			}
			
			/*
			for(ProblemInstanceSeedPair pisp : pisps)
			{
				headerRow.add(pisp.getProblemInstance().getInstanceName() + "," + pisp.getSeed());
			}*/
			
			String[] header = objectiveMatrixHeaderRow.toArray(new String[0]);
			
			objectiveMatrixCSV.writeNext(header);
			
			header = runMatrixHeaderRow.toArray(new String[0]);
			runMatrixCSV.writeNext(header);
			
			
			for(Entry<ProblemInstanceSeedPair, Map<ParameterConfiguration, AlgorithmRunResult>> ent : transposedMap.entrySet())
			{
				
				ArrayList<String> objectiveRow = new ArrayList<String>();
				ArrayList<String> resultLineRow = new ArrayList<String>();
				
				objectiveRow.add(ent.getKey().getProblemInstance().getInstanceName());
				
				resultLineRow.add(ent.getKey().getProblemInstance().getInstanceName());
				
				
				objectiveRow.add(String.valueOf(ent.getKey().getSeed()));
				resultLineRow.add(String.valueOf(ent.getKey().getSeed()));
				
				
				
				for(ParameterConfiguration config : inOrderConfigs)
				{
					
					AlgorithmRunResult run = ent.getValue().get(config);
					if(run == null)
					{
						throw new IllegalStateException("Expected all configurations to have the exact same pisps");
					}
					
					if(!run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().equals(ent.getKey()))
					{
						throw new IllegalStateException("DataStructure corruption detected ");
					}
					
					objectiveRow.add(String.valueOf(runObj.getObjective(run)));
					resultLineRow.add(run.getResultLine());
				}
				
				
				
				
				String[] nextRow = objectiveRow.toArray(new String[0]);
				String[] nextRunRow = resultLineRow.toArray(new String[0]);
				
				objectiveMatrixCSV.writeNext(nextRow);
				runMatrixCSV.writeNext(nextRunRow);
				
			}
		} finally
		{
			objectiveMatrixCSV.close();
			runMatrixCSV.close();
		}
		
		
		
	}


	/**
	 * Writes a CSV File which has the matrix of runs 
	 * @param runs
	 * @param validationOptions
	 * @param outputDir
	 * @param cutoffTime
	 * @param runObj
	 * @param overallObj
	 * @return - Overall objective over test set (For convinence)
	 * @throws IOException
	 */
	private static Map<ParameterConfiguration, Double> writeInstanceResultFile(List<AlgorithmRunResult> runs, ValidationOptions validationOptions, double cutoffTime,  RunObjective runObj, OverallObjective intraInstanceObjective, OverallObjective interInstanceObjective, TrajectoryFile trajFile, Map<ParameterConfiguration, Integer> configIDToMap, String outputDir) throws IOException
	{
		
		File f =  getFile(trajFile,  "validationPerformanceDebug",validationOptions.outputFileSuffix,"csv", outputDir);

		log.info("Instance performance (for debug) written to: {}", f.getAbsolutePath());
		
		CSVWriter writer = new CSVWriter(new FileWriter(f));
		
		Map<ParameterConfiguration, Double> calculatedOverallObjectives = new LinkedHashMap<ParameterConfiguration, Double>();
	
		
		Map<ParameterConfiguration, List<AlgorithmRunResult>> configToRunMap = new LinkedHashMap<ParameterConfiguration, List<AlgorithmRunResult>>();
		
		for(AlgorithmRunResult run : runs)
		{
			ParameterConfiguration config = run.getAlgorithmRunConfiguration().getParameterConfiguration();
			if(configToRunMap.get(config) == null)
			{
				configToRunMap.put(config, new ArrayList<AlgorithmRunResult>(1000));
			}
			configToRunMap.get(config).add(run);
			
		}
		
		for(Entry<ParameterConfiguration, List<AlgorithmRunResult>> ent : configToRunMap.entrySet())
		{
			Map<ProblemInstance, List<AlgorithmRunResult>> map = new LinkedHashMap<ProblemInstance,List<AlgorithmRunResult>>();
			
			runs = ent.getValue();
			
			
			int maxRunLength =0;
			for(AlgorithmRunResult run : runs)
			{
				ProblemInstance pi = run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getProblemInstance();
				if(map.get(pi) == null)
				{
					map.put(pi, new ArrayList<AlgorithmRunResult>());
				}
				
				List<AlgorithmRunResult> myRuns = map.get(pi);
				
				
				myRuns.add(run);
				
				maxRunLength = Math.max(myRuns.size(), maxRunLength);
			}
			
		
			ArrayList<String> headerRow = new ArrayList<String>();
			headerRow.add("Validation Configuration ID:" + configIDToMap.get(ent.getKey()));
			headerRow.add("Instance");
			headerRow.add("OverallObjective");
			
			for(int i=1; i <= maxRunLength; i++ )
			{
				headerRow.add("Run #" + i);
			}
			
			
			writer.writeNext(headerRow.toArray(new String[0]));
		
			List<Double> overallObjectives = new ArrayList<Double>();
			
			
			for(Entry<ProblemInstance, List<AlgorithmRunResult>> piRuns : map.entrySet())
			{
				List<String> outputLine = new ArrayList<String>();
				outputLine.add("");
				outputLine.add(piRuns.getKey().getInstanceName());
				List<AlgorithmRunResult> myRuns = piRuns.getValue();
				
				
	
				List<Double> results = new ArrayList<Double>(myRuns.size());
				
				for(int i=0; i < myRuns.size(); i++)
				{
					results.add(runObj.getObjective(myRuns.get(i)));
				}
				
				double overallResult = intraInstanceObjective.aggregate(results, cutoffTime);
				outputLine.add(String.valueOf(overallResult));
				
				overallObjectives.add(overallResult);
				for(AlgorithmRunResult run : piRuns.getValue())
				{
					outputLine.add(String.valueOf(runObj.getObjective(run)));
				}
				
				
				writer.writeNext(outputLine.toArray(new String[0]));
				
			}
			
			
			double overallObjective = interInstanceObjective.aggregate(overallObjectives,cutoffTime);
			String[] args = { "", "Overall Objective On Test Set", String.valueOf(overallObjective)};
			writer.writeNext(args);
			
			calculatedOverallObjectives.put(ent.getKey(), overallObjective);
			
		}
		
			writer.close();
			
			return calculatedOverallObjectives;
	}

	

	
	private static File getFile(TrajectoryFile trajFile, String filename, String suffix, String extension, String outputDir) {
		suffix = (suffix.trim().equals("")) ? "" : "-" + suffix.trim();


		String trajectoryFileName = trajFile.getLocation().getName();


		String baseName;
		if (trajectoryFileName.lastIndexOf(".") >= 0) {
			baseName = trajectoryFileName.substring(0, trajectoryFileName.lastIndexOf("."));
		} else {
			baseName = trajectoryFileName;
		}

		if (outputDir != null)
		{
			return new File(outputDir + File.separator + filename +"-" + baseName + suffix+"."+extension.replaceAll(Matcher.quoteReplacement("\\."), ""));
		} else if(trajFile.getLocation().getParent() != null)
		{
			return new File(trajFile.getLocation().getParent() + File.separator + filename +"-" + baseName + suffix+"."+extension.replaceAll(Matcher.quoteReplacement("\\."), ""));
		} else
		{
			return new File(filename +"-" + baseName + suffix+"."+extension.replaceAll(Matcher.quoteReplacement("\\."), ""));
		}
		
				
	}
	
	private void appendInstanceResultFile(Map<TrajectoryFileEntry, ValidationResult> finalPerformance, TrajectoryFile trajFile, ValidationOptions validationOptions, boolean useWallTime,  Map<ParameterConfiguration, Integer> idMap, String outputDir) throws IOException {
		
		
		File validationFile = getFile(trajFile,  "validationResults",validationOptions.outputFileSuffix,"csv", outputDir);
		
		log.info("Main validation results file written to: {}", validationFile.getAbsolutePath());
		
		if(!validationFile.exists())
		{
			validationFile.createNewFile();
			
		} else
		{
			validationFile.delete();
			validationFile.createNewFile();
		}
		
	
		
		
		StringBuilder sbValidation = new StringBuilder("\"Time\",\"Training (Empirical) Performance\",\"Test Set Performance\",\"AC Overhead Time\",\"Validation Configuration ID\",\n");
		
		for(Entry<TrajectoryFileEntry, ValidationResult > ent : finalPerformance.entrySet())
		{
			double time;
					
			if(useWallTime)
			{
				time = ent.getKey().getWallTime();
			} else
			{
				time = ent.getKey().getTunerTime();
			}
			
			double empiricalPerformance = ent.getKey().getEmpericalPerformance();
			double testSetPerformance = ent.getValue().getPerformance();
			double acOverhead = ent.getKey().getACOverhead();
				
			sbValidation.append(time).append(",").append(empiricalPerformance).append(",").append(testSetPerformance).append(",").append(acOverhead).append(",\"").append(idMap.get(ent.getKey().getConfiguration())).append("\",\n");
		}

		
		if(!validationFile.canWrite())
		{
			log.error("Could not write trajectory file would have written: {}" , sbValidation.toString());
		} else
		{
			PrintWriter output = new PrintWriter(new FileOutputStream(validationFile,true));
			output.append(sbValidation);
			output.close();
		}
		
		
		
}
	
	private static void writeLegacyStateFile( String suffix, List<AlgorithmRunResult> runs, TrajectoryFile trajFile, OverallObjective interRunObjective, OverallObjective intraRunObjective, RunObjective runObj, String outputDir)
	{
		RunHistory rh = new NewRunHistory( interRunObjective, intraRunObjective, runObj);
		for(AlgorithmRunResult run : runs)
		{
			try {
				rh.append(run);
			} catch (DuplicateRunException e) {
				throw new DeveloperMadeABooBooException("Duplicate run was detected by runHistory, this shouldn't be possible in validation");
			}
		}


        StateFactory sf;
        if(outputDir != null)
        {
            sf = new LegacyStateFactory(outputDir + File.separator + "state" + suffix +"-run-" + trajFile.getLocation().getName() , null);
        } else
        {
            sf = new LegacyStateFactory(trajFile.getLocation().getParent() + File.separator + "state" + suffix +"-run-" + trajFile.getLocation().getName() , null);
        }

		
		
		
		StateSerializer ss = sf.getStateSerializer("it", 1);
		
		
		ss.setRunHistory(rh);
		ss.save();
			
	}

	


	
}

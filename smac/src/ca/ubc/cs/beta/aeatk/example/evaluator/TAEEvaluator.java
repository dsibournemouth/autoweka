package ca.ubc.cs.beta.aeatk.example.evaluator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;
import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.InstanceListWithSeeds;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;
import ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator.InstanceSeedGenerator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueue;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.experimental.queuefacade.basic.BasicTargetAlgorithmEvaluatorQueueResultContext;

import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Evaluates an algorithm exec config with a TAE on a collection of instances and report results.
 * @author afrechet
 */
public class TAEEvaluator {
	
    private static Logger log;
	
	/**
	 * Parse arguments using jCommander and executes TAE evaluation.
	 * @param args
	 * @see TAEEvaluatorOptions
	 */
	public static void main(String[] args) {
		
		//Parse comamnd line arguments, buidling options object.
		TAEEvaluatorOptions options = new TAEEvaluatorOptions();
		try
		{
			JCommanderHelper.parseCheckingForHelpAndVersion(args,options, options.fAvailableTAEOptions);
		}
		catch (ParameterException e)
		{
			throw e;
		}
		finally
		{
		    options.fLoggingOptions.initializeLogging();
		    log = LoggerFactory.getLogger(TAEEvaluator.class);
		}
		
        //Setup report file.
        log.info("Setting up report file.");
        String reportFilename = options.fExperimentDir + File.separator + options.getOuputFileName();
        switch(options.fReportType)
        {
            case JSON:
                reportFilename += ".json";
                break;
                
            case CSV:
                reportFilename += ".csv";
                break;
            default:
                throw new IllegalArgumentException("Unrecognized output report type "+options.fReportType+".");
        }
        if(!options.fOverwriteReport)
        {
            if(!checkIfOverwrite(reportFilename))
            {
                log.info("Cannot overwrite currently existing report at \"{}\".",reportFilename);
                return;
            }           
        }
        
		//Build TAE and TAE queue.
		try(TargetAlgorithmEvaluator aTAE = options.getTAE())
		{
			BasicTargetAlgorithmEvaluatorQueue aTAEQueue = new BasicTargetAlgorithmEvaluatorQueue(aTAE,true);
			
			//Create runs.
			AlgorithmExecutionConfiguration aExecConfig = options.fAlgorithmExecutionOptions.getAlgorithmExecutionConfig();
			
			ParameterConfiguration aConfiguration = options.getConfiguration();
			InstanceListWithSeeds instancesGen;
            try {
                instancesGen = options.fProblemInstanceOptions.getTrainingProblemInstances(options.fExperimentDir, options.fSeed, options.fAlgorithmExecutionOptions.deterministic, true, false);
            } catch (IOException e) 
            {
                log.error("Error while reading instances from file:",e);
                throw new IllegalStateException("Could not read instances from file.");
            }
			List<ProblemInstance> instances = instancesGen.getInstances();
			InstanceSeedGenerator seedGen = instancesGen.getSeedGen();
			
			//Build instance submission.
			log.info("Submitting instance executions...");
			List<AlgorithmRunConfiguration> aRunConfigs = new ArrayList<AlgorithmRunConfiguration>(instances.size());
			
			int aInstanceIndex = 1;
			for(ProblemInstance instance : instances)
			{
				log.info("Creating instance submission {}/{}.",aInstanceIndex++,instances.size());
				log.info(instance.getInstanceName());
				ProblemInstanceSeedPair aPISP = new ProblemInstanceSeedPair(instance, seedGen.getNextSeed(instance));
				AlgorithmRunConfiguration aRunConfig = new AlgorithmRunConfiguration(aPISP,options.fCutoff,aConfiguration,aExecConfig);
				aRunConfigs.add(aRunConfig);
				
			}
			log.info("...done.");
			log.info("Submitting {} runs asynchronously.",aRunConfigs.size());
			aTAEQueue.evaluateRunAsync(aRunConfigs);
			
			if(options.fExitAfterSubmission){
				log.info("Exiting after submission, goodbye!");
				return;
			}

			log.info("Taking results from TAE queue.");
	         //Get back instances.
            log.info("Waiting for the completed batch from the queue...");
			BasicTargetAlgorithmEvaluatorQueueResultContext aRunContext;
            try {
                aRunContext = aTAEQueue.take();
            } catch (InterruptedException e) {
                log.error("There was an interruption while polling a run from the TAE queue:.",e);
                throw new IllegalStateException("Could not poll from the TAE queue.");
            }
            log.info("Got the batch.");
            List<AlgorithmRunResult> aRuns = aRunContext.getAlgorithmRuns();
            
            if(aRuns.size()!=aRunConfigs.size())
            {
                throw new IllegalStateException("Got "+aRuns.size()+" runs back, but submitted "+aRunConfigs.size()+".");
            }
            
            log.info("Writing results to file.");
            switch(options.fReportType)
            {
                case JSON:
                    writeJSONRuns(aRuns, reportFilename);
                    break;
                    
                case CSV:
                    writeCSVRuns(aRuns, reportFilename, options);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized output report type "+options.fReportType+".");
            }
            
            log.info("Report written to \"{}\".",reportFilename);
		}

	}
	
	/*
     * Two trick methods to avoid any output at critical sections of the code when the user is prompted for information.
     */
    private static OutputStream bout;
    private static PrintStream old;
    private synchronized static void startOutputCapture()
    {
        bout = new ByteArrayOutputStream();
        old = System.out;
        System.setOut(new PrintStream(bout));
    }
    private synchronized static String stopOutputCapture()
    {
        System.setOut(old);
        String boutString = bout.toString();
        System.out.println(boutString);
        return boutString;
    }
    private static boolean checkIfOverwrite(String aFilename)
    {
        //Check if given report file exists
        if(new File(aFilename).exists())
        {
            //See if user wants to append/override/cancel.
            log.warn("Outputfile {} already exists.",aFilename);
            Scanner in = new Scanner(System.in);
            try
            {
                while(true)
                {
                    startOutputCapture();
                    old.flush();
                    old.println("Given output file already exists. Do you want to overwrite [o] or cancel [c] ? ");
                    String answer = in.nextLine();
                    stopOutputCapture();
                    
                    answer = answer.toLowerCase();
                    
                    if (answer.equals("o") || answer.equals("overwrite"))
                    {
                        log.info("Current outputfile will be overwritten.");
                        return true;
                    }
                    else if (answer.equals("c") || answer.equals("cancel"))
                    {
                        return false;
                    }
                    else
                    {
                        System.out.println("Could not understand \""+answer+"\"");
                    }
                    
                }
            }
            finally
            {
                in.close();
            }
        }
        else
        {
            return true;
        }
    }
    
    private static void writeJSONRuns(List<AlgorithmRunResult> aRuns, String aFilename)
    {
        final FileOutputStream fout;
        final ObjectMapper map = new ObjectMapper();
        final  JsonFactory factory = new JsonFactory();
        final JsonGenerator g;
        
        final File f = new File(aFilename);
        
        try 
        {
            fout = new FileOutputStream(f);
            
            factory.setCodec(map);
            g = factory.createGenerator(fout);
            
            SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
            map.configure(SerializationFeature.INDENT_OUTPUT, true);
            map.registerModule(sModule);
            
            g.writeObject(aRuns);
            g.flush();
        } 
        catch (IOException e) {
            e.printStackTrace();
            log.error("Error while write runs to JSON file:",e);
            throw new IllegalStateException("Could not write runs to JSON.");
        }
    }
    
    private static void writeCSVRuns(List<AlgorithmRunResult> aRuns, String aFilename, TAEEvaluatorOptions aOptions)
    {
        CSVWriter aWriter;
        try
        {
            aWriter = new CSVWriter(new FileWriter(new File(aFilename),false));
            
            try
            {
                //Write file header.
        
                aWriter.writeNext(new String[]
                        {
                        "TAE Name",
                        "TAE Configuration",
                        "Cutoff",
                        "Seed",
                        "Instance",
                        "Run Result",
                        "Run Time",
                        "Additional Run Data"
                        }
                );
                
                int aRunIndex = 1;
                for(AlgorithmRunResult aRun : aRuns)
                {
                    log.info("Processing run {}/{}.",aRunIndex++,aRuns.size());
                    log.info("Writing run {} to output file.",aRun.toString());
                    aWriter.writeNext(new String[]
                            {
                            aOptions.fTAEName,
                            aOptions.fConfig,
                            Double.toString(aRun.getAlgorithmRunConfiguration().getCutoffTime()),
                            Long.toString(aRun.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getSeed()),
                            aRun.getAlgorithmRunConfiguration().getProblemInstanceSeedPair().getProblemInstance().getInstanceName(),
                            aRun.getRunStatus().toString(),
                            Double.toString(aRun.getRuntime()),
                            aRun.getAdditionalRunData()
                            }
                    );
                }
                
                log.info("...done.");
            }
            finally
            {
                aWriter.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
            log.error("Error while creating outputfile writer:",e);
            throw new IllegalArgumentException("Could not create CSV output file writer.");
        }
    }

}

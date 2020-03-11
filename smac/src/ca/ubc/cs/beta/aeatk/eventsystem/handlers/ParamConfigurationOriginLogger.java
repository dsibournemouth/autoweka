package ca.ubc.cs.beta.aeatk.eventsystem.handlers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.eventsystem.EventHandler;
import ca.ubc.cs.beta.aeatk.eventsystem.events.ac.AutomaticConfigurationEnd;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.tracking.ParamConfigurationOriginTracker;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;

/**
 * Logs all the origin events to disk;
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class ParamConfigurationOriginLogger implements EventHandler<AutomaticConfigurationEnd>{

	private final ParamConfigurationOriginTracker configTracker;
	private final RunHistory runHistory;
	private final String outputDir;
	private final static Logger log = LoggerFactory.getLogger(ParamConfigurationOriginLogger.class);
	private final long startTime;
	private final double cutoffTime;
	
	public ParamConfigurationOriginLogger(ParamConfigurationOriginTracker configTracker, String outputDir, RunHistory runHistory, long startTime, double cutoffTime)
	{
		this.configTracker = configTracker;
		this.outputDir = outputDir;
		this.startTime = startTime;
		this.runHistory = runHistory;
		this.cutoffTime = cutoffTime;
	}
	@Override
	public void handleEvent(AutomaticConfigurationEnd event) {
	
		try {
			(new File(outputDir)).mkdirs();
			File saveFile = new File(outputDir + File.separator + "configuration-generation.csv");
			FileWriter writer = new FileWriter(saveFile); 
			StringBuilder sb = new StringBuilder();
			sb.append("\"Wall Time\",\"Configuration ID\",\"Run History ID\",\"Generation Count\",\"PISP Count\",\"Final Performance\",");
			
			ArrayList<String> originNames = new ArrayList<String>(configTracker.getOriginNames());
			Collections.sort(originNames);
			
			for(String header : originNames)
			{
				sb.append("\"Origin-" + header + "\",");
			}
			sb.append("\n");
					
			
			writer.append(sb.toString());
		
			for(ParameterConfiguration config : configTracker)
			{
				if(runHistory.getTotalNumRunsOfConfigExcludingRedundant(config) == 0)
				{
					continue;
				}
				StringBuilder line = new StringBuilder();
				double wallTimeInSeconds = (configTracker.getCreationTime(config)-startTime)/1000.0;
				line.append("\"" + wallTimeInSeconds + "\",");
				line.append("\"" + config.getFriendlyIDHex() + "\",");
				line.append("\"" + runHistory.getThetaIdx(config) + "\",");
				line.append("\"" + configTracker.getGenerationCount(config) + "\",");
				line.append("\"" + runHistory.getTotalNumRunsOfConfigExcludingRedundant(config) + "\",");
				line.append("\"" + runHistory.getEmpiricalCost(config, runHistory.getProblemInstancesRan(config), cutoffTime) + "\",");
				
				for(String header : originNames)
				{
					String addlData = configTracker.getOrigins(config).get(header);
					if(addlData == null)
					{
						addlData = "";
					}
					line.append("\"" + addlData +"\",");
					
				}
				
				line.append("\n");
				writer.append(line.toString());
				
			}
			
			writer.close();
			log.debug("Configuration Origins Saved in {}", saveFile.getAbsolutePath());
		} catch (IOException e) {
			log.error("Couldn't write Config Origin File {}",e);
		}
		
	}

}

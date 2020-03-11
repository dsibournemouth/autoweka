package ca.ubc.cs.beta.aeatk.trajectoryfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;
import ca.ubc.cs.beta.aeatk.misc.csvhelpers.ConfigCSVFileHelper;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration.ParameterStringFormat;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationStringMissingParameterException;

public class TrajectoryFileParser {

	private static final Logger log = LoggerFactory.getLogger(TrajectoryFileParser.class);
	
	/**
	 * Parses a SMAC Trajectory file, this file should have `name='value'` pairs in every column starting from the 5th 
	 * @param configs		CSV File To Parse
	 * @param configSpace   Config Space to parse from
	 * @return SkipListMap that maps the time of the incumbent to a <cpuOverhead, incumbent> pair.
	 */
	private static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseSMACTrajectoryFile(ConfigCSVFileHelper configs, ParameterConfigurationSpace configSpace,boolean useTunerTimeAsWallTime)
	{
		ConcurrentSkipListMap<Double,  TrajectoryFileEntry> skipList = new ConcurrentSkipListMap<Double, TrajectoryFileEntry>();

		ParameterConfiguration defaultConfiguration = configSpace.getDefaultConfiguration();

		for(int i=0; i < configs.getNumberOfDataRows(); i++)
		{		
			String time = configs.getStringDataValue(i, 0).replaceAll("\"", "");
	
			String[] dataRow =  configs.getDataRow(i);
			
			for(int j=0; j < dataRow.length; j++)
			{
				dataRow[j] = dataRow[j].replaceAll("\"", "");
			}

			StringBuilder sb = new StringBuilder();

			for(int j=5; j < dataRow.length; j++)
			{
				if(dataRow[j].contains("="))
				{
					// Ugh something has broken horribly in all these formats
					// We need this because for some reason the CSV Reader isn't picking up the quotations correctly.
					// This file format is dying thankfully so we can just hack it.
					// It was expected that a cell like " " oteh=4 , uoetnh=5, " would be treated as one but right now it isn't. So we need to stop cells afterward
					// From being read in. e.g. "a=43,b=5","53.2" we need to make sure that "53.2" is not part of the thing we parse.
					sb.append(dataRow[j]).append(",");
				} else
				{
					break;
				}

			}
			
			double tunerTime = Double.valueOf(dataRow[0]);
			double empiricalPerformance = Double.valueOf(dataRow[1]);
			double wallTime = Double.valueOf(dataRow[2]);
			if(wallTime == -1)
			{
				wallTime = tunerTime;
			}
			//3 is the theta Idx of it
			double overhead = Double.valueOf(dataRow[4]);
			
			ParameterConfiguration configObj;
			try
			{
				configObj = configSpace.getParameterConfigurationFromString(sb.toString(), ParameterStringFormat.STATEFILE_SYNTAX);
			} catch(ParameterConfigurationStringMissingParameterException e)
			{
				//This hack here relies on the fact that multiple values specified will over write each other in sequential order.
				 configObj = configSpace.getParameterConfigurationFromString(defaultConfiguration.getFormattedParameterString(ParameterStringFormat.STATEFILE_SYNTAX) + "," + sb.toString(), ParameterStringFormat.STATEFILE_SYNTAX);
				 //log.warn("Detected incomplete parameter settings on line {}: {}. Imputed with default values to be: {}. The algorithm may behave differently with these imputed values than if they were not set. ", i+2 /* lines start with 1 and not 0, and there is a header row */, sb.toString(), configObj.getFormattedParameterString(ParameterStringFormat.STATEFILE_SYNTAX) );
			}
		
			TrajectoryFileEntry tfe = new TrajectoryFileEntry(configObj,tunerTime, wallTime, empiricalPerformance, overhead );
			
			skipList.put(Double.valueOf(time), tfe);
			
		}
		return skipList;
	}
	/**
	 * Parses a ParamILS Trajectory file, starting from column 5 the values of all parameters should be specified, the order of values must be alphabetical
	 * @param configs 		CSV Configuration Helper
	 * @param configSpace 	Configuration Space to draw examples from
	 * @return SkipListMap that maps the time of the incumbent to a <cpuOverhead, incumbent> pair.
	 */
	private static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseParamILSTrajectoryFile(ConfigCSVFileHelper configs, ParameterConfigurationSpace configSpace, boolean useTunerTimeAsWallTime)
	{
		ConcurrentSkipListMap<Double,  TrajectoryFileEntry> skipList = new ConcurrentSkipListMap<Double, TrajectoryFileEntry>();
		List<String> paramNames = new ArrayList<String>(configSpace.getParameterNames());
		Collections.sort(paramNames);
		
		for(int i=0; i < configs.getNumberOfDataRows(); i++)
		{

			String time = configs.getStringDataValue(i, 0);
			
			
			String[] dataRow =  configs.getDataRow(i);
			StringBuilder sb = new StringBuilder();
			
			int dataOffset = 5;
			for(int j=0; j < paramNames.size(); j++)
			{
				sb.append(paramNames.get(j)).append("=").append("'").append(dataRow[j+dataOffset]).append("',");
			}
			//System.out.println(time + "=>" + sb.toString());
			double tunerTime = Double.valueOf(dataRow[0]);
			Double empiricalPerformance = Double.valueOf(dataRow[1]);
			Double wallTime = Double.valueOf(dataRow[2]);
			Double overhead = Double.valueOf(dataRow[4]);
			
			if(wallTime == -1)
			{
				wallTime = tunerTime;
			}
		
			ParameterConfiguration configObj = configSpace.getParameterConfigurationFromString(sb.toString(), ParameterStringFormat.STATEFILE_SYNTAX);
			
			TrajectoryFileEntry tfe = new TrajectoryFileEntry(configObj, tunerTime, wallTime, empiricalPerformance, overhead);
			
			skipList.put(Double.valueOf(time), tfe);
			
		}
		return skipList;
	}
	
	
	/**
	 * Parses a Corrupted Hybrid ParamILS/SMAC Trajectory File
	 * @param configs 		CSV Configuration Helper
	 * @param configSpace 	Configuration Space to draw examples from
	 * @return SkipListMap that maps the time of the incumbent to a <cpuOverhead, incumbent> pair.
	 */
	private static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseHybridBrokenTrajectoryFile(ConfigCSVFileHelper configs, ParameterConfigurationSpace configSpace, boolean useTunerTimeAsWallTime)
	{
		ConcurrentSkipListMap<Double,  TrajectoryFileEntry> skipList = new ConcurrentSkipListMap<Double, TrajectoryFileEntry>();
		List<String> paramNames = new ArrayList<String>(configSpace.getParameterNames());
		Collections.sort(paramNames);
		
		for(int i=0; i < configs.getNumberOfDataRows(); i++)
		{

			String time = configs.getStringDataValue(i, 0);
			
			
			String[] dataRow =  configs.getDataRow(i);
			StringBuilder sb = new StringBuilder();
			
			int dataOffset = 5;
			for(int j=0; j < configs.getDataRow(i).length-dataOffset; j++)
			{
				sb.append(dataRow[j+dataOffset]).append("',");
			}
			//System.out.println(time + "=>" + sb.toString());
			double tunerTime = Double.valueOf(dataRow[0]);
			Double empiricalPerformance = Double.valueOf(dataRow[1]);
			Double wallTime = Double.valueOf(dataRow[2]);
			Double overhead = Double.valueOf(dataRow[4]);
			
			if(wallTime == -1)
			{
				wallTime = tunerTime;
			}
		
			ParameterConfiguration configObj = configSpace.getParameterConfigurationFromString(sb.toString(), ParameterStringFormat.STATEFILE_SYNTAX);
			
			TrajectoryFileEntry tfe = new TrajectoryFileEntry(configObj, tunerTime, wallTime, empiricalPerformance, overhead);
			
			skipList.put(Double.valueOf(time), tfe);
			
		}
		return skipList;
	}
	
	
	/**
	 * Parses a Trajectory File (both SMAC and ParamILS Formats)
	 * 
	 * NOTE: SMAC is tried first
	 * 
	 * @param configs 		CSV File representing the trajectory file
	 * @param configSpace 	Configuration Space to create Configurations in
	 * @return SkipListMap that maps the time of the incumbent to a <cpuOverhead, incumbent> pair.
	 */
	public static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseTrajectoryFile(ConfigCSVFileHelper configs, ParameterConfigurationSpace configSpace, boolean useTunerTimeAsWallTime)
	{
		ConcurrentSkipListMap<Double,TrajectoryFileEntry> skipList;
		
		try {
			skipList = TrajectoryFileParser.parseSMACTrajectoryFile(configs, configSpace, useTunerTimeAsWallTime);
		} catch(ArrayIndexOutOfBoundsException e )
		{
			//e.printStackTrace();
			log.debug("Trajectory File is not in SMAC Format, falling back to ParamILS Format");
			
			try 
			{
				skipList = TrajectoryFileParser.parseParamILSTrajectoryFile(configs, configSpace, useTunerTimeAsWallTime);
			} catch(RuntimeException e2)
			{
				log.debug("Trajectory File is not in ParamILS Format, falling back to Hybrid/Broken format");
				skipList = TrajectoryFileParser.parseHybridBrokenTrajectoryFile(configs, configSpace, useTunerTimeAsWallTime);
			}
		}
		return skipList;
		
		
	}
	
	public static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseTrajectoryFile(ConfigCSVFileHelper configs, ParameterConfigurationSpace configSpace)
	{
		return parseTrajectoryFile(configs, configSpace, false);
	}

	/**
	 * Parses a Trajectory File (both SMAC and ParamILS Formats)
	 * 
	 * NOTE: SMAC is tried first
	 * 
	 * @param trajectoryFile 	Trajectory file to parse
	 * @param configSpace 		Configuration Space to create Configurations in
	 * @return SkipListMap that maps the time of the incumbent to a <cpuOverhead, incumbent> pair.
	 * @throws FileNotFoundException, IOException 
	 */
	
	public static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseTrajectoryFile(File trajectoryFile, ParameterConfigurationSpace configSpace, boolean useTunerTimeAsWallTime) throws FileNotFoundException, IOException
	{
		CSVReader configCSV = new CSVReader(new FileReader(trajectoryFile),',',(char) 1);
		try {
			ConfigCSVFileHelper configs = new ConfigCSVFileHelper(configCSV.readAll(),1,0);
			return parseTrajectoryFile(configs, configSpace, useTunerTimeAsWallTime);
		} finally
		{
			configCSV.close();
		}
		
	}
	
	
	public static List<TrajectoryFileEntry> parseTrajectoryFileAsList(File trajectoryFile, ParameterConfigurationSpace configSpace, boolean useTunerTimeAsWallTime) throws FileNotFoundException, IOException
	{
		 ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseTrajectoryFile = parseTrajectoryFile(trajectoryFile, configSpace,useTunerTimeAsWallTime);
		 
		 List<TrajectoryFileEntry> tfes = new ArrayList<TrajectoryFileEntry>(parseTrajectoryFile.size());
		
		 for(TrajectoryFileEntry tfe : parseTrajectoryFile.values())
		 {
			 tfes.add(tfe);
		 }
		 
		 return tfes;
	}
	
	public static ConcurrentSkipListMap<Double, TrajectoryFileEntry> parseTrajectoryFile(File trajectoryFile, ParameterConfigurationSpace configSpace) throws FileNotFoundException, IOException
	{
		return parseTrajectoryFile(trajectoryFile,configSpace, false);
	}
	
	
	public static List<TrajectoryFileEntry> parseTrajectoryFileAsList(File trajectoryFile, ParameterConfigurationSpace configSpace) throws FileNotFoundException, IOException
	{
		return parseTrajectoryFileAsList(trajectoryFile,configSpace, false);
	}
	
	
}

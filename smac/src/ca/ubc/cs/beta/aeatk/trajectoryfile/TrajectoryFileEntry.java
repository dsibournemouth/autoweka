package ca.ubc.cs.beta.aeatk.trajectoryfile;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;

/**
 * Value object corresponding roughly to the significant rows in a trajectory file entry;
 * @author Steve Ramage 
 *
 */
public class TrajectoryFileEntry implements Comparable<TrajectoryFileEntry>
{
	private final ParameterConfiguration config;
	private final double empiricalPerformance;
	private final double acOverhead;
	private final double tunerTime;
	private final double wallTime;
	
	/**
	 * Creates a new Trajectory File Entry
	 * @param config 					configuration to log
	 * @param tunerTime 				tuner time when incumbent selected
	 * @param walltime					Wallclock time of entry
	 * @param empiricalPerformance  	empirical performance of incumbent
	 * @param acOverhead 				overhead time of automatic configurator
	 */
	public TrajectoryFileEntry(ParameterConfiguration config, double tunerTime, double walltime , double empiricalPerformance, double acOverhead)
	{
		this.config = config;
		this.empiricalPerformance = empiricalPerformance;
		this.acOverhead = acOverhead;
		this.wallTime = walltime;
		this.tunerTime = tunerTime;
	}
	

	public ParameterConfiguration getConfiguration()
	{
		return config;
	}
	
	public double getEmpericalPerformance()
	{
		return empiricalPerformance;
	}
	
	public double getACOverhead()
	{
		return acOverhead;
	}
	
	public double getTunerTime()
	{
		return tunerTime;
	}

	public double getWallTime()
	{
		return wallTime;
	}
	@Override
	public int compareTo(TrajectoryFileEntry o) {
		if(tunerTime - o.tunerTime < 0)
		{
			return -1;
		} else if( tunerTime - o.tunerTime == 0)
		{
			
			if(config.equals(o.config)) return 0;
			
			return (config.getFriendlyID() - o.config.getFriendlyID());
		} else
		{
			return 1;
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof TrajectoryFileEntry)
		{
			TrajectoryFileEntry oTFE = (TrajectoryFileEntry) o;
			
			if(oTFE.acOverhead != acOverhead)
			{
				return false;
			}
					
			if(oTFE.empiricalPerformance != empiricalPerformance)
			{
				return false;
			}
			
			if(oTFE.wallTime != oTFE.wallTime)
			{
				return false;
			}
					
			if(oTFE.tunerTime != oTFE.tunerTime)
			{
				return false;
			}
			
			return oTFE.config.equals(config);
			
		} 
		return false;
	}
	
	public int hashCode()
	{
		long val = Double.doubleToLongBits(tunerTime) ^ Double.doubleToLongBits(acOverhead) ^ Double.doubleToLongBits(wallTime) ^ Double.doubleToLongBits(empiricalPerformance);
		int hash = ((int) (val >> 32) ) ^ ((int) (val));
		return hash ^ config.hashCode();
		
 	}
	public String toString()
	{
		return "<"+getTunerTime() +","+ getEmpericalPerformance() +","+ getWallTime() + "," + config.getFriendlyIDHex() +">"; 
	}
	
}
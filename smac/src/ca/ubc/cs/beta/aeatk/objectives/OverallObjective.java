package ca.ubc.cs.beta.aeatk.objectives;

import java.util.Collection;

import org.apache.commons.math.stat.StatUtils;

/**
 * Enumeration that lists the various objectives for aggregating runs
 * 
 * NOTE: Not all are implemented and few are tested
 * 
 * @author sjr
 *
 */
public enum OverallObjective {
	/**
	 * Take the mean of the runs
	 */
	MEAN,
	/**
	 * Median of the runs
	 */
	//MEDIAN,
	/**
	 * The 90th percentile of the runs
	 */
	//Q90,

	
	/**
	 * Penalized Mean of 1000 for runs that took too long
	 */
	MEAN1000,
	/**
	 * Penalized Mean of 10 for runs that took too long
	 */
	MEAN10;
	/**
	 * Geometric mean of the runs
	 */
	//GEOMEAN;
	
	
	
	
		
		

	public double aggregate(Collection<Double> c, double cutoffTime)
	{
		double[] values = new double[c.size()];
		int i=0;
		for(double d : c)
		{
			values[i] = d;
			switch(this)
			{
			case MEAN10:
				values[i] = (values[i] >= cutoffTime) ? values[i] * 10 : values[i];
				break;
			case MEAN1000:
				values[i] = (values[i] >= cutoffTime) ? values[i] * 1000 : values[i];
				break;
			default:
			}
			
			
			i++;
		}
		
		switch(this)
		{
		case MEAN:
		case MEAN10:
		case MEAN1000:
			
			return StatUtils.mean(values);
		
		/*case MEDIAN:
			return StatUtils.percentile(values, 0.5);
		case Q90:
			return StatUtils.percentile(values, 0.9);
		
		case GEOMEAN:
			return StatUtils.geometricMean(values);		
			*/
		default:
			throw new UnsupportedOperationException(this.toString() + " is not a supported aggregation method");
		}
		
		
	}

	public double getPenaltyFactor() {
		switch(this)
		{
		case MEAN:
		//case GEOMEAN:
		//case MEDIAN:
		//case Q90:
			
			return 1;
		case MEAN10:
			return 10;
		case MEAN1000:
			return 1000;
			
		default: 
			throw new UnsupportedOperationException(this.toString() + " is not a supported aggregation method");
		}
	}
}

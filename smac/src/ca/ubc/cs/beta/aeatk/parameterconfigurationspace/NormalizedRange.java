package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

import java.io.Serializable;

/**
 * Maps a value on some interval to [0,1] and back
 * @see ParameterConfigurationSpace
 */
public class NormalizedRange implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2338679156299268217L;
	
	/**
	 * Stores the minimum value after being transformed
	 */
	private final double minNormalizedValue;
	
	/**
	 * Stores the maximum value after being transformed
	 */
	private final double maxNormalizedValue;
	
	/**
	 * Stores the minimum legal value we can return
	 */
	private final double minUnnormalizedValue;
	
	/**
	 * Stores the maximum legal value we can return
	 */
	private final double maxUnnormalizedValue;
	
	private final boolean normalizeToLog;
	private final boolean intValuesOnly;
	
	/**
	 * 
	 * @param minValue 			double for the minimum legal value in range
	 * @param maxValue			double for the maximum legal value in range
	 * @param normalizeToLog	<code>true</code> if we should take the log before normalizing, <code>false</code> otherwise
	 * @param intValuesOnly		<code>true</code> if the range is discrete (i.e. integers)
	 * 
	 * @throws IllegalArgumentException if minValue >= maxValue, any are infinite or NaN, or we are to log negative values.
	 */
	public NormalizedRange(double minValue, double maxValue, boolean normalizeToLog, boolean intValuesOnly)
	{
		
		
		
		this.normalizeToLog = normalizeToLog;
		this.intValuesOnly = intValuesOnly;
		
		this.minUnnormalizedValue = minValue;
		this.maxUnnormalizedValue = maxValue;
		if(intValuesOnly)
		{
			minValue -= 0.5;
			maxValue += 0.5;
		
		}
		
		
		
		if(normalizeToLog)
		{
			if ((minValue <= 0) || (maxValue <= 0))
			{
				throw new IllegalArgumentException("Log Scales cannot have negative or zero values in Param File: min:" + minValue + " max: " + maxValue);
			}
			
			minValue = Math.log10(minValue);
			maxValue = Math.log10(maxValue);
		}
		
		this.minNormalizedValue = minValue;
		this.maxNormalizedValue = maxValue;
		
		if(this.minNormalizedValue >= this.maxNormalizedValue)
		{
			throw new IllegalArgumentException("Min must be strictly less than max: " + minValue + " >= " + maxValue);
		}
		
		if(Double.isNaN(minValue) || Double.isInfinite(minValue))
		{
			throw new IllegalArgumentException("Min must be a real value");
		}
		
		if(Double.isNaN(maxValue) || Double.isInfinite(maxValue))
		{
			throw new IllegalArgumentException("Max must be a real value");
		}

	}
	
	/**
	 * 
	 * @param x number to normalize
	 * @return number in [0,1]
	 */
	public double normalizeValue(double x)
	{
		if(normalizeToLog)
		{
			x = Math.log10(x);
		}
		
		if (x < minNormalizedValue || x > maxNormalizedValue)
		{
			
			double a = minNormalizedValue;
			
			double b = maxNormalizedValue;
			if(intValuesOnly)
			{
				a = Math.round(a+0.5);
				b = Math.round(b-0.5);
			}
			
			if(normalizeToLog)
			{
				throw new IllegalArgumentException("Value " + Math.pow(10,x) + " is outside of domain [" + Math.pow(10,a) + "," + Math.pow(10,b) + "]");
			} else
			{
				throw new IllegalArgumentException("Value " + x + " is outside of domain [" + a + "," + b + "]");
			}
		}
		
		return (x - minNormalizedValue) / (maxNormalizedValue - minNormalizedValue);
	}
	
	/**
	 * 
	 * @param x number in [0,1]
	 * @return number in original range
	 */
	public double unnormalizeValue(double x)
	{
		
		if (x < 0 || x > 1)
		{
			throw new IllegalArgumentException("Value is outside of [0,1]");
		}
		
		double value; 
		if(normalizeToLog)
		{
			value = Math.pow(10, x*(maxNormalizedValue-minNormalizedValue) + minNormalizedValue);
		} else
		{
			value = x*(maxNormalizedValue-minNormalizedValue) + minNormalizedValue;
		}
		
		if(intValuesOnly)
		{
			value = Math.round(value);
		}
		
		value = Math.max(value, minUnnormalizedValue);
		value = Math.min(value, maxUnnormalizedValue);
		
		
		return value;
	}
	
	@Override
	public String toString()
	{
		return "(NormalizeRange: {Min: " + minNormalizedValue + " Max: " + maxNormalizedValue + ((normalizeToLog) ? " LOG " : "") + ((intValuesOnly) ? " INT " : "") + "})";
	}

	/**
	 * 
	 * @return <code>true</true> if the range is only integers, false otherwise
	 */
	public boolean isIntegerOnly() {
		return intValuesOnly;
	}
	
	
}
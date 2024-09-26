package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.exceptions.ParameterConfigurationLockedException;
import ca.ubc.cs.beta.aeatk.json.serializers.ParameterConfigurationSpaceJson;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace.Conditional;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace.ParameterType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.congrace.exp4j.Calculable;
import net.jcip.annotations.NotThreadSafe;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import net.objecthunter.exp4j.operator.Operator;


/**
 * This class represents an element in the associated {@link ParameterConfigurationSpace} and provides a natural {@link java.util.Map} like interface for accessing it's members 
 * but also uses an effective and fast storage mechanism for this. 
 * <p>
 * <b>WARNING:</b>This is not a general purpose <code>Map</code> implementation. Many map operations, like entry set provide unmodifiable views.
 * <p>
 * Differences between this and <code>Map</code>:
 * <p>
 * 1) The key and value space are fixed for each parameter to the corresponding ParamConfigurationSpace <br/>
 * 2) You cannot remove keys, nor can you add keys that don't exist. (i.e. you can only replace existing values)</br>
 * 3) The fastest way to iterate over this map is through <code>keySet()</code>.</br>
 * 4) EntrySet and valueSet() are not implemented, size() is constant and unaffected by removing a key<br/>
 * 5) Two objects are considered equal if and only if all there active parameters are equal. 
 * 
 * <p><b>Thread Safety:</b>This class is NOT thread safe under mutations but typically
 * the lifecycle of a ParamConfiguration object is that it is created with specific values,
 * and then never modified again. This class is thread safe under non-mutation operations.
 * 
 *
 */
@NotThreadSafe
@JsonDeserialize(using=ParameterConfigurationSpaceJson.ParamConfigurationDeserializer.class)
@JsonSerialize(using=ParameterConfigurationSpaceJson.ParamConfigurationSerializer.class)
public class ParameterConfiguration implements Map<String, String>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 879997991870028528L;

	/**
	 *  Stores a map of paramKey to index into the backing arrays
	 */
	private final Map<String, Integer> paramKeyToValueArrayIndexMap;
	
	/**
	 * @see ParameterConfigurationSpace
	 * Do _NOT_ write to this array, it is shared with all other configurations 
	 */
	private final boolean[] parameterDomainContinuous;
	
	/**
	 * For each numerical index in the backing array, if categorical what is the size of the domain.
	 * Do _NOT_ write to this array, it is shared with all other configurations
	 */
	private final int[] categoricalSize;
	

	/**
	 * Configuration space we are from
	 */
	private final ParameterConfigurationSpace configSpace;
	
	/**
	 * The array that actually stores our values. We store categorical values as their index in
	 * the configSpace.getValues( key )
	 * <p>
	 * e.g. if the List contains three values "foo", "bar" and "dog", and we want to store "bar"
	 * we would store 2.0. 
	 * <p>
	 * For continuous parameters we just store there raw value.
	 * <p>
	 * For non active parameters we store a NaN
	 */
	private final double[] valueArray;
	
	/** 
	 * Stores whether the map has been changed since the previous read
	 * 
	 * Prior to a read if this is true, we will call cleanUp()
	 */
	private volatile boolean isDirty = true; 
	
	
	/**
	 * Stores whether the parameter in the array is active or not (i.e. are all the parameters it is conditional upon set correctly).
	 * This value is lazily updated whenever it is read if this configuration is marked dirty.
	 */
	private final boolean[] activeParams;

	
	/**
	 * Value array used in comparisons with equal() and hashCode()
	 * (All Inactive Parameters are hidden)
	 */
	private final double[] valueArrayForComparsion;

	
	/**
	 * If set to true, the configuration can no longer be modified.
	 */
	private volatile boolean locked = false; 

	/**
	 * Default Constructor 
	 * @param configSpace 					paramconfigurationspace we are from
	 * @param valueArray 					array that represents our values. (DO NOT MODIFY THIS)
	 * @param categoricalSize 				array that has the size of the domain for categorical variables. (DO NOT MODIFY THIS) 
	 * @param parameterDomainContinuous		array that tells us whether an entry in the value array is continuous. (DO NOT MODIFY THIS)
	 * @param paramKeyToValueArrayIndexMap	map from param keys to index into the value arry.
	 */
	ParameterConfiguration(ParameterConfigurationSpace configSpace ,double[] valueArray, int[] categoricalSize, boolean[] parameterDomainContinuous, Map<String, Integer> paramKeyToValueArrayIndexMap )
	{
		this.configSpace = configSpace;
		this.valueArray = valueArray;
		
		this.categoricalSize = categoricalSize;
		this.parameterDomainContinuous = parameterDomainContinuous;
		this.paramKeyToValueArrayIndexMap = paramKeyToValueArrayIndexMap;
		this.myID = idPool.incrementAndGet();
		this.activeParams = new boolean[valueArray.length];
		isDirty = true;
		this.valueArrayForComparsion = new double[valueArray.length];
	}
		
	
	
	/**
	 * Copy constructor
	 * @param oConfig - configuration to copy
	 */
	public ParameterConfiguration(ParameterConfiguration oConfig)
	{
		if(oConfig.isDirty) oConfig.cleanUp();
		
		this.isDirty = oConfig.isDirty;
		
		this.myID = oConfig.myID;
		this.configSpace = oConfig.configSpace;
		this.valueArray = oConfig.valueArray.clone();
		this.categoricalSize = oConfig.categoricalSize;
		this.parameterDomainContinuous = oConfig.parameterDomainContinuous;
		this.paramKeyToValueArrayIndexMap = oConfig.paramKeyToValueArrayIndexMap;
		
		this.activeParams = oConfig.activeParams.clone();
		//this.activeParametersSet = oConfig.activeParametersSet;
		this.valueArrayForComparsion = oConfig.valueArrayForComparsion.clone();
		this.lastHash = oConfig.lastHash;
		
		//DO NOT CHANGE THIS TO BE TRUE, as new configurations should be mutable.
		this.locked = false;
		
	}
	
	
	
	
	
	
	
	@Override
	public int size() {
		return valueArray.length;
	}

	@Override
	public boolean isEmpty() {
		return (valueArray.length == 0);
	}

	@Override
	public boolean containsKey(Object key) {			
		
		return ((paramKeyToValueArrayIndexMap.get(key) != null));
	}



	public boolean containsValue(Object value) {
		for(String s : this.values())
		{
			if(s.equals(value))
			{
				return true;
			}
		} 
		
		return false;
		
	}

	

	@Override
	public String get(Object key) {
		
		Integer index = paramKeyToValueArrayIndexMap.get(key);
		if(index == null)
		{
			return null;
		}
		
		double value = valueArray[index];
		
		if(Double.isNaN(value))
		{
			return null;
		}
		
		if(parameterDomainContinuous[index])
		{
			NormalizedRange range = configSpace.getNormalizedRangeMap().get(key);
			if(range.isIntegerOnly())
			{
				long intValue = ((long) Math.round(range.unnormalizeValue(value)));
				if(configSpace.getParameterTypes().get(key) == ParameterType.ORDINAL)
				{
					return configSpace.getValuesMap().get(key).get( (int) intValue);
				} else
				{
					return String.valueOf(intValue);
				}
				
			} else
			{
				return String.valueOf(range.unnormalizeValue(value));
			}
			
		} else
		{
			if(value == 0)
			{
				return null;
			} else
			{		
				return configSpace.getValuesMap().get(key).get((int) value - 1);
			}
		}
	}



	/**
	 * Returns a copy of the parameter setting, this copy will be mutable.
	 * @return
	 */
	public ParameterConfiguration copy()
	{
		return new ParameterConfiguration(this);
	}
	
	
	/**
	 * Replaces a value in the Map
	 * 
	 * <b>NOTE:</b> This operation can be slow and could be sped up if the parser file had a Map<String, Integer> mapping Strings to there integer equivilants.
	 * Also note that calling this method will generally as a side effect change the FriendlyID of this ParamConfiguration Object
	 * 
	 * 
	 * @param key string name to store
	 * @param newValue string value to store
	 * @return previous value in the array
	 */
	public String put(String key, String newValue) 
	{
		
		if(this.locked)
		{
			
			if(get(key).equals(newValue))
			{
				return newValue;
			} else
			{
				throw new ParameterConfigurationLockedException("This parameter setting has been locked (via the lock() method), this prevents any further changes to this instance. You should use the copy() method to obtain a new instance that is still modifiable. Unfortunately defensive copying was leaking too much memory.");
			}
			
		}
		
		
		
		//Now th
		isDirty = true;
		
		//builtMap = null;
		
		
		
		/* We find the index into the valueArray from paramKeyIndexMap,
		 * then we find the new value to set from it's position in the getValuesMap() for the key. 
		 * NOTE: i = 1 since the valueArray numbers elements from 1
		 */
		
		
		Integer index = paramKeyToValueArrayIndexMap.get(key);
		if(index == null)
		{
			throw new IllegalArgumentException("This key does not exist in the Parameter Space: " + key);

		}
		
		String oldValue = get(key);
		
		if(newValue == null)
		{
			valueArray[index] = Double.NaN;
		}
		else if(parameterDomainContinuous[index])
		{
			
			if(configSpace.getParameterTypes().get(key) == ParameterType.ORDINAL)
			{
				newValue = String.valueOf(configSpace.getCategoricalValueMap().get(key).get(newValue));
				if(newValue == null)
				{
					throw new IllegalArgumentException("Value is not legal for this parameter: " + key + " Value:" + newValue);
				}
			}
			
			valueArray[index] = configSpace.getNormalizedRangeMap().get(key).normalizeValue(Double.valueOf(newValue));
			
		} else
		{
			List<String> inOrderValues = configSpace.getValuesMap().get(key);
			int i=1;		
			boolean valueFound = false;
			
			
			for(String possibleValue : inOrderValues)
			{
				if (possibleValue.equals(newValue))
				{
					this.valueArray[index] = i;
					valueFound = true;
					break;
				} 
				i++;
			}
			
			if(valueFound == false)
			{
				throw new IllegalArgumentException("Value is not legal for this parameter: " + key + " Value:" + newValue);
			}
			
			
		}
		
		/*
		 * After three years I don't think we need these tests anymore :)
		 * 
		 *
		if(parameterDomainContinuous[index] && newValue != null)
		{
			
			double d1 = Double.valueOf(get(key));
			double d2 = Double.valueOf(newValue);
			
			if(Math.abs(d1/d2 - 1) >  Math.pow(10, -12))
			{
				throw new IllegalStateException("Not Sure Why this happened: " + get(key) + " vs. " + newValue);
			}
				
		} else
		{
			if(get(key) == null)
			{
				if(newValue != null)
				{
					throw new IllegalStateException("Not Sure Why this happened: " + get(key) + " vs. " + newValue);	
				}
			} else if(!get(key).equals(newValue))
			{
				throw new IllegalStateException("Not Sure Why this happened: " + get(key) + " vs. " + newValue);
			}
		}
		*/
		return oldValue;
	}

	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public String remove(Object key) {
	
		throw new UnsupportedOperationException();
	}


	@Override
	public void putAll(Map<? extends String, ? extends String> m) {

		
		for(Entry<? extends String, ? extends  String> ent : m.entrySet())
		{
			this.put(ent.getKey(), ent.getValue());
		}
	
		
		
	}


	/**
	 * This method is not implemented
	 * @throws UnsupportedOperationException
	 */
	public void clear() {
		throw new UnsupportedOperationException();
		
	}


	@Override
	/**
	 * Returns a Set that will iterate in the order
	 */
	public Set<String> keySet() {
		LinkedHashSet<String> keys = new LinkedHashSet<String>();
		for(String s : paramKeyToValueArrayIndexMap.keySet())
		{
			keys.add(s);
		}
		
		return keys;
	}



	public Collection<String> values() {
		
		if(isDirty) cleanUp();
		return new ValueSetCollection();
		//return getRealMap().values();
	}


	/**
	 * A real map view of this configuration, it is nulled out when the map is made dirty.
	 */
	//private volatile Map<String, String> builtMap = null;
	
	/**
	 * Retrieves a real map view (one that has all methods supported) 
	 * @return
	 */
	/*
	private Map<String, String> getRealMap()
	{
		
		if(builtMap == null)
		{
			Map<String, String> myMap = new LinkedHashMap<String, String>();
			for(String name : this.configSpace.getParameterNamesInAuthorativeOrder())
			{
				myMap.put(name, this.get(name));
			}
			builtMap = Collections.unmodifiableMap(myMap);
		}
		
		
		return builtMap;
	}*/

	
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		
		if(isDirty) cleanUp();
		return new ParameterConfigurationEntrySet();
	}
	
	/**
	 * Returns a copy of the value array
	 * @return clone of the value array with inactive values intact
	 */
	public double[] toValueArray()
	{
		return valueArray.clone();
	}
	
	
	public double[] toComparisonValueArray() {
		if(isDirty) cleanUp();
		
		return valueArrayForComparsion.clone();
	}
	
	
	@Override
	public String toString()
	{
		if(isDirty) cleanUp();
		
		
	
		return getFriendlyIDHex();
	}
	
	/**
	 * Two instances are equal if they come from the same configuration space and their active parameters have the same values
	 * 
	 * <b>Note:</b> Integer value parameters will fail this test.
	 * 
	 * @param o object to check equality with
	 */
	
	public boolean equals(Object o)
	{

		if(this == o) return true;
		if (o instanceof ParameterConfiguration)
		{
			
			ParameterConfiguration opc = (ParameterConfiguration )o;
			if(isDirty) cleanUp();
			if(opc.isDirty) opc.cleanUp();
			
			if(!configSpace.equals(opc.configSpace)) return false;
			
			
			for(int i=0; i < valueArrayForComparsion.length; i++)
			{

				if(Math.abs(valueArrayForComparsion[i] - opc.valueArrayForComparsion[i]) > EPSILON)
				{
					return false;
				}
			}
			
			return true;
		} else
		{
			return false;
		}
	}
	
	volatile boolean hashSet = false;
	int lastHash = 0;
	
	@Override
	public int hashCode()	
	{ 
		if(isDirty || !hashSet)
		{
			if(isDirty) cleanUp();
			
			
			float[] values = new float[valueArrayForComparsion.length];
			
			
			for(int i=0; i < values.length; i++)
			{
				values[i] = (float) valueArrayForComparsion[i];
				
			}
			
			lastHash = Arrays.hashCode(values);
			
			hashSet = true;
		}
		
		
		return lastHash;
		
		
		
	}
	
	
	/**
	 * Builds a formatted string consisting of the active parameters 
	 * 
	 * @param preKey - String to appear before the key name
	 * @param keyValSeperator - String to appear between the key and value
	 * @param valueDelimiter - String to appear on either side of the value
	 * @param glue - String to placed in between various key value pairs
	 * @return formatted parameter string 
	 * @deprecated Clients should always specify a String Format {@link #getFormattedParameterString(ParameterStringFormat)}
	 */
	@Deprecated
	public String getFormattedParameterString(String preKey, String keyValSeperator,String valueDelimiter,String glue)
	{
		//Should use the String Format method
		return _getFormattedParameterString(preKey, keyValSeperator, valueDelimiter, glue, true);
	
		
	}
	/**
	 * Converts configuration into string format with the given tokens
	 * 
	 * @param preKey 					string that occurs before a key
	 * @param keyValSeperator 			string that occurs between the key and value
	 * @param valueDelimiter 			string that occurs on either side of the value
	 * @param glue 						string that occurs between pairs of key values
	 * @param hideInactiveParameters	<code>true</code> if we should drop inactive parameters, <code>false</code> otherwise
	 * @return formatted parameter string
	 */
	protected String _getFormattedParameterString(String preKey, String keyValSeperator,String valueDelimiter,String glue, boolean hideInactiveParameters)
	{
		Set<String> activeParams = getActiveParameters();
		StringBuilder sb = new StringBuilder();
		boolean isFirstParameterInString = true;
		
		for(String key : keySet())
		{
			if(get(key) == null) continue;
			if((!activeParams.contains(key)) && hideInactiveParameters) continue;
			if(!isFirstParameterInString)
			{
				sb.append(glue);
			}
			isFirstParameterInString = false;
			sb.append(preKey).append(key).append(keyValSeperator).append(valueDelimiter).append(get(key)).append(valueDelimiter);
		}
		return sb.toString();
	}
	
	/**
	 * Returns a string representation of this object
	 * @deprecated Clients should always specify a String Format {@link #getFormattedParameterString(ParameterStringFormat)}
	 * @return string representation of this object
	 */
	@Deprecated
	public String getFormattedParameterString()
	{
		return _getFormattedParameterString("-", " ","'"," ",true);
	}
	
	/**
	 * Returns a string representation of this object, according to the given {@link ParameterStringFormat}
	 * <br/>
	 * <b>Implementation Note:</b>No new String Formats should be able to generate the Strings "DEFAULT","<DEFAULT>","RANDOM","<RANDOM>", no matter how obnoxious the user specifying the param file is
	 * @param stringFormat stringformat to use
	 * @return string representation
	 */
	public String getFormattedParameterString(ParameterStringFormat stringFormat)
	{
		
		double[] valueArray = this.valueArray;
		switch(stringFormat)
		{
			case FIXED_WIDTH_ARRAY_STRING_MASK_INACTIVE_SYNTAX:
				if(isDirty) cleanUp();
				valueArray = this.valueArrayForComparsion;
				
			case FIXED_WIDTH_ARRAY_STRING_SYNTAX:
				StringWriter sWriter = new StringWriter();
				PrintWriter pWriter = new PrintWriter(sWriter);
				
				for(int i=0; i < valueArray.length; i++)
				{
					pWriter.format("%20s", valueArray[i]);
					if(i+1 != valueArray.length) pWriter.append(",");
				}
				return sWriter.toString();
			
		
		
			case ARRAY_STRING_MASK_INACTIVE_SYNTAX:
				if(isDirty) cleanUp();
				valueArray = this.valueArrayForComparsion;
				
			case ARRAY_STRING_SYNTAX:
				StringBuilder sb = new StringBuilder();
				for(int i=0; i < valueArray.length; i++)
				{
					sb.append(valueArray[i]);
					if(i+1 != valueArray.length) sb.append(",");
				}
				return sb.toString();
				
			case NODB_OR_STATEFILE_SYNTAX:
				return getFormattedParameterString(ParameterStringFormat.NODB_SYNTAX);
			//case SILLY:
			//	return "RANDOM";
		default:
			return _getFormattedParameterString(stringFormat.getPreKey(), stringFormat.getKeyValueSeperator(), stringFormat.getValueDelimeter(), stringFormat.getGlue(), stringFormat.hideInactiveParameters());
		}
		
	}
	
	
	/**
	 * Stores information about the various string formats we support
	 * <p>
	 * <b>Note:</b> Only some of these use the preKey, keyVal seperator, and glue 
	 * <b>WARNING:</b>DEFAULT, &gt;DEFAULT&lt;,RANDOM, &gt;RANDOM&lt; cannot be valid configuration strings for all format, because we will always parse these back as the default or random configuration respectively.
	 */
	public enum ParameterStringFormat
	{
			/**
			 * Uses a -(name) 'value' -(name) 'value' ... format [hiding inactive parameters]
			 */
			NODB_SYNTAX("-"," ", "'", " ", true), 
			/**
			 * Stores a number and colon before entry of <code>NODB_SYNTAX</code>. Used only for deserializing
			 */
			NODB_SYNTAX_WITH_INDEX("-"," ", "'", " ", true),
			/**
			 * Stores in a name='value',name='value'... format [preserving inactive parameters]
			 */
			STATEFILE_SYNTAX(" ","=","'",",",false), 
			/**
			 * Stores a number and colon before an entry of <code>STATEFILE_SYNTAX</code>. Used only for deserializing
			 */
			STATEFILE_SYNTAX_WITH_INDEX(" ", "=","'",",", false),
			/**
			 * Stores in a name='value',name='value'... format [removing inactive parameters]
			 */
			STATEFILE_SYNTAX_NO_INACTIVE(" ", "=" , "'" , ",",true),
			
			/**
			 * Uses a -Pname=value -Pname=value -Pname=value format
			 */
			SURROGATE_EXECUTOR("-P","=",""," ",true),
			
		
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile
			 */
			ARRAY_STRING_SYNTAX("","","","",false),
			
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile (hides Inactive)
			 */
			ARRAY_STRING_MASK_INACTIVE_SYNTAX("","","","", true),
			
			
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile
			 * All values are spaced to take 15 characters
			 */
			FIXED_WIDTH_ARRAY_STRING_SYNTAX("","","","",false),
			
			/**
			 * Stores the values as an array (value array syntax). This format is non human-readable and fragile (hides Inactive)
			 * All values are spaced to take 15 characters
			 */
			FIXED_WIDTH_ARRAY_STRING_MASK_INACTIVE_SYNTAX("","","","", true),
			
			/**
			 * Stores values in a NODB syntax, parses values from either.
			 */
			NODB_OR_STATEFILE_SYNTAX("","","","",true)
			
			//SILLY("","","","",false)
			;
			
			
			
			
			
		private final String preKey;
		private final String keyValSeperator;
		private final String valDelimiter;
		private final String glue;
		private final boolean hideInactive;
		
		
		private ParameterStringFormat(String preKey, String keyValSeperator, String valDelimeter, String glue, boolean hideInactive)
		{
			this.preKey = preKey;
			this.keyValSeperator = keyValSeperator;
			this.valDelimiter = valDelimeter;
			this.glue = glue;
			this.hideInactive = hideInactive;
		}

		public boolean hideInactiveParameters() {
			return hideInactive;
		}

		public String getPreKey() {
			return preKey;
		}
		
		public String getGlue()
		{
			return glue;
		}
		
		public String getValueDelimeter()
		{
			return valDelimiter;
		}
		
		public String getKeyValueSeperator()
		{
			return keyValSeperator;
		}
	
	}
	
	
	
	/**
	 * Returns a list of configurations in the neighbourhood of this one (Forbidden Configurations are excluded)
	 * @param  rand 					An object that will be used to generate neighbours for numerical parameters.
	 * @param  numNumericalNeighbours 	The number of neighbours numerical parameters should have
	 * @return list of configurations in the neighbourhood
	 */
	public List<ParameterConfiguration> getNeighbourhood(Random rand, int numNumericalNeighbours)
	{
		Set<String> activeParams = getActiveParameters();
		List<ParameterConfiguration> neighbours = new ArrayList<ParameterConfiguration>(numberOfNeighboursExcludingForbidden(numNumericalNeighbours, activeParams));
		
		/*
		 * i is the number of parameters
		 * j is the number of neighbours
		 */
		for(int i=0; i < configSpace.getParameterNamesInAuthorativeOrder().size(); i++)
		{
			double[] newValueArray = valueArray.clone();
			
			int failuresForParameter = 0;
			for(int j=1; j <= numberOfNeighboursForParameter(i,activeParams.contains(configSpace.getParameterNamesInAuthorativeOrder().get(i)),numNumericalNeighbours); j++)
			{
				newValueArray[i] = getNeighbourForParameter(i,j,rand);
			
				
				ParameterConfiguration config = new ParameterConfiguration(configSpace, newValueArray.clone(), categoricalSize, parameterDomainContinuous, paramKeyToValueArrayIndexMap);
				
				if(config.isForbiddenParameterConfiguration()) 
				{	
					
					failuresForParameter++;
					if(failuresForParameter < 100 && this.parameterDomainContinuous[i])
					{
						j--;
					} 
					continue;
				
				}
				
				neighbours.add(config);
			}
		}
		
		
		if(neighbours.size() > numberOfNeighboursExcludingForbidden(numNumericalNeighbours, activeParams))
		{
			throw new IllegalStateException("Expected " + numberOfNeighboursExcludingForbidden(numNumericalNeighbours, activeParams) + " neighbours (should be greater than or equal to) but got " + neighbours.size());
		}
		return neighbours;
		
		
	}
	
	/**
	 * Returns <code>true</code> if the parameters differ in exactly 1 place (only considers active parameters)
	 * 
	 * @param oConfig 	the other configuration to check
	 * @return
	 */
	public boolean isNeighbour(ParameterConfiguration oConfig)
	{
		
		if(!oConfig.getParameterConfigurationSpace().equals(getParameterConfigurationSpace()))
		{
			return false;
		}
		
		if(isDirty) cleanUp();		
		if(oConfig.isDirty) oConfig.cleanUp();
		
		
		int differences = 0;
		for(int i=0; i < this.activeParams.length; i++)
		{
			if(this.activeParams[i] && oConfig.activeParams[i])
			{
				
				if(this.valueArrayForComparsion[i] != oConfig.valueArrayForComparsion[i])
				{
					differences++;
				}
			}
		}
		
		return (differences == 1) ? true : false;
	}
	
	
	/**
	 * Returns the number of neighbours for this configuration
	 * @return number of neighbours that this configuration has
	 */
	private int numberOfNeighboursExcludingForbidden(int numNumericalNeighbours,Set<String> activeParams)
	{
		int neighbours = 0;
		
		 
		
		for(int i=0; i < configSpace.getParameterNamesInAuthorativeOrder().size(); i++)
		{
			
			neighbours += numberOfNeighboursForParameter(i, activeParams.contains(configSpace.getParameterNamesInAuthorativeOrder().get(i)), numNumericalNeighbours);
		}
		return neighbours;
		
	}
	
	/**
	 * Returns the number of Neighbours for the specific index into the param Array
	 * @param valueArrayIndex	index into the valueArray for this parameter
	 * @param isParameterActive boolean for if this parameter is active
	 * @return 0 if inactive, number of neighbours if active
	 */
	private int numberOfNeighboursForParameter(int valueArrayIndex, boolean isParameterActive, int neighboursForNumericalParameters)
	{
		if(isParameterActive == false) return 0;
		
		if(configSpace.searchSubspaceActive[valueArrayIndex]) return 0;
		
		if(parameterDomainContinuous[valueArrayIndex])
		{
		  return neighboursForNumericalParameters;
		} else
		{
		  return categoricalSize[valueArrayIndex] - 1;
		}
	}
	
	/**
	 * Returns a neighbour for this configuration in array format
	 * 
	 * @param valueArrayIndex   index of the parameter which we are generating a neighbour for
	 * @param neighbourNumber   number of the neighbour to generate
	 * @return
	 */
	private double getNeighbourForParameter(int valueArrayIndex, int neighbourNumber, Random rand)
	{
		if(parameterDomainContinuous[valueArrayIndex])
		{ 
			//Continuous arrays sample from a
			//normal distrbution with mean valueArray[i] and stdDeviation 0.2
			//0.2 is simply a magic constant
			double mean = valueArray[valueArrayIndex];
			
			
			
			while(true)
			{
				double randValue = 0.2*rand.nextGaussian() + mean;
				
				if(randValue >= 0 && randValue <= 1)
				{
					
					NormalizedRange nr = configSpace.normalizedRangesByIndex[valueArrayIndex]; 
					randValue = nr.normalizeValue(nr.unnormalizeValue(randValue));
					
					return randValue;
				}
			}
		}  else
		{ 
			//For categorical parameters we return the number of the neighbour 
			//up to our value in the parameter, and then 1 more than this after
			
			//e.g. if our value was 2 we would return
			//  0 => 0
			//  1 => 1 
	        //  2 => 3
			//  3 => 4
			//  ...
			if(neighbourNumber < valueArray[valueArrayIndex])
			{
				return neighbourNumber;
			} else
			{
				return neighbourNumber+1;
			}
		}
	}
	
	//private volatile Set<String> activeParametersSet;
	
	/**
	 * Recomputes the active parameters and valueArrayForComparision and marks configuration clean again
	 * We also change our friendly ID
	 */
	private void cleanUp()
	{	
		
		Set<String> activeParams = _getActiveParameters();
		
		for(Entry<String, Integer> keyVal : this.paramKeyToValueArrayIndexMap.entrySet())
		{
			
			
			this.activeParams[keyVal.getValue()] = activeParams.contains(keyVal.getKey()); 
			
			
			if(this.activeParams[keyVal.getValue()])
			{
				if(this.parameterDomainContinuous[keyVal.getValue()])
				{
					this.valueArrayForComparsion[keyVal.getValue()] = valueArray[keyVal.getValue()];
				} else
				{
					this.valueArrayForComparsion[keyVal.getValue()] = valueArray[keyVal.getValue()];
				}
				

			} else
			{
				this.valueArrayForComparsion[keyVal.getValue()] = Double.NaN;
			}
		}
		myID = idPool.incrementAndGet();
		//activeParametersSet = Collections.unmodifiableSet(activeParams);
		isDirty = false;
		
		
		if(!activeParams.equals(getActiveParameters()))
		{
			throw new IllegalStateException("Expected our internal set representation to match the array set implementation" + activeParams + " vs " + getActiveParameters());
		}
	}
	
	
	/**
	 * Returns the keys that are currently active
	 * @return set containing the key names that are currently active
	 */
	public Set<String> getActiveParameters()
	{
		if(isDirty)
		{
			cleanUp();
		}
		
		return new ActiveParametersSet();
		
		
	}
	
	/**
	 * Since the ConfigSpace already now an order of parameters to check active status, 
	 * we simply iterate over it and check the conditions
	 * @return
	 */
	private Set<String> _getActiveParameters()
	{
		Set<String> activeParams= new TreeSet<String>();
		Map<Integer, ArrayList<ArrayList<Conditional>>> conds = configSpace.getNameConditionsMap();
		List<String> param_names = configSpace.getParameterNamesInAuthorativeOrder();
		for(String param : configSpace.getActiveCheckOrderString()){
			
			Integer param_id = configSpace.getParamKeyIndexMap().get(param);
			
			//if no conditions on this parameter, it is always active
			if (!conds.containsKey(param_id)) {
				activeParams.add(param);
				continue;
			}
			
			ArrayList<ArrayList<Conditional>> clauses = conds.get(param_id);
			
			//only one of the clauses has to be true
			for (ArrayList<Conditional> clause : clauses){
				boolean all_satisfied = true;
				// all conditions in a clause have to be satisfied
				for (Conditional cond: clause){
					Integer parent_id = cond.parent_ID;
					String parent_name = param_names.get(parent_id);
					String parent_value_string = get(parent_name);
					double encodedParentPresentValue;

					// check whether parent is active; if not, child is also not active
					if (! activeParams.contains(parent_name)){
						all_satisfied = false;
						break;
					}
					
					//translate value
					
					String tmpParentValueString = parent_value_string;
					switch(configSpace.getParameterTypes().get(parent_name))
					{
					
					case ORDINAL:
						tmpParentValueString =  String.valueOf(configSpace.getCategoricalValueMap().get(parent_name).get(parent_value_string));
						
					case INTEGER:
					case REAL:
						encodedParentPresentValue = Double.parseDouble(tmpParentValueString);
						
						break;
					case CATEGORICAL:
						
						encodedParentPresentValue = (double) configSpace.getCategoricalValueMap().get(parent_name).get(parent_value_string);
					
						break;
					default:
						throw new IllegalStateException("Unknown Type");
					}
					

					// check condition
					
					
					boolean satisfied = cond.op.conditionalClauseMatch(encodedParentPresentValue, cond.values);
					
					all_satisfied = all_satisfied & satisfied;
				
					if (!all_satisfied){ //if one condition is not satisfied, the complete clause is falsified; no further check necessary
						break;
					}
				}
				if (all_satisfied) {
					activeParams.add(param);
					break;
				}
			}
		}
		
		return activeParams;
	}
	
	
	
	private static final AtomicInteger idPool = new AtomicInteger(0);
	private int myID;
	/**
	 * Friendly IDs are just unique numbers that identify this configuration for logging purposes
	 * you should never rely on this for programmatic purposes. 
	 * 
	 * If you change the configuration the id will be regenerated.
	 * 
	 * <b>Note</b>: While a given ID should refer to a specific configuration, the converse is not true.
	 * 
	 * @return unique id for this param configuration
	 */
	public int getFriendlyID() {
		
		if(isDirty) cleanUp();
		
		return myID;
	}

	public String getFriendlyIDHex()
	{
		String hex = Integer.toHexString(getFriendlyID());
		
		StringBuilder sb = new StringBuilder("0x");
		while(hex.length() + sb.length() < 6)
		{
			sb.append("0");
		}
		sb.append(hex.toUpperCase());
		return sb.toString();
	}
	/**
	 * Checks whether this configuration is forbidden
	 * @return <code>true</code> if the parameter is forbidden, false otherwise
	 */
	public boolean isForbiddenParameterConfiguration()
	{
		
		if(configSpace.isForbiddenParameterConfigurationByClassicClauses(valueArray))
		{
			return true;
		}
		
		if(!configSpace.hasNewForbidden())
		{
			return false;
		} else
		{
			/*for(ExpressionBuilder eb : configSpace.bl)
			{
				Expression calc = eb.build();
				*/
			
			
			
			List<Expression> expressions = configSpace.tlExpressions.get();
			if(expressions == null)
			{
				expressions = new ArrayList<Expression>();
				
				
				for(ExpressionBuilder builder : configSpace.bl)
				{
					Expression exp = null;
					
					
					
					 
					StringBuilder errorMessage = new StringBuilder(" Besides the operator and functions listed on http://www.objecthunter.net/exp4j/, the following are supported: ");
					
					List<String> operators = new ArrayList<>();
					for(Operator o : ForbiddenOperators.operators)
					{
						operators.add(o.getSymbol());
					}
					errorMessage.append(operators);
					try
					{
						  exp = builder.build();
					} catch(IllegalArgumentException e)
					{
						
						throw new IllegalArgumentException("The following forbidden line seems to be invalid: " + configSpace.expressions.get(builder) + "." + errorMessage, e);
					}
					
					for(Entry<String, Double> ent : configSpace.forbiddenOrdinalAndCategoricalVariableValues.entrySet())
					{
						exp.setVariable(ent.getKey(), Double.valueOf(ent.getValue()));
					}
					ValidationResult res = exp.validate(false);
					if(!res.isValid())
					{
						throw new IllegalArgumentException("The following forbidden line seems to be invalid: " + configSpace.expressions.get(builder) + " exp4j says:" + res.getErrors() + "." + ( configSpace.expressions.get(builder).indexOf(',') >= 0 ? " Guess: Is the , suppose to be an &&?":"")  + errorMessage );
					}
					
					expressions.add(exp);
				}
				
				configSpace.tlExpressions.set(expressions);
			}
			
			
		
			//List<Expression> expressions = configSpace.cl;
			
			for(Expression calc : expressions)
			{
				
				
				int i=0; 
				
				//synchronized(calc)
				{
				
					for(String name : configSpace.getParameterNamesInAuthorativeOrder())
					{
						if(configSpace.getNormalizedRangeMap().get(name) != null )
						{
							//variables.put(name, configSpace.getNormalizedRangeMap().get(name).unnormalizeValue(this.valueArray[i]));
							
							double d = configSpace.getNormalizedRangeMap().get(name).unnormalizeValue(this.valueArray[i]);
							if(configSpace.getParameterTypes().get(name) == ParameterType.ORDINAL || configSpace.getParameterTypes().get(name) == ParameterType.CATEGORICAL)
							{
								String value = configSpace.getValuesMap().get(name).get((int) d);
								
								calc.setVariable(name, Double.valueOf(configSpace.forbiddenParameterConstants.get(value)));
							} else
							{
								calc.setVariable(name, d);
							}
							
						} else
						{
							//variables.put(name, Double.valueOf(this.get(name)));
							
						
								
								
							calc.setVariable(name,Double.valueOf(configSpace.forbiddenParameterConstants.get(this.get(name))));
						}
						i++;
					}
					
					try 
					{
						if (calc.evaluate() == 0)
						{
							continue;
						} else
						{
							return true;
						}
					} catch(RuntimeException e)
					{
						throw new IllegalArgumentException("Error occured evaluating configuration for forbiddenness",e);
					}
				} 
				
			}
			
			return false;
		}
		

	}


	/**
	 * Returns the configuration space for this configuartion
	 * @return configSpace for this configuration
	 */ 
	public ParameterConfigurationSpace getParameterConfigurationSpace() {
		return configSpace;
	}

	

	private static final double EPSILON = Math.pow(10, -14);

	public boolean isInSearchSubspace() {
		
		for(int i=0; i < valueArray.length; i++)
		{
			if(configSpace.searchSubspaceActive[i])
			{
				if(Math.abs(valueArray[i]-configSpace.searchSubspaceValues[i]) > EPSILON)
				{
					return false;
				}
			}
		}
		
		return true;
	}



	/**
	 * @return
	 */
	public boolean isLocked()
	{
		return locked;
	}
	
	/**
	 * When invoked the parameter configuration becomes immutable.
	 */
	public void lock()
	{
		locked = true;
	}


	private class ValueSetCollection extends AbstractCollection<String>
	{

		@Override
		public int size() {
			return ParameterConfiguration.this.size();
		}

		@Override
		public Iterator<String> iterator() {
			return new Iterator<String>()
			{

				private int i=0;
				final int creationID = ParameterConfiguration.this.myID;
				
				
				@Override
				public boolean hasNext() {
					if(isDirty || creationID != ParameterConfiguration.this.myID) throw new ConcurrentModificationException("Detected change to the parameter settings");
					return (i < size());
				}

				@Override
				public String next() {
					if(isDirty || creationID != ParameterConfiguration.this.myID) throw new ConcurrentModificationException("Detected change to the parameter settings");
					if( i >= size() )
					{
						throw new NoSuchElementException();
					}
					return get(configSpace.getParameterNamesInAuthorativeOrder().get(i++));
					
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
				
			};
			
		}

	
		
		
	}
	
	
	private class ParameterConfigurationEntrySet extends AbstractSet<java.util.Map.Entry<String, String>>
	{

		@Override
		public int size() {
			return ParameterConfiguration.this.size();
		}

	
		@Override
		public Iterator<java.util.Map.Entry<String, String>> iterator() {
			
			
			return new Iterator<Entry<String, String>>()
			{

				final int creationID = ParameterConfiguration.this.myID;
				
				
				private int i=0; 
				@Override
				public boolean hasNext() {
					
					if(isDirty || creationID != ParameterConfiguration.this.myID) throw new ConcurrentModificationException("Detected change to the parameter settings");
					return (i < size());
				}

				@Override
				public java.util.Map.Entry<String, String> next() {
					
					if(isDirty || creationID != ParameterConfiguration.this.myID) throw new ConcurrentModificationException("Detected change to the parameter settings");
					String key = ParameterConfiguration.this.configSpace.getParameterNamesInAuthorativeOrder().get(i);
					i++;
					return new AbstractMap.SimpleImmutableEntry<String, String>(key, ParameterConfiguration.this.get(key));
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
					
				}
				
			};
		}
	}

	private final class ActiveParametersSet extends AbstractSet<String>
	{

		
		@Override
		public Iterator<String> iterator() {

			
			int firstIndex = activeParams.length;
			 
			
			for(int i=0; i < activeParams.length; i++)
			{
				if(activeParams[i])
				{
					firstIndex = i;
					break;
				}
			}
			
			
			final int firstRealIndex = firstIndex;
			
			
			return new Iterator<String>()
			{
			
				int i=firstRealIndex;
				
				final int creationID = ParameterConfiguration.this.myID;
				
				
				@Override
				public boolean hasNext() {
					
					if(isDirty || creationID != ParameterConfiguration.this.myID) throw new ConcurrentModificationException("Detected change to the parameter settings");
					
					
					return (i < valueArray.length);
				}

				@Override
				public String next() {
					
					if(isDirty || creationID != ParameterConfiguration.this.myID) throw new ConcurrentModificationException("Detected change to the parameter settings");
					
					String result = configSpace.getParameterNamesInAuthorativeOrder().get(i);
					
					
					
					i=-i;
					for(int j=-i+1; j < activeParams.length; j++)
					{
						if(activeParams[j])
						{
							i=j;
							break;
						}
					}
					
					if (i <= 0)
					{
						i = valueArray.length;
					}
					return result;
					
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
					
			
		}

		private volatile int size = -1;
		@Override
		public int size() {
			if(size == -1)
			{
				 size = 0;
				for(String ent : this)
				{
					size++;
				}
			
			}
			
			return size;
		}
		
		@Override
		public boolean contains(Object o)
		{
			Integer index = configSpace.getParamKeyIndexMap().get(o);
			
			if(index == null)
			{
				return false;
			}
			
			return activeParams[index];
		}
	}
}

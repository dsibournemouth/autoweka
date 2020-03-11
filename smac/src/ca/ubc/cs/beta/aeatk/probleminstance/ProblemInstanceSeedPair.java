package ca.ubc.cs.beta.aeatk.probleminstance;

import java.io.Serializable;

import ca.ubc.cs.beta.aeatk.json.serializers.ProblemInstanceJson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Immutable Class that represents an Algorithm Instance and Seed Pair
 * @author seramage
 *
 */
@JsonSerialize(using=ProblemInstanceJson.ProblemInstanceSeedPairSerializer.class)
@JsonDeserialize(using=ProblemInstanceJson.ProblemInstanceSeedPairDeserializer.class)
public class ProblemInstanceSeedPair implements Comparable<ProblemInstanceSeedPair>,Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7686639875384341346L;
	private final ProblemInstance pi;
	private final long seed;
	
	public ProblemInstanceSeedPair(ProblemInstance pi, long seed)
	{
		if(pi == null)
		{
			throw new IllegalArgumentException("ProblemInstance cannot be null");
		}
		this.pi = pi;
		this.seed = seed;
	}
	
	public ProblemInstance getProblemInstance()
	{
		return pi;
	}
	 
	public long getSeed()
	{
		return seed;
	}
	
	public boolean equals(Object o)
	{
		if(this == o) return true;
		if(o instanceof ProblemInstanceSeedPair)
		{
			ProblemInstanceSeedPair aisp = (ProblemInstanceSeedPair) o;
			return ((aisp.seed == seed) && pi.equals(aisp.pi)); 
		}
		return false;
	}
	
	public int hashCode()
	{
		
		return (int) ((seed >>> 32) ^ ((int) seed ) ^ pi.hashCode()); 
	}
	
	public String toString()
	{
		return "<Instance:" + pi.getInstanceID() + ", Seed:" + seed + ">";
	}

	@Override
	public int compareTo(ProblemInstanceSeedPair o) {
		int idDiff = pi.getInstanceID() - o.pi.getInstanceID();
		if(idDiff != 0)
		{
		       return idDiff;
		}
		
		long seedDiff =  seed - o.seed;
		
		if(seedDiff < 0)
		{
			return -1;
		} else if(seedDiff > 0)
		{
			return +1;
		}
		return 0;
		
		
		
	}

}

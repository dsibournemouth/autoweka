package ca.ubc.cs.beta.aeatk.random;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ec.util.MersenneTwister;

import net.jcip.annotations.ThreadSafe;

/***
 * An object that maintains a map of all the random objects in use,
 * and seeds them differently as needed. If used carefully this should allow you to vary parts 
 * of your application behaviour, while keeping the other constant.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
@ThreadSafe
public class SeedableRandomPool implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8404659826687752125L;
	
	private final int poolSeed;
	private final RandomFactory<? extends Random> fact;
	private final Map<String, Random> randomMap = new ConcurrentHashMap<String, Random>();
	private final Map<String, Integer> randomSeedMap;
	private final Set<String> usedNames = new TreeSet<String>();
	private final Set<String> specifiedInitialSeeds;
	
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private Map<String, Integer> originalSeeds;

	
	/**
 	 * @param poolSeed  The initial seed for the pool
	 */
	public SeedableRandomPool(long poolSeed)
	{
		//casting to int will just drop the first few bits
		this((int) poolSeed, new DefaultRandomFactory(MersenneTwister.class), Collections.<String, Integer> emptyMap() );
	}
	

	/**
 	 * @param poolSeed  The initial seed for the pool
	 */
	public SeedableRandomPool(int poolSeed)
	{
		this(poolSeed, new DefaultRandomFactory(MersenneTwister.class), Collections.<String, Integer> emptyMap() );
	}
	
	/**
	 * 
	 * @param poolSeed  The initial seed for the pool	
	 * @param randomClass   A class object that extends Random and has a 1-arg constructor that takes an integer or a long.
	 */
	public SeedableRandomPool(int poolSeed, final Class<? extends Random> randomClass)
	{
		this(poolSeed, new DefaultRandomFactory(randomClass), Collections.<String, Integer> emptyMap() );
	}
	
	/**
	 * @param poolSeed  The initial seed for the pool
	 * @param fact		   A factory method that allows us to create random objects, repeated invokations for the same seed should ALWAYS return a new object 			
	 */
	public SeedableRandomPool(int poolSeed, RandomFactory<? extends Random> fact)
	{
		this(poolSeed, fact, Collections.<String, Integer> emptyMap());
	}
	
	/**
	 * 
	 * @param poolSeed  	The initial seed for the objects
	 * @param initialSeeds  Initial seeds for each object
	 */
	public SeedableRandomPool(int poolSeed, Map<String, Integer> initialSeeds)
	{
		this(poolSeed, new DefaultRandomFactory(MersenneTwister.class) , initialSeeds);
	}
	
	
	/**
	 * 
	 * @param poolSeed  	The initial seed for the objects
	 * @param randomClass   A class object that extends Random and has a 1-arg constructor that takes an integer or a long.
	 */
	public SeedableRandomPool(int poolSeed, final Class<? extends Random> randomClass, Map<String, Integer> initialSeeds)
	{
		this(poolSeed, new DefaultRandomFactory(randomClass) , initialSeeds);
	}
	
	/**
	 * 
	 * @param poolSeed  		The initial seed for the objects
	 * @param randomFactory		A factory method that allows us to create random objects, repeated invokations for the same seed should ALWAYS return a new object 			
	 */
	public SeedableRandomPool(int poolSeed, RandomFactory<? extends Random> randomFactory, Map<String, Integer> initialSeeds)
	{
		this.poolSeed = poolSeed;
		this.fact = randomFactory;
		this.randomSeedMap = new ConcurrentHashMap<String, Integer>(initialSeeds);
		this.originalSeeds = Collections.unmodifiableMap(new HashMap<String, Integer>(initialSeeds));
		this.specifiedInitialSeeds = new HashSet<String>(initialSeeds.keySet());
	}
	
	/**
	 * Returns an unmodifable view of the map of initial seeds.
	 * @return
	 */
	public synchronized Map<String, Integer> getInitialSeeds()
	{
		return originalSeeds;
	}
	/**
	 * Returns a random object for a given name
	 * @param   enumeration   An enumeration for the object
	 * @return	Random object appropriately seeded if the seed was set explicitly or if not the seed used is defined to be the hashCode() of the string XOR initial seed.
	 */
	public synchronized Random getRandom(Enum<?> enumeration)
	{
		return getRandom(enumeration.name());
	}
	
	/**
	 * Returns a random object for a given name
	 * @param name 	The name of the random object
	 * @return	Random object appropriately seeded if the seed was set explicitly or if not the seed used is defined to be the hashCode() of the string XOR initial seed.
	 */
	public synchronized Random getRandom(String name)
	{
		this.usedNames.add(name);
		Random random = randomMap.get(name);
		if(random == null)
		{
			Integer seed = getSeed(name);
			random = fact.getRandom(seed);
			randomMap.put(name, random);
		}
		
		return random;
	}


	public synchronized int getSeed(Enum<?> enumeration)
	{
		return getSeed(enumeration.name());
	}
	/**
	 * Returns the seed for a given pool
	 */
	public synchronized int getSeed(String poolName) {
		Integer seed = this.randomSeedMap.get(poolName);
		if(seed == null)
		{
			seed = poolName.hashCode() ^ poolSeed;
			this.randomSeedMap.put(poolName,  seed);
		}
		return seed;
	}
	
	/**
	 * @return a mapping of all strings to seeds used
	 */
	public synchronized Map<String, Integer> getAllSeeds()
	{
		return new HashMap<String, Integer>(this.randomSeedMap);
	}
	
	/**
	 * Returns the original seed used
	 * @return
	 */
	public synchronized int getPoolSeed()
	{
		return this.poolSeed;
	}
	
	/**
	 * Returns the set of names used
	 */
	public synchronized Set<String> getUsedNames()
	{
		return new TreeSet<String>(this.usedNames);
	}
	
	public synchronized Set<String> getSpecifiedInitialNames()
	{
		return new TreeSet<String>(this.specifiedInitialSeeds);
	}
	
	public synchronized void logUsage()
	{
		log.debug("Seed for Seed Pool Was {}", poolSeed);
		TreeMap<String, Integer> map = new TreeMap<String, Integer>();
		map.putAll(this.randomSeedMap);
		for(Entry<String, Integer> seedPair : map.entrySet())
		{ 
			Object[] args = {seedPair.getKey(), seedPair.getValue(), this.specifiedInitialSeeds.contains(seedPair.getKey()), this.usedNames.contains(seedPair.getKey())};
			
			//This awful hack is so that we log properly when running unit tests, it means that this stuff will only be logged
			boolean unitTesting = false; //System.getProperty("java.class.path").toLowerCase().contains("junit") || System.getProperty("java.class.path").toLowerCase().contains("testng");
						
			if(this.specifiedInitialSeeds.contains(seedPair.getKey()) || unitTesting)
			{
				log.debug("Seed for {} was {}, Manually Set: {}  Used: {}",args);
			} else
			{
				log.trace("Seed for {} was {}, Manually Set: {}  Used: {}",args);
			}
		}
	}
	
	private static class DefaultRandomFactory implements RandomFactory<Random>
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -8877498257963036865L;
		private final Class<? extends Random> randomClass;
		public DefaultRandomFactory(Class<? extends Random> randomClass)
		{
			this.randomClass = randomClass;
			
		}
		
		@Override
		public Random getRandom(long seed) {
			
			try {
			Constructor<? extends Random> randConstruct = randomClass.getConstructor(long.class);
					
			if(randConstruct != null)
			{
				return randConstruct.newInstance(seed);
			}
				
			
			randConstruct = randomClass.getConstructor(int.class);
			
			if(randConstruct != null)
			{
				if(seed <= Integer.MAX_VALUE && seed >= Integer.MIN_VALUE)
				{
					return randConstruct.newInstance(seed);
				} else
				{
					throw new IllegalArgumentException("Seed (" + seed + ") specified is too big to fit in a integer, and we didn't find a long constructor");
				}
			}
					
			throw new IllegalArgumentException("Could not find a constructor that took an long or an integer as it's only parameter");
			
			} catch(IllegalArgumentException e)
			{
				throw e;
			} catch (Exception e) {
				throw new IllegalStateException("Error while generating random pool", e);
			} 
			
		}
	}
	
	
}



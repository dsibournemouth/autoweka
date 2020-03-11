package ca.ubc.cs.beta.aeatk.probleminstance.seedgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;

/**
 * Generates seeds for instances using a pre-specified list
 *
 *  
 */
public class SetInstanceSeedGenerator implements InstanceSeedGenerator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8634557199869232040L;
	private final LinkedHashMap<String, List<Long>> instances;
	
	private final Map<String, Integer> piKeyToIntMap = new HashMap<String, Integer>();
	private final List<Queue<Long>> seeds;
	private final List<String> instanceOrder;
	private final int initialSeedCount;
	private final boolean allInstanceHaveSameNumberOfSeeds;

	private final Map<Integer, Set<Long>> blacklistedSeeds = new HashMap<Integer, Set<Long>>();
	/**
	 * Standard Constructor
	 * 
	 * <b>Note:</b> We don't use <class>ProblemInstance</class> here because we generally create this object
	 * before we have created the <class>ProblemInstance</class>  {@link ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceHelper} is what generally creates this
	 * @param instances 		 map from instance name (string) to a list of seeds (long)
	 * @param instanceOrder		 list of instance names (string)
	 * @param maxSeedsPerConfig	 clamp max seeds per config to this value if more seeds are given (Not implemented currently)
	 */
	public SetInstanceSeedGenerator(
			LinkedHashMap<String, List<Long>> instances, List<String> instanceOrder,  int maxSeedsPerConfig) {
		if(maxSeedsPerConfig < 0) maxSeedsPerConfig = Integer.MAX_VALUE;
		this.instances = instances;
		this.instanceOrder = instanceOrder;
		seeds = new ArrayList<Queue<Long>>(instances.size());
		int i=0;
		
		//Stores the total number of seeds
		int seedSum = 0;
		
		//Stores the total 
		int lastSeedCount = -1;
		
		boolean allInstancesHaveSameNumberOfSeeds = true;
		for(Entry<String, List<Long>> ent : instances.entrySet())
		{
			piKeyToIntMap.put(ent.getKey(), i++);
			
			if(ent.getValue().size() > maxSeedsPerConfig)
			{
				ent.setValue(ent.getValue().subList(0, maxSeedsPerConfig));
			}
				
			
			
			seedSum+= ent.getValue().size();
			
			
			
			if(lastSeedCount != -1)
			{
				if(allInstancesHaveSameNumberOfSeeds)
				{
					allInstancesHaveSameNumberOfSeeds = lastSeedCount==ent.getValue().size();
				}
			} else
			{
				lastSeedCount = ent.getValue().size();
			}
		}
		
		this.initialSeedCount = seedSum;
		this.allInstanceHaveSameNumberOfSeeds = allInstancesHaveSameNumberOfSeeds;
		
		
		reinit();
	}
	
	
	
	

	@Override
	public void reinit() {
		seeds.clear();
		
		for(Entry<String, List<Long>> ent : instances.entrySet())
		{
			seeds.add(new LinkedList<Long>(ent.getValue()));
		}
		

	}

	@Override
	public int getNextSeed(ProblemInstance pi) {
		if(hasNextSeed(pi))
		{
			return (int) (long) seeds.get(piKeyToIntMap.get(pi.getInstanceName())).poll();
		} else
		{
			throw new IllegalStateException("No more Seeds for Problem Instance: " + pi.getInstanceName());
		}
		
	}

	@Override
	public boolean hasNextSeed(ProblemInstance pi) {
	
		if(this.blacklistedSeeds.get(pi.getInstanceID()) == null)
		{
			this.blacklistedSeeds.put(pi.getInstanceID(), new HashSet<Long>());
		}
		
		
		
		Collection<Long> blackListedSeeds = this.blacklistedSeeds.get(pi.getInstanceID());
		seeds.get(piKeyToIntMap.get(pi.getInstanceName())).removeAll(blackListedSeeds);
		
		
		return !seeds.get(piKeyToIntMap.get(pi.getInstanceName())).isEmpty();
	}
	
	@Override
	public List<ProblemInstance> getProblemInstanceOrder(Collection<ProblemInstance> c)
	{
		Map<String, ProblemInstance> instanceMap = new HashMap<String, ProblemInstance>();
		
		for(ProblemInstance pi : c)
		{
			instanceMap.put(pi.getInstanceName(), pi);
		}
		
		
		List<ProblemInstance> piOrder = new ArrayList<ProblemInstance>(instanceOrder.size());
		for(String instance : instanceOrder)
		{
			ProblemInstance pi = instanceMap.get(instance);
			if(pi == null)
			{
				pi = instanceMap.get(instance.replaceAll("//", "/"));
			}
			
			if(pi == null)
			{
				throw new IllegalStateException("Couldn't find instance that matches : " + instance );
			}
			piOrder.add(pi);
		}
		
		return piOrder;
	}





	@Override
	public int getInitialInstanceSeedCount() {

		return initialSeedCount;
	}





	@Override
	public boolean allInstancesHaveSameNumberOfSeeds() {

		return allInstanceHaveSameNumberOfSeeds;
	}





	@Override
	public void take(ProblemInstance instance, long seed) {
		if(this.blacklistedSeeds.get(instance.getInstanceID()) == null)
		{
			this.blacklistedSeeds.put(instance.getInstanceID(), new HashSet<Long>());
		}
		this.blacklistedSeeds.get(instance.getInstanceID()).add(seed);
	}

}

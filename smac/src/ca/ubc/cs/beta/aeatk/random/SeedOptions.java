package ca.ubc.cs.beta.aeatk.random;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import ca.ubc.cs.beta.aeatk.misc.jcommander.validator.NonNegativeInteger;
import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.Semantics;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ec.util.MersenneTwister;

@UsageTextField(hiddenSection=true)
public class SeedOptions extends AbstractOptions{

	@UsageTextField(level=OptionLevel.DEVELOPER)
	@Parameter(names={"--seed-offset","--seedOffset"}, description="offset of numRun to use from seed (this plus --numRun should be less than INTEGER_MAX)")
	public int seedOffset = 0 ;
	
	@UsageTextField(level=OptionLevel.INTERMEDIATE, defaultValues="Randomly generated")
	@Semantics(name="SEED", domain="SCENARIO")
	@Parameter(names={"--num-run","--numrun","--numRun","--seed"}, required=false, description="number of this run (used for file generation, etc). This also controls the seed.", validateWith=NonNegativeInteger.class)
	public int numRun = (new MersenneTwister()).nextInt( 1000000000) + 1000;
	
	@UsageTextField(level=OptionLevel.DEVELOPER)
	@DynamicParameter(names="-S", description="Sets specific seeds (by name) in the random pool (e.g. -SCONFIG=2 -SINSTANCE=4). To determine the actual names that will be used you should run the program with debug logging enabled, it should be output at the end.")
	public Map<String, String> initialSeedMap = new TreeMap<String, String>();
	
	
	public SeedableRandomPool getSeedableRandomPool()
	{
		
		Map<String, Integer> initSeeds = new HashMap<String, Integer>();
		for(Entry<String, String> ent : initialSeedMap.entrySet())
		{
		
			try {
				initSeeds.put(ent.getKey(),Integer.valueOf(ent.getValue()));
			} catch (NumberFormatException e)
			{
				throw new ParameterException("All Random Pool Seeds must be integer, key: " + ent.getKey() + " value: " + ent.getValue() + " doesn't seem to be one. ");
			}
			
			
		}
		
		return new SeedableRandomPool(numRun + seedOffset, initSeeds);
				
		
	}
}

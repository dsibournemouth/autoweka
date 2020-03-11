package ca.ubc.cs.beta.aeatk.runhistory;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.Set;
//

//import ca.ubc.cs.beta.smac.OverallObjective;
//import ca.ubc.cs.beta.smac.RunObjective;
//import ca.ubc.cs.beta.smac.ac.InstanceSeedGenerator;

//
//public class DebugRunHistory implements RunHistory{
//
//	private final RunHistory h1;
//	private final RunHistory h2;
//	
//	public DebugRunHistory(RunHistory h1, RunHistory h2)
//	{
//		this.h1= h1;
//		this.h2= h2;
//	}
//
//	@Override
//	public void append(AlgorithmRun run) {
//		h1.append(run);
//		h2.append(run);
//	}
//
//	public <X> X assertEquals(X obj, X obj2)
//	{
//		if(obj.equals(obj2)) return obj;
//		
//		if(obj instanceof int[][])
//		{
//			if (Arrays.deepEquals((int[][])obj, (int[][]) obj2))
//			{
//				return obj;
//			} else
//			{
//				throw new IllegalStateException("Not Equals");
//			}
//			
//			
//			
//		}
//		
//		if(obj instanceof double[][])
//		{
//			if (Arrays.deepEquals((double[][])obj, (double[][]) obj2))
//			{
//				return obj;
//			} else
//			{
//				throw new IllegalStateException("Not Equals");
//			}
//			
//			
//			
//		}
//		
//		
//		
//		if(obj instanceof double[])
//		{
//			if(Arrays.equals((double[]) obj, (double[]) obj2))
//			{
//				return obj;
//			} else
//			{
//				throw new IllegalStateException("Not Equals");
//			}
//		}
//		
//		throw new IllegalArgumentException("Not sure how to do equality on this");
//	}
//	@Override
//	public RunObjective getRunObjective() {
//		return assertEquals(h1.getRunObjective(), h2.getRunObjective());
//	}
//
//	@Override
//	public OverallObjective getOverallObjective() {
//
//		return assertEquals(h1.getOverallObjective(), h2.getOverallObjective());
//	}
//
//	@Override
//	public void incrementIteration() {
//		h1.incrementIteration();
//		h2.incrementIteration();
//	}
//
//	@Override
//	public int getIteration() {
//		if(h1.getIteration() - h2.getIteration() == 0) return h1.getIteration();
//		throw new IllegalStateException("Not Equals");
//	}
//
//	@Override
//	public Set<ProblemInstance> getInstancesRan(ParamConfiguration config) {	
//		return assertEquals(h1.getInstancesRan(config), h2.getInstancesRan(config));
//	}
//
//	@Override
//	public Set<ProblemInstanceSeedPair> getAlgorithmInstanceSeedPairsRan(
//			ParamConfiguration config) {
//		return assertEquals(h1.getAlgorithmInstanceSeedPairsRan(config),h2.getAlgorithmInstanceSeedPairsRan(config));
//	}
//
//	@Override
//	public double getEmpiricalCost(ParamConfiguration config,
//			Set<ProblemInstance> instanceSet, double cutoffTime) {
//
//		double d1 = h1.getEmpiricalCost(config, instanceSet, cutoffTime);
//		double d2 = h2.getEmpiricalCost(config, instanceSet, cutoffTime);
//		if(d1 - d2 == 0)
//		{
//			return d1;
//		} else
//		{
//			throw new IllegalStateException("not Equals");
//		}
//	}
//	
//	@Override
//	public double getEmpiricalCost(ParamConfiguration config,
//			Set<ProblemInstance> instanceSet, double cutoffTime,
//			Map<ProblemInstance, Map<Long, Double>> hallucinatedValues) {
//		double d1 = h1.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues);
//		double d2 = h2.getEmpiricalCost(config, instanceSet, cutoffTime, hallucinatedValues);
//		if(d1 - d2 == 0)
//		{
//			return d1;
//		} else
//		{
//			throw new IllegalStateException("not Equals");
//		}
//	}
//	
//
//	Random cloneRandom(Random r)
//	{	try{
//			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
//			ObjectOutputStream out = new ObjectOutputStream(byteArray);
//			out.writeObject(r);
//			out.close();
//			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(byteArray.toByteArray()));
//			return (Random) in.readObject();
//		} catch(Throwable t)
//		{
//			throw new IllegalStateException(t);
//		}
//	}
//	@Override
//	public ProblemInstance getRandomInstanceWithFewestRunsFor(
//			ParamConfiguration config, List<ProblemInstance> instanceList,
//			Random rand) {
//
//		Random rand2 = cloneRandom(rand);
//		return assertEquals(h1.getRandomInstanceWithFewestRunsFor(config, instanceList, rand), h2.getRandomInstanceWithFewestRunsFor(config, instanceList, rand2));
//	}
//
//	@Override
//	public ProblemInstanceSeedPair getRandomInstanceSeedWithFewestRunsFor(
//			ParamConfiguration config, List<ProblemInstance> instanceList,
//			Random rand) {
//		// TODO Auto-generated method stub
//		Random rand2 = cloneRandom(rand);
//		return assertEquals(h1.getRandomInstanceSeedWithFewestRunsFor(config, instanceList, rand), h2.getRandomInstanceSeedWithFewestRunsFor(config, instanceList, rand2));
//	}
//
//	@Override
//	public int getTotalNumRunsOfConfig(ParamConfiguration config) {
//		// TODO Auto-generated method stub
//		int d1 = h1.getTotalNumRunsOfConfig(config);
//		int d2 = h2.getTotalNumRunsOfConfig(config);
//		if(d1 - d2 == 0) return d1;
//		throw new IllegalStateException("Not Equals");
//	}
//
//	@Override
//	public double getTotalRunCost() {
//		// TODO Auto-generated method stub
//		double d1 = h1.getTotalRunCost();
//		double d2 = h2.getTotalRunCost();
//		if(d1 - d2 == 0) return d1;
//		throw new IllegalStateException("Not Equals");
//		
//	}
//
//	@Override
//	public double[] getRunResponseValues() {
//		// TODO Auto-generated method stub
//		return assertEquals(h1.getRunResponseValues(),h2.getRunResponseValues());
//	}
//
//	@Override
//	public Set<ProblemInstance> getUniqueInstancesRan() {
//		// TODO Auto-generated method stub
//		return assertEquals(h1.getUniqueInstancesRan(),h2.getUniqueInstancesRan());
//	}
//
//	@Override
//	public Set<ParamConfiguration> getUniqueParamConfigurations() {
//		// TODO Auto-generated method stub
//		return assertEquals(h1.getUniqueParamConfigurations(), h2.getUniqueParamConfigurations());
//	}
//
//	@Override
//	public int[][] getParameterConfigurationInstancesRanByIndex() {
//		// TODO Auto-generated method stub
//		return assertEquals(h1.getParameterConfigurationInstancesRanByIndex(),h2.getParameterConfigurationInstancesRanByIndex());
//	}
//
//	@Override
//	public List<ParamConfiguration> getAllParameterConfigurationsRan() {
//		// TODO Auto-generated method stub
//		return assertEquals(h1.getAllParameterConfigurationsRan(), h2.getAllParameterConfigurationsRan());
//	}
//
//	@Override
//	public double[][] getAllConfigurationsRanInValueArrayForm() {
//
//		return assertEquals(h1.getAllConfigurationsRanInValueArrayForm(),h2.getAllConfigurationsRanInValueArrayForm());
//	}
//
//	@Override
//	public List<AlgorithmRun> getAlgorithmRuns() {
//
//		return assertEquals(h1.getAlgorithmRuns(),h2.getAlgorithmRuns());
//	}
//
//	@Override
//	public List<RunData> getAlgorithmRunData() {
//		return assertEquals(h1.getAlgorithmRunData(),h2.getAlgorithmRunData());
//	}
//
//	@Override
//	public InstanceSeedGenerator getInstanceSeedGenerator() {
//		return h1.getInstanceSeedGenerator();
//
//	}
//
//	@Override
//	public Set<ProblemInstanceSeedPair> getCappedAlgorithmInstanceSeedPairs(
//			ParamConfiguration config) {
//		return assertEquals(h1.getCappedAlgorithmInstanceSeedPairs(config), h2.getCappedAlgorithmInstanceSeedPairs(config));
//	}
//
//	
//}

package ca.ubc.cs.beta.aeatk.model.data;

import java.util.Map;

import ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps;
import ca.ubc.cs.beta.aeatk.misc.math.MessyMathHelperClass;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;

public class RawSanitizedModelData implements SanitizedModelData {
	
	private final ParameterConfigurationSpace configSpace;
	protected final double[][] configs;
	private final double[] responseValues;
	private final double[][] prePCAInstanceFeatures;
	private double[] means;
	private double[] stdDev;
	private double[] pcaCoeff;
	private double[][] pcaVec;
	private final int[] constantColumns;
	
	private final boolean logModel;
	private int[][] theta_inst_idxs;
	private boolean[] censoredResponseValues;
	
	public RawSanitizedModelData(double[][] instanceFeatures, double[][] paramValues, double[] responseValues, int[] usedInstances, boolean logModel, int[][] theta_inst_idxs, boolean[] censoredResponseValues)
	{
		this(instanceFeatures, paramValues, responseValues, usedInstances, logModel, theta_inst_idxs, censoredResponseValues, null);
	}
	public RawSanitizedModelData(double[][] instanceFeatures, double[][] paramValues, double[] responseValues, int[] usedInstancesIdxs, boolean logModel, int[][] theta_inst_idxs, boolean[] censoredResponseValues, ParameterConfigurationSpace configSpace)
	{
		this.configSpace = configSpace;
		this.configs = paramValues;
		this.responseValues = responseValues;
		this.logModel = logModel;
		this.prePCAInstanceFeatures = ArrayMathOps.copy(instanceFeatures);
		this.theta_inst_idxs = theta_inst_idxs;
		this.censoredResponseValues = censoredResponseValues;
		
		
		MessyMathHelperClass pca = new MessyMathHelperClass();
		double[][] usedInstanceFeatures = new double[usedInstancesIdxs.length][];
		
		for(int i=0; i < usedInstanceFeatures.length; i++)
		{
			usedInstanceFeatures[i] = instanceFeatures[usedInstancesIdxs[i]];
		}
		int[] constFeatures = pca.constantColumnsWithMissingValues(usedInstanceFeatures);
		instanceFeatures = pca.copyMatrixAndRemoveColumns(instanceFeatures, constFeatures);
		
		this.constantColumns = constFeatures;
		
		
		
		
		
		if(logModel)
		{
			pca.max(responseValues, SanitizedModelData.MINIMUM_RESPONSE_VALUE);
			pca.log10(responseValues);
			
			
		}
		

		means = new double[0];
		stdDev = new double[0];
		pcaCoeff = new double[0];
		pcaVec = new double[0][];

	}
	
	@Override
	public double[][] getPrePCAInstanceFeatures() {
		return this.prePCAInstanceFeatures;
	}

	@Override
	public double[][] getPCAVectors() {
		return this.pcaVec;
	}

	@Override
	public double[] getPCACoefficients() {
		return this.pcaCoeff;
		
	}

	@Override
	public int[] getDataRichIndexes() {
		return new int[0];
	}

	@Override
	public double[] getMeans() {
		return means;
	}

	@Override
	public double[] getStdDev() {
		return stdDev;
	}

	@Override
	public double[][] getPCAFeatures() {
		return this.prePCAInstanceFeatures;
	}

	@Override
	public double[][] getConfigs() {

		return this.configs;
	}

	@Override
	public double[] getResponseValues() {

		return this.responseValues;
	}
	
	@Override
	public int[] getCategoricalSize()
	{
		return configSpace.getCategoricalSize();
	}
	 
	@Override
	public Map<Integer, int[][]> getNameConditionsMapParentsArray() {
		return configSpace.getNameConditionsMapParentsArray();
	}; 
	
	@Override
	public Map<Integer, double[][][]> getNameConditionsMapParentsValues() {
		return configSpace.getNameConditionsMapParentsValues();
	}
	
	@Override
	public Map<Integer, int[][]> getNameConditionsMapOp() {
		return configSpace.getNameConditionsMapOp();
	}
	
	/*
	@Override
	public int[][] getCondParents()
	{
		return configSpace.getCondParentsArray();
	}
	*/
	
	/*
	@Override
	public int[][][] getCondParentVals()
	{
		return configSpace.getCondParentValsArray();
	}
	*/

	@Override
	public double transformResponseValue(double d) {
		if(logModel)
		{
			
			return Math.log10(Math.max(d, SanitizedModelData.MINIMUM_RESPONSE_VALUE));
		} else
		{
			return d;
		}
	}
	@Override
	public int[][] getThetaInstIdxs() {
		return this.theta_inst_idxs;
	}
	@Override
	public boolean[] getCensoredResponses() {
		return this.censoredResponseValues;
	}
	@Override
	public int[] getConstantColumns() {
		return constantColumns;
	}
	@Override
	public boolean isEmptyFeatures() {
		throw new IllegalStateException("This method only exists for PCA transformations and doesn't have a proper meaning outside. I have not thought about how this method would be used outside of PCA.");
		//return false;
	}
	
}

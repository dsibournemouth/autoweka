package ca.ubc.cs.beta.aeatk.model.data;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Map;

import java.util.Arrays;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.math.ArrayMathOps;
import ca.ubc.cs.beta.aeatk.misc.math.MessyMathHelperClass;
import ca.ubc.cs.beta.aeatk.misc.math.MessyMathHelperClass.Operation;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;

/**
 * This class roughly does all the processing for sanitizing data
 * @author seramage
 *
 */
public class PCAModelDataSanitizer extends AbstractSanitizedModelData {

	
	private final double[][] pcaVec;
	private final double[] pcaCoeff;
	private final int[] sub;
	private final int[] constantColumnsAt10toMinus6;
	private final double[] means;
	private final double[] stdDevs;
	private final double[][] pcaFeatures;
	private final double[][] prePCAInstanceFeatures;
	private final double[] responseValues;
	private final ParameterConfigurationSpace configSpace;
	private final double[][] configs;
	
	private final boolean emptyFeatures;
	private final boolean logModel;
	/**
	 * Debugging crap that basically writes the arguments to a file that you can then use to test outside of Matlab
	 */
	public static int index = 0;
	public static final String filename = "/tmp/lastoutput-mds";
	static boolean writeOutput = true;
	private Logger log = LoggerFactory.getLogger(getClass());
	private int[][] theta_inst_idxs;
	private boolean[] censoredResponseValues;
//	
//	public static void main(String[] args)
//	{
//		for(int i=0; i < 10; i++)
//		{
//			/*
//			double[][] m1 = {{ 1,2},{3,4},{5,6}};
//			double[][] m2 = {{1,2,3},{4,5,6}};
//			System.out.println(explode(Arrays.deepToString((new MessyMathHelperClass()).matrixMultiply(m1, m2))));
//			 */
//			File f = new File(filename + "-" + 1);
//			ObjectInputStream in;
//			try {
//				in = new ObjectInputStream(new FileInputStream(f));
//			
//			double[][] instanceFeatures  = (double[][]) in.readObject();
//			double[][] paramValues = (double[][]) in.readObject();
//			double[] responseValues = (double[]) in.readObject();
//			int[] usedInstances = (int[]) in.readObject();
//			in.close();
//		
//			writeOutput = false;
//			
//			int numPCA = 7;
//			
//			boolean logModel = true;
//			
//			
//			SanitizedModelData mdc = new PCAModelDataSanitizer(instanceFeatures, paramValues, numPCA, responseValues, usedInstances, logModel);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
//	}
	
	public static String explode(String s)
	{
		return s.replaceAll("]","}\n").replaceAll("\\[", "{");
	}
	
	public PCAModelDataSanitizer(double[][] instanceFeatures, double[][] paramValues, int numPCA, double[] responseValues,  boolean logModel, int[][] theta_inst_idxs, boolean[] censoredResponseValues )
	{
		this(instanceFeatures, paramValues, numPCA, responseValues, logModel,  theta_inst_idxs,  censoredResponseValues , null);
	}
	
	public static boolean printFeatures = false;
	
	/*
	 * Dimensions:
	 * instanceFeatures: #instances * # features
	 * paramValues: #uniq configs * #dimensions
	 * responseValue: #runs 
	 * theta_inst_idxs: #runs*2; THIS IS 1-based! Each value for the theta_ids is between 1 and #uniq confis; each value for the inst_ids is between 1 and #instances
	 * censoredResponseValues: #runs
	 */
	public PCAModelDataSanitizer(double[][] instanceFeatures, double[][] paramValues, int numPCA, double[] responseValues, boolean logModel, int[][] theta_inst_idxs, boolean[] censoredResponseValues , ParameterConfigurationSpace configSpace)
	{
		//=== Setting the previous redundant input int[] usedInstancesIdxs directly from theta_inst_idxs: it's a 0-based vector of the instances used (i.e., {the union over the second column of theta_inst_idxs}-1).  
		Set<Integer> usedInstIdxs = new TreeSet<>();
		for (int i = 0; i < theta_inst_idxs.length; i++) {
			usedInstIdxs.add(theta_inst_idxs[i][1]);
		}
		int[] usedInstancesIdxs = new int[usedInstIdxs.size()];
		int count = 0;
		for (int idx : usedInstIdxs) {
			usedInstancesIdxs[count++] = idx-1;
		} 
		
		
		this.configSpace = configSpace;
		this.configs = paramValues;
		this.responseValues = responseValues;
		this.theta_inst_idxs = theta_inst_idxs;
		this.censoredResponseValues = censoredResponseValues;
		
		this.prePCAInstanceFeatures = ArrayMathOps.copy(instanceFeatures);
		
		double[][] instanceFeaturesGreaterThan10toMinus6 = ArrayMathOps.copy(instanceFeatures);
		
		MessyMathHelperClass pca = new MessyMathHelperClass();
		double[][] usedInstanceFeatures = new double[usedInstancesIdxs.length][];
		
		for(int i=0; i < usedInstanceFeatures.length; i++)
		{
			usedInstanceFeatures[i] = instanceFeaturesGreaterThan10toMinus6[usedInstancesIdxs[i]];
		}
		
		/****
		* NOTE: Yes this code is removing constant columns twice, once at the level 
		* of 10^-6 and once at the level of 10^-5. This is what MATLAB did (verified 2014)
		**/
		
		
		int[] constFeaturesAt10toMinus6 = pca.constantColumnsWithMissingValues(usedInstanceFeatures);
		instanceFeaturesGreaterThan10toMinus6 = pca.copyMatrixAndRemoveColumns(instanceFeaturesGreaterThan10toMinus6, constFeaturesAt10toMinus6);
	
		constantColumnsAt10toMinus6 = constFeaturesAt10toMinus6;
		
		log.trace("Discarding {} constant inputs of {} in total.", constFeaturesAt10toMinus6.length, prePCAInstanceFeatures[0].length);
	
		double[][] instanceFeaturesT = pca.transpose(instanceFeaturesGreaterThan10toMinus6);
		
		
		double[] firstStdDev = pca.getRowStdDev(instanceFeaturesT);
		//double[][] pcaedFeatures =pca.getPCA(instanceFeatures, numPCA); 
		
		this.logModel = logModel;
		if(logModel)
		{
			pca.max(responseValues, SanitizedModelData.MINIMUM_RESPONSE_VALUE);
			pca.log10(responseValues);
		
		}
		
		//TODO: Give this variable an intellegent name
		int[] mySub = pca.getSub(firstStdDev);

		if(mySub.length == 0)
		{
			//throw new IllegalStateException("Not sure what to do in this case at the moment");
			sub = new int[0];
			means = new double[0];
			stdDevs = new double[0];
			pcaCoeff = new double[0];
			pcaVec = new double[0][];
			pcaFeatures = new double[instanceFeaturesGreaterThan10toMinus6.length][1];
			emptyFeatures = true;
			return;
		} else if (instanceFeaturesGreaterThan10toMinus6[0].length < numPCA)
		{
			sub = new int[0];
			means = new double[0];
			stdDevs = new double[0];
			pcaCoeff = new double[0];
			pcaVec = new double[0][];
			pcaFeatures = instanceFeaturesGreaterThan10toMinus6;
			emptyFeatures = false;
			return;
		} else
		{
			emptyFeatures = false;
			sub = mySub;
		}
		
		double[][] instanceFeaturesGreaterThan10toMinus5 = pca.copyMatrixAndKeepColumns(instanceFeaturesGreaterThan10toMinus6, sub);
		instanceFeaturesT = pca.transpose(instanceFeaturesGreaterThan10toMinus5);
		means = pca.getRowMeans(instanceFeaturesT);
		stdDevs = pca.getRowStdDev(instanceFeaturesT);
		
		pca.perColumnOperation(instanceFeaturesGreaterThan10toMinus5, means, Operation.SUBTRACT);
		pca.perColumnOperation(instanceFeaturesGreaterThan10toMinus5, stdDevs, Operation.DIVIDE);
		
		pcaCoeff = pca.getPCACoeff(instanceFeaturesGreaterThan10toMinus5, numPCA);
		pcaVec = pca.getPCA(instanceFeaturesGreaterThan10toMinus5, numPCA);
		
		
		//double[][] pcaVecT = pca.transpose(pcaVec);
//		pcaFeatures = pca.matrixMultiply(instanceFeatures, pcaVec);

/*
		System.out.print("Constant Columns:" + constantColumnsAt10toMinus6.length + ":");
		System.out.println(Arrays.toString(constantColumnsAt10toMinus6));
		
		System.out.print("Sub:" + sub.length + ":");
		System.out.println(Arrays.toString(sub));
		
		System.out.print("Means:" + means.length + ":");
		System.out.println(Arrays.toString(means));
		
		System.out.print("Pre-PCA:" + prePCAInstanceFeatures[0].length + ":");
		System.out.println(Arrays.toString(prePCAInstanceFeatures[0]) + ":");
	*/	
		
		pcaFeatures = applyTransformation(prePCAInstanceFeatures,emptyFeatures, constantColumnsAt10toMinus6, sub, means, stdDevs, pcaVec);
		/*
		if(RoundingMode.ROUND_NUMBERS_FOR_MATLAB_SYNC)
		{
			System.out.println("PCA Features Hash: " + ArrayMathOps.matlabHashCode(pcaFeatures));
		}
		*/
		
	}
	
	public static double[][] applyTransformation(double[][] instanceFeatures,boolean emptyFeatures,  int[] constantColumns, int[] sub, double[] means, double[] stdDevs,  double[][] pcaVec){

		
		if(emptyFeatures)
		{
			return new double[instanceFeatures.length][1];
		}
		
		MessyMathHelperClass pca = new MessyMathHelperClass();
		double[][] result = pca.copyMatrixAndRemoveColumns(instanceFeatures, constantColumns);
		
		if (sub.length == 0){
			return result;
		} 

		result = pca.copyMatrixAndKeepColumns(result, sub);
		pca.perColumnOperation(result, means, Operation.SUBTRACT);
		pca.perColumnOperation(result, stdDevs, Operation.DIVIDE);
		return pca.matrixMultiply(result, pcaVec);
	}

	
	@Override
	public double[][] getPrePCAInstanceFeatures()
	{
		return prePCAInstanceFeatures;
	}
	
	@Override
	public double[][] getPCAVectors() {
		return pcaVec;
	}

	
	@Override
	public double[] getPCACoefficients() {
		return pcaCoeff;
	}

	
	@Override
	public int[] getDataRichIndexes() {
		return sub;
	}

	@Override
	public double[] getMeans() {
		return means;
	}

	
	@Override
	public double[] getStdDev() {
		return stdDevs;
	}
	
	
	@Override
	public double[][] getPCAFeatures()
	{
		return pcaFeatures;
	}
	
	@Override
	public double[][] getConfigs()
	{
		return configs;
	}

	@Override
	public double[] getResponseValues()
	{
		return responseValues;
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

		int[][] theta_inst_idxs = new int[this.theta_inst_idxs.length][0];
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			theta_inst_idxs[i] = this.theta_inst_idxs[i].clone();
		}
				
		return theta_inst_idxs; 
	}

	@Override
	public boolean[] getCensoredResponses() {
		return this.censoredResponseValues;
	}

	@Override
	public int[] getConstantColumns() {
		return constantColumnsAt10toMinus6;
	}
	
	@Override
	public boolean isEmptyFeatures()
	{
		return emptyFeatures;
	}
}

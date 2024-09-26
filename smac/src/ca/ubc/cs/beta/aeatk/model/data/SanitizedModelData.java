package ca.ubc.cs.beta.aeatk.model.data;

import java.util.Map;

/**
 * Takes all the data from the automatic configurator, and applies various transformations to it.
 * 
 * Methods here that are deprecated basically need to be rethought out in light of the new Decorator approach to sanitizing
 * the model data. (e.g. The clients of this, shouldn't care whether we are PCAing or not they should just get the new features, same 
 * with transformation of the columns, etc...)
 * 
 * @deprecated There is no alternative to this class at present but it's fairly ugly, expect this class
 * to be removed in future.
 * 
 * @author seramage
 *
 */
@Deprecated
public interface SanitizedModelData {

	public static final double MINIMUM_RESPONSE_VALUE = 0.005;
	
	
	@Deprecated
	/**
	 * Get the instance features prior to MessyMathHelperClass
	 * @return matrix of instance features
	 */
	public double[][] getPrePCAInstanceFeatures();

	@Deprecated
	/**
	 * Return the MessyMathHelperClass Vectors
	 * @return pca vectors from features
	 */
	public double[][] getPCAVectors();

	@Deprecated
	/**
	 * Return MessyMathHelperClass Coefficients
	 * @return pca coefficients
	 */
	public double[] getPCACoefficients();

	@Deprecated
	/**
	 * Not sure what this returns, I believe columns that aren't all constant
	 * @return array of non constant columns 
	 */
	public int[] getDataRichIndexes();

	@Deprecated
	/**
	 * Gets the mean of every column 
	 * @return array of means for every column
	 */
	public double[] getMeans();

	@Deprecated
	/**
	 * Gets the standard deviation of every column
	 * @return array containing the standard deviation of every column
	 */
	public double[] getStdDev();

	@Deprecated
	/**
	 * Returns the MessyMathHelperClass reduced features
	 * @return array of features that have been reduced by MessyMathHelperClass
	 */
	public double[][] getPCAFeatures();

	/**
	 * Return an array containing all the parameter configurations in array format
	 * @return array of configurations ran in value array format {@link ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration#toValueArray()}
	 */
	public double[][] getConfigs();
	
	
	/**
	 * Returns the response values (transformed if necessary)
	 * @return	response values
	 */
	public double[] getResponseValues();

	/**
	 * Returns the categorical size of each parameter of the configuration array
	 * @return	array specifying the size of the domain of each categorical parameter of a valueArray.
	 * @see ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration for more details
	 */
	public int[] getCategoricalSize();

	/**
	 * Returns the indexes of the parents of each configuration
	 * @see ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration for more details
	 * @return array of conditional parents
	 */
	//public int[][] getCondParents();

	/**
	 * Stores the conditional parent values required to make a parameter active
	 * @see ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration ParamConfiguration for more details
	 * @return  array of values required to be set for a parameter to be active
	 */
	//public int[][][] getCondParentVals();
	
	/**
	 * maps variable index to disjunctions of conjunctions of parent variables
	 * @see ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration for more details
	 */
	public Map<Integer, int[][]> getNameConditionsMapParentsArray();	
	/**
	 * maps variable index to disjunctions of conjunctions of parent values in conditional
	 * @see ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration for more details
	 */
	public Map<Integer, double[][][]> getNameConditionsMapParentsValues();
	/**
	 * maps variable index to disjunctions of conjunctions of conditional operator
	 * @see ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration for more details
	 */
	public Map<Integer, int[][]> getNameConditionsMapOp();
	
	/**
	 * Transforms a response value according to some function
	 * (e.g.) log
	 * @param d input value to be transformed
	 * @return the transformed response value
	 */
	public double transformResponseValue(double d);

	/**
	 * Returns the Theta Instance Indexes for the response values 
	 * @return an Nx2 array where 
	 */
	public int[][] getThetaInstIdxs();

	/**
	 * Returns an array for each response value indicating whether it was censored or not. 
	 * @return array which for each element says whether response[i] is censored
	 */
	public boolean[] getCensoredResponses();
	
	/**
	 * Return columns that are constant (to a strict definition of constant)
	 * @return
	 */
	public int[] getConstantColumns();

	/**
	 * True if the features are empty after constant columns are removed 
	 */
	boolean isEmptyFeatures();

	
}

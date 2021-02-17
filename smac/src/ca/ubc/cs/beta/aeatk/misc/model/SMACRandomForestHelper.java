package ca.ubc.cs.beta.aeatk.misc.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.options.RandomForestOptions;
import ca.ubc.cs.beta.models.fastrf.RegtreeBuildParams;
/**
 * Utility class that converts the RandomForestOptions object into a RegtreeBuildParams objects
 * @author sjr
 *
 */
public class SMACRandomForestHelper {

	private static final Logger log = LoggerFactory.getLogger(SMACRandomForestHelper.class);
	
	/**
	 * Converts the rfOptions and other parameters into the required RegtreeBuildParams
	 * 
	 * @param rfOptions options object specifying settings for RandomForest construction
	 * @param numberOfFeatures   number of features we will build with
	 * @param categoricalSize	 sizes of the categorical values
	 * @param nameConditionsMapParentsArray maps variable index to disjunctions of conjunctions of parent variables
	 * @param nameConditionsMapParentsValues maps variable index to disjunctions of conjunctions of parent values in conditional
	 * @param nameConditionsMapOp maps variable index to disjunctions of conjunctions of conditional operator
	 * @return regtreeBuildParams object for Random Forest construction
	 */
	public static RegtreeBuildParams getRandomForestBuildParams(RandomForestOptions rfOptions, int numberOfFeatures, int[] categoricalSize, Map<Integer, int[][]> nameConditionsMapParentsArray, Map<Integer, double[][][]> nameConditionsMapParentsValues, Map<Integer, int[][]> nameConditionsMapOp, Random rand)
	{
	/*
	 * Parameter File Generator
	 */
	//	public RegtreeBuildParams(boolean doBootstrapping, int splitMin, double ratioFeatures, int[] catDomainSizes)
	
	
	/*
	 * Most of the defaults are either read from the config or were 
	 * pilfered from a run of the MATLAB
	 * The actual values may need to be more intelligently chosen.
	 */
	RegtreeBuildParams buildParams = new RegtreeBuildParams(false, rfOptions.splitMin, rfOptions.ratioFeatures, categoricalSize);
	buildParams.splitMin = rfOptions.splitMin;
	buildParams.ratioFeatures = rfOptions.ratioFeatures;//(5.0/6);
	
	buildParams.logModel = ((rfOptions.logModel == null) ? 1 :(((rfOptions.logModel) ? 1 : 0)));
	buildParams.storeResponses = rfOptions.storeDataInLeaves;
	buildParams.random = rand;
	//System.out.println("Random: " + buildParams.random.nextInt());
	buildParams.minVariance = rfOptions.minVariance;
	
	if(rfOptions.brokenVarianceCalculation)
	{
		log.warn("Model set to use broken variance calculation, this may affect performance");
		buildParams.brokenVarianceCalculation = true;
	} else
	{
		buildParams.brokenVarianceCalculation = false;
	}
	
	//int numberOfParameters = params.getParameterNames().size();
	//int numberOfFeatures = features.getDataRow(0).length;
	
	/**
	 * THis needs to be the length of the number of parameters in a configuration + the number of features in a configuration
	 */
	
	
	buildParams.catDomainSizes = new int[categoricalSize.length+ numberOfFeatures];
	System.arraycopy(categoricalSize, 0, buildParams.catDomainSizes, 0, categoricalSize.length);
	
	
	if(rfOptions.ignoreConditionality)
	{
		//TODO: Make this a ModelDataSanitizer
		buildParams.nameConditionsMapOp = null;
		buildParams.nameConditionsMapParentsArray = null;
		buildParams.nameConditionsMapParentsValues = null;
	}
	else {
		buildParams.nameConditionsMapOp = new HashMap<Integer, int[][]>(nameConditionsMapOp);
		buildParams.nameConditionsMapParentsArray = new HashMap<Integer, int[][]>(nameConditionsMapParentsArray);
		buildParams.nameConditionsMapParentsValues = new HashMap<Integer, double[][][]>(nameConditionsMapParentsValues);
	}

	return buildParams;	
	}
}

package ca.ubc.cs.beta.aeatk.model.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.math.distribution.TruncatedNormalDistribution;
import ca.ubc.cs.beta.aeatk.misc.model.SMACRandomForestHelper;
import ca.ubc.cs.beta.aeatk.misc.watch.AutoStartStopWatch;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.model.data.SanitizedModelData;
import ca.ubc.cs.beta.aeatk.options.RandomForestOptions;
import ca.ubc.cs.beta.models.fastrf.RandomForest;
import ca.ubc.cs.beta.models.fastrf.RegtreeBuildParams;

/**
 * Builds a model with Capped Data using the algorithm defined in:
 * 
 * Bayesian Optimization With Censored Response Data 
 * Frank Hutter, Holger Hoos, and Kevin Leyton-Brown
 * 2011
 * (http://www.cs.ubc.ca/labs/beta/Projects/SMAC/papers/11-NIPS-workshop-BO-with-censoring.pdf)
 * 
 * @author sjr
 *
 */
public class AdaptiveCappingModelBuilder implements ModelBuilder{

	
	protected final RandomForest forest;
	protected final RandomForest preprocessedForest;
	
	private static final Logger log = LoggerFactory.getLogger(AdaptiveCappingModelBuilder.class);
	
	/**
	 * Builds an adaptive capping model builder
	 *  
	 * @param mds 					    sanitized model data to build model from
	 * @param rfOptions				    options object that controls the model
	 * @param rand						random object
	 * @param imputationIterations		number of imputation iterations
	 * @param cutoffTime				cutoffTime 
	 * @param penaltyFactor 		    penalty factor for capped runs
	 */
	public AdaptiveCappingModelBuilder(SanitizedModelData mds, RandomForestOptions rfOptions, Random rand, int imputationIterations, double cutoffTime, double penaltyFactor)
	{
		this(mds, rfOptions,rand,imputationIterations,cutoffTime, penaltyFactor,1);
	}

	/**
	 * Builds an adaptive capping model builder
	 *  
	 * @param mds 					    sanitized model data to build model from
	 * @param rfOptions				    options object that controls the model
	 * @param rand						random object
	 * @param imputationIterations		number of imputation iterations
	 * @param cutoffTime				cutoffTime 
	 * @param penaltyFactor 		    penalty factor for capped runs
	 * @param subsamplePercentage       percentageOfPoints to subsample
	 */
	public AdaptiveCappingModelBuilder(SanitizedModelData mds, RandomForestOptions rfOptions, Random rand, int imputationIterations, double cutoffTime, double penaltyFactor, double subsamplePercentage)
	{
		
		double maxPenalizedValue = mds.transformResponseValue(cutoffTime*penaltyFactor);
		double transformedCutoffTime = mds.transformResponseValue(cutoffTime*penaltyFactor);
		int[][] theta_inst_idxs = mds.getThetaInstIdxs();
		boolean[] censoringIndicators = mds.getCensoredResponses();
		
		
		/**
		 * General Algorithm is as follows
		 * 
		 * 1) Build a tree with non censored data
		 * 
		 *    while(numIterations < imputedLimit && mean value of all imputed values increases by epsilon)
		 *    {
		 *       foreach(tree)
		 *       {
		 *       	Subsample the data points.
		 *       	Build the response vector for the data points used.
		 *       	For each censored value in the response vector, sample from the last built tree.
		 *    	 }
		 *    
		 *    }
		 *    
		 */
		/*
		if(log.isTraceEnabled())
		{
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter(sWriter);
			
			for(int i=0; i < censoringIndicators.length; i++)
			{
				pWriter.format("%4d : %8s  %b  %8f %n", i, Arrays.toString(theta_inst_idxs[i]), censoringIndicators[i], mds.getResponseValues()[i]);
			}
			log.trace("Adaptive Capping Inputs:\n {} "+ sWriter.toString());
		}
		*/
		//=== Get predictors, response values, and censoring indicators from RunHistory.
		
		//=== Change to 0-based indexing
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			theta_inst_idxs[i][0]--;
			theta_inst_idxs[i][1]--;
		}
		
		double[] responseValues = mds.getResponseValues();
	

		//=== Initialize subsets corresponding to censored/noncensored values.
		ArrayList<int[]> censoredThetaInst = new ArrayList<int[]>(responseValues.length);
		ArrayList<int[]> nonCensoredThetaInst = new ArrayList<int[]>(responseValues.length);
		ArrayList<Double> nonCensoredResponses = new ArrayList<Double>(responseValues.length);

		//=== Break into censored and noncensored.
		int censoredCount = 0;
		for(int i=0; i < responseValues.length; i++)
		{
			if(!censoringIndicators[i])
			{
				nonCensoredThetaInst.add(theta_inst_idxs[i]);
				nonCensoredResponses.add(responseValues[i]);
			} else
			{
				censoredThetaInst.add(theta_inst_idxs[i]);
				censoredCount++;
			}
		}
		int[][] non_cens_theta_inst_idxs = nonCensoredThetaInst.toArray(new int[0][]);
		double[] non_cens_responses = convertToPrimitiveArray(nonCensoredResponses.toArray(new Double[0]));
		
		log.debug("Building Random Forest with {} censored runs out of {} total ", censoredCount, censoringIndicators.length);
		
		//=== Building random forest with non censored data.
		RandomForest rf = buildRandomForest(mds,rfOptions,non_cens_theta_inst_idxs, non_cens_responses, false, subsamplePercentage, rand);
		
	
		int numTrees = rfOptions.numTrees;
		
		int numDataPointsInTree = responseValues.length;

		
		if(subsamplePercentage < 1)
		{
			
			numDataPointsInTree *= subsamplePercentage;
			Object[] args = { numDataPointsInTree, responseValues.length, subsamplePercentage};
			log.debug("Subsampling number in points in imputed trees to {} out of {} ({} %)",args);
		}
		
		//=== Initialize map from censored response indices to Map from trees to their dataIdxs for that response (only for trees that actually have that data point).
		Map<Integer, Map<Integer, List<Integer>>> censoredSampleIdxs = new LinkedHashMap<Integer, Map<Integer, List<Integer>>>();
		for (int i = 0; i < numDataPointsInTree; i++) {
			if(censoringIndicators[i]){
				censoredSampleIdxs.put(i,new HashMap<Integer,List<Integer>>());
			}
		}
		
		//=== Set up dataIdx once and for all (via bootstrap sampling), and keep track of which runs are censored in censoredSampleIdxs.
	    int[][] dataIdxs = new int[numTrees][numDataPointsInTree];
	    
		if(rfOptions.fullTreeBootstrap)
		{
			//==== Use same data points in each tree
			for (int j = 0; j < numTrees; j++) {
		        for (int k = 0; k < numDataPointsInTree; k++) {
	               dataIdxs[j][k] = k;
		        }
		    }
			
		} else
		{
			//==== Random sample data points to use in each tree
		    for (int j = 0; j < numTrees; j++) {
		        for (int k = 0; k < numDataPointsInTree; k++) {
	               dataIdxs[j][k] = rand.nextInt(numDataPointsInTree);
		        }
		    }

		}
		
        //====Initialize mapping of censored indexes (in responseValue) to tree index and dataIdxs.
		for (int j = 0; j < numTrees; j++) {
	        for (int k = 0; k < numDataPointsInTree; k++) {
	        	int dataIndex = dataIdxs[j][k];
               if (censoringIndicators[dataIndex]){
            	   if(censoredSampleIdxs.get(dataIndex).get(j) == null){
                	   censoredSampleIdxs.get(dataIndex).put(j, new ArrayList<Integer>());
            	   }            		   
            	   censoredSampleIdxs.get(dataIndex).get(j).add(k);
               }
	        }
	    }
		
	    
	    /**
		 * While imputed values change more than a limit, continue.
		 */
		
		double[][] yHallucinated = new double[numTrees][numDataPointsInTree];
		
		//=== Initialize yHallucinated to the observed data (for censored data points that's a lower bound).
		for(int tree=0; tree<yHallucinated.length; tree++){
			for (int treeResponseValueIndex = 0; treeResponseValueIndex < yHallucinated[tree].length; treeResponseValueIndex++){
				int responseValueIndex = dataIdxs[tree][treeResponseValueIndex];
				yHallucinated[tree][treeResponseValueIndex] = responseValues[responseValueIndex];
			}
		}
		
	
		
		for(int i=0; i < imputationIterations; i++)
		{
			double differenceFromLastMean = 0;
			if( censoredSampleIdxs.isEmpty() ) break;
			//=== Get predictions for all censored values once and for all in this iteration. 
			int Xlength = mds.getConfigs()[0].length + mds.getPCAFeatures()[0].length;
			double[][] predictors = new double[censoredSampleIdxs.size()][Xlength];
			int j=0;
			//=== Loop over all the censored data points.
			for (Integer sampleIdxToUse: censoredSampleIdxs.keySet()){
				double[] configArray = mds.getConfigs()[theta_inst_idxs[sampleIdxToUse][0]];
				double[] featureArray = mds.getPCAFeatures()[theta_inst_idxs[sampleIdxToUse][1]];
				
				for(int m=0; m < configArray.length; m++)
				{
					predictors[j][m] = configArray[m];
				}
				for(int m=0; m < featureArray.length; m++)
				{
					predictors[j][m+configArray.length] = featureArray[m];
				}
				j++;
			}
			//== Now predict.
			double[][] prediction = RandomForest.apply(rf, predictors);
			
			/*
			 * Test code
			 * if you see it and don't like it feel free to delete it
			 * 
			List<Integer> list = new ArrayList<Integer>(censoredSampleIdxs.keySet());
			double[] lastPrediction = new double[censoredSampleIdxs.size()];
			double overPredictionSum = 0;
			double underPredictionSum = 0; 
			System.out.println("Imputation "+i+":");
			for(int z=0; z < prediction.length; z++)
			{
				
				
				double mean = prediction[z][0];
				double var = prediction[z][1];
				if(mean > lastPrediction[z])
				{
					overPredictionSum += mean - lastPrediction[z];
				} else
				{
					underPredictionSum += lastPrediction[z] - mean;
				}
				
				
			
				System.out.println(z+","+  list.get(z) + "," + responseValues[list.get(z)] +","+prediction[z][0]+"," + prediction[z][1] + "," + lastPrediction[z]);
				lastPrediction[z] = mean;
			}
			
			System.out.println("Over Prediction: " + overPredictionSum + " Under Prediction Sum: " + underPredictionSum);
			*/
			j=0;
			//=== Loop over all the censored data points.
			for (Entry<Integer, Map<Integer, List<Integer>>> ent : censoredSampleIdxs.entrySet()){
				int sampleIdxToUse = ent.getKey();
				
				//=== Collect number of samples we need to take for this point.
				Map<Integer, List<Integer>> treeDataIdxsMap = ent.getValue();
				int numSamplesToGet = 0;
				for (List<Integer> l : treeDataIdxsMap.values()){
					numSamplesToGet += l.size();
				}
				//System.out.println(sampleIdxToUse);
				
				//=== Get the samples (but cap them at maxValue). 
				
				TruncatedNormalDistribution tNorm = new TruncatedNormalDistribution(prediction[j][0], prediction[j][1], responseValues[sampleIdxToUse],rand);
				j++;
				
				double[] samples;
					if(rfOptions.imputeMean)
					{
						samples = new double[numSamplesToGet];
						for(int k=0; k < samples.length; k++)
						{
							samples[k] = prediction[j][0];
						}
					} else if(rfOptions.shuffleImputedValues)
					{
						samples = tNorm.getValuesAtStratifiedShuffledIntervals(numSamplesToGet);
					} else
					{
						samples = tNorm.getValuesAtStratifiedIntervals(numSamplesToGet);
					}
				
				
				for (int k = 0; k < samples.length; k++) 
				{
					
					if(rfOptions.penalizeImputedValues && samples[k] >= transformedCutoffTime)
					{
						samples[k] = maxPenalizedValue;
					}
					samples[k] = Math.min(samples[k], maxPenalizedValue);
				}

				
				//=== Populate the trees at their dataIdxs with the samples (and update differenceFromLastMean)
				int count=0;
				double increaseThisDataPoint = 0;
				for( Entry<Integer, List<Integer>> ent2 : treeDataIdxsMap.entrySet() ){
					int tree = ent2.getKey();
					List<Integer> responseLocationsInTree = ent2.getValue();
					for(int k = 0; k<responseLocationsInTree.size(); k++){
						int responseLocationInTree = responseLocationsInTree.get(k);
						increaseThisDataPoint += (samples[count] - yHallucinated[tree][responseLocationInTree]);
						yHallucinated[tree][responseLocationInTree] = samples[count++];
					}
				}
				if(count != 0)
				{
					differenceFromLastMean += (increaseThisDataPoint / count);
				}
			}
			differenceFromLastMean /= censoredSampleIdxs.size();			
			
			//=== Build a new random forest.
			log.debug("Building random forest with imputed values iteration {}", i);
			rf = buildImputedRandomForest(mds,rfOptions,theta_inst_idxs, dataIdxs, yHallucinated, false, rand);
			
			if(differenceFromLastMean < Math.pow(10,-10) && i >= 1)
			{
				log.trace("Means of imputed values stopped increasing in imputation iteration {} (increase {})",i,differenceFromLastMean);
				break;
			} else
			{
		    	log.trace("Mean increase in imputed values in imputation iteration {} is {}", i, differenceFromLastMean);
	        }
		}
		
		forest = rf;
		
		if(rfOptions.preprocessMarginal)
		{
			log.trace("Preprocessing marginal for Random Forest");
			preprocessedForest = RandomForest.preprocessForest(forest, mds.getPCAFeatures());
		} else
		{
			preprocessedForest = null;
		}
	}
	
	
	@Override
	public RandomForest getRandomForest()
	{
		return forest;
	}
	@Override
	public RandomForest getPreparedRandomForest() {
		return preprocessedForest;
	}
	
	/**
	 * Converts an array of Double[] to a double[] array.
	 * @param arr array of Double[]
	 * @return primitive value array
	 */
	private double[] convertToPrimitiveArray(Double[] arr)
	{
		double[] d = new double[arr.length];
		for(int i=0; i < d.length; i++)
		{
			d[i] = arr[i].doubleValue();
		}
		return d;
	}

//  This isn't used currently, maybe it will be used in the future, you can delete this if you want
//	@SuppressWarnings("unused")
//	/**
//	 * Converts an array of Integer[] to int[] 
//	 * @param arr	array of Integers
//	 * @return		array of int
//	 */
//	private int[] convertToPrimitive(Integer[] arr)
//	{
//		int[] d = new int[arr.length];
//		for(int i=0; i < d.length; i++)
//		{
//			d[i] = arr[i].intValue();
//		}
//		return d;
//	}
	
	/**
	 * Builds a Random forest 
	 * @param mds 				sanitized model data
	 * @param rfOptions			options for building the random forest
	 * @param theta_inst_idxs	array of [thetaIdx, instanceIdx] values
	 * @param responseValues	response values for model
	 * @param preprocessed		<code>true</code> if we should build a model with preprocessed marginals, <code>false</code> otherwise
	 * @return constructed random forest
	 */
	private static RandomForest buildRandomForest(SanitizedModelData mds, RandomForestOptions rfOptions, int[][] theta_inst_idxs, double[] responseValues, boolean preprocessed, double subsamplePercentage, Random rand)
	{
		
		double[][] features = mds.getPCAFeatures();
		
		
		double[][] configs = mds.getConfigs();
		
		int[] categoricalSize = mds.getCategoricalSize();
		Map<Integer, int[][]> nameConditionsMapParentsArray = mds.getNameConditionsMapParentsArray();
		Map<Integer, double[][][]> nameConditionsMapParentsValues = mds.getNameConditionsMapParentsValues();
		Map<Integer, int[][]> nameConditionsMapOp = mds.getNameConditionsMapOp();
		
		/*
		System.out.println("y = \n" + Arrays.toString(responseValues));
		System.out.println("categoricalSize = \n" + Arrays.toString(categoricalSize));
		System.out.println("parent_param_idxs = \n" + Arrays.deepToString(condParents));
		*/
		
		int numTrees = rfOptions.numTrees;
		
		
/*		
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			theta_inst_idxs[i][0]--;
			theta_inst_idxs[i][1]--;
		}
*/
		RegtreeBuildParams buildParams = SMACRandomForestHelper.getRandomForestBuildParams(rfOptions, features[0].length, categoricalSize, nameConditionsMapParentsArray, nameConditionsMapParentsValues, nameConditionsMapOp, rand);
		
		
		RandomForest forest;
		
		log.trace("Building Random Forest with {} data points ", responseValues.length);
		StopWatch sw = new StopWatch();
		if(rfOptions.fullTreeBootstrap)
		{
			
			 int N = responseValues.length;
			 int[][] dataIdxs = new int[numTrees][N];
		        for (int i = 0; i < numTrees; i++) {
		            for (int j = 0; j < N; j++) {
		                dataIdxs[i][j] = j;
		            }
		        }
		        
		        sw.start();
		      forest = RandomForest.learnModel(numTrees, configs, features, theta_inst_idxs, responseValues, dataIdxs, buildParams);
		      
		} else if(subsamplePercentage < 1)
		{
				int N = (int) (subsamplePercentage * responseValues.length);
				log.debug("Subsampling {} points out of {} total for random forest construction", N, responseValues.length);
				int[][] dataIdxs = new int[numTrees][N];
		        for (int i = 0; i < numTrees; i++) {
		            for (int j = 0; j < N; j++) {
		                dataIdxs[i][j] = buildParams.random.nextInt(N);
		            }
		        }   
		        
		        sw.start();
				forest = RandomForest.learnModel(numTrees, configs, features, theta_inst_idxs, responseValues, dataIdxs, buildParams);
			
		} else
		{
			  sw.start();
			  forest = RandomForest.learnModel(numTrees, configs, features, theta_inst_idxs, responseValues, buildParams);
		}
		
		log.debug("Building Random Forest took {} seconds ", sw.stop() / 1000.0);

		
		return forest;
	}
	
	/**
	 * 
	 * @param mds 				sanitized model data
	 * @param rfOptions			options for building the random forest
	 * @param dataIdxs			array of values of theta_inst_idxs to use when building each tree. [For instance if we are building 10 trees, the size will be int[10][N] where N is the number of entries to build with]
	 * @param theta_inst_idxs	array of [thetaIdx, instanceIdx] values
	 * @param responseValues	array of values of the response to use when building each tree. [ For instance if we are building 10 trees, the size will be double[10][N] where N is the number of entries to build with]
	 * @param preprocessed		<code>true</code> if we should build a model with preprocessed marginals, <code>false</code> otherwise
	 * @return
	 */
	private static RandomForest buildImputedRandomForest(SanitizedModelData mds, RandomForestOptions rfOptions, int[][] theta_inst_idxs,int[][] dataIdxs, double[][] responseValues, boolean preprocessed, Random rand)
	{
		
		double[][] features = mds.getPCAFeatures();
		
		
		double[][] configs = mds.getConfigs();
		
		int[] categoricalSize = mds.getCategoricalSize();
		Map<Integer, int[][]> nameConditionsMapParentsArray = mds.getNameConditionsMapParentsArray();
		Map<Integer, double[][][]> nameConditionsMapParentsValues = mds.getNameConditionsMapParentsValues();
		Map<Integer, int[][]> nameConditionsMapOp = mds.getNameConditionsMapOp();
		
		/*
		System.out.println("y = \n" + Arrays.toString(responseValues));
		System.out.println("categoricalSize = \n" + Arrays.toString(categoricalSize));
		System.out.println("parent_param_idxs = \n" + Arrays.deepToString(condParents));
		*/
		
		int numTrees = rfOptions.numTrees;
		

/*		
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			theta_inst_idxs[i][0]--;
			theta_inst_idxs[i][1]--;
		}
*/		
		RegtreeBuildParams buildParams = SMACRandomForestHelper.getRandomForestBuildParams(rfOptions, features[0].length, categoricalSize, nameConditionsMapParentsArray, nameConditionsMapParentsValues, nameConditionsMapOp, rand);
		
	
		log.trace("Building Random Forest with {} data points ", responseValues[0].length);
		
		RandomForest forest;
		
		StopWatch sw = new AutoStartStopWatch();
		
		/*
		if(log.isTraceEnabled())
		{
			StringWriter sWriter = new StringWriter();
			PrintWriter pWriter = new PrintWriter(sWriter);
			
			for(int tree=0; tree < rfOptions.numTrees; tree++)
			{
				
				pWriter.println("==== Theta Matrix ====");
				for(int i=0; i < configs.length; i++)
				{
					pWriter.format("%4d : %s %n", i, Arrays.toString(configs[i]));
				}
				pWriter.println("==== Features Matrix ====");
				for(int i=0; i < features.length; i++)
				{
					pWriter.format("%4d : %s %n", i, Arrays.toString(features[i]));
				}
				
				pWriter.println("==== Trees =====");
			
				pWriter.format("Tree %4d :", tree);
				
				for(int value = 0; value < responseValues[tree].length; value++)
				{
					if(value % 10  == 0)
					{
						pWriter.format("%n %4d:", value);
					}
					pWriter.format( " %8f(%8s) ", responseValues[tree][value], dataIdxs[tree][value]);
					
				}
				pWriter.append("\n");
				//pWriter.format("%8f" , forest.Trees[tree].nodevar[0])				
			}
			log.trace("Model Input Values:\n{} ", sWriter.toString());
		}
		*/
		
		forest = RandomForest.learnModelImputedValues(numTrees, configs, features, theta_inst_idxs, responseValues, dataIdxs, buildParams);
		
		log.trace("Building Random Forest took {} seconds ", sw.stop() / 1000.0);

		
		return forest;
	}
	
	
	
	
	
}

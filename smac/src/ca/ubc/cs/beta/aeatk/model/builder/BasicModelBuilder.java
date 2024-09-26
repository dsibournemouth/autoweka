package ca.ubc.cs.beta.aeatk.model.builder;

import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.model.SMACRandomForestHelper;
import ca.ubc.cs.beta.aeatk.misc.watch.StopWatch;
import ca.ubc.cs.beta.aeatk.model.data.SanitizedModelData;
import ca.ubc.cs.beta.aeatk.options.RandomForestOptions;
import ca.ubc.cs.beta.models.fastrf.RandomForest;
import ca.ubc.cs.beta.models.fastrf.RegtreeBuildParams;

/**
 * Formats the input arguments and begins building the actual model.
 * 
 * 
 * @author sjr
 *
 */
public class BasicModelBuilder implements ModelBuilder{

	
	protected final RandomForest forest;
	protected final RandomForest preprocessedForest;
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	
	public BasicModelBuilder(SanitizedModelData smd, RandomForestOptions rfConfig, Random rand)
	{
		this(smd, rfConfig, 1, rand);
	}
	public BasicModelBuilder(SanitizedModelData smd, RandomForestOptions rfConfig, double subsamplePercentage, Random rand)
	{
		
		double[][] features = smd.getPCAFeatures();
		
		
		double[][] configs = smd.getConfigs();
		double[] responseValues = smd.getResponseValues();
		int[] categoricalSize = smd.getCategoricalSize();
		Map<Integer, int[][]> nameConditionsMapParentsArray = smd.getNameConditionsMapParentsArray();
		Map<Integer, double[][][]> nameConditionsMapParentsValues = smd.getNameConditionsMapParentsValues();
		Map<Integer, int[][]> nameConditionsMapOp = smd.getNameConditionsMapOp();
		
		/*
		System.out.println("y = \n" + Arrays.toString(responseValues));
		System.out.println("categoricalSize = \n" + Arrays.toString(categoricalSize));
		System.out.println("parent_param_idxs = \n" + Arrays.deepToString(condParents));
		*/
		
		int numTrees = rfConfig.numTrees;
		
		/**
		 * N x 2 array of response values
		 */
		
	
		int[][] theta_inst_idxs = smd.getThetaInstIdxs();
		
		
		for(int i=0; i < theta_inst_idxs.length; i++)
		{
			theta_inst_idxs[i][0]--;
			theta_inst_idxs[i][1]--;
		}
		RegtreeBuildParams buildParams = SMACRandomForestHelper.getRandomForestBuildParams(rfConfig, features[0].length, categoricalSize, nameConditionsMapParentsArray, nameConditionsMapParentsValues, nameConditionsMapOp, rand);
		
		
		
	
		
		log.trace("Building Random Forest with {} data points ", responseValues.length);
		/*
		if(log.isTraceEnabled())
		{
			log.trace("Building Random Forest with Parameters: {}", buildParams);
			StringWriter sWriter = new StringWriter();
			PrintWriter out = new PrintWriter(sWriter);
			
			
			out.println("==== Theta Inst & Response Values ====");
			for(int i=0; i < responseValues.length; i++)
			{
				out.format("%4d : %8s  %8f %n", i, Arrays.toString(theta_inst_idxs[i]), smd.getResponseValues()[i]);
			}
			 
			out.println("==== Theta Matrix ====");
			for(int i=0; i < configs.length; i++)
			{
				out.format("%4d : %s %n", i, Arrays.toString(configs[i]));
			}
			out.println("==== Features Matrix ====");
			for(int i=0; i < features.length; i++)
			{
				out.format("%4d : %s %n", i, Arrays.toString(features[i]));
			}
			
			log.trace("Build  Information \n {}", sWriter.toString());
		}	
		*/
		
		StopWatch sw = new StopWatch();
		if(rfConfig.fullTreeBootstrap)
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
				log.trace("Subsampling {} points out of {} total", N, responseValues.length);
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

		
		if(rfConfig.preprocessMarginal)
		{
			log.trace("Preprocessing marginal for Random Forest");
			preprocessedForest = RandomForest.preprocessForest(forest, features);
			//RandomForest.save(preprocessedForest);

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
	
	
}

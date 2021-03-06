
package autoweka.instancegenerators;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import autoweka.InstanceGenerator;
import autoweka.Util;

import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;

import dps.DPS;

// In order to work, PATH has to include C:\Program Files\MATLAB\MATLAB Runtime\v85\runtime\win64\
/**
 * DPS Correntropy based, hierarchical density preserving data split
 * 
 * R = DPS(A,LEVELS,LABS)
 * 
 * [R H] = DPS(A,LEVELS,LABS)
 * 
 * INPUT
 * 
 * A Input data (rows = observations)
 * 
 * LEVELS Number of split levels, default: 3
 * 
 * LABS Labels for the data (optional, if no labels are given unsupervised split
 * is performed)
 * 
 * OUTPUT
 * 
 * R Index array with rotation set with 2^LEVELS folds
 * 
 * H Hierarchy of splits
 * 
 * DESCRIPTION
 * 
 * Density Preserving Sampling (DPS) divides the input dataset into a given
 * number of folds (2^LEVELS) by maximizing the correntropy between the folds
 * and can be used as an alternative for cross-validation. The procedure is
 * deterministic, so unlike cross-validation it does not need to be repeated.
 * 
 * REFERENCE
 * 
 * Budka, M. and Gabrys, B., 2012. Density Preserving Sampling: Robust and
 * Efficient Alternative to Cross-validation for Error Estimation. IEEE
 * Transactions on Neural Networks and Learning Systems, DOI:
 * 10.1109/TNNLS.2012.2222925.
 */
public class DensityPreservingSampling
  extends InstanceGenerator {

  private Instances[] folds = null;

  private boolean cached = false;

  public DensityPreservingSampling(InstanceGenerator generator) {
    super(generator);
  }

  public DensityPreservingSampling(String instanceFileName) {
    super(instanceFileName);
  }

  public DensityPreservingSampling(Instances training, Instances testing) {
    super(training, testing);
  }

  public Instances _getTrainingFromParams(String paramString) {
    return getInstances(true, getTraining(),
	Util.parsePropertyString(paramString));
  }

  public Instances _getTestingFromParams(String paramString) {
    return getInstances(false, getTraining(),
	Util.parsePropertyString(paramString));
  }

  protected Instances getInstances(boolean trainingFold, Instances instances,
      Properties params) {
    // int seed = Integer.parseInt(params.getProperty("seed", "0"));
    int numLevels = Integer.parseInt(params.getProperty("numLevels", "-1"));
    int numFolds = (int) Math.pow(2, numLevels);
    int currentFold = Integer.parseInt(params.getProperty("fold", "-1"));

    if (numLevels <= 0)
      throw new RuntimeException("numLevels must be set to something > 0");

    if (currentFold < 0 || currentFold > numFolds)
      throw new RuntimeException("fold must be set to something in [1,"
	  + numFolds + "]");

    if (folds == null)
      folds = new Instances[numFolds];

    // Check if data is cached in a file
    if (!cached) {
      try {
	for (int i = 0; i < numFolds; i++) {
	  if (folds[i] != null)
	    continue;

	  folds[i] = Util.loadDataSource(new FileInputStream(trainArff
	      + ".dps." + i));
	  if (folds[i].classIndex() == -1) {
	    if (classIndex.equals("last"))
	      folds[i].setClassIndex(folds[i].numAttributes() - 1);
	    else
	      folds[i].setClassIndex(Integer.parseInt(classIndex));
	  }
	}

	cached = true;
      }
      catch (Exception e) {
	// If file doesn't exist
	cached = false;
      }
    }

    Instances outputData = getTraining();
    outputData.delete();

    // If cached, return the stored folds
    if (cached) {
      if (trainingFold) {
	// Merge all folds but the selected one
	for (int i = 0; i < numFolds; i++) {
	  if (i != currentFold) {
	    outputData.addAll(folds[i]);
	    // outputData = Instances.mergeInstances(outputData, folds[i]);
	  }
	}
	return outputData;
      }
      else {
	return folds[currentFold];
      }
    }

    // If not cached, get DPS folds

    // Random rand = new Random(seed);
    Instances randData = getTraining();
    // randData.randomize(rand);

    double data[][] = null;

    // TODO Improve the DPS call to get all the folds at the same time instead
    // of only one
    try {
      DPS d = new DPS();
      Object[] output = new Object[1]; // data
      Object[] input = new Object[4]; // A, LEVELS, FOLD, TRANSPOSE

      // Transform the Instances to a double matrix
      double matrix[][] = new double[randData.numAttributes()][randData
	  .numInstances()];
      for (int i = 0; i < randData.numAttributes(); i++) {
	matrix[i] = randData.attributeToDoubleArray(i);
      }

      // Set DPS parameters
      input[0] = matrix; // input matrix
      input[1] = numLevels; // levels (so num_folds = 2^levels)
      // matlab indices start from 1 !!!!
      if (trainingFold)
	input[2] = -(currentFold + 1); // all but this one
      else
	input[2] = currentFold + 1;
      input[3] = 1; // transpose the matrix

      // Run DPS to get the actual fold
      d.get_dps_folds(output, input);

      // Transform from matlab matrix to double matrix
      data = (double[][]) ((MWNumericArray) output[0]).toDoubleArray();

      // Transform the instances back from double to DenseInstance
      for (int i = 0; i < data.length; i++) {
	outputData.add(new DenseInstance(1.0, data[i]));
      }

    }
    catch (MWException e) {
      e.printStackTrace();
      outputData = null;
    }

    // Save data into file for caching
    if (!trainingFold && outputData != null) {
      folds[currentFold] = outputData;
      ArffSaver saver = new ArffSaver();
      saver.setInstances(folds[currentFold]);
      try {
	saver.setFile(new File(trainArff + ".dps." + currentFold));
	saver.writeBatch();
      }
      catch (IOException e) {
	// Cannot save the file for some reason
	e.printStackTrace();
      }

    }

    return outputData;

  }

  public List<String> getAllInstanceStrings(String paramStr) {
    Properties params = Util.parsePropertyString(paramStr);

    int seed, numLevels;
    try {
      seed = Integer.parseInt(params.getProperty("seed", "0"));
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to parse seed", e);
    }
    try {
      numLevels = Integer.parseInt(params.getProperty("numLevels", "-1"));
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to parse numFolds", e);
    }

    if (numLevels <= 0)
      throw new RuntimeException("numLevels must be set to something > 0");

    int numFolds = (int) Math.pow(2, numLevels);
    List<String> instanceStrings = new ArrayList<String>(numFolds);
    for (int i = 0; i < numFolds; i++) {
      // Should probably change this to using a Properties object, but meh
      instanceStrings.add("seed=" + seed + ":numLevels=" + numLevels + ":fold="
	  + i);
    }
    return instanceStrings;
  }
  /*
   * public static void main(String []args){ // To generate DPS folds and save
   * them String datasetName = args[0]; String props =
   * "testArff=C__COLONESCAPE__:\\Users\\Manuel\\workspace-autoweka\\autoweka\\datasets\\"
   * +datasetName+
   * "-test30perc.arff:type=trainTestArff:trainArff=C__COLONESCAPE__:\\Users\\Manuel\\workspace-autoweka\\autoweka\\datasets\\"
   * +datasetName+"-train70perc.arff"; DensityPreservingSampling dps = new
   * DensityPreservingSampling(props); int numLevels = 3; int numFolds =
   * (int)Math.pow(2, numLevels); Properties params = new Properties();
   * params.setProperty("numLevels", Integer.toString(numLevels));
   * 
   * for(int i=0; i<numFolds; i++){ params.setProperty("fold",
   * Integer.toString(i)); dps.getInstances(false, null, params); }
   * 
   * }
   */
}


package autoweka.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import autoweka.InstanceGenerator;

public class GetBestFromTrajectoryGroupCSV
  extends GetBestFromTrajectoryGroup {

  protected boolean isRegression = true;

  public GetBestFromTrajectoryGroupCSV(String trajGroupFileName) {
    super(trajGroupFileName);
  }

  /**
   * Point this main method at a .trajectory file, and be presented with what
   * you should actually run your dataset on
   */
  public static void main(String[] args) {
    GetBestFromTrajectoryGroupCSV res = new GetBestFromTrajectoryGroupCSV(
	args[0]);

    res.isRegression = res.experiment.resultMetric.equals("rmse") ? true
	: false;

    InstanceGenerator mInstanceGenerator = InstanceGenerator.create(res.experiment.instanceGenerator, res.experiment.datasetString);
    int sizeTestingSet = mInstanceGenerator.getTesting().size();
    
    File trajectoryFile = new File(args[0]);
    String predictionsFilename = trajectoryFile.getParentFile()
	.getAbsolutePath()
	+ trajectoryFile.separator
	+ "predictions."
	+ res.seed + ".csv";
    

    ArrayList<Double> errors = loadErrors(predictionsFilename, res.isRegression);

    double testError = -1;
    if (res.isRegression) {
      testError = rmse(errors);
    }
    else {
      testError = misclassificationRate(errors, sizeTestingSet);
    }

    // Output:
    // Experiment name,
    // Seed of best configuration,
    // Number of trajectories,
    // Number of evaluations (for the best seed),
    // Number of evaluations (in total),
    // Number of MemOut evaluations
    // Number of TimeOut evaluations
    // CV Error (for the best configuration),
    // Test Error (for the best configuration)
    // Best configuration

    System.out.println(res.experiment.name + "," + res.seed + ","
	+ res.numTrajectories + "," + res.numEval + "," + res.totalNumEval
	+ "," + res.numMemOut + "," + res.numTimeOut + "," + res.errorEstimate
	+ "," + testError + "," + res.classifierClass + " "
	+ res.classifierArgs);
  }

  private static ArrayList<Double> loadErrors(String filename,
      boolean isRegression) {
    BufferedReader br = null;
    String line = "";
    String cvsSplitBy = ",";

    ArrayList<Double> error = new ArrayList<Double>();
    try {

      br = new BufferedReader(new FileReader(filename));
      line = br.readLine(); // ignore header
      while ((line = br.readLine()) != null) {

	// use comma as separator
	String[] prediction = line.split(cvsSplitBy);
	
	if (prediction.length < 3)
	  continue;

	if (isRegression) {
	  try {
	    error.add(Double.parseDouble(prediction[3]));
	  }
	  catch (NumberFormatException e) {
	    error.add(Double.NaN);
	  }
	}
	else {
	  error.add(prediction[3].equals("") && !prediction[2].equals("?") ? 0.0 : 1.0);
	}

      }

    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      if (br != null) {
	try {
	  br.close();
	}
	catch (IOException e) {
	  e.printStackTrace();
	}
      }
    }
    return error;
  }

  private static double rmse(ArrayList<Double> error) {
    double rmse = 0;
    int numMissing = 0;
    for (int i = 0; i < error.size(); i++) {
      if (Double.isNaN(error.get(i)))
	numMissing++;
      else
	rmse += error.get(i) * error.get(i);
    }
    rmse = Math.sqrt(rmse / (error.size() - numMissing));
    return rmse;
  }

  private static double misclassificationRate(ArrayList<Double> error, int sizeTestingSet) {
    if (error.size() > sizeTestingSet){
      return -1; //something went wrong
    }
    
    double rate = 0;
    for (Double e: error) {
      rate += e;
    }
    
    // consider missing predictions as error
    rate += sizeTestingSet - error.size();
    
    return 100 * rate / sizeTestingSet; // return percentage
  }

}

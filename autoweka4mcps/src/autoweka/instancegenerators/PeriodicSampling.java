
package autoweka.instancegenerators;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import weka.core.Instances;
import autoweka.InstanceGenerator;
import autoweka.Util;

public class PeriodicSampling
  extends InstanceGenerator {

  public PeriodicSampling(InstanceGenerator generator) {
    super(generator);
  }

  public PeriodicSampling(String instanceFileName) {
    super(instanceFileName);
  }

  public PeriodicSampling(Instances training, Instances testing) {
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
    int numFolds = Integer.parseInt(params.getProperty("numFolds", "-1"));
    int currentFold = Integer.parseInt(params.getProperty("fold", "-1"));

    if (numFolds <= 0)
      throw new RuntimeException("numFolds must be set to something > 0");

    if (currentFold < 0 || numFolds <= currentFold)
      throw new RuntimeException("fold must be set to something in [0,"
	  + numFolds + "]");

    currentFold++; // force folds between [1, numFolds]

    Instances data = getTraining();

    Instances output = new Instances(data); // to keep header
    output.delete();

    int numInstances = data.size();

    // Training set
    if (trainingFold) {
      for (int i = 1; i <= numInstances; i++) {
	if (i % currentFold != 0) {
	  output.add(data.get(i - 1));
	}
      }
    }
    // Testing set
    else {
      for (int i = currentFold - 1; i < numInstances; i += numFolds) {
	output.add(data.get(i));
      }
    }

    return output;
  }

  public List<String> getAllInstanceStrings(String paramStr) {
    Properties params = Util.parsePropertyString(paramStr);

    int seed, numFolds;
    try {
      seed = Integer.parseInt(params.getProperty("seed", "0"));
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to parse seed", e);
    }
    try {
      numFolds = Integer.parseInt(params.getProperty("numFolds", "-1"));
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to parse numFolds", e);
    }

    if (numFolds <= 0)
      throw new RuntimeException("numFolds must be set to something > 0");

    List<String> instanceStrings = new ArrayList<String>(numFolds);
    for (int i = 0; i < numFolds; i++) {
      // Should probably change this to using a Properties object, but meh
      instanceStrings.add("seed=" + seed + ":numFolds=" + numFolds + ":fold="
	  + i);
    }
    return instanceStrings;
  }
}

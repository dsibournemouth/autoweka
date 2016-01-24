
package autoweka.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import autoweka.Experiment;
import autoweka.TrajectoryParser;
import autoweka.TrajectoryPointPredictionRunner;

/**
 * Utility class that just combines running an experiment, extracting a
 * trajectory, then saving the model of the incumbent
 */
class AdaptiveExperimentRunner {

  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.out
	  .println("AdaptiveExperimentRunner requires at least 3 arguments - the experiment folder, the number of batches and the seed (and optionally skip-run option)");
      System.exit(1);
    }

    File expFolderFile = new File(args[0]);
    int numberOfBatches = Integer.parseInt(args[1]);
    String seed = args[2];
    boolean skipRun = false;
    if (args.length > 3)
      skipRun = args[3].equals("skip-run") ? true : false;

    if (!expFolderFile.exists() || !expFolderFile.isDirectory()) {
      System.out
	  .println("The first argument does not appear to be an experiment folder");
      System.exit(1);
    }
    String expFolder = expFolderFile.getAbsolutePath();
    String expName = expFolderFile.getName();

    System.out.println(expFolder + " " + expName);

    for (int batchNumber = 1; batchNumber < numberOfBatches; batchNumber++) {

      // Move the previous trajectory, model and predictions
      String[] oldFiles = new String[] {expName + ".trajectories." + seed,
	  "trained." + seed + ".model",
	  "trained." + seed + ".attributeselection",
	  "predictions." + seed + ".csv"};

      File batchFolder = new File(expFolder + File.separator + "batch"
	  + (batchNumber - 1));
      if (!batchFolder.exists())
	batchFolder.mkdirs();

      for (String path: oldFiles) {
	File tmpFile = new File(expFolder + File.separator + path);
	if (tmpFile.exists()) {
	  Files.move(tmpFile.toPath(), (new File(batchFolder, path).toPath()));
	}
      }

      // Run the experiment
      if (!skipRun) {
	String[] expArgs = new String[] {"-noexit", "-batchNumber",
	    Integer.toString(batchNumber), expFolder, seed};
	Experiment.main(expArgs);
      }

      // Extract the trajectory
      String[] trajParseArgs = new String[] {"-single", expFolder, seed,
	  "-batchNumber", Integer.toString(batchNumber)};
      TrajectoryParser.main(trajParseArgs);

      // And get some predictions/train the model
      String[] runnerArgs = new String[] {
	  expFolder + File.separator + expName + ".trajectories." + seed,
	  "-savemodel"};
      TrajectoryPointPredictionRunner.main(runnerArgs);
    }
  }
};

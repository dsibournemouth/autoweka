/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.tools;

import autoweka.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author ntdun
 */
public class ExpRunnerService {

    public void runExp(String experimentFolder, String allocatedMemory, String seedNumber) {


            File file = new File(experimentFolder);
            String experimentsRootFolder = FilenameUtils.getFullPathNoEndSeparator(file.getAbsolutePath());

            File experimentsDirectory = new File(experimentsRootFolder);
            String applicationFolder = FilenameUtils.getFullPathNoEndSeparator(experimentsDirectory.getAbsolutePath());

            Util.makePath(experimentFolder + File.separator + "out" + File.separator + "logs");
            Util.makePath(experimentFolder + File.separator + "out" + File.separator + "runstamps");
            File logFile = new File(experimentFolder + File.separator + "out" + File.separator + "logs" + File.separator + seedNumber + ".log");
            File stampFile = new File(experimentFolder + File.separator + "out" + File.separator + "runstamps" + File.separator + seedNumber + ".stamp");
            try {
                stampFile.createNewFile();
                logFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ExpRunnerService.class.getName()).log(Level.SEVERE, null, ex);
            }
            //stampFile.deleteOnExit();

            String runCommand = "java -Xmx" + allocatedMemory + "m -cp \"" + applicationFolder + "\\smac-v2.10.03-master-778\\lib\\smac.jar;" + applicationFolder + "\\smac-v2.10.03-master-778\\lib\\*\" ca.ubc.cs.beta.aeatk.ant.execscript.Launcher ca.ubc.cs.beta.smac.executors.SMACExecutor "
                    + "--seed " + seedNumber + " --scenarioFile autoweka.scenario --logModel false --adaptiveCapping false --runGroupName autoweka --terminate-on-delete " + experimentFolder + "\\out\\runstamps\\0.stamp --kill-runs-on-file-delete " + experimentFolder + "\\out\\runstamps\\0.stamp"
                    + " --initialIncumbent RANDOM --initialN 1 --acq-func EI --executionMode SMAC --cli-log-all-call-strings true --clean-old-state-on-success true --abort-on-first-run-crash false --number-init-configs 1 ";

            System.out.println(runCommand);

      

    }

}

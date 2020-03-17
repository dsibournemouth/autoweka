/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoweka.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.Document;

/**
 *
 * @author ntdun
 */
public class CommandExperimentRun {

    private String experimentFolder;
//    
//    private String mCurrentExperimentFolder;
    private String mCurrentSeed;
    private Process mProc;
    private Observer mObserver;
    
    private boolean isCompleted = false;

    public CommandExperimentRun() {

    }

    public Process getmProc() {
        return mProc;
    }

    public void setmProc(Process mProc) {
        this.mProc = mProc;
    }

    public boolean isIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
    
    

    public CommandExperimentRun(String experimentFolder, String mCurrentSeed) {
        this.experimentFolder = experimentFolder;
        this.mCurrentSeed = mCurrentSeed;
    }

    public String getmCurrentSeed() {
        return mCurrentSeed;
    }

    public void setmCurrentSeed(String mCurrentSeed) {
        this.mCurrentSeed = mCurrentSeed;
    }

    public String getExperimentFolder() {
        return experimentFolder;
    }

    public void setExperimentFolder(String experimentFolder) {
        this.experimentFolder = experimentFolder;
    }

    public void runExp() {

        try {
            //Check to make sure that an experiment file exists in there
            if (experimentFolder.isEmpty()) {
                throw new RuntimeException("No Experiment Folder has been set");
            }
            File expFolder = new File(experimentFolder);
            File exp = new File(expFolder.getAbsolutePath() + File.separator + expFolder.getName() + ".experiment");
            if (!exp.exists() || !exp.isFile()) {
                throw new RuntimeException(exp.getAbsolutePath() + " does not appear to be a valid experiment");
            }
            String seedText = mCurrentSeed;
            if (seedText.isEmpty()) {
                throw new RuntimeException("No seed defined");
            }

//            File seedLog = new File(expFolder + File.separator + "out" + File.separator + "logs" + File.separator + seedText + ".log");            
//            if(seedLog.exists())
//            {
//                if(JOptionPane.showConfirmDialog(this, "Warning: The experiment " + expFolder.getName() + " with seed " + seedText + " has already been run. Do you wish to run it again?", "Experiment Run Exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION){
//                    return;
//                }
//            }
            //Store these for the stopButton being pressed....
//            mCurrentExperimentFolder = expFolder.getAbsolutePath();
            //mCurrentSeed = mSeedText.getText();
            //We passed all the checks, actually run it
           new Thread(new Runnable() {
                public void run() {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(autoweka.Util.getJavaExecutable(), "-Xmx1024m", "-cp",
                                autoweka.Util.getAbsoluteClasspath(), "autoweka.tools.ExperimentRunner",
                                experimentFolder, mCurrentSeed);
                        pb.redirectErrorStream(true);

                        //Get it going 
                        mProc = pb.start();

                        //Make sure we try and kill this when we need to
                        Thread killerHook = new autoweka.Util.ProcessKillerShutdownHook(mProc);
                        Runtime.getRuntime().addShutdownHook(killerHook);

                        //Get some input to throw up in the text area
                        BufferedReader reader = new BufferedReader(new InputStreamReader(mProc.getInputStream()));
                        String line;

                       
                        while (!isCompleted) {
                            
                            line = reader.readLine ();
                            if (line!= null) {
                            } else {
                                isCompleted = true;
                            }
                            
                            //System.out.println(line);
                        }
//                        mRunButton.setEnabled(true);
//                        mStopButton.setEnabled(false);


              

                        Runtime.getRuntime().removeShutdownHook(killerHook);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (mObserver != null) {
                            mObserver.update(null, null);
                        }
                    }
                }
            }).start();

      

//            mRunButton.setEnabled(false);
//            mStopButton.setEnabled(true);
        } catch (Exception e) {
//            mStopButton.setEnabled(false);
            //Something went wrong...
//            UIUtil.showExceptionDialog(this, "Failed to run experiment", e);
            if (mObserver != null) {
                mObserver.update(null, null);
            }
//            mRunButton.setEnabled(true);
        }

    }
    
    
    

}

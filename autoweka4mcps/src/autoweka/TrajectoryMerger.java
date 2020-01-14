package autoweka;

import java.io.File;
import java.util.ArrayList;
import java.io.FileInputStream;

/** 
 *  Given a bunch of folders that have experiments in them, merges all the trajectories into a single file for easy analysis
 */
public class TrajectoryMerger
{
    public static void main(String[] args) throws Exception
    {
        ArrayList<String> experimentFolders = new ArrayList<String>();
        //TODO: Parse options if we have any
        for(String arg: args)
            experimentFolders.add(arg);

        for(String experimentPath: experimentFolders)
        {
            File folder = new File(experimentPath);
            TrajectoryGroup group = mergeExperimentFolder(experimentPath);
            group.toXML(experimentPath + File.separator + folder.getName() + ".trajectories");
        }
    }

    public static TrajectoryGroup mergeExperimentFolder(String experimentPath)
    {
        try
        {
            //Get me the experiment
            File folder = new File(experimentPath);
            Experiment experiment = Experiment.fromXML(experimentPath + File.separator + folder.getName() + ".experiment");

            TrajectoryGroup group = new TrajectoryGroup(experiment);

            int numEvaluations = 0;
            
            System.out.println("Experiment " + experimentPath);
            //Now, figure out what trajectories are there
            File[] experimentDirFiles = new File(experimentPath + File.separator).listFiles();
            for(File f: experimentDirFiles)
            {
                if(!f.getName().matches("^" + folder.getName() + ".trajectories.[^\\.]+"))
                    continue;

                String seed = f.getName().substring(f.getName().lastIndexOf(".")+1);

                TrajectoryGroup childGroup = TrajectoryGroup.fromXML(new FileInputStream(f));
                numEvaluations += childGroup.getTrajectory(seed).getNumEvaluations();
                //TODO: Check to make sure the experiments match
                group.addTrajectory(childGroup.getTrajectory(seed));
                
            }
            group.setNumEvaluations(numEvaluations);
            return group;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}


package autoweka;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Abstract class that SMBO Methods must provide that allows for trajectories to be extracted after a run has completed
 */
public abstract class TrajectoryParser
{
    
    /** Does the work for a specific trajectory */
    public abstract Trajectory parseTrajectory(Experiment experiment, File folder, String seed);

    /** Call this on a specific experiment to automatically create the correct trajectory parser that will
     *  be used to extract the data
     */
    public static void main(String[] args)
    {
        String targetSeed = null;
        ArrayList<String> experimentFolders = new ArrayList<String>();

        //We need to figure out what mode we are in
        if(args[0].equals("-batch"))
        {
            //We're in batch mode, and we need to do everything
            for(String arg: args)
                experimentFolders.add(arg);
        }
        else
        {
            //Backwards compatiblity hack
            if(args[0].equals("-single")){
                List<String> a = new ArrayList<String>(Arrays.asList(args));
                a.remove(0);
                args = a.toArray(new String[0]);
            }
            if(args.length != 2)
                throw new IllegalArgumentException("Single mode requires an experiment folder and a seed");
            //Get the experiment
            experimentFolders.add(args[0]);
            targetSeed = args[1];
        }

        for(String experimentPath: experimentFolders)
        {
            //Get me the experiment
            File folder = new File(experimentPath);
            Experiment experiment = Experiment.fromXML(experimentPath + File.separator + folder.getName() + ".experiment");
            
            if (experiment.type.equals("RandomSearch"))
              continue;

            TrajectoryGroup group = new TrajectoryGroup(experiment);

            System.out.println("Experiment " + experimentPath);
            //Now, figure out what seeds there are
            if(targetSeed == null)
            {
                File[] logs = new File(experimentPath + File.separator + "out" + File.separator + "logs" + File.separator).listFiles();
                for(File f: logs)
                {
                    String logName = f.getName();
                    String seed = logName.substring(0, logName.lastIndexOf('.'));

                    System.out.println("  Seed" + seed);
                    try
                    {
                        group.addTrajectory(getTrajectory(experiment, folder, seed));
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                group.toXML(experimentPath + File.separator + folder.getName() + ".trajectories");
            }
            else
            {
                //We're only doing a specific seed
                System.out.println("  Seed" + targetSeed);
                group.addTrajectory(getTrajectory(experiment, folder, targetSeed));
                group.toXML(experimentPath + File.separator + folder.getName() + ".trajectories." + targetSeed);
            }
        }
    }

    public static Trajectory getTrajectory(Experiment experiment, File folder, String seed)
    {
        //Get a Trajectory parser for this experiment
        TrajectoryParser parser;
        Class<?> cls;
        try
        {
            cls = Class.forName(experiment.trajectoryParserClassName);
            parser = (TrajectoryParser)cls.newInstance();
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + experiment.trajectoryParserClassName + "': " + e, e);
        }
        catch(Exception e)
        {
            e.getCause().printStackTrace();
            throw new RuntimeException("Failed to instantiate '" + experiment.trajectoryParserClassName + "': " + e, e);
        }

        return parser.parseTrajectory(experiment, folder, seed);
    }
}

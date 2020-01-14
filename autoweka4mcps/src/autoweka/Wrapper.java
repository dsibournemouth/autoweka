package autoweka;

import avatar.config.AppConst;
import avatar.config.LoggerUtil;
import avatar.config.MLComponentConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import test.uts.aai.pbmn.AutoProcessTestApp;
import uts.aai.avatar.model.MLComponent;

import uts.aai.avatar.service.PetriNetsExecutionEngine;
import uts.aai.avatar.service.SurrogatePipelineMapping;
import uts.aai.pn.model.*;
import uts.aai.utils.IOUtils;

/**
 * Generic class that gets called from an SMBO method, completes the evaluation,
 * and returns the result back up to the SMBO method.
 *
 * The command line arguments for these wrappers consist of two portions, a
 * number of options that specify seeds/properties that relate to the wrapper
 * itself, followed by the -wrapper argument. Depending on the SMBO method, this
 * is where you specify the datasetString that you want to pass off to the
 * InstanceGenerator, along with any other options that are required. The
 * remainder of the arguments after the sub classes have processed the -wrapper
 * are going to be passed off to the classifier runner for evaluation.
 *
 * There are a number of 'events' that occur during the run of a wrapper - if
 * you need to do something special here you should just be able to override
 * these and inject the correct behaviour as needed
 */
public class Wrapper {

    protected String mSeed = "";
    protected String mExperimentSeed = "0";
    protected String mInstance = null;
    protected float mTimeout = 0;
    protected ClassifierRunner mRunner;
    protected Properties mProperties;
    protected String mResultMetric = null;
    protected boolean isAvatar = false;
    protected IOUtils iou = new IOUtils();

    /**
     * Runs the wrapper with the given command line arguments - see the class
     * description for full details
     */
    public void run(String[] argsArray) {

        LinkedList<String> args = new LinkedList<String>(Arrays.asList(argsArray));

//        for (String arg : args) {
//            LoggerUtil.logAvatar("AVATAR - arg : " + arg);
//        }
        ArrayList<String> wrapperConstructorArgs = new ArrayList<String>();
        ArrayList<String> wrapperArgs = new ArrayList<String>();
        String configFileName = null;

        //Get the experiment seed
        mExperimentSeed = System.getenv().get("AUTOWEKA_EXPERIMENT_SEED");

        //First, we need to scan through the args list
        boolean inWrapper = false;
        while (!args.isEmpty()) {
            String arg = args.poll();
            if (!inWrapper && arg.equals("-experimentseed")) {
                //See if it's telling us to get it from an env variable
                mExperimentSeed = args.poll();
            } else if (!inWrapper && arg.equals("-wrapper")) {
                inWrapper = true;
                //First, we need to extract a bunch of things
                _processWrapperParameterStart(args);
            } else if (inWrapper) {
                //Strip out the single quotes if they are there
                if (arg.startsWith("'") && arg.endsWith("'")) {
                    wrapperArgs.add(arg.substring(1, arg.length() - 1));
                } else {
                    wrapperArgs.add(arg);
                }
            } else {
                if (arg.equals("-propsfile")) {
                    configFileName = args.poll();
                    continue;
                } else if (arg.equals("-prop")) {
                    wrapperConstructorArgs.add(arg);
                    wrapperConstructorArgs.add(args.poll());
                    continue;
                }
                //Otherwise, ask the subclass if they want this
                _processParameter(arg, args);
            }
        }

        LoggerUtil.logAvatar("AVATAR - ConfigureFile -" + configFileName + "-");

        if (mExperimentSeed == null) {
            LoggerUtil.logAvatar("WARNING: No experiment seed defined, using default of 0");
            mExperimentSeed = "0";
        }

        //Make sure we have stuff
        if (mInstance == null) {
            throw new RuntimeException("Subclass did not set the instance string");
        }

        //Replace all the {SEED}s we can find in the instance string
        //TODO: Should we repalce all args?
        mInstance = mInstance.replace("{SEED}", mExperimentSeed);

        mProperties = new Properties();
        if (configFileName != null) {
            try {
                mProperties.load(new FileInputStream(configFileName));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load config file: " + e.getMessage(), e);
            }
        }

        //Get the properties that were specified on the command line
        Util.parseCommandLineProperties(mProperties, wrapperConstructorArgs);

        //What kind of evaluation type are we using?
        mResultMetric = mProperties.getProperty("resultMetric", null);
        if (mResultMetric == null) {
            LoggerUtil.logAvatar("WARNING: No evaluation method specified, defaulting to error rate");
            mResultMetric = "errorRate";
        }

        // Java 8 , print key and values
        //    mProperties.forEach((key, value) -> LoggerUtil.logAvatar("Key : " + key + ", Value : " + value));
        isAvatar = Boolean.parseBoolean(mProperties.getProperty("isAvatar", "false"));
        LoggerUtil.logAvatar("isAvatar: " + mProperties.getProperty("isAvatar"));

        Boolean avatarEvaluatedResult = true;
        //AVATAR
        if (isAvatar) {
            LoggerUtil.logAvatar("AVATAR is Active");
            avatarEvaluatedResult = _evaluateWithAVATAR(wrapperArgs);
            //iou.writeData(String.valueOf(avatarEvaluatedResult) + "\n", "C:\\experiments\\tmp\\avatar\\rs-avatar.txt");
        } else {
            LoggerUtil.logAvatar("AVATAR is Deactive");
        }

        //Let the wrapper do anything ahead of time that would be good
        _preRun();

        ClassifierResult res = null;

        mRunner = new ClassifierRunner(mProperties);
        res = _doRun(wrapperArgs);

//
//        
//        if (avatarEvaluatedResult) {
//            //Build the classifier runner
//            mRunner = new ClassifierRunner(mProperties);
//            res = _doRun(wrapperArgs);
//
//        } else {
//            mRunner = new ClassifierRunner(mProperties);
//            res = new ClassifierResult(mResultMetric);
//            res.setCompleted(false);
//          
//            
//        }
        //Post event
        _postRun();

        //Process the result
        _processResults(res);

        LoggerUtil.logAvatar("\n");
    }

    /**
     * Actually does the run of the Classifier Runner.
     *
     * You should only override this if you need to do a number of different
     * runs for each wrapper invocation
     */
    protected boolean _evaluateWithAVATAR(ArrayList<String> wrapperArgs) {

        LoggerUtil.logAvatar("AVATAR - Training Instances - ");

        String datasetString = mProperties.getProperty("datasetString");

        LoggerUtil.logAvatar("AVATAR - prop datasetString: " + datasetString);
        String[] datasetStringConfigs = datasetString.split(":trainArff=");
        String dataPath = datasetStringConfigs[1].replaceAll("__COLONESCAPE__:", ":");

        LoggerUtil.logAvatar("AVATAR - prop: " + dataPath);

        File tmpDir = new File(dataPath);
        boolean exists = tmpDir.exists();

        if (exists) {
            LoggerUtil.logAvatar("AVATAR - prop: FILE EXISTS");
        } else {
            LoggerUtil.logAvatar("AVATAR - prop: FILE NOT EXISTS");
        }
        
        
        
        
        MLComponentConfiguration config = new MLComponentConfiguration();
        String workingDir = System.getProperty("user.dir");
        workingDir = workingDir.substring(0, workingDir.lastIndexOf('\\'));
        workingDir = workingDir.substring(0, workingDir.lastIndexOf('\\'));
        String metaKnowledgeFilePath = workingDir + "\\avatar\\meta_knowledge.json";
        
        LoggerUtil.logAvatar("AVATAR - workingDir: " + metaKnowledgeFilePath);
        List<MLComponent> listOfLoadedMLComponents = config.loadListOfMLComponents(metaKnowledgeFilePath);
        SurrogatePipelineMapping surrogatePipelineMapping = new SurrogatePipelineMapping(listOfLoadedMLComponents);
        PetriNetsPipeline petriNetsPipeline = surrogatePipelineMapping.mappingFromWekaPipeline2PetriNetsPipeline(filterMLComponents(wrapperArgs, listOfLoadedMLComponents), dataPath);

        PetriNetsExecutionEngine engine = new PetriNetsExecutionEngine(petriNetsPipeline, listOfLoadedMLComponents);
        //long startTime = System.currentTimeMillis();

        boolean rs = engine.execute();
        LoggerUtil.logAvatar("AVATAR AVATAR AVATARAVATARAVATAR ******* - eval: " + rs);

        //long endTime = System.currentTimeMillis();
        //LoggerUtil.logAvatar("AVATAR - EVAL RESULT - " + rs + " - Surrogate Pipeline Validation Time: " + (endTime - startTime) + " ms");
        return rs;
    }

    protected ArrayList<String> filterMLComponents(ArrayList<String> wrapperArgs, List<MLComponent> loadedListOfMLComponents) {
        ArrayList<String> allComponents = new ArrayList<>();
        ArrayList<String> classifiers = new ArrayList<>();

        ArrayList<String> listOfSearchMethods = new ArrayList<>();
        listOfSearchMethods.add("weka.core.neighboursearch.BallTree");
        listOfSearchMethods.add("weka.core.neighboursearch.CoverTree");
        listOfSearchMethods.add("weka.core.neighboursearch.KDTree");
        listOfSearchMethods.add("weka.core.neighboursearch.LinearNNSearch");

        ArrayList<String> listOfDistanceMethods = new ArrayList<>();
        listOfDistanceMethods.add("weka.core.ChebyshevDistance");
        listOfDistanceMethods.add("weka.core.EuclideanDistance");
        listOfDistanceMethods.add("weka.core.ManhattanDistance");
        listOfDistanceMethods.add("weka.core.MinkowskiDistance");

        String simplifiedPPStr = "";
        String fullPPStr = "";
        String atttributeSelectionStr = "";

        for (String wrapperArg : wrapperArgs) {

            if (wrapperArg.contains("weka.")) {

                fullPPStr += wrapperArg.trim() + " - ";
                if (getComponentByID(wrapperArg.trim(), loadedListOfMLComponents) != null) {
                    if (wrapperArg.contains("weka.filters.")
                            && !(wrapperArg.contains("weka.filters.CategorizedMultiFilter"))
                            && !(wrapperArg.contains("weka.filters.AllFilter"))
                            && !(wrapperArg.contains("weka.filters.supervised.attribute.AttributeSelection"))) {
                        allComponents.add(wrapperArg.trim());
                        simplifiedPPStr += wrapperArg.trim() + " - ";
                    }

                    if (wrapperArg.contains("weka.classifiers.") // && !(wrapperArg.contains("weka.classifiers.meta."))
                            // && !(wrapperArg.contains("weka.classifiers.meta.FilteredClassifier"))
                            ) {

                        // if ( (wrapperArg.contains("weka.classifiers.meta.RandomCommittee")) 
                        //   || (!(wrapperArg.contains("weka.classifiers.meta.")))  ) {
                        if (wrapperArg.contains("weka.classifiers.lazy.IBk")) {

                            String ibkSearchMethod = "";
                            String searchDistanceMethod = "";

                            String[] tempComponents = fullPPStr.split(" - ");

                            for (int i = tempComponents.length - 1; i >= 0; i--) {

                                String componentStr = tempComponents[i].trim();
                                for (String iStr : listOfSearchMethods) {
                                    if (componentStr.contains(iStr)) {
                                        ibkSearchMethod = iStr;
                                        break;
                                    }
                                }

                                for (String iStr : listOfDistanceMethods) {
                                    if (componentStr.contains(iStr)) {
                                        searchDistanceMethod = iStr;
                                        break;
                                    }
                                }

                                if (!ibkSearchMethod.equals("") && !searchDistanceMethod.equals("")) {
                                    break;
                                }
                            }

                            classifiers.add(wrapperArg.trim() + "~" + ibkSearchMethod + "+" + searchDistanceMethod);

                        } else if (wrapperArg.contains("weka.classifiers.rules.DecisionTable")) {

                            String[] tempComponents = fullPPStr.split(" - ");

                            for (int i = tempComponents.length - 2; i >= 0; i--) {
                                if (tempComponents[i].contains("weka.classifiers.")) {
                                    classifiers.add(wrapperArg.trim() + "~" + tempComponents[tempComponents.length - 2]);
                                    break;
                                }
                            }

                        } else {
                            classifiers.add(wrapperArg.trim());
                        }
                        //}

                    }
                }

                if (wrapperArg.contains("weka.filters.supervised.attribute.AttributeSelection")) {

                    atttributeSelectionStr += wrapperArg.trim() + "~";
                }

                if (atttributeSelectionStr.contains("weka.filters.supervised.attribute.AttributeSelection")
                        && wrapperArg.contains("weka.attributeSelection.")
                        && StringUtils.countMatches(atttributeSelectionStr, "+") < 2) {
                    atttributeSelectionStr += wrapperArg.trim() + "+";
                }

            }

        }

        if (atttributeSelectionStr.contains("weka.filters.supervised.attribute.AttributeSelection")) {
            atttributeSelectionStr = atttributeSelectionStr.substring(0, atttributeSelectionStr.length() - 1);
            allComponents.add(0, atttributeSelectionStr);
            simplifiedPPStr = atttributeSelectionStr + " - " + simplifiedPPStr;
            LoggerUtil.logAvatar("AVATAR - INPUT - Attribute Selection: " + atttributeSelectionStr);

        }

        if (classifiers.size() > 0) {

            String classifierFinalStr = "";
            String lastClassifierComponent = classifiers.get(classifiers.size() - 1);
            if (classifiers.size() >= 2
                    && (lastClassifierComponent.contains("weka.classifiers.meta.")
                    || lastClassifierComponent.contains("weka.classifiers.lazy.LWL"))) {

                classifierFinalStr = lastClassifierComponent + "~" + classifiers.get(classifiers.size() - 2);

            } else {

                classifierFinalStr = classifiers.get(classifiers.size() - 1);

            }
            allComponents.add(classifierFinalStr);
            simplifiedPPStr += classifierFinalStr;
        }

       // iou.writeData(fullPPStr + "," + simplifiedPPStr + "\n", "C:\\experiments\\tmp\\avatar\\pipelines.txt");
        return allComponents;

    }

    protected MLComponent getComponentByID(String componentId, List<MLComponent> loadedListOfMLComponents) {

        for (MLComponent mLComponent : loadedListOfMLComponents) {
            if (mLComponent.getComponentId().equals(componentId)) {
                return mLComponent;
            }
        }
        return null;
    }

    /**
     * Actually does the run of the Classifier Runner.
     *
     * You should only override this if you need to do a number of different
     * runs for each wrapper invocation
     */
    protected ClassifierResult _doRun(List<String> runnerArgs) {
        //Run it
        ClassifierResult res = new ClassifierResult(mResultMetric);
        res.setCompleted(false);
        com.sun.management.OperatingSystemMXBean OSBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        long startTime = OSBean.getProcessCpuTime();
        //For debuging those stupid issues
        String instanceStr = "";
        for (String s : runnerArgs) {
            LoggerUtil.logAvatar("AVATAR- Adding arg: " + s);
            instanceStr += s + " - ";
        }

       // iou.writeData(instanceStr + "\n", "C:\\experiments\\tmp\\avatar\\wrapper-pipelines.txt");

        LoggerUtil.logAvatar("AVATAR- mInstance: " + mInstance);

        try {
            res = mRunner.run(mInstance, mResultMetric, mTimeout, mSeed, runnerArgs);
        } catch (Throwable e) {
            e.printStackTrace();
            long stopTime = OSBean.getProcessCpuTime();
            res.setTrainingTime(1.0f + ((stopTime - startTime) * 1e-9f));
        }

        return res;
    }

    /**
     * Override this when you want to get at parameters as soon as you hit the
     * -wrapper.
     *
     * Consume any extra arguments from the Queue that should not be passed on
     * to the ClassifierRunner
     */
    protected void _processWrapperParameterStart(Queue<String> args) {
        //Just move this one along - we don't consume anything
    }

    /**
     * Override this when you want to get at parameters before the -wrapper.
     *
     * Consume any extra arguments from the Queue that you don't want to process
     * again
     */
    protected void _processParameter(String arg, Queue<String> args) {
        //Just move this one along
    }

    /**
     * Called just before _doRun();
     */
    protected void _preRun() {
    }

    /**
     * Called just after _doRun();
     */
    protected void _postRun() {
    }

    /**
     * Called once the run has completed (or been terminated), the results
     * should be sent back to the SMBO method here
     */
    protected void _processResults(ClassifierResult res) {
    }
}

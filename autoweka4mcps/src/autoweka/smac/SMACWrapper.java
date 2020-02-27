package autoweka.smac;

import java.io.File;
import java.util.Queue;

import autoweka.ClassifierResult;
import autoweka.Wrapper;
import avatar.config.LoggerUtil;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class SMACWrapper extends Wrapper {

    private boolean mRawEval = false;

    public static void main(String[] args) {

//        for (String arg: args) {
//            LoggerUtil.logAvatar("AVATAR - SMAC: " + arg);
//        }
//        
        SMACWrapper wrapper = new SMACWrapper();
        wrapper.run(args);
    }

    @Override
    protected void _processWrapperParameterStart(Queue<String> args) {
        //First argument is the instance file - smac adds a path here
        mInstance = (new File(args.poll())).getName();
        //The instance info... ignore
        args.poll();
        //The timeout
        mTimeout = Float.parseFloat(args.poll());
        //The cutoff length... ignore
        args.poll();
        //The seed
        mSeed = args.poll();
    }

    @Override
    protected void _processParameter(String arg, Queue<String> args) {
        if (arg.equals("-raw")) {
            mRawEval = true;
        }
    }

    @Override
    protected void _processResults(ClassifierResult res) {
        //Get the score
        float score = res.getScore();
        if (mRawEval) {
            score = res.getRawScore();
        }

        //Did we complete?
        String resultStr = "SAT";
        if (!res.getCompleted()) {
            resultStr = "CRASHED";
        } else {

           // UUID uuid = UUID.randomUUID();
            //String model_path = workingDir + "\\model\\" + uuid.toString();
//            File directory = new File(workingDir + "\\model");
//            if (!directory.exists()) {
//                directory.mkdir();
//            }
//
//            try {
//                if (res.getAttributeSelection() != null) {
//                    weka.core.SerializationHelper.write(model_path + ".attributeselection", res.getAttributeSelection());
//                } else {
//                    File oldFile = new File(model_path + ".attributeselection");
//                    if (oldFile.exists()) {
//                        oldFile.delete();
//                    }
//                }
//                weka.core.SerializationHelper.write(model_path + ".model", res.getClassifier());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
        }

        StringBuilder extraResultsSB = new StringBuilder();
        int i = 0;
        while (mProperties.containsKey("extraRun" + i)) {
            //Run this instance
            ClassifierResult evalRes = mRunner.evaluateClassifierOnTesting(res.getClassifier(), mProperties.getProperty("extraRun" + i), mResultMetric, mTimeout);
            extraResultsSB.append("(");
            extraResultsSB.append(evalRes.getEvaluationTime());
            extraResultsSB.append(" ");
            extraResultsSB.append(evalRes.getRawScore());
            extraResultsSB.append(") ");
            i++;
        }
        //We need to add the norm penalty
        if (mRawEval) {
            extraResultsSB.append("[");
            extraResultsSB.append(res.getNormalizationPenalty());
            extraResultsSB.append("] ");
        }
        if (res.getMemOut()) {
            extraResultsSB.append("MEMOUT ");
        }

        extraResultsSB.append(res.getPercentEvaluated());

        //Print the result string
        System.out.println("Result for ParamILS: " + resultStr + ", " + res.getTime() + ", 0, " + score + ", " + mSeed + ", EXTRA " + extraResultsSB.toString());
        System.out.println("AVATAR - " + res.getClassifier().toString());

        System.exit(0);
    }

//    private void saveModel(ClassifierResult res) {
//        
//        ObjectOutputStream oos;
//        oos = new ObjectOutputStream(new FileOutputStream("mymodel.model"));
//        oos.writeObject(res.getClassifier());
//        oos.flush();
//        oos.close();
//        
//        
//    }
}

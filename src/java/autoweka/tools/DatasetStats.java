package autoweka.tools;

import java.io.File;

import org.apache.commons.lang.ArrayUtils;

import weka.classifiers.Evaluation;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.Instances;
import autoweka.InstanceGenerator;


class DatasetStats
{
    public static void main(String[] args)
    {
        boolean latexOutput = false;
        if (args[0].equals("latex")){
          latexOutput = true;
          args = (String[]) ArrayUtils.subarray(args, 1, args.length);
        }
        
        File firstFile = new File(args[0]);
        String[] allFilesString;
        if (firstFile.isDirectory()){
          File[] allFiles;
          allFiles = firstFile.listFiles((f, n) -> n.endsWith(".zip"));
          allFilesString = new String[allFiles.length];
          for(int i=0; i<allFiles.length; i++){
            allFilesString[i] = allFiles[i].getAbsolutePath();
          }
          args = allFilesString;
        }
        
        
        
        //Go through each arg and check if it's a zip
        for(String zip : args){
            File f = new File(zip);
            String name = f.getName().replace(".zip", "");
            if(!latexOutput){
              System.out.println("Dataset: " + zip);
            }
            
            zip = zip.replace(":", "__COLONESCAPE__:");
            InstanceGenerator  generator;
            try{
                generator = InstanceGenerator.create("autoweka.instancegenerators.Default", "type=zipFile:zipFile=" + zip);
            }catch(Exception e){
                e.printStackTrace();
                continue;
            }

            //Get some stats about the types of stuff in it
            Instances training = generator.getTraining();
            Instances testing = generator.getTesting();
            int numString = 0;
            int numDate = 0;
            int numNominal = 0;
            int numNumeric = 0;
            int numClasses = training.numClasses();
            for(int i = 0; i < training.numAttributes(); i++){
                if(i == training.classIndex())
                    continue;

                Attribute at = training.attribute(i);
                switch(at.type()){
                    case Attribute.NUMERIC:
                        numNumeric++;
                        break;
                    case Attribute.NOMINAL:
                        numNominal++;
                        break;
                    case Attribute.DATE:
                        numDate++;
                        break;
                    case Attribute.STRING:
                        numString++;
                        break;
                    default:
                        throw new RuntimeException("Invalid attribute type '" + at.type() + "'");
                }
            }
            
            double errorRate = -1;
            try{
              ZeroR classifer = new ZeroR();
              classifer.buildClassifier(training);
              Evaluation eval = new Evaluation(training);
              eval.evaluateModel(classifer, testing);
              errorRate = eval.pctIncorrect();
            }
            catch(Exception e){
              e.printStackTrace();
            }
            
            if(latexOutput){
              System.out.println(String.format("%s & %d & %d & %d & %d & %d & %.02f \\\\", name, numNumeric, numNominal, numClasses, training.size(), testing.size(), errorRate));  
            }
            else{
              System.out.println(" Num Training: " + training.size());
              System.out.println(" Num Testing:  " + testing.size());
              System.out.println("   Num Numeric:  " + numNumeric);
              System.out.println("   Num Nominal:  " + numNominal);
              System.out.println("   Num Date:     " + numDate);
              System.out.println("   Num String:   " + numString);
              System.out.println("   Num Classes:   " + numClasses);
              System.out.println(String.format("Majority class error: %.02f %%", errorRate));
            }
        }
    }
}

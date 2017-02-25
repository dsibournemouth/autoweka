package autoweka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.AbstractClassifier;

/**
 * Utility class that can convert arguments from Auto-WEKA to WEKA
 */
public class WekaArgumentConverter
{
    /**
     * Converts arguments from the Auto-WEKA format into something that WEKA can actually understand.
     */
    public static Arguments convert(List<String> args){
        List<ArgumentPair> sortedArgPairs = sortArgs(args);
        return processArgs(sortedArgPairs);
    }
    
    public static String convertToString(String args){
        List<String> listArgs = Arrays.asList(args.split(" "));
        Arguments wekaArgs = WekaArgumentConverter.convert(listArgs);
        return wekaArgs.propertyMap.get("targetclass") + " " + Util.joinStrings(" ",
        	Util.quoteStrings(Util.escapeQuotes(wekaArgs.argMap.get("classifier"))));
    }
    
    public static String convertToPnml(String args) {
        String pnml = "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<pnml>\n";
        
        List<String> listArgs = Arrays.asList(args.split(" "));
        Arguments wekaArgs = WekaArgumentConverter.convert(listArgs);
        Map<String, String> propertyMap = wekaArgs.propertyMap;
        Map<String, List<String>> argMap = wekaArgs.argMap;
        
        String targetClassifierName = propertyMap.get("targetclass");

        if(targetClassifierName == null || targetClassifierName.isEmpty())
        {
            throw new RuntimeException("No target classifier name specified");
        }
        
        String[] argsArray = argMap.get("classifier").toArray(new String[0]);
        AbstractClassifier classifier;
        Class<?> cls;
        try
        {
            cls = Class.forName(targetClassifierName);
            classifier = (AbstractClassifier)cls.newInstance();
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException("Could not find class '" + targetClassifierName + "': " + e.getMessage(), e);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to instantiate '" + targetClassifierName + "': " + e.getMessage(), e);
        }
        try
        {
            classifier.setOptions(argsArray);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Failed to set classifier options: " + e.getMessage(), e);
        }
        
        pnml += "<net type='http://www.informatik.hu-berlin.de/top/pntd/ptNetb' id='noID'>\n";
        pnml += "<place id='i'><name><text>i</text></name></place>\n";
        pnml += "<place id='o'><name><text>o</text></name></place>\n";
        pnml += "<transition id='meta'><name><text>" + targetClassifierName + "</text></name>\n"
                + "<toolspecific tool='WoPeD' version='1.0'><subprocess>true</subprocess><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n";
        pnml += "<arc id='f_i_meta' source='i' target='meta'><inscription><text>f_i_meta</text></inscription></arc>\n";
        pnml += "<arc id='f_meta_o' source='meta' target='o'><inscription><text>f_meta_i</text></inscription></arc>\n";
        pnml += "<page id='meta'>\n"
                + "<net>\n"
                + "<place id='i'><name><text>i</text></name></place>\n"
                + "<place id='o'><name><text>o</text></name></place>\n"
                + "<transition id='predictor'><name><text>FilteredClassifier</text></name></transition>\n"
                + "<arc id='f_i_predictor' source='i' target='predictor'><inscription><text>f_i_predictor</text></inscription></arc>\n"
                + "<arc id='f_predictor_o' source='predictor' target='o'><inscription><text>f_predictor_o</text></inscription></arc>\n"
                + "</net></page>\n";
        pnml += "</net></pnml>";
        return pnml;
    }

    public static class Arguments{
        private Arguments(Map<String, String> _propertyMap, Map<String, List<String>> _argMap){
            propertyMap = _propertyMap;
            argMap = _argMap;
        }
        /**
         * Contains a bunch of properties like the 'targetclass', 'attributeeval', 'attributesearch', and 'attributetime'
         */
        public Map<String, String> propertyMap;
        /**
         * Contains up to three elements, 'attributesearch' for all params related to the search process, 'attributeeval' for the evaluators, and 'classifier' which has all the arguments for the classifier
         */
        public Map<String, List<String>> argMap;
    }
    
    private static Arguments processArgs(List<ArgumentPair> argList)
    {
        String quotedString = null;
        int quoteDepth = 0;
        int actualQuote = 0;
        Map<String, List<String>> argMap = new HashMap<String, List<String>>();
        HashMap<String, String> propertyMap = new HashMap<String, String>();

        PrefixElement[] prefixElements = new PrefixElement[]{ new PrefixElement("assearch_", "attributesearch"), 
                                                              new PrefixElement("aseval_", "attributeeval"),
                                                              new PrefixElement("", "classifier") };

        String[] propertyNames = new String[]{"targetclass", "attributeeval", "attributesearch", "attributetime"};
        //Make sure that the dest map has everything that we'd want
        for(PrefixElement ele: prefixElements){
            if(argMap.get(ele.mapName) == null){
                argMap.put(ele.mapName, new ArrayList<String>());
            }
        }

        for(ArgumentPair arg: argList)
        {
            //What is the current argument?
            if(arg.name.equals("REMOVED") || arg.name.contains("HIDDEN") || arg.value.equals("REMOVE_PREV"))
            { 
                //We don't want to do anything with this arg
                continue;
            }

            boolean gobbeled = false;
            for(String property : propertyNames){
                if(arg.name.equals("-" + property)){
                    propertyMap.put(property, arg.value);
                    gobbeled = true;
                }
            }
            if(gobbeled)
                continue;

            //Figure out what array list we should be inserting into
            List<String> dest = null;
            for(PrefixElement ele : prefixElements) 
            {
                if(arg.name.startsWith(ele.prefix) || arg.name.startsWith(ele.prefix, 1)){
                    //We made sure earlier that this already exists
                    dest = argMap.get(ele.mapName);
                    break;
                }
            }
            //Check to make sure we have something
            if(dest == null)
            {
                //Well crap, we don't
                throw new RuntimeException("Couldn't find a home for the arg '" + arg.name + "'");
            }

            if(arg.name.contains("LOG_"))
            {
                //Undo the log_10
                arg.value = String.format("%f", Math.pow(10, Float.parseFloat(arg.value)));
            }

            if(arg.name.contains("INT_"))
            {
                int val = (int)Math.round(Float.parseFloat(arg.value));
                arg.value = String.format("%d", val);
            }


            String sanitizedName = arg.name;
            if(arg.name.lastIndexOf('_') != -1)
                sanitizedName = "-" + arg.name.substring(1+arg.name.lastIndexOf('_'));

            if(quotedString == null)
            {
                //Should we actually be the start of a quote?
                if(arg.name.endsWith("QUOTE_START"))
                {
                    quotedString = "";//"\"";
                    quoteDepth++;
                    continue;
                }
                else if(arg.name.contains("QUOTE_START"))
                {
                    //We need to add this parameter name, then start a quoted string
                    dest.add(sanitizedName);
                    quoteDepth++;
                    quotedString = "";//"\"";
                    if(!arg.value.equals("REMOVED"))
                        quotedString += arg.value + " ";
                }
                else if(arg.name.contains("DASHDASH")){
                    dest.add("--");
                }
                else
                {
                    //Actually push it back
                    dest.add(sanitizedName);
                    if(!arg.value.equals("REMOVED"))
                        dest.add(arg.value);
                }
            }
            else
            {
                //Should we pop this qoute?
                if(arg.name.endsWith("QUOTE_END"))
                {
                    quotedString = quotedString.trim(); //+ "\"";
                    if (actualQuote>0){
                    	String tmpSlash = "";
	                	if(actualQuote>1){
	                		for(int i=1; i<actualQuote; i++)
                				tmpSlash += "\\";
	                	}
                    	quotedString += tmpSlash + "\" ";
                    	actualQuote--;
                    }
                    quoteDepth--;
                    if(quoteDepth == 0)
                    {
                        dest.add(quotedString);
                        quotedString = null;
                        continue;
                    }
                }
                else{
	                //Should we actually be the start of a quote?
	                if(arg.name.endsWith("QUOTE_START"))
	                {
	                	String tmpSlash = "";
	                	if(actualQuote>0){
	                		for(int i=0; i<actualQuote; i++)
                				tmpSlash += "\\";
	                	}
	                	
                		quotedString += tmpSlash + "\"";
	                    quoteDepth++;
	                    actualQuote++;
	                    continue;
	                }
	                else if(arg.name.contains("QUOTE_START"))
	                {
	                	String tmpSlash = "";
	                	if(actualQuote>0){
	                		for(int i=0; i<actualQuote; i++)
                				tmpSlash += "\\";
	                	}
	                	
                		quotedString += sanitizedName + " " + tmpSlash + "\"";
	                    
	                    actualQuote++;
	                    quoteDepth++;
	                }
	                else
	                {
	                    if(sanitizedName.equals("-DASHDASH")){
	                        sanitizedName = "--";
	                    }
	                    quotedString += sanitizedName + " ";
	                }
	
	                if(!arg.value.equals("REMOVED"))
	                    quotedString += arg.value + " ";
                }
            }
        }
        //for(String s: argMap.get("classifier"))
            //System.out.println("arg: " + s);

        if(quotedString != null)
            throw new RuntimeException("Unbalanced QUOTE markers in arguments" + quoteDepth);
        
        // Regex to remove backslashes and quotes of elements without parameters
        List<String> classifierParams = argMap.get("classifier");
        for(int i=0; i<classifierParams.size(); i++) {
            String current = classifierParams.get(i);
            String currentModified = current.replaceAll("\\\\+\"([a-zA-Z0-9\\.]+)\\\\+\"", "$1");
            classifierParams.set(i, currentModified);
        }
        
        argMap.put("classifier", classifierParams);

        return new Arguments(propertyMap, argMap);
    }



    private static List<ArgumentPair> sortArgs(List<String> args)
    {
        ArrayList<ArgumentPair> argPairs = new ArrayList<ArgumentPair>();
        for(int i = 0; i < args.size(); i+=2)
        {
            //System.out.println(args.get(i));
            ArgumentPair arg = new ArgumentPair(args.get(i), args.get(i+1));
            //Is the name actually a double dash?
            argPairs.add(arg);
        }
        //java.util.Collections.sort(argPairs;
        Collections.sort(argPairs, new Comparator<ArgumentPair>() {

	  @Override
	  public int compare(ArgumentPair o1, ArgumentPair o2) {
	    return o1.name.toUpperCase().compareTo(o2.name.toUpperCase());
	  }

      });
        /*for(ArgumentPair arg : argPairs)
        {
           System.out.println(arg.name + " = " + arg.value);
        }*/
        return argPairs;
    }

    private static class ArgumentPair implements Comparable<ArgumentPair>
    {
        public ArgumentPair(String _name, String _value)
        {
            name = _name;
            value = _value;
        }

        public int compareTo(ArgumentPair rhs)
        {
            return name.compareTo(rhs.name);
        }
        
        public String name;
        public String value;
    }
    
    //Curse you java without your std::pair
    private static class PrefixElement{
        public PrefixElement(String _prefix, String _mapName){
            prefix = _prefix;
            mapName = _mapName;
        }
        public String prefix;
        public String mapName;
    }
    
    public static void main(String[] args) {
      System.out.println(WekaArgumentConverter.convertToString(args[0]));
    }

}

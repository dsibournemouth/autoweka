package autoweka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.IteratedFilteredClassifierEnhancer;
import weka.classifiers.meta.FilteredClassifier;
import weka.filters.AllFilter;
import weka.filters.CategorizedMultiFilter;
import weka.filters.Filter;

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
        
        pnml += "<net type='http://www.informatik.hu-berlin.de/top/pntd/ptNetb' id='noID'>\n"
              + "<place id='i'><name><text>i</text></name><initialMarking><text>1</text></initialMarking></place>\n"
              + "<place id='o'><name><text>o</text></name></place>\n"
              + "<transition id='meta'><name><text>" + targetClassifierName + "</text></name>\n"
              + "<toolspecific tool='WoPeD' version='1.0'><subprocess>true</subprocess><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n"
              + "<arc id='f_i_meta' source='i' target='meta'><inscription><text>f_i_meta</text></inscription></arc>\n"
              + "<arc id='f_meta_o' source='meta' target='o'><inscription><text>f_meta_i</text></inscription></arc>\n";
                
        if (classifier instanceof IteratedFilteredClassifierEnhancer) {
            // meta-predictor with multiple base classifiers
            pnml += "<page id='meta'><net>\n"
                    + "<place id='i'><name><text>i</text></name></place>\n"
                    + "<place id='o'><name><text>o</text></name></place>\n"
                    + "<transition id='split'><name><text>Split</text></name><toolspecific tool='WoPeD' version='1.0'><operator id='split' type='101'/><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n"
                    + "<transition id='join'><name><text>Join</text></name><toolspecific tool='WoPeD' version='1.0'><operator id='join' type='102'/><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n"
                    + "<arc id='f_i_split' source='i' target='split'><inscription><text>f_i_split</text></inscription></arc>\n"
                    + "<arc id='f_join_o' source='join' target='o'><inscription><text>f_join_o</text></inscription></arc>\n";
            
            for(int i=0; i<((IteratedFilteredClassifierEnhancer) classifier).getNumIterations(); i++) {
                pnml += "<place id='pre_predictor%i'><name><text>pre_predictor%i</text></name></place>\n"
                        + "<place id='post_predictor%i'><name><text>post_predictor%i</text></name></place>\n"
                        + "<transition id='predictor%i'><name><text>FilteredClassifier</text></name>"
                        + "<toolspecific tool='WoPeD' version='1.0'><subprocess>true</subprocess><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n"
                        + "<arc id='f_split_pre_predictor%i' source='split' target='pre_predictor%i'><inscription><text>f_split_pre_predictor%i</text></inscription></arc>\n"
                        + "<arc id='f_post_predictor%i_join' source='post_predictor%i' target='join'><inscription><text>f_post_predictor%i_join</text></inscription></arc>\n" 
                        + "<arc id='f_pre_predictor%i_predictor%i' source='pre_predictor%i' target='predictor%i'><inscription><text>f_pre_predictor%i_predictor%i</text></inscription></arc>\n"
                        + "<arc id='f_predictor%i_post_predictor%i' source='predictor%i' target='post_predictor%i'><inscription><text>f_predictor%i_post_predictor%i</text></inscription></arc>\n"
                        + convertFilteredClassifierToPnml((FilteredClassifier) classifier, "pre_predictor", "post_predictor");
                
                pnml = pnml.replaceAll("%i", Integer.toString(i));
            }
            
            pnml += "</net></page>\n";            
        }
        else if (classifier instanceof FilteredClassifier) {
            pnml += "<page id='meta'><net>\n"
                    + "<place id='i'><name><text>i</text></name></place>\n"
                    + "<place id='o'><name><text>o</text></name></place>\n"
                    + "<transition id='predictor'><name><text>FilteredClassifier</text></name>\n"
                    + "<toolspecific tool='WoPeD' version='1.0'><subprocess>true</subprocess><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n"
                    + "<arc id='f_i_predictor' source='i' target='predictor'><inscription><text>f_i_predictor</text></inscription></arc>\n"
                    + "<arc id='f_predictor_o' source='predictor' target='o'><inscription><text>f_predictor_i</text></inscription></arc>\n"
                    + convertFilteredClassifierToPnml((FilteredClassifier) classifier, "i", "o")
                    + "</net></page>\n";

            pnml = pnml.replaceAll("%i", "");
        }
        
        pnml += "</net></pnml>";
        return pnml;
    }
    
    public static String convertFilteredClassifierToPnml(FilteredClassifier classifier, String prev, String post) {
        FilteredClassifier filteredClassifier = (FilteredClassifier) classifier.getClassifier();
        CategorizedMultiFilter metaFilter = (CategorizedMultiFilter) filteredClassifier.getFilter();
        AbstractClassifier baseClassifier = (AbstractClassifier) filteredClassifier.getClassifier();
        
        String pnml = "<page id='predictor%i'><net>\n"
                    + "<place id='"+prev+"%i'><name><text>"+prev+"%i</text></name></place>\n"
                    + "<place id='"+post+"%i'><name><text>"+post+"%i</text></name></place>\n"
                    + "<place id='post_filters%i'><name><text>post_filters%i</text></name></place>\n"
                    + "<transition id='filters%i'><name><text>" + metaFilter.toString() + "</text></name>"
                    + "<toolspecific tool='WoPeD' version='1.0'><subprocess>true</subprocess><time>0</time><timeUnit>1</timeUnit><orientation>1</orientation></toolspecific></transition>\n"
                    + "<transition id='baseclassifier%i'><name><text>" + baseClassifier.getClass().toString().substring(6) + "</text></name></transition>\n"
                    + "<arc id='f_"+prev+"%i_filters%i' source='"+prev+"%i' target='filters%i'><inscription><text>f_"+prev+"%i_filters%i</text></inscription></arc>\n"
                    + "<arc id='f_filters%i_post_filters%i' source='filters%i' target='post_filters%i'><inscription><text>f_filters%i_post_filters%i</text></inscription></arc>\n"
                    + "<arc id='f_post_filters%i_baseclassifier%i' source='post_filters%i' target='baseclassifier%i'><inscription><text>f_post_filters%i_baseclassifier%i</text></inscription></arc>\n"
                    + "<arc id='f_baseclassifier%i_"+post+"%i' source='baseclassifier%i' target='"+post+"%i'><inscription><text>f_baseclassifier%i_"+post+"%i</text></inscription></arc>\n"
                    + getPnml(metaFilter, prev)
                    + "</net></page>\n";
        
        return pnml;
    }
    
    public static String getPnml(CategorizedMultiFilter metaFilter, String prev) {
        String disabled = AllFilter.class.getName();
        
        Filter missingValueFilter = metaFilter.getMissingValuesHandling();
        Filter outlierFilter = metaFilter.getOutlierHandling();
        Filter transformation = metaFilter.getTransformation();
        Filter dimensionalityReduction = metaFilter.getDimensionalityReduction();
        Filter sampling = metaFilter.getSampling();
        
        String filters[] = {missingValueFilter.toString(), outlierFilter.toString(), transformation.toString(), dimensionalityReduction.toString(), sampling.toString()};
        int numActiveFilters = 0;
        for (String filter : filters) {
            if (filter != disabled) {
                numActiveFilters++;
            }
        }
        
        prev += "%i";
        String pnml = "<page id='filters%i'><net>\n"
                + "<place id='" + prev + "'><name><text>" + prev + "</text></name></place>\n"
                + "<place id='post_filters%i'><name><text>post_filters%i</text></name></place>\n";
        
        if (missingValueFilter.toString() != disabled) {
            
            pnml += "<transition id='missing_value%i'><name><text>" + missingValueFilter.toString() + "</text></name></transition>\n"
                  + "<arc id='f_" + prev + "_missing_value%i' source='" + prev + "' target='missing_value%i'><inscription><text>f_" + prev + "_missing_value%i</text></inscription></arc>\n";
            if (--numActiveFilters == 0) {
                pnml += getLastArc("missing_value%i");
            }
            else {
                pnml += "<place id='post_missing_value%i'><name><text>post_missing_value%i</text></name></place>\n"
                      + "<arc id='f_missing_value%i_post_missing_value%i' source='missing_value%i' target='post_missing_value%i'><inscription><text>f_missing_value%i_post_missing_value%i</text></inscription></arc>\n";
            }
            prev = "post_missing_value%i";
        }
        
        if (outlierFilter.toString() != disabled) {
            pnml += "<transition id='outlier%i'><name><text>" + outlierFilter.toString() + "</text></name></transition>\n"
                  + "<arc id='f_" + prev + "_outlier%i' source='" + prev + "' target='outlier%i'><inscription><text>f_" + prev + "_outlier%i</text></inscription></arc>\n";
            if (--numActiveFilters == 0) {
                pnml += getLastArc("outlier%i");
            }
            else {
                pnml += "<place id='post_outlier%i'><name><text>post_outlier%i</text></name></place>\n"
                      + "<arc id='f_outlier%i_post_outlier%i' source='outlier%i' target='post_outlier%i'><inscription><text>f_outlier%i_post_outlier%i</text></inscription></arc>\n";
            }
            prev = "post_outlier%i";
        }
        
        if (transformation.toString() != disabled) {
            pnml += "<transition id='transformation%i'><name><text>" + transformation.toString() + "</text></name></transition>\n"
                  + "<arc id='f_" + prev + "_transformation%i' source='" + prev + "' target='transformation%i'><inscription><text>f_" + prev + "_transformation%i</text></inscription></arc>\n";
            if (--numActiveFilters == 0) {
                pnml += getLastArc("transformation%i");
            }
            else {
                pnml += "<place id='post_transformation%i'><name><text>post_transformation%i</text></name></place>\n"
                      + "<arc id='f_transformation%i_post_transformation%i' source='transformation%i' target='post_transformation%i'><inscription><text>f_transformation%i_post_transformation%i</text></inscription></arc>\n";
            }
            prev = "post_transformation%i";
        }
        
        if (dimensionalityReduction.toString() != disabled) {
            pnml += "<transition id='dimensionality%i'><name><text>" + dimensionalityReduction.toString() + "</text></name></transition>\n"
                  + "<arc id='f_" + prev + "_dimensionality%i' source='" + prev + "' target='dimensionality%i'><inscription><text>f_" + prev + "_dimensionality%i</text></inscription></arc>\n";
            if (--numActiveFilters == 0) {
                pnml += getLastArc("dimensionality%i");
            }
            else {
                pnml += "<place id='post_dimensionality%i'><name><text>post_dimensionality%i</text></name></place>\n"
                      + "<arc id='f_dimensionality%i_post_dimensionality%i' source='dimensionality%i' target='post_dimensionality%i'><inscription><text>f_dimensionality%i_post_dimensionality%i</text></inscription></arc>\n";
            }
            prev = "post_dimensionality%i";
        }
        
        if (sampling.toString() != disabled) {
            pnml += "<transition id='sampling%i'><name><text>" + sampling.toString() + "</text></name></transition>\n"
                  + "<arc id='f_" + prev + "_sampling%i' source='" + prev + "' target='sampling%i'><inscription><text>f_" + prev + "_sampling%i</text></inscription></arc>\n";
            pnml += getLastArc("sampling%i"); // last filter
        }
        
        pnml += "</net></page>\n";
        
        return pnml;
    }
    
    public static String getLastArc(String prev) {
        return "<arc id='f_" + prev + "_post_filters%i' source='" + prev + "' target='post_filters%i'><inscription><text>f_" + prev + "_post_filters%i</text></inscription></arc>\n";
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

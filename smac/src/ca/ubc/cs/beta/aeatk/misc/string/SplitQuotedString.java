package ca.ubc.cs.beta.aeatk.misc.string;

import java.util.ArrayList;
import java.util.List;

/***
 * 
 * @author Chris Thornton <cwthornt@cs.ubc.ca>
 */
public class SplitQuotedString {
	 
    /** 
     * Splits a string based on spaces, grouping atoms if they are inside non escaped double quotes.
     * 
     */
    static private List<String> goodSplitQuotedString(String str)
    {
        List<String> strings = new ArrayList<String>();
        boolean inQuotes = false;
        boolean quoteStateChange = false;
        StringBuffer buffer = new StringBuffer();
        //Find some spaces, 
        for(int i = 0; i < str.length(); i++){
            //Have we toggled the quote state?
            char c = str.charAt(i);
            quoteStateChange = false;
            if(c == '"' && (i == 0 || str.charAt(i-1) != '\\')){
                inQuotes = !inQuotes;
                quoteStateChange = true;
            }
            //Peek at the next character - if we have a \", we need to only insert a "
            if(c == '\\' && i < str.length()-1 && str.charAt(i+1) == '"'){
                c = '"';
                i++;
            }

            //If we're not in quotes, and we've hit a space...
            if(!inQuotes && str.charAt(i) == ' '){
                //Do we actually have somthing in the buffer?
                if(buffer.length() > 0){
                    strings.add(buffer.toString());
                    buffer.setLength(0);
                }
            }else if(!quoteStateChange){
                //We only want to add stuff to the buffer if we're forced to by quotes, or we're not a "
                buffer.append(c);
            }
        }
        //Add on the last string if needed
        if(buffer.length() > 0){
            strings.add(buffer.toString());
        }

        return strings;
    }

    public void splitTest(){
    	for(int i=0; i < 145; i++)
    	{
    		for(String s : splitQuotedString(" This is \"my split\" string\" \"with lots o\' fish \\\"and even escaped\\\" ")){
                System.out.println(s + ",");
            }
    	}
        
    }
    
    
    public static String[] splitQuotedString(String s)
    {
    	return new ArrayList<String>(goodSplitQuotedString(s)).toArray(new String[0]);
    }
    
    
    
    
}

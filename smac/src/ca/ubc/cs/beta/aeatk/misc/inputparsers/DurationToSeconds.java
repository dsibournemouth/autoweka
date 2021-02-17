package ca.ubc.cs.beta.aeatk.misc.inputparsers;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Utility class that converts some strings to seconds
 * 
 * @author sjr
 */
public class DurationToSeconds {


	private static final Pattern firstFormat = Pattern.compile("\\A\\s*(\\d+:)?(\\d+:)?(\\d+:)?(\\d+)\\s*\\z");
	
	private static final Pattern secondFormat = Pattern.compile("\\A\\s*(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s?)?\\s*\\z");


	/**
	 * Returns the number of seconds encoded in the user supplied string
	 * 
	 * This supports two formats currently:
	 * DDdHHhMMmSSs (example 3d14h15m9s)
	 * DD:HH:MM:SS  (example 3:14:15:9)
	 * 
	 * @param inputString the string to convert
	 * @return number of seconds repre
	 */
	public static int numberOfSecondsFromString(String inputString)
	{
		
		
		if(inputString.trim().length() == 0)
		{
			throw new IllegalArgumentException("Got an empty input string");
		}
		Matcher matchOne = firstFormat.matcher(inputString);
		
		while(matchOne.find())
		{
			LinkedList<Integer> values = new LinkedList<Integer>();
			//Ensure the list has atleast 4 values
			values.push(0);
			values.push(0);
			values.push(0);
			for(int i=1; i <= matchOne.groupCount(); i++)
			{
				Integer value; 
			
				String match = matchOne.group(i);
				if(match == null) continue;
				try {
					 value = Integer.valueOf(matchOne.group(i));
				} catch(NumberFormatException e)
				{
					try {
						
						value = Integer.valueOf(match.substring(0, match.length()-1));
					} catch(NumberFormatException e2)
					{
						//Silently fail
						continue;
					}
				}
				
				values.push(value);
			}
			
			int returnValue =  values.get(0) + values.get(1)*60 + values.get(2)*3600 + values.get(3)*86400;
			return returnValue;
		}
		
		
		Matcher matchTwo = secondFormat.matcher(inputString);
		while(matchTwo.find())
		{
		
			LinkedList<Integer> values = new LinkedList<Integer>();
			//Ensure the list has atleast 4 values
			int seconds = 0;
			int minutes = 0;
			int hours = 0;
			int days = 0;
			
			for(int i=1; i <= matchTwo.groupCount(); i++)
			{
				Integer value; 
			
				String match = matchTwo.group(i);
				if(match == null) continue;
				try {
					 value = Integer.valueOf(matchTwo.group(i));
					 seconds = value;
				} catch(NumberFormatException e)
				{
					try {
						
						
						
						value = Integer.valueOf(match.substring(0, match.length()-1));
						
						
						char type = match.substring(match.length()-1).charAt(0);
						
						switch(type)
						{
						case 's':
							seconds = value;
							break;
						case 'm':
							minutes = value;
							break;
						case 'h':
							hours = value;
							break;
						case 'd':
							days = value;
							break;
						default:
							throw new IllegalStateException("Expected something in the set {s,m,h,d} got " + type);
						}
						
					} catch(NumberFormatException e2)
					{
						//Silently fail
						continue;
					}
				}
				
				values.push(value);
			}
			
			int returnValue = seconds + minutes*60 + hours*3600 + days*86400;
			return returnValue;
		}

		
		throw new IllegalArgumentException("Expected string seperated with d,h,m,s (once only), or up to 4 colons, got : " + inputString);
	}
	
}

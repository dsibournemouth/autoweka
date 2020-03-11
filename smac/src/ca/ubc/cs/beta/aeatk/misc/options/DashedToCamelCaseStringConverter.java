package ca.ubc.cs.beta.aeatk.misc.options;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DashedToCamelCaseStringConverter {

	
	private static final Pattern WORD_SEPERATOR = Pattern.compile("-(\\w*)");
	private static final Pattern CAMEL_CASE_CAPS =  Pattern.compile("[A-Z]");
	public static void main(String[] args)
	{
	
		
		System.out.println(compressCamelCase(getCamelCase("random-the-guy-dog")));
		System.out.println(compressCamelCase(getCamelCase("cores")));
		
	}
	
	public static String getCamelCase(String arguments)
	{
		if(arguments.startsWith("--"))
		{
			arguments = arguments.substring(2);
		}
		
		Matcher matcher = WORD_SEPERATOR.matcher(arguments);
		
		String replacement = arguments;
		while(matcher.find())
		{
			
			//System.out.println(matcher.group() + " " + matcher.start() +" "+ matcher.end());
			
			replacement = replacement.replaceFirst("-\\w", arguments.substring(matcher.start()+1, matcher.start() + 2).toUpperCase());
					
		}
		
		return replacement;
	}
	
	public static String compressCamelCase(String arguments)
	{
		
	
		Matcher match =CAMEL_CASE_CAPS.matcher(arguments);
		StringBuilder sb = new StringBuilder();
		
		sb.append(arguments.charAt(0));
		boolean foundAnything = false;
		while(match.find())
		{
			sb.append(arguments.substring(match.start(),match.end()).toLowerCase());
			foundAnything = true;
		}
		if(foundAnything)
		{
			return sb.toString();
		} else
		{
			return arguments;
		}
	}
	
	
	private DashedToCamelCaseStringConverter()
	{
		
	}
	
}

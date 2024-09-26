package ca.ubc.cs.beta.aeatk.misc.bashcompletion;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.beust.jcommander.JCommander;

import ca.ubc.cs.beta.aeatk.misc.MapList;
import ca.ubc.cs.beta.aeatk.misc.options.UsageSection;
import ca.ubc.cs.beta.aeatk.options.docgen.UsageSectionGenerator;

/**
 * Generates bash autocompletion script for options
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class BashCompletion {
	
	public static void main(String[] args) throws Exception
	{
		try {
			BashCompletionOptions opts = new BashCompletionOptions();
			JCommander jcom = new JCommander(opts,true, true);
			jcom.parse(args);
			
			
			Object obj = Class.forName(opts.clazz).newInstance();
		
			List<UsageSection> sections = UsageSectionGenerator.getUsageSections(obj);
			
			
			String completionScript = bash(sections, opts.commandName);
			
			
			if(opts.debug)
			{
				System.out.println(completionScript);
			}
			FileWriter fw = new FileWriter(new File(opts.outputFile),true);
			
			fw.write(completionScript);
			fw.flush();
			fw.close();
			System.out.println("Bash completion script for " + opts.commandName + " written to: " + (new File(opts.outputFile)).getAbsolutePath() + "");
			System.exit(0);
		} catch(Throwable t)
		{
			System.err.println("Couldn't generate bash completion script");
			t.printStackTrace();
			
			System.exit(1);
		}
		
	}
	
	
	public static boolean isDomainASet(String domain)
	{
		
		if(domain == null || domain.length() < 2)
		{
			return false;
		}
		domain = domain.trim();
		if(domain.charAt(0) != '{')
		{
			return false;
		}
		
		if(domain.charAt(domain.length()-1) != '}')
		{
			return false;
		}
		
		
		return true;
	
	}
	
	public static boolean hasNonCamelCase(List<String> strings)
	{
		for(String s : strings)
		{
			s = s.trim();
			
			if(s.matches("^--[a-z0-9\\-]+$"))
			{
				return true;
			}
		} 
		return false;
	}
	
	public static List<String> removeCamelCase(List<String> strings)
	{
		strings = new ArrayList<String>(strings);
		
		Iterator<String> s = strings.iterator();
		while(s.hasNext())
		{
			String str = s.next();
			if(str.matches("^--.*[a-z0-9][A-Z][a-z0-9].*$"))
			{
				s.remove();
			}
			
		}
		
		return strings;
	}
	public static String bash(List<UsageSection> sections, String commandName)
	{
		StringWriter s = new StringWriter();
		PrintWriter pw = new PrintWriter(s);
		
		SortedSet<String> sorted = new TreeSet<String>();
		
		MapList<String, String> domainToOptionsMap = new MapList<String, String>(new TreeMap<String, List<String>>());
		
		
		for(UsageSection sec : sections)
		{
			for(String attr : sec)
			{
				
				List<String> aliases = Arrays.asList(sec.getAttributeAliases(attr).replaceAll("\\s*,\\s*"," ").split(" "));
				
				if(hasNonCamelCase(aliases))
				{
					aliases = removeCamelCase(aliases);
				}
				
				//pw.append(attr);
				sorted.addAll(aliases);
				
				String domain = sec.getAttributeDomain(attr).trim();
				
				if(isDomainASet(domain))
				{
					if(domain != null && domain.trim().length() > 0)
					{
						domainToOptionsMap.addAllToList(domain, aliases);
					}
				}if(domain.trim().equals(UsageSectionGenerator.FILE_DOMAIN))
				{
					domainToOptionsMap.addAllToList(domain, aliases);
				}
				
				
			}
		}
		

		for(String key : sorted)
		{
			if(key.trim().startsWith("--"))
			{
				pw.append(key);
				pw.append(" ");
			}
			
		}
		
		
		
		StringBuilder allCases = new StringBuilder();
		allCases.append("\t#Adapted from http://www.debian-administration.org/article/317/An_introduction_to_bash_completion_part_2\n");
		allCases.append("\tlocal cur prev opts base\n");
		allCases.append("\tCOMPREPLY=()\n");
		allCases.append("\tcur=\"${COMP_WORDS[COMP_CWORD]}\"\n");
		allCases.append("\tprev=\"${COMP_WORDS[COMP_CWORD-1]}\"\n");
		allCases.append("\tcase \"${prev}\" in\n");
		for(Entry<String, List<String>> ent : domainToOptionsMap.entrySet())
		{
			
			String key = ent.getKey();
		
			if(key.equals(UsageSectionGenerator.FILE_DOMAIN))
			{
				StringBuilder sb = new StringBuilder("\t");
				for(String start : ent.getValue())
				{
					
					sb.append(start).append("|");
				}
				sb.setCharAt(sb.length() - 1, ')');
				//sb.append("\n\t\tCOMPREPLY=( $(compgen -f -- ${cur}) )\n");
				//sb.append("\n\t\t c")
				sb.append("\n\t\t _filedir\n");
				//sb.append("\n\t\t local files=( \"$2\"* ); [[ -e ${files[0]} ]] && COMPREPLY=( \"${files[@]}\" )\n ");
				//sb.append("\t\tif [[ ${#COMPREPLY[@]} == 1 ]] ; then\n");
				//sb.append("\t\t echo $COMPREPLY[0]");
				//sb.append("\t\t\tCOMPREPLY[0]=${COMPREPLY[0]% *}\n");
				//sb.append("\t\t echo $COMPREPLY[0]");
				//sb.append("\n\t\t fi\n");
				//sb.append("\n\t\tCOMPREPLY=( $(complete -o filenames -- ${cur}) )\n");
				sb.append("\t\treturn 0\n");
				sb.append("\t\t;;\n");
				allCases.append(sb);
			} else
			{
			
				String possibleCompletions = key.trim().substring(1, key.trim().length() - 1);
				
				possibleCompletions = possibleCompletions.replaceAll(","," "); 
				
				
				StringBuilder sb = new StringBuilder("\t");
				for(String start : ent.getValue())
				{
					
					sb.append(start).append("|");
				}
				sb.setCharAt(sb.length() - 1, ')');
				sb.append("\n\t\tanswers=\"").append(possibleCompletions).append("\"\n");
				sb.append("\t\tCOMPREPLY=( $(compgen -W \"${answers}\" -- ${cur}) )\n ");
				sb.append("\t\treturn 0\n");
				sb.append("\t\t;;\n");
				
				
				allCases.append(sb);
			}
		}
		
		
		allCases.append("\t*)\n");
		allCases.append("\t\tanswers=\"").append(s).append("\"\n");
		allCases.append("\t\tif [[ ${cur} == -* ]] ; then\n"+
		"        		COMPREPLY=( $(compgen -W \"${answers}\" -- ${cur}) )\n"+
		"	        	return 0\n"+
		"    		fi\n");
		
		allCases.append("\t\t;;\n\tesac\n");
		
		String commandNameSafeString = commandName.replaceAll("-","_");
		
		
		
		String bashScriptPrefix = 
		"_"+commandNameSafeString+"()\n" +  
		"{\n" + allCases.toString() +
		"}\n" + 
		"complete -F _"+commandNameSafeString+" "+commandName + "\n\n";
		
		return bashScriptPrefix;
	}
	

}

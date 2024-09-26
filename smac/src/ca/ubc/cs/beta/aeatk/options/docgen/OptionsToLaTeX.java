package ca.ubc.cs.beta.aeatk.options.docgen;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import com.beust.jcommander.JCommander;

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageSection;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init.TargetAlgorithmEvaluatorLoader;

public class OptionsToLaTeX {

	public static void main(String[] args) throws Exception
	{		
		
		try {
			OptionsToLaTexOptions opts = new OptionsToLaTexOptions();
			JCommander jcom = new JCommander(opts,true, true);
			jcom.parse(args);
			
			
			Object obj = Class.forName(opts.clazz).newInstance();
		
			List<UsageSection> sections;
			if(opts.tae)
			{
				Map<String,AbstractOptions> opt2 = TargetAlgorithmEvaluatorLoader.getAvailableTargetAlgorithmEvaluators();
				sections = UsageSectionGenerator.getUsageSections(obj, opt2);
			} else
			{
				sections = UsageSectionGenerator.getUsageSections(obj);
			}
			 
			
			
			String completionScript = latex(sections, opts.level, opts.aliases);
			
			FileWriter fw = new FileWriter(new File(opts.outputFile),false);
			
			fw.write(completionScript);
			fw.flush();
			fw.close();
			System.out.println("Options LaTeX written to: " + (new File(opts.outputFile)).getAbsolutePath() + "");
			System.exit(0);
		} catch(Throwable t)
		{
			System.err.println("Couldn't generate bash completion script");
			t.printStackTrace();
			
			System.exit(1);
		}
		
		
	}

	
	
	
	public static String latex(List<UsageSection> sections, OptionLevel level, boolean aliases)
	{
		StringWriter s2 = new StringWriter();
		PrintWriter pw2 = new PrintWriter(s2);
		//pw.append("\\documentclass[a4paper,12pt]{article}\n");
		pw2.append("\\documentclass[manual.tex]{subfiles}\n");
		
		pw2.append("\\begin{document}\n");
		for(UsageSection sec : sections)
		{
			
			StringWriter s = new StringWriter();
			PrintWriter pw = new PrintWriter(s);
			
			
			
			boolean isHiddenSection = sec.isSectionHidden();

			if(!isHiddenSection)
			{
				pw.append("\t\\subsubsection{").append(sec.getSectionName()).append("}\n\n");
				pw.append(sec.getSectionDescription()).append("\n");
				
			}
			pw.append("\t\\begin{description}[itemsep=.5pt,parsep=.5pt]");
			int options = 0;
			
			for(OptionLevel aLevel : OptionLevel.values())
			{
				
				boolean foundOne = false;
				for(String name : sec)
				{
					
					if(sec.isAttributeHidden(name)) continue;
					
					
					if(!aLevel.equals(sec.getAttributeLevel(name)))
					{
						continue;
					}
						
					
					if(!level.higherOrEqual(aLevel))
					{
						continue;
					}
					
					options++;
					
					if(!foundOne)
					{
						pw.append("\t\t\\item{\\quad\\large\\textsc{"+  aLevel.name().substring(0, 1) +  aLevel.name().toLowerCase().substring(1)  + " Options}}\n");
						foundOne= true;
					}
					
					String printedName = name.replaceAll("-", "-~\\$\\\\!\\$");
					pw.append("\t\t\\item[").append(printedName).append("]");
					String description = sec.getAttributeDescription(name);
					
					//== Escape some special characters
					
					description = description.replaceAll("\\$", Matcher.quoteReplacement("\\$"));
					description = description.replaceAll("\\_", "\\\\_");
					description = description.replaceAll(">=","\\$\\\\geq\\$");
					description = description.replaceAll("<","\\$<\\$");
					description = description.replaceAll(">","\\$>\\$");
					description = description.replaceAll("\\*", "\\$\\\\times\\$");
					description = description.replaceAll("--", "-~\\$\\\\!\\$-");
					description = description.replaceAll("&", "\\\\&");
				
					pw.append(" ").append(description).append("\n\n");
					
					
					StringWriter s3 = new StringWriter();
					PrintWriter pw3 = new PrintWriter(s3);
					
					pw3.append("\t\t\\vspace{-5pt}");
					pw3.append("\t\t\\begin{description}[itemsep=.5pt,parsep=.5pt]\n");
					
					
					boolean item = false;
					if(sec.isAttributeRequired(name))
					{
						pw3.append("\t\t\t\\item[REQUIRED]\n");
						item = true;
					}
					
					if(aliases)
					{
						pw3.format("\t\t\t\\item[Aliases:] %s %n", sec.getAttributeAliases(name).replaceAll("\\_", "\\\\_").replaceAll("--", "-~\\$\\\\!\\$-"));
						item = true;
					}
					
					if(sec.getAttributeDefaultValues(name).length() > 0)
					{
						String defaultValue = sec.getAttributeDefaultValues(name);
						defaultValue = defaultValue.replaceAll("<","\\$<\\$");
						defaultValue = defaultValue.replaceAll(">","\\$>\\$");
						defaultValue = defaultValue.replaceAll("\\_",Matcher.quoteReplacement("\\_"));
						pw3.format("\t\t\t\\item[Default Value:] %s %n", defaultValue);
						item = true;
					}
					
					if(sec.getAttributeDomain(name).length() > 0)
					{
						String domain = sec.getAttributeDomain(name);
						
						if(domain.trim().startsWith("{") && domain.trim().endsWith("}"))
						{
						
							domain = domain.trim();
							String[] vals = domain.substring(1, domain.length() - 1).split(",");
							
							StringBuilder sb = new StringBuilder("\\{$");
							for(String v : vals)
							{
								if(v.trim().length() > 0)
								{
									sb.append("\\mathsf{").append(v).append("}, ");
								}
							}
							sb.setCharAt(sb.length() - 2, ' ');
							sb.append("$\\}");
							
							domain = sb.toString();
						} else
						{
							domain = domain.replaceAll("\\{", "\\$\\\\{");
							domain = domain.replaceAll("\\}", "\\\\}\\$");
							domain = domain.replaceAll("Infinity","\\$\\\\infty\\$");
							domain = domain.replaceAll(" U ", " \\$\\\\bigcup\\$ ");
						}
						
						domain = domain.replaceAll("\\_",Matcher.quoteReplacement("\\_"));
						
						pw3.format("\t\t\t\\item[Domain:] %s %n", domain);
						item = true;
					}
					
					pw3.append("\t\t\\end{description}\n");
					
					if(item)
					{
						pw3.flush();
						pw.append(s3.toString());
					}
					
				}
			}
			
			
			
			pw.append("\t\\end{description}\n\n");
			
			if(options > 0)
			{
				pw.flush();
				pw2.println(s);
			}
			
		}
		
		pw2.append("\\end{document}");
		
		pw2.flush();
		
		
		String result = s2.toString();
		
		Map<String, String> map = new HashMap<String, String>();
				
		map.put("\\'{e}","é");
		
		for(Entry<String, String> replacements : map.entrySet())
		{
			result = result.replaceAll(replacements.getValue(), Matcher.quoteReplacement(replacements.getKey()));
		}
		return result;
		
	}
	
	
		
}

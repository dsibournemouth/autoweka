package ca.ubc.cs.beta.aeatk.options.docgen;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import ca.ubc.cs.beta.aeatk.misc.options.OptionLevel;
import ca.ubc.cs.beta.aeatk.misc.options.UsageSection;

public class OptionsToUsage {

	public static void usage(List<UsageSection> sections)
	{
		usage(sections, false, OptionLevel.DEVELOPER);
	}
	
	public static void usage(List<UsageSection> sections, boolean showHidden, OptionLevel level)
	{
		PrintWriter pw2 = new PrintWriter(System.out);
		
		System.out.println("Usage:\n");
		boolean displayedConverter = false;
		for(UsageSection sec : sections)
		{
			
			if(!displayedConverter && sec.isConverterOptionObject())
			{
				
				pw2.format("%n%n%s","[NOTE]: All options that follow are not options that are able to be invoked on the command line, instead they should be specified in an option file, that will be read in as an argument to one of the above arguements \n\n");
				displayedConverter = true;
			}
			
			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			
			PrintWriter pw = new PrintWriter(bout);
			
			if(!sec.isSectionHidden())
			{
				pw.format(sec.getSectionBanner(), sec.getSectionName());
				pw.format("\t%s %n %n", sec.getSectionDescription());
				
			
				
			}
			int options = 0;
			for(OptionLevel l : OptionLevel.values())
			{
				
				
				if(!level.higherOrEqual(l))
				{
					continue;
				}

				
				
				boolean firstForSection = true;
				for(String name : sec)
				{
					
					
					if(!sec.getAttributeLevel(name).equals(l))
					{
						//Not the best way to do sorting, but effective.
						continue;
					}
					
					String required = "    ";
					if(sec.isAttributeHidden(name) && showHidden)
					{
						required = "[H]";
					} else if(sec.isAttributeHidden(name))
					{
						continue;
					} else
					{
						required = "   ";
					}
					
					if(firstForSection && sec.getNumberOfAttributes()!=0)
					{
						pw.format("\t ====[ " +l.name()+ " Options ]====%n%n");
						firstForSection = false;
					}
					
					options++;
					
					
					
					if(sec.isAttributeRequired(name))
					{
						required = "[R]";
					}
					
					
					pw.format("%-10s %s %n", required, sec.getAttributeAliases(name));
					if(sec.getAttributeDescription(name).trim().length() > 0)
					{
						pw.format("\t\t%s%n", sec.getAttributeDescription(name));
					} else
					{
						
						System.err.println(name + " has no DESCRIPTION");
						System.err.flush();
					}
					
					if(sec.getAttributeDomain(name).trim().length() > 0)
					{
						pw.format("\t\tDomain: %41s%n", sec.getAttributeDomain(name));
					}
					
					
					if(!sec.isAttributeRequired(name) && sec.getAttributeDefaultValues(name).trim().length() > 0)
					{
						pw.format("\t\tDefault: %40s%n", sec.getAttributeDefaultValues(name));
					}
					
					pw.format("%n");
				}
			}
			
			if(options > 0)
			{
				pw.flush();
				pw2.println(bout.toString());
			}
		}
		
		pw2.flush();
		
		System.out.println("\t[R] denotes a parameter is required");
		
		if(showHidden)
		{
			System.out.println("\t[H] denotes a parameter that is hidden and not to be trifled with");
		}
		
	}
	
}

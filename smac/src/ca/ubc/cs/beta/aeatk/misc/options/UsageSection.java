package ca.ubc.cs.beta.aeatk.misc.options;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Stores everything for a "Section" of the Usage Screen
 * 
 * @author Steve Ramage 
 *
 */
public class UsageSection implements Iterable<String> {
	private final String sectionName;
	private final String sectionBanner;
	private final String sectionDescription;
	private final Map<String, String> attributesToDescriptionMap = new TreeMap<String, String>();
	private final Set<String> requiredAttributes = new HashSet<String>();
	private final Map<String, String> defaultValues = new HashMap<String, String>();
	private final Map<String, String> domainMap = new HashMap<String, String>();
	private final Map<String, String> aliasMap = new HashMap<String, String>();
	private final Map<String, Boolean> hiddenMap = new HashMap<String, Boolean>();
	private final Object object;
	private final boolean hidden;
	private final NoArgumentHandler noargHandler;
	private final boolean converterFileOption;
	private final Map<String, OptionLevel> levelMap = new HashMap<String, OptionLevel>();
	
	/**
	 * Constructs a new usage section
	 * @param sectionName 			The name of this section
	 * @param sectionBanner			The banner to put around the section's title.
	 * @param sectionDescription 	The Description of this section
	 * @param hidden				<code>true</code> if we shouldn't display the sectionName or description when displaying options.
	 * @param object				Object this section is associated with
	 * @param converterFileOption			<code>true</code> if this object was created as a relatedObject annotation 
	 */
	public UsageSection(String sectionName, String sectionBanner, String sectionDescription, boolean hidden, Object object, NoArgumentHandler handler, boolean converterFileOption)
	{
		this.sectionName = sectionName;
		this.sectionBanner = sectionBanner;
		this.sectionDescription = sectionDescription;
		this.hidden = hidden;
		this.object = object;
		this.noargHandler = handler;
		this.converterFileOption = converterFileOption;
		
	}
	
	public Object getObject()
	{
		return object;
	}
	
	public String getSectionName()
	{
		return sectionName;
	}
	
	public String getSectionBanner()
	{
		return sectionBanner;
	}
	
	public String getSectionDescription()
	{
		return sectionDescription;
	}
	
	/**
	 * Add an attribute to this section
	 * @param name			Name of the attribute
	 * @param description	Description of the attribute
	 * @param defaultValue	Default value of the attribute
	 * @param required		<code>true</code> if this attribute is required
	 * @param domain		A human readable string that tells us what arguments are allowed
	 * @param allAliases	A human readable string that tells us about all the aliases for the name
	 * @param hidden	    <code>true</code> if this attribute is hidden
	 * @param level 
	 */
	public void addAttribute(String name, String description, String defaultValue, boolean required, String domain, String allAliases, boolean hidden, OptionLevel level)
	{
		if(name == null) throw new IllegalArgumentException("name can't be null");
		name = name.trim();
		if(description != null)	description = description.trim();
		if(defaultValue != null) defaultValue = defaultValue.trim();
		
		attributesToDescriptionMap.put(name, description);
		defaultValues.put(name, defaultValue);
		if(required)
		{
			requiredAttributes.add(name);
		}
		
		domainMap.put(name, domain);
		aliasMap.put(name,allAliases);
		hiddenMap.put(name, hidden);
		levelMap.put(name,level);
	}

	@Override
	public Iterator<String> iterator() {
		return attributesToDescriptionMap.keySet().iterator();
	}
	
	public int getNumberOfAttributes()
	{
		return attributesToDescriptionMap.size();
	}
	
	public boolean isAttributeRequired(String name)
	{
		return requiredAttributes.contains(name);
	}
	
	public String getAttributeDescription(String name)
	{
		return attributesToDescriptionMap.get(name);
	}
	
	public String getAttributeDefaultValues(String name)
	{
		return defaultValues.get(name);
	}
	
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Name:").append(sectionName).append("\n");
		sb.append("Description:").append(sectionDescription).append("\n");
		
		for(String s : this)
		{
			sb.append("\t").append(s).append(" ").append(getAttributeDescription(s)).append("\n");
			sb.append("\t");
			if(isAttributeRequired(s))
			{
				sb.append("[R] ");
			}
			sb.append("Default Value:").append(getAttributeDefaultValues(s)).append("\n");
			sb.append("\tAliases:").append(getAttributeAliases(s)).append("\n");
			if(getAttributeDomain(s).length() > 0)
			{
				
				sb.append("\tDomain:").append(getAttributeDomain(s)).append("\n");
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}

	public String getAttributeAliases(String s) {

		return aliasMap.get(s);
	}
	
	public String getAttributeDomain(String s)
	{
		return domainMap.get(s);
	}

	public boolean isSectionHidden() {

		return hidden;
	}


	public OptionLevel getAttributeLevel(String s)
	{
		return levelMap.get(s);
	}
	public boolean isAttributeHidden(String name) {

		return hiddenMap.get(name);
	}

	public NoArgumentHandler getHandler() {
		return this.noargHandler;
	}
	
	public boolean isConverterOptionObject()
	{
		return this.converterFileOption;
	}

}

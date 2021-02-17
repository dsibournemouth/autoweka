package ca.ubc.cs.beta.aeatk.options;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ca.ubc.cs.beta.aeatk.misc.options.DashedToCamelCaseStringConverter;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

/**
 * Parent Class for most Options objects to allow there settings to be serialized into strings 
 * 
 * AbstractOptions serialization is not meant to be used between versions at all, because they're generally too fluid.
 * Steve is awesome.
 */
public abstract class AbstractOptions implements Serializable {

	public String toString()
	{
		return this.toString(0);
	}
	public String toString(final int initialTabs)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("[").append(this.getClass().getSimpleName()).append("]").append("\n");
		try {
		for(Field f : this.getClass().getDeclaredFields())
		{
			boolean isAccessible = f.isAccessible();
			f.setAccessible(true);
			StringBuilder line = new StringBuilder();
		
			if(f.getAnnotation(Parameter.class) != null || f.getAnnotation(ParametersDelegate.class) != null || f.getAnnotation(DynamicParameter.class) != null)
			{
				boolean isAbstractOption = false;
				for(int i=0; i < initialTabs; i++)
				{
					sb.append("\t");
				}
				
				line.append(f.getName());
				line.append(" = ");
				
				if(f.getName().toLowerCase().contains("password"))
				{
					line.append("<<<PASSWORD CENSORED>>>");
					
				
				} else
				{
					Class<?> o = f.getType();
					if(o.isPrimitive())
					{
						line.append(f.get(this).toString());
					} else
					{
						Object obj = f.get(this);
						if(obj == null)
						{
							line.append("null");
						} else if(obj instanceof File)
						{
							line.append(((File) obj).getAbsolutePath());
						} else if (obj instanceof String)
						{
							line.append(obj);
						} else if (obj instanceof Long)
						{
							line.append(obj.toString());
						} else if(obj instanceof Integer)
						{
							line.append(obj.toString());
						} else if (obj instanceof Enum)
						{
							line.append(((Enum<?>) obj).name());
						} else if (obj instanceof AbstractOptions)
						{
							isAbstractOption = true;
							line.append(((AbstractOptions) obj).toString(initialTabs+2));
						}  else if( obj instanceof List)
						{
							line.append(Arrays.toString(((List<?>) obj).toArray()));
						} else if(obj instanceof Map)
						{
							line.append(obj.toString());
						} else if(obj instanceof Boolean)
						{
							line.append(obj.toString());
						}
						else {
							/*
							 * We take a cautious approach here, we want every object to have a MEANINGFUL toString() method
							 * so we only add types for things we know provide this
							 */
							
							
							//if(obj.toString().equals(System.))
							throw new IllegalArgumentException("Failed to convert type configuration option to a string " + f.getName() + "=" +  obj + " type: " + o) ;
						}
					}
					
				}
				if(!isAbstractOption == true)
				{
					sb.append(" ");
				}
				sb.append(line).append("\n");
			}
			f.setAccessible(isAccessible);
		}
		return sb.toString();
		} catch(RuntimeException e)
		{
			throw e;
			
		} catch(Exception e)
		{
			throw new RuntimeException(e); 
		}
		
	}
			

	public List<String> configToString()
	{
		//I don't see what the point of the string builder here is
		StringBuilder sb = new StringBuilder();
		ArrayList<String> list = new ArrayList<String>();
		for(Field f : this.getClass().getDeclaredFields())
		{

			boolean isAccessible = f.isAccessible();
			f.setAccessible(true);
			
			if(!f.isAnnotationPresent(ParametersDelegate.class)) continue;
			
			ParametersDelegate ant = f.getAnnotation(ParametersDelegate.class);
			
			if(ant != null)
			{
				try {
					
					
				
					
					Object o = f.get(this);
					if(o == null) continue;
					
					if(o instanceof AbstractOptions)
					{
						list.addAll( ((AbstractOptions) o).configToString());
						sb.append(" ").append(((AbstractOptions) o).configToString());
					}
					sb.append(" ");
					
				} catch (Exception e) {


				} 
				
			}
			f.setAccessible(isAccessible);
		}
		
		for(Field f : this.getClass().getDeclaredFields())
		{
			boolean isAccessible = f.isAccessible();
			f.setAccessible(true);
			
			if(!f.isAnnotationPresent(Parameter.class)) continue;
			
			
			
			Parameter ant = f.getAnnotation(Parameter.class);
			
			if(ant != null)
			{
				try {
					
					
				
					
					Object o = f.get(this);
					if(o == null) continue;
					
					if(o instanceof Boolean)
					{
						boolean b = (Boolean) o;
								
						if(b)
						{
							list.add(ant.names()[0]);
							sb.append(ant.names()[0]).append(" ");
						}
					} else if(o instanceof File)
					{
						list.add(ant.names()[0]);
						list.add(((File) o ).getAbsolutePath());
						
						sb.append(ant.names()[0]).append(" ");
						sb.append( ((File) o).getAbsolutePath() );
					} else if (o instanceof Integer)
					{
						list.add(ant.names()[0]);
						list.add(o.toString());
						
						sb.append(ant.names()[0]).append(" ");
						sb.append(o);
					}else if (o instanceof Long)
					{
						list.add(ant.names()[0]);
						list.add(o.toString());
						sb.append(ant.names()[0]).append(" ");
						sb.append(o);
					} else if (o instanceof String)
					{
						sb.append(ant.names()[0]).append(" ");
						sb.append("\""+o+"\"");
						list.add(ant.names()[0]);
						list.add(o.toString());
					}  
					else if (o instanceof Double)
					{
						sb.append(ant.names()[0]).append(" ");
						sb.append(o);
						list.add(ant.names()[0]);
						list.add(o.toString());
					} else if(o instanceof Enum)
					{
						sb.append(ant.names()[0]).append(" ");
						sb.append(((Enum<?>) o).name());
						list.add(ant.names()[0]);
						list.add(((Enum<?>) o).name());
					} else if(o instanceof Collection)
					{
						sb.append(ant.names()[0]).append(" ");
						sb.append("\""+o+"\"");
						list.add(ant.names()[0]);
						list.add(o.toString());
					} else if(o instanceof AbstractOptions)
					{
		
						list.addAll( ((AbstractOptions) o).configToString());
						sb.append(" ").append(((AbstractOptions) o).configToString());
	
					}
					else 
					{
						System.err.println("No idea what o is " + o.getClass()  +" value:" + o + " name " + ant.names()[0] + ". We essentially have no way of faithfully inverting the object back to whatever cli argument generated it");
					}
					sb.append(" ");
					
				} catch (Exception e) {
					e.printStackTrace();
				} 
				
			}
			
			DynamicParameter pmap = f.getAnnotation(DynamicParameter.class);

			System.out.println(f.getName());
			if(pmap != null)
			{

				Object o;
				try {
					o = f.get(this);
					if(o == null) continue;
					
					if( o instanceof Map)
					{
						sb.append(pmap.names()[0]).append(" ");
						sb.append(o);
						list.add(ant.names()[0]);
						list.add(o.toString());
					} else
					{
						System.err.println("No idea what o is " + o.getClass()  +" value:" + o + " name " + ant.names()[0] + ". We essentially have no way of faithfully inverting the object back to whatever cli argument generated it");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
				
			}
			
		f.setAccessible(isAccessible);	
		}
		return list;
	}
	

	/**
	 * Converts all options into a Map<String, String> as best as possible for RunGroup naming.
	 * @param opts
	 */
	public void populateOptionsMap(Map<String, String> opts)
	{
		for(Field f : this.getClass().getDeclaredFields())
		{
			boolean isAccessible = f.isAccessible();
			f.setAccessible(true);
			if(!f.isAnnotationPresent(Parameter.class)) continue;
			
			
			
			Parameter ant = f.getAnnotation(Parameter.class);
			
			if(ant != null)
			{
				try {
					Object o = f.get(this);
					if(o == null) continue;
					
					if ((o instanceof Boolean) || (o instanceof File) || (o instanceof Integer) || (o instanceof Long) ||  (o instanceof String) || (o instanceof Double) || (o instanceof Enum))
					{
						for(String name : ant.names())
						{
							

							if(!name.startsWith("--"))
							{
								continue;
							}
							
							name = name.substring(2);
							if(!name.contains("-"))
							{	
								opts.put(name, o.toString());
							}
							String camelized = DashedToCamelCaseStringConverter.getCamelCase(name);
							if(!opts.containsKey(camelized))
							{
								opts.put(camelized, o.toString());
							}
						}
					
					} else if(o instanceof Collection)
					{
						//Skip Collections
					} else if(o instanceof AbstractOptions)
					{
						AbstractOptions abs = (AbstractOptions) o;
						abs.populateOptionsMap(opts);
					} else if(o instanceof Map)
					{
						//Skip Maps
					}
					else 
					{
						System.err.println("No idea what o is " + o.getClass()  +" value:" + o + " name " + ant.names()[0]);
					}
					
					
				} catch (Exception e) {
					e.printStackTrace();
				} 
				
			}
			
		
		
			f.setAccessible(isAccessible);
		}
		
		for(Field f : this.getClass().getDeclaredFields())
		{
			boolean isAccessible = f.isAccessible();
			f.setAccessible(true);
			
			if(!f.isAnnotationPresent(ParametersDelegate.class)) continue;
			{
				try {
					Object o = f.get(this);
					
					if(o instanceof AbstractOptions)
					{
						AbstractOptions abs = (AbstractOptions) o;
						abs.populateOptionsMap(opts);
					}
				} catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			f.setAccessible(isAccessible);
		}
		
	}
	
	
	
}

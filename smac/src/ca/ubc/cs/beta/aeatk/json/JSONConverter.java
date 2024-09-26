package ca.ubc.cs.beta.aeatk.json;

import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public abstract class JSONConverter<K>
{
	private final TypeReference<K> magic;
	
	public JSONConverter()
	{
		
		final Type superClass = getClass().getGenericSuperclass();
		TypeReference<K> ref = new TypeReference<K>()
		{
			//See TypeReference javadoc for an explanation of what this does
			//it essentially circumvents type erasure.
			
			@Override
			 public Type getType() { 
				  return ((ParameterizedType) superClass).getActualTypeArguments()[0];		  
			}
		};
		
		this.magic = ref;
		
		
		//System.out.println("My Type:" + this.magic.getType());
	}
	/**
	 * Retrieves a JSON representation of the object
	 * @param obj - object to convert to JSON.
	 * @return json representation
	 */
	public String getJSON(K obj)
	{

		ObjectMapper map = new ObjectMapper();
		SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
		map.registerModule(sModule);
		StringWriter writer = new StringWriter();
		try {
			map.writeValue(writer, obj);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		return writer.toString();
	}
	
	/**
	 * Returns an object based on the JSON
	 * 
	 * The second parameter should be created as an anonymous class, and it uses
	 * 
	 * @param json   json representation	 
	 * @return
	 */
	public K getObject(String json)
	{
		ObjectMapper map = new ObjectMapper();
		SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
		map.registerModule(sModule);
		try {
			K restored = map.readValue(json, magic);
			return restored;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	

	/**
	 * Returns an object based on the JSON
	 * 
	 * The second parameter should be created as an anonymous class for instance
	 * 
	 * <br>
	 * <code>
	 *  jsonConverter.getObject(json, new TypeReference&lt;AlgorithmRun&gt;() { });
	 *  </code>
	 *  <br>
	 *  or 
	 *  <br>
	 *  <code>
	 *  jsonConverter.getObject(json, new TypeReference&lt;List&lt;AlgorithmRun&gt;&gt; () { });
	 * </code>
	 * 
	 * <br>
	 * @param json   json representation	 
	 * @param sol 	 type reference object (see javadoc of this method for how to get) 
	 * @return
	 */
	public K getObject(String json, TypeReference<K> sol)
	{
		ObjectMapper map = new ObjectMapper();
		SimpleModule sModule = new SimpleModule("MyModule", new Version(1, 0, 0, null));
		map.registerModule(sModule);
		try {
			
			K restored = map.readValue(json, sol);
			return restored;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	
}

package ca.ubc.cs.beta.aeatk.json.serializers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonDeserializerHelper {

	@SuppressWarnings("unchecked")
	public static <T> T getDeserializedVersion(JsonParser jp, DeserializationContext ctxt, Class<?> cls) throws JsonMappingException, JsonProcessingException, IOException
	
	
	{
		return (T) jp.readValueAs(cls);
	
		//return (T) jp.getCodec().getDeserializer(ctxt, ctxt.getTypeFactory().constructType(cls)).deserialize(jp, ctxt);
		/*jp.readValueAs(new TypeReference<>)
		if(jp.getCodec() instanceof ImprovedObjectMapper)
		{
			
			ImprovedObjectMapper mapper = (ImprovedObjectMapper) jp.getCodec();
		
		} else
		{
			throw new JsonMappingException("Unfortuntately these convertors only work if the codec is an Improved Object Mapper at this time. The problem is that the caches are associated with specific instances of the serializer");
		}*/
	}
	
	
	
	//private static final AtomicInteger lastLoggedSize = new AtomicInteger(0);
	
	public static <K,V> Map<K,V> getMap()
	{
		return Collections.synchronizedMap(new WeakHashMap<K,V>());
	}
	public static synchronized <K> Map<Integer, K> getCache(Map<ObjectCodec, Map<Integer,K>> cacheMap, ObjectCodec c)
	{
		synchronized(cacheMap)
		{
			if(cacheMap.get(c) == null)
			{
				cacheMap.put(c, new HashMap<Integer, K>());
				
			} 

			return cacheMap.get(c);
		}
		
	}
}

package ca.ubc.cs.beta.aeatk.json.serializers;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfigurationSpace;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ProblemInstanceJson  {

	public static final String PI_ID = "@pi-id";

	public static final String PI_INSTANCE_ID_DEPRECATED = "pi-instance-id(deprecated)";

	public static final String PI_FEATURES = "pi-features";

	public static final String PI_INSTANCE_SPECIFIC_INFO = "pi-instance-specific-info";

	public static final String PI_NAME = "pi-name";

	public static final String PISP_SEED = "pisp-seed";

	public static final String PISP_PI = "pisp-pi";

	public static final String PISP_ID = "@pisp-id";
	
	
	public static class ProblemInstanceDeserializer extends StdDeserializer<ProblemInstance>
	{

		
		private static final Map<ObjectCodec, Map<Integer, ProblemInstance>> cacheMap = JsonDeserializerHelper.getMap(); 	
		
		protected ProblemInstanceDeserializer() {
			
			super(ProblemInstance.class);
			//System.out.println("TEST");
		}

		@Override
		public ProblemInstance deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			
		
			if(jp.getCurrentToken()==JsonToken.START_OBJECT)
			{
				jp.nextToken();
			}
	
		
			
			
			String instanceName = null;
			int instanceDeprecatedId = 0;
			Map<String, Double> features = new TreeMap<String, Double>();
			String instanceSpecificInformation = null;
			
			int pi_id = 0;
			
			while(jp.nextValue() != null)
			{
			
				if(jp.getCurrentToken() == JsonToken.END_OBJECT)
				{
					break;
				}
				
				if(jp.getCurrentName() == null)
				{
					continue;
				}
				
				//System.out.println(jp.getCurrentName() + "=>" + jp.getCurrentToken());
				
				switch(jp.getCurrentName())
				{
					case PI_NAME:
						instanceName = jp.getValueAsString();
					
						break;
					case PI_INSTANCE_SPECIFIC_INFO:
						instanceSpecificInformation = jp.getValueAsString();
						break;
					case PI_INSTANCE_ID_DEPRECATED:
						instanceDeprecatedId = jp.getValueAsInt();
						break;
					case PI_FEATURES:
						
						JsonNode node = jp.getCodec().readTree(jp);
							
						Iterator<Entry<String, JsonNode>> i = node.fields();
						
						while(i.hasNext())
						{
							Entry<String, JsonNode> ent = i.next();
							features.put(ent.getKey(), ent.getValue().asDouble());
						}
					
						break;
						
					case PI_ID:
						pi_id = jp.getValueAsInt();
						break;
				default:
					System.out.println("Not sure what this is:" + jp.getCurrentName());
					break;
					
				}
			}
			
			final Map<Integer, ProblemInstance> cache =   JsonDeserializerHelper.getCache(cacheMap, jp.getCodec());
			
			if( cache.get(pi_id) != null)
			{
				return cache.get(pi_id);
			} else
			{
				ProblemInstance pi = new ProblemInstance(instanceName, instanceDeprecatedId, features, instanceSpecificInformation);
				
				if(pi_id >0 )
				{
					cache.put(pi_id, pi);
				}
				
				return pi;
			}
		}

	}
	
	public static class ProblemInstanceSerializer extends JsonSerializer<ProblemInstance>	{


		
		private final ConcurrentHashMap<ProblemInstance, Integer> map = new ConcurrentHashMap<ProblemInstance, Integer>();
		
		private final AtomicInteger idMap = new AtomicInteger(1);
		

		@Override
		public void serialize(ProblemInstance value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException 
		{
			jgen.writeStartObject();
			
	
			boolean firstWrite = (map.putIfAbsent(value,idMap.incrementAndGet()) == null);
			
			Integer id = map.get(value);		
			jgen.writeObjectField(PI_ID,id);
			
			if(firstWrite)
			{
				jgen.writeObjectField(PI_NAME, value.getInstanceName());
				jgen.writeObjectField(PI_INSTANCE_SPECIFIC_INFO, value.getInstanceSpecificInformation());
				jgen.writeObjectField(PI_FEATURES, value.getFeatures());
				jgen.writeObjectField(PI_INSTANCE_ID_DEPRECATED, value.getInstanceID());
			} 
			
			jgen.writeEndObject();
		}
		
	}
	
	public static class ProblemInstanceSeedPairDeserializer extends StdDeserializer<ProblemInstanceSeedPair>
	{
	

		private static final Map<ObjectCodec, Map<Integer, ProblemInstanceSeedPair>> cacheMap = JsonDeserializerHelper.getMap();
		
		protected ProblemInstanceSeedPairDeserializer() {
			super(ProblemInstanceSeedPair.class);
		}

		@Override
		public ProblemInstanceSeedPair deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException 
		{

			if(jp.getCurrentToken() == JsonToken.START_OBJECT)
			{
				jp.nextToken();
			}
			
		
			ProblemInstance pi = null;
			long seed = Integer.MIN_VALUE;;
			
			int pisp_id = -1;
			
			boolean readFullValue = false;
			while(jp.nextValue() != null)
			{
		
				
				if(jp.getCurrentToken() == JsonToken.END_OBJECT)
				{
					break;
				}
				
				if(jp.getCurrentName() == null)
				{
					continue;
				}
				
				
				
				
				switch(jp.getCurrentName())
				{
					case PISP_PI:
						pi = JsonDeserializerHelper.getDeserializedVersion(jp, ctxt, ProblemInstance.class);
						readFullValue = true;
						break;
					case PISP_SEED:
						seed = jp.getValueAsLong();
						readFullValue = true;
						break;
					case PISP_ID:
						pisp_id = jp.getValueAsInt();
						break;
					default:
						break;
				}
			}
			
			
			final Map<Integer, ProblemInstanceSeedPair> cache =   JsonDeserializerHelper.getCache(cacheMap, jp.getCodec());
			if( cache.get(pisp_id) != null)
			{
				return cache.get(pisp_id);
			} else
			{
				
				
				if(!readFullValue)
				{
					/*
					System.out.println(cache.get(Integer.valueOf(pisp_id)));
					System.out.println(cache.entrySet());

					for(Entry<?,?> cacheEntry : cache.entrySet())
					{
						System.out.println(cacheEntry.getKey() + "(" + cacheEntry.getKey().getClass().getSimpleName() + ") " + " equals: " + cacheEntry.getKey().equals(pisp_id) + " =>" + cacheEntry.getValue());
					}
					*/
					throw new JsonMappingException("Short Object form for " + ProblemInstanceSeedPair.class.getSimpleName() + " detected, but no previously cached version found for ID:" + pisp_id + " cache contains: " + cache.keySet() + " entries ");
				}
				ProblemInstanceSeedPair pisp = new ProblemInstanceSeedPair(pi, seed);
				
				if(pisp_id >0)
				{
					cache.put(pisp_id, pisp);
				}
				
				return pisp;
			}
			
		}

	}
	
	
	public static class ProblemInstanceSeedPairSerializer extends JsonSerializer<ProblemInstanceSeedPair>	{


		private final ConcurrentHashMap<ProblemInstanceSeedPair, Integer> map = new ConcurrentHashMap<>();
		
		private final AtomicInteger idMap = new AtomicInteger(1);
		

		@Override
		public void serialize(ProblemInstanceSeedPair value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException 
		{
			jgen.writeStartObject();
			
	
			boolean firstWrite = (map.putIfAbsent(value,idMap.incrementAndGet()) == null);
			
			Integer id = map.get(value);		
			jgen.writeObjectField(PISP_ID,id);
			
			
			if(firstWrite)
			{
				jgen.writeObjectField(PISP_PI, value.getProblemInstance());
				jgen.writeObjectField(PISP_SEED, value.getSeed());
			} 
			
			jgen.writeEndObject();
		}
		
	}
	
}

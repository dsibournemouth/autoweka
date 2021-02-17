package ca.ubc.cs.beta.aeatk.json.serializers;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmexecutionconfiguration.AlgorithmExecutionConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.misc.version.AEATKVersionInfo;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class AlgorithmRunConfigurationJson  {

	public static final String RC_SAMPLE_IDX = "rc-sample-idx";

	public static final String RC_PC = "rc-pc";

	public static final String RC_PISP = "rc-pisp";

	public static final String RC_CUTOFF = "rc-cutoff";

	public static final String RC_ALGO_EXEC_CONFIG = "rc-algo-exec-config";

	public static final String RC_ID = "@rc-id";
	
	public static class RunConfigDeserializer extends StdDeserializer<AlgorithmRunConfiguration>
	{
		
		
		
		private static final AtomicBoolean warnSampleIdx = new AtomicBoolean(false);
		
		
		private static final Map<ObjectCodec, Map<Integer, AlgorithmRunConfiguration>> cacheMap = JsonDeserializerHelper.getMap();
		
		protected RunConfigDeserializer() {
			super(AlgorithmRunConfiguration.class);
		}

		@Override
		public AlgorithmRunConfiguration deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException 
		{
			
			
			if(jp.getCurrentToken() ==JsonToken.START_OBJECT)
			{
				jp.nextToken();
			}
							
			final Map<Integer, AlgorithmRunConfiguration> cache =   JsonDeserializerHelper.getCache(cacheMap, jp.getCodec());
			
			ProblemInstanceSeedPair pisp = null;
			AlgorithmExecutionConfiguration execConfig = null;
			ParameterConfiguration config = null;
			int sampleIdx = 0;
			double cutoffTime = Double.NEGATIVE_INFINITY;
			
			int rc_id = -1;
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
					case RC_PC:
						config = JsonDeserializerHelper.getDeserializedVersion(jp, ctxt, ParameterConfiguration.class);;
						break;
					case RC_PISP:
						pisp = JsonDeserializerHelper.getDeserializedVersion(jp, ctxt, ProblemInstanceSeedPair.class); 
						break;
					case RC_CUTOFF:
						cutoffTime = jp.getValueAsDouble();
						break;
					case RC_ALGO_EXEC_CONFIG:
						execConfig = JsonDeserializerHelper.getDeserializedVersion(jp, ctxt, AlgorithmExecutionConfiguration.class);
						break;
					case RC_SAMPLE_IDX:
						sampleIdx = jp.getIntValue();
						break;
					case RC_ID:
						rc_id = jp.getValueAsInt();
						break;
					
				default:
					break;
					
				}
			}
			
			if(sampleIdx != 0)
			{
				if(!warnSampleIdx.getAndSet(true))
				{
					Logger log = LoggerFactory.getLogger(getClass());
					log.warn("This version of " + (new AEATKVersionInfo()).getProductName() + " does not support sample ids");
				}
			}
			
			
			if(rc_id > 0 && cache.get(rc_id) != null)
			{
				return cache.get(rc_id);
			} else
			{
				AlgorithmRunConfiguration rc = new AlgorithmRunConfiguration(pisp, cutoffTime, config, execConfig);
				
				if(rc_id > 0)
				{
					cache.put(rc_id, rc);
				}
				return rc;
			}
			
		}

	}
	
	public static class RunConfigSerializer extends StdSerializer<AlgorithmRunConfiguration>	{

	




		protected RunConfigSerializer() {
			super(AlgorithmRunConfiguration.class);
		}


		private final ConcurrentHashMap<AlgorithmRunConfiguration, Integer> map = new ConcurrentHashMap<>();
		
		private final AtomicInteger idMap = new AtomicInteger(1);
		

		@Override
		public void serialize(AlgorithmRunConfiguration value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException 
		{
			jgen.writeStartObject();
			
	
			boolean firstWrite = (map.putIfAbsent(value,idMap.incrementAndGet()) == null);
			
			Integer id = map.get(value);		
			jgen.writeObjectField(RC_ID,id);
			
			if(firstWrite)
			{
				jgen.writeObjectField(RC_ALGO_EXEC_CONFIG, value.getAlgorithmExecutionConfiguration());
				jgen.writeObjectField(RC_CUTOFF, value.getCutoffTime());
				jgen.writeObjectField(RC_PISP, value.getProblemInstanceSeedPair());
				jgen.writeObjectField(RC_PC, value.getParameterConfiguration());
				jgen.writeObjectField(RC_SAMPLE_IDX, 0);
			} 
			
			jgen.writeEndObject();
		}
		
	}
	
	
	
}

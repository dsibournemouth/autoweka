package ca.ubc.cs.beta.aeatk.state.nullFactory;

import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.runhistory.RunHistory;
import ca.ubc.cs.beta.aeatk.state.StateSerializer;

public class NullStateSerializer implements StateSerializer{
	
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void setRunHistory(RunHistory runHistory) {
		//Null Interface
		
	}

	@Override
	public void save() {
		log.trace("Null State Serializer Selected, no data saved");
		
	}

	@Override
	public void setIncumbent(ParameterConfiguration config) {
		//Null Interface
	}



	@Override
	public void setObjectStateMap(Map<String, Serializable> objectState) {
		//Null Interface
	}

}

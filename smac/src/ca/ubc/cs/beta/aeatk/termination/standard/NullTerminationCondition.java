package ca.ubc.cs.beta.aeatk.termination.standard;

import java.util.Collection;
import java.util.Collections;

import ca.ubc.cs.beta.aeatk.termination.ValueMaxStatus;

public class NullTerminationCondition extends AbstractTerminationCondition {

	@Override
	public boolean haveToStop() {
		return false;
	}

	@Override
	public Collection<ValueMaxStatus> currentStatus() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public String getTerminationReason() {
		return "";
	}

}

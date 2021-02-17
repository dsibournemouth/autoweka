package ca.ubc.cs.beta.aeatk.termination;

/**
 * Enumeration used to differentiate different types of Termination Conditions, some
 * are semantically important, others no one cares about.
 * 
 * 
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public enum ConditionType {
	TUNERTIME,
	WALLTIME,
	NUMBER_OF_RUNS,
	EXCEPTION_LIMIT,
	OTHER
}

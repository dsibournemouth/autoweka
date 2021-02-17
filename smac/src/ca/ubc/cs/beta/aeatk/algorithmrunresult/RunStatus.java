package ca.ubc.cs.beta.aeatk.algorithmrunresult;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * Enumeration that represents all possible legal values of an AlgorithmRun
 * <p>
 * <b>Note:</b> All aliases should be specified in upper case in the enum 
 * declaration.
 * 
 * @see ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult
 *
 */
public enum RunStatus {
	
	/**
	 * Signifies that the algorithm ran out of time and had to be cutoff
	 */
	TIMEOUT(false, 0, true, true),
	
	/**
	 * Signifies that the algorithm completed successfully (and optionally found the result was SATISFIABLE) 
	 */
	SAT(true,1, true, false, "SAT","SATISFIABLE","SUCCESS"),
	
	/**
	 * Signifies that the algorithm completed successfully, and that the target algorithm result was UNSATISFIABLE.
	 * <p>
	 * <b>NOTE:</b> SAT & UNSAT are hold overs from SAT solvers, neither of these are error conditions. If you are not running a SAT solver
	 * you should almost certainly report SAT when completed. 
	 */
	UNSAT(true, 2, true, false, "UNSAT", "UNSATISFIABLE"),
	
	
	/**
	 * Signifies that the algorithm did not complete and unexpectedly crashed or failed.
	 */
	CRASHED(false, -1, true, false),

	
	/**
	 * Signifies not only that the algorithm failed unexpectedly but that it's probable that subsequent attempts are most likely going to fail also
	 * and that we should simply not continue with any attempts 
	 *
	 */
	ABORT(false, -2, true, false),
	
	
	/**
	 * Signifies that the algorithm is still currently running
	 * In general this should be used with care, there is 
	 * no guarantee that anything in this state is consistent. 
	 * <br/>
	 * <b>NOTE:</b> Wrappers are NOT permitted to output this run result
	 */
	RUNNING(false,Integer.MIN_VALUE, false, false),
	
	/**
	 * Signifies that run ran out of time, but at our request
	 * This should be handled identically to <code>TIMEOUT</code>
	 * expect that the runObjective of this kind of run should be 
	 * the runTime() and not the captime.
	 * <br/>
	 * <b>NOTE:</b>Wrappers are NOT permitted to output this run result
	 */
	KILLED(false,-3, false, true);
	
	/**
	 * Stores whether the run should be considered as decided
	 */
	private final boolean decided;
	
	/**
	 * Stores the numeric result code used in some serializations of run results
	 * @see ca.ubc.cs.beta.aeatk.state.legacy.LegacyStateFactory
	 * @see ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult
	 */
	private final int resultCode;
	
	/**
	 * Stores whether or not this response is permitted to be output by wrappers
	 */
	private final boolean permittedByWrappers;
	
	
	/**
	 * Stores whether or not this run completed successful AND should be treated as censored
	 */
	private final boolean successfulAndCensored;
	/**
	 * Maps known synonyms of a RunResult for lookup by String
	 */
	private final Set<String> resultKey = new HashSet<String>();
	
	
	private RunStatus(boolean decided, int resultCode, boolean permittedByWrappers, boolean completeButCensored)
	{
		this.decided = decided;
		this.resultCode = resultCode;
		this.resultKey.add(this.toString());
		this.permittedByWrappers = permittedByWrappers;
		this.successfulAndCensored = completeButCensored;
	}
	
	private RunStatus(boolean decided, int resultCode, boolean permittedByWrappers, boolean completeButCensored, String... keys)
	{
		this.decided = decided;
		this.resultCode = resultCode;
		this.resultKey.addAll(Arrays.asList(keys));
		this.permittedByWrappers = permittedByWrappers;
		this.successfulAndCensored = completeButCensored;
		
	}
	
	/**
	 * Converts a String into a RunResult
	 * 
	 * @param key 			string to convert into a runresult
	 * @return 				runresult that represents the string
	 * @throws 				IllegalArgumentException when the string does not match any known runresult
	 */
	public static RunStatus getAutomaticConfiguratorResultForKey(String key)
	{
		/*
		 * Note this method could be faster if just built the map to begin with
		 */
		key = key.toUpperCase();
		for(RunStatus r : RunStatus.values())
		{
			if(r.resultKey.contains(key))
			{
				return r;
			}
		}
		throw new IllegalArgumentException("No Match For Result from Automatic Configurator: " + key);
	}
	
	/**
	 * Converts a result code into a runresult
	 * 
	 * @param resultCode 		integer to be mapped back into a RunResult
	 * @return runresult	 	corresponding to the the resultcode
	 * @throws					IllegalArgumentException when the integer does not match any known runresult 			
	 */
	public static RunStatus getAutomaticConfiguratorResultForCode(int resultCode)
	{
		/*
		 * Note this method could be faster if just built the map to begin with
		 */
		for(RunStatus r : RunStatus.values())
		{
			if(r.resultCode== resultCode)
			{
				return r;
			}
		}
		throw new IllegalArgumentException("No Match For Result from Automatic Configurator");
	}
	
	/**
	 * 
	 * @return result code for this runresult 
	 */
	public int getResultCode()
	{
		return resultCode;
	}
	
	/**
	 * Returns a boolean determining whether this run is Successful (see corresponding link for more information as to what this is for)
	 * 
	 * @deprecated doesn't seem to be used by anything (use {@link #isDecided()}) or == RunResult.SAT) 
	 * 
	 * @see ca.ubc.cs.beta.aeatk.state.legacy.LegacyStateFactory
	 * @return <code>true</code> if and only if this is a successful run, false otherwise
	 */
	public boolean isSolved()
	{
		return this.resultCode == 1;
	}
	
	/**
	 * Returns a boolean determining whether this run was decided (that is it completed and gave us a useful answer about the instance)
	 * 
	 * 
	 * @return <code>true</code> if and only if this run is SAT, UNSAT (or other runs that indicated it finished without incident or TIMEOUT). 
	 */
	public boolean isDecided()
	{
		return this.decided;
	}
	/**
	 * Returns the aliases for this Run Result
	 * @return a set containing all equivilant aliases for this result
	 */
	public Set<String> getAliases() {
		return Collections.unmodifiableSet(this.resultKey);
	}

	/**
	 * Returns <code>true</code> if a wrapper is permitted to output this RunResult.
	 * @return <code>true</code> if a wrapper can output this response, <code>false</code> otherwise.
	 */
	public boolean permittedByWrappers() {
	
		return permittedByWrappers;
	}
	
	/**
	 * Returns <code>true</code> if this means we should treat the {@link ca.ubc.cs.beta.aeatk.objectives.RunObjective} value as finished successfully yet unknown.
	 * 
	 * Specifically CRASHED, ABORT, and RUNNING do not count as complete, but TIMEDOUT and KILLED DO.
	 * @return <code>true</code> if this run was successful and censored (that is it is only a lower bound), <code>false</code> otherwise.
	 */
	public boolean isSuccessfulAndCensored()
	{
		return successfulAndCensored;
	}
}

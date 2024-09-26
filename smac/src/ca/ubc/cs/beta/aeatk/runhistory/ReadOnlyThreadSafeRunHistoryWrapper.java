package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.Collection;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;

/**
 * RunHistory object that cannot be appended to.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class ReadOnlyThreadSafeRunHistoryWrapper extends ThreadSafeRunHistoryWrapper {

	public ReadOnlyThreadSafeRunHistoryWrapper(RunHistory runHistory)
	{
		super(runHistory);
	}
	
	@Override
	public void append(Collection<AlgorithmRunResult> runs)	throws DuplicateRunException {
		throw new UnsupportedOperationException("Cannot append run to read only " + RunHistory.class.getSimpleName() );
	
	}
	
	
	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException {
		throw new UnsupportedOperationException("Cannot append run to read only " + RunHistory.class.getSimpleName() );	
	}
	
}

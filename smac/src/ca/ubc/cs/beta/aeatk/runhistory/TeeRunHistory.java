package ca.ubc.cs.beta.aeatk.runhistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;


/**
 * TeeRunHistory is a RunHistory object that on top of notifying the decorated RunHistory object, also notifies another one but otherwise acts as a transparent decorator.
 * <br>
 * <b>Note:</b>Duplicate runs in the branch are simply silenced.
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 */
public class TeeRunHistory extends AbstractRunHistoryDecorator{

	private RunHistory branch;

	private Logger log = LoggerFactory.getLogger(this.getClass());
	public TeeRunHistory(RunHistory out, RunHistory branch) {
		super(out);
		this.branch = branch;
	}

	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException {
		rh.append(run);
		
		try {
			branch.append(run);
		} catch(DuplicateRunException e)
		{
			log.trace("Branch RunHistory object detected duplicate run: {}", run);
		}
	}

	@Override
	public int getOrCreateThetaIdx(ParameterConfiguration initialIncumbent) {
		
		try {
		return this.rh.getOrCreateThetaIdx(initialIncumbent);
		} finally
		{
			this.branch.getOrCreateThetaIdx(initialIncumbent);
		}
	}
	
	
}

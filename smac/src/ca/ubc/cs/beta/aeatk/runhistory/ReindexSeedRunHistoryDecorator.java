package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jcip.annotations.NotThreadSafe;
import ca.ubc.cs.beta.aeatk.algorithmrunconfiguration.AlgorithmRunConfiguration;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.AlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.algorithmrunresult.ExistingAlgorithmRunResult;
import ca.ubc.cs.beta.aeatk.exceptions.DuplicateRunException;
import ca.ubc.cs.beta.aeatk.misc.MapList;
import ca.ubc.cs.beta.aeatk.parameterconfigurationspace.ParameterConfiguration;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstance;
import ca.ubc.cs.beta.aeatk.probleminstance.ProblemInstanceSeedPair;


/**
 * Decorator that changes seeds of runs to by there run number
 * this is primarily useful for merging many different state files
 * together and getting there runs for different configurations on the same instance to agree.
 * 
 * This isn't ideal, but it's generally better than having a bunch of runs for the same PI differ by seed.
 * 
 * <br>
 * <b>NOTE:</b> This class is not thread safe 
 *
 *
 *
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
@NotThreadSafe
public class ReindexSeedRunHistoryDecorator extends AbstractRunHistoryDecorator implements RunHistory{

	
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Random rand;
	
	public ReindexSeedRunHistoryDecorator(RunHistory rh, Random rand) {
		super(rh);
		this.rand = rand;
	}


	private final Map<ProblemInstanceSeedPair, ProblemInstanceSeedPair> pispTransform = new HashMap<ProblemInstanceSeedPair, ProblemInstanceSeedPair>();

	private final MapList<ProblemInstance, ProblemInstanceSeedPair> mpi = new MapList<ProblemInstance, ProblemInstanceSeedPair>(new HashMap<ProblemInstance, List<ProblemInstanceSeedPair>>());
	
	AtomicInteger nextSeed = new AtomicInteger(0);
	
	AtomicInteger duplicateRunsDropped = new AtomicInteger(0);
	@Override
	public void append(AlgorithmRunResult run) throws DuplicateRunException {
		
		
		
		ProblemInstanceSeedPair pisp = run.getAlgorithmRunConfiguration().getProblemInstanceSeedPair();
		
		if(run.getAlgorithmRunConfiguration().getParameterConfiguration().getParameterConfigurationSpace().getDefaultConfiguration().equals(run.getAlgorithmRunConfiguration().getParameterConfiguration()))
		{
			log.trace("Transforming run of default configuration {}", run);
		}
		
		
		
		if(pispTransform.get(pisp) != null)
		{
			AlgorithmRunConfiguration newRc = new AlgorithmRunConfiguration(pispTransform.get(pisp), run.getAlgorithmRunConfiguration().getCutoffTime(), run.getAlgorithmRunConfiguration().getParameterConfiguration(), run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration());
			
			ExistingAlgorithmRunResult er = new ExistingAlgorithmRunResult( newRc, run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(),pispTransform.get(pisp).getSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

			try { 
				rh.append(er);
			return;
			} catch(DuplicateRunException e)
			{
				log.trace("Duplicate run has been dropped, so far: {} ",  duplicateRunsDropped.incrementAndGet());
			}
		} else
		{
		
			
			List<ProblemInstanceSeedPair> possiblePisps = new ArrayList<ProblemInstanceSeedPair>(mpi.getList(pisp.getProblemInstance()));
			Collections.shuffle(possiblePisps, rand);
			for(ProblemInstanceSeedPair newPisp : possiblePisps)
			{
				AlgorithmRunConfiguration newRc = new AlgorithmRunConfiguration(newPisp, run.getAlgorithmRunConfiguration().getCutoffTime(), run.getAlgorithmRunConfiguration().getParameterConfiguration(), run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration());
				
				ExistingAlgorithmRunResult er = new ExistingAlgorithmRunResult( newRc, run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(),newPisp.getSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

				try { 
					rh.append(er);
					pispTransform.put(pisp, newPisp);
					return;
				} catch(DuplicateRunException e)
				{
					//Don't care about this
				}
			}
			
			
			
			ProblemInstanceSeedPair newPisp = new ProblemInstanceSeedPair(pisp.getProblemInstance(), nextSeed.incrementAndGet()); 
			
			
			AlgorithmRunConfiguration newRc = new AlgorithmRunConfiguration(newPisp, run.getAlgorithmRunConfiguration().getCutoffTime(), run.getAlgorithmRunConfiguration().getParameterConfiguration(), run.getAlgorithmRunConfiguration().getAlgorithmExecutionConfiguration());
			
			ExistingAlgorithmRunResult er = new ExistingAlgorithmRunResult( newRc, run.getRunStatus(), run.getRuntime(), run.getRunLength(), run.getQuality(),newPisp.getSeed(), run.getAdditionalRunData(), run.getWallclockExecutionTime());

			try { 
				rh.append(er);
				pispTransform.put(pisp, newPisp);
				return;
			} catch(DuplicateRunException e)
			{
				log.trace("Duplicate run has been dropped, so far: {} ",  duplicateRunsDropped.incrementAndGet());
			}
			
		
		
		
		}
		
		
		
		
		
	
	
	}
	@Override
	public int getOrCreateThetaIdx(ParameterConfiguration initialIncumbent) {
		return this.rh.getOrCreateThetaIdx(initialIncumbent);
	}
	
}

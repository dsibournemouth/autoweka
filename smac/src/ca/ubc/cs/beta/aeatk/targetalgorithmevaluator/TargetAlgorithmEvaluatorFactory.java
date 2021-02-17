package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator;

import java.util.Map;

import ca.ubc.cs.beta.aeatk.options.AbstractOptions;

/**
 * Factory class for TargetAlgorithmEvalutars
 * 
 * <b>Implementation Note:</b> Unlike what you may think this interface does not exist because
 * someone had drunk design pattern Kool-aid. We load TargetAlgorithmEvaluators via SPI
 * and unfortunately we need to use a no arg constructor, but we clearly need a one arg constructor, hence this interface.
 * 
 * @author Steve Ramage 
 *
 */
public interface TargetAlgorithmEvaluatorFactory {

	
	/**
	 * Returns a friendly name (WITHOUT WHITE SPACE) representing the evaluator
	 * <p>
	 * This is used to present the user with options, so should be constant as the user will specify them on the command line
	 * for instance if your targetalgorithmevalutor simply returns Random results, it might be used via an option --algoExecSystem RANDOM
	 * <p> 
	 * Where RANDOM is what is returned from this method.
	 * 
	 * @return a friendly name for the command line 
	 */
	public String getName();
	
	/**
	 * Retrieves a Target Algorithm Evaluator using the default options, if possible throwing an exception if not.
	 * 
	 * This method exists mainly for convienence of writing unit tests, there is no guarantee that a TAE be able to create itself without intervention from the user (by manipulating the options object)
	 *  
	 * @return the target algorithm evaluator
	 * @throws RuntimeException if this operation cannot be completed.
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator();
	
	/**
	 * Retrieves a Target Algorithm Evaluator
	 * @param 	execConfig    The Execution Configuration for the Target Algorithm
	 * @param   options		  Options 
	 * @return	the target algorithm evaluator
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(AbstractOptions options);
	
	/**
	 * Retrieves a Target Algorithm Evaluator
	 * @param 	execConfig    The Execution Configuration for the Target Algorithm
	 * @param   optionsMap	  Options for all available Target Algorithm Evaluators (the following entry is guaranteed to exist in the map: <getName(), getOptionObject()>)
	 * @return	the target algorithm evaluator
	 */
	public TargetAlgorithmEvaluator getTargetAlgorithmEvaluator(Map<String,AbstractOptions> optionsMap);
	
	
	
	/**
	 * Retrieves an object for use with configuration. It should be compatible with JCommander annotations. 
	 * This object will be passed configured to the TargetAlgorithmEvaluator 
	 * 
	 * Implementations of this object should NOT include required parameters as they will be required regardless of whether this is being used or not.
	 * 
	 * @return object
	 */
	public AbstractOptions getOptionObject();


	
	
}

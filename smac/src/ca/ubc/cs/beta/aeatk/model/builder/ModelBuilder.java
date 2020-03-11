package ca.ubc.cs.beta.aeatk.model.builder;

import ca.ubc.cs.beta.models.fastrf.RandomForest;
/**
 * Model Builder Interface
 * 
 * Implementations of this class should, once construction allow you to retrieve the model objects constructed.
 * 
 * In future, this class will be decoupled from RandomForest.
 * 
 * @author sjr
 *
 */
public interface ModelBuilder {

	/**
	 * Retrieves the built Random Forest
	 * @return RandomForest
	 */
	RandomForest getRandomForest();

	/**
	 * Retrieves a Random Forest which has it's Marginal Pre-processed
	 * @return RandomForest with preprocessed Marginals
	 */
	RandomForest getPreparedRandomForest();

}

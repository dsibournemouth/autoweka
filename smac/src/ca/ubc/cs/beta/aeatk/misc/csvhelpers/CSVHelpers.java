package ca.ubc.cs.beta.aeatk.misc.csvhelpers;


/**
 * Value Object for three different ConfigCSVFileHelper objects
 *
 */
public class CSVHelpers {
	public final ConfigCSVFileHelper configs, features, matrix;
	
	public CSVHelpers(ConfigCSVFileHelper configs, ConfigCSVFileHelper features, ConfigCSVFileHelper matrix)
	{
		this.configs = configs;
		this.features = features;
		this.matrix = matrix;
	}
}

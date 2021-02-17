package ca.ubc.cs.beta.aeatk.misc.version;

/**
 * Small class that returns a product name and version information
 * <p>
 * Primarily anything that is used as a dynamic runtime component 
 * should implement and expose this interface via SPI.
 * <p>
 * 
 * 
 * @author Steve Ramage 
 */
public interface VersionInfo {

	/**
	 * Retrieve the product name
	 * @return name of the product
	 */
	public String getProductName();
	
	/**
	 * Retrieve the version of this product
	 * @return version of the product
	 */
	public String getVersion();
	
	
}

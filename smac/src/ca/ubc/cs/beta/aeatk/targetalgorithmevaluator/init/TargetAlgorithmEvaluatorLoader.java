package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ubc.cs.beta.aeatk.misc.spi.SPIClassLoaderHelper;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluator;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.TargetAlgorithmEvaluatorFactory;
import ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.base.cli.CommandLineTargetAlgorithmEvaluatorFactory;


/**
 * Loads Target Algorithm Evaluators
 * <p>
 * <b>Implementation Note:</b> This class cannot use logging (except in the getTAE method) as it will be accessed before options are parsed.
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class TargetAlgorithmEvaluatorLoader {

	private static final String NO_TAES_ERROR = "WARNING: I could not find ANY Target Algorithm Evaluators on the classpath."
			+ "\n If you made this JAR (or setup the classpath) yourself chances are you did not setup SPI correctly."
			+ "\n You must ensure that in the JAR (or on the classpath) there is a META-INF/services/" + TargetAlgorithmEvaluatorFactory.class.getCanonicalName() +" file\n"
			+" \n In this file should list every implementation of that interface" 
			+" \n\n>>>>HOW TO FIX THIS ERROR:<<<<\n"
			+ "\n 1) If you are using Eclipse see this page (note it's spi-0.2.4 not 0.2.1.jar): https://code.google.com/p/spi/wiki/EclipseSettings"
			+ "\n  OR \n 2) If you are using Ant/Maven ensure that the spi-0.2.4.jar is on the classpath, and annotation processing is enabled."
			+ "\n  OR \n 3) A worse option is to make this file manually, for most cases you simply need to have the following line: " + CommandLineTargetAlgorithmEvaluatorFactory.class.getCanonicalName() 
			+ "\n\n For information on what SPI is see: http://docs.oracle.com/javase/tutorial/ext/basics/spi.html#register-service-providers"
			+ "\n You may also want to look at the AEATK Manual / Developer Reference for more information"
			+ "\n NOTE: I will attempt to fallback to using the CLI TAE only\n NOTE: Sleeping for 4 seconds\n";


	/**
	 * Retrieves a Target Algorithm Evaluator configured with the correct options
	 * @param execConfig					configuration object for target algorithm execution
	 * @param name							the name of the Target Algorithm Evaluator to return
	 * @param taeOptions						The abstract options associated with this target algorithm evaluator
	 * @return a configured <code>Target Algorithm Evaluator</code>
	 */
	public static TargetAlgorithmEvaluator getTargetAlgorithmEvaluator( String name, Map<String, AbstractOptions> taeOptions)
	{
		Logger log = LoggerFactory.getLogger(TargetAlgorithmEvaluatorLoader.class);
		ClassLoader loader = SPIClassLoaderHelper.getClassLoader();
		
		Iterator<TargetAlgorithmEvaluatorFactory> taeIt = ServiceLoader.load(TargetAlgorithmEvaluatorFactory.class, loader).iterator();
		
		boolean noTAEs = true;
		while(taeIt.hasNext())
		{
		
			noTAEs = false;
			try { 
				TargetAlgorithmEvaluatorFactory tae= taeIt.next();
				//log.debug("Found Target Algorithm Evaluator {}", tae.getName());
				
				if(tae.getName().contains(" "))
				{
					log.warn("Target Algorithm Evaluator has white space in it's name, this is a violation of the contract of {}. Sleeping for 5 seconds", TargetAlgorithmEvaluatorFactory.class.getName());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				if(tae.getName().trim().equals(name.trim()))
				{					
					return tae.getTargetAlgorithmEvaluator( taeOptions);
				}
			
			} catch(ServiceConfigurationError e)
			{
				
				log.error("Error occured while retrieving Target Algorithm Evaluator", e);
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e2) {
					Thread.currentThread().interrupt();
				}
			}
		}
			
		
		if(noTAEs)
		{
			CommandLineTargetAlgorithmEvaluatorFactory cfact = new CommandLineTargetAlgorithmEvaluatorFactory();
			
			if(name.trim().equals(cfact.getName()))
			{
				log.warn("No Target Algorithm Evaluators detected, see previous warning on how to fix. Falling back to " + cfact.getName() + " manually");
				
				return cfact.getTargetAlgorithmEvaluator(taeOptions);
			} else
			{
				log.error("No TAEs were detected, but not sure where " + name + " is, please fix as per the following: {} " , NO_TAES_ERROR);
			
			}
		}
		
		
		
		
		throw new IllegalArgumentException("No Target Algorithm Evalutor found for name: " + name);
	
	}


	/**
	 * Returns a Mapping of Target Algorithm Evaluators by their name and associated Option objects
	 * @return map contains taeName and their options
	 */
	public static Map<String, AbstractOptions> getAvailableTargetAlgorithmEvaluators()
	{
		return getAvailableTargetAlgorithmEvaluators(SPIClassLoaderHelper.getClassLoader());
	}
	
	private static Map<String,AbstractOptions> getAvailableTargetAlgorithmEvaluators(ClassLoader loader)
	{
		
		//Whatever map you use here, it should support NULL values.
		Map<String, AbstractOptions> taeOptionsMap = new TreeMap<String,AbstractOptions>();
		
		Iterator<TargetAlgorithmEvaluatorFactory> taeIt = ServiceLoader.load(TargetAlgorithmEvaluatorFactory.class, loader).iterator();

		boolean noTAEsFound = true;
		
		while(taeIt.hasNext())
		{
			noTAEsFound = false;
			try { 
			
				/**
				 * Generally this method is called before we have parsed the logging options
				 * If the TAE inadvertently setups logging before hand, we will not be able to 
				 * configure it afterwards. We try our best to load a security manager
				 * that causes an exception if logging is loaded.
				 * 
				 * 
				 * If this is causing problems you can simply comment this method out
				 * and the subsequent reset method, their only purpose is to make it so that 
				 * logging isn't silently broken.
				 * 
				 * Servlet Environments will probably not let us do this, these methods
				 * should be robust in those cases and hopefully no one is just 
				 * using a TAE in a servlet environmen
				 * 
				 */
				SecurityManager oldSecurityManager = initSecurityManagerIfPossible();
				TargetAlgorithmEvaluatorFactory tae = null;
				String name = null;
				try {
					
					tae = taeIt.next();
					name = tae.getName();
					
					
					
					
					if(name.contains(" "))
					{
						System.err.println("Target Algorithm Evaluator has white space in it's name, this is a violation of the contract of "+ TargetAlgorithmEvaluatorFactory.class.getName());
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
					try 
					{
						try
						{
								int modifiers = tae.getClass().getMethod("getOptionObject").getModifiers();
								if(Modifier.isAbstract(modifiers))
								{
									String msg = tae.getName() + " has an abstract method, it is most likely that this plugin needs updating to a current version. It cannot be loaded";
									System.err.println(msg);
									System.err.flush();
									System.out.println(msg);
									System.out.flush();
									try {
										Thread.sleep(2048);
									} catch (InterruptedException e) {
										Thread.currentThread();
									}
									
									continue;
								}
								
						} catch (SecurityException e) {
							//Just print this crap and continue with the next TAE
							e.printStackTrace();
							continue;
						} catch (NoSuchMethodException e) {
							//Just print this crap and continue with the next TAE
							e.printStackTrace();
							continue;
						}
							
						AbstractOptions options = tae.getOptionObject();
						taeOptionsMap.put(name, options);
						
					} catch(AbstractMethodError e)
					{
						System.err.println("Error occurred while processing " + tae.getName() + " if you are running in eclipse, maybe do a full clean");
						throw e;
					}
					
				} finally
				{
					resetSecurityManagerIfPossible(oldSecurityManager);	
				}
				
			
			} catch(ServiceConfigurationError e)
			{
				
				
				if(e.getCause() instanceof LoggingLoadingSecurityException)
				{
					System.err.println(e.getCause().getMessage());
				} else if( (e.getCause() != null   && e.getCause().getCause() instanceof LoggingLoadingSecurityException))
				{
					System.err.println(e.getCause().getCause().getMessage());					
				}else
				{	
					System.err.println("Error occured while retrieving Target Algorithm Evaluator if you are running in eclipse, maybe do a full clean");
				}
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e2) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		if(noTAEsFound)
		{
			System.err.println(NO_TAES_ERROR);
			
			try {
				Thread.sleep(4096);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				
			}
			CommandLineTargetAlgorithmEvaluatorFactory cfact = new CommandLineTargetAlgorithmEvaluatorFactory();
			taeOptionsMap.put(cfact.getName(), cfact.getOptionObject());
		}
					
		//Options can be modified in the map, but the map keys and values itself can't be
		return Collections.unmodifiableMap(taeOptionsMap);
	}

		
	/**
	 * We will try our best to install a security manager if possible 
	 * that will prevent logging from being loaded  
	 */
	private static SecurityManager initSecurityManagerIfPossible() {

		SecurityManager old = System.getSecurityManager();
		try {
			System.setSecurityManager(new TAEPluginSecurityManager());
			return old;
		} catch(SecurityException e)
		{ //We can't set a security manager, oh well.
			return null;
		}
		
	}

	
	/**
	 * Used to reset the security manager if 
	 * @param oldSecurityManager
	 */
	private static void resetSecurityManagerIfPossible(SecurityManager oldSecurityManager)
	{
	
		try {
			System.setSecurityManager(oldSecurityManager);
		} catch(SecurityException e)
		{
			//Yes we will silently drop this exception. 
		}
		
	}




	
	
	
}

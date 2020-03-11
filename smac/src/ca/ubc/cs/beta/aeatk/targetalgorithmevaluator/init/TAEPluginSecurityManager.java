package ca.ubc.cs.beta.aeatk.targetalgorithmevaluator.init;

import java.security.Permission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/***
 * Custom security manager that prevents us frob using the slf4j or logback packages
 * <br>
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class TAEPluginSecurityManager extends SecurityManager {

	
	
	public static final Set<String> forbiddenPackages;
	
	static{
		HashSet<String> newSet = new HashSet<String>();
		newSet.add("org.slf4j");
		newSet.add("ch.qos.logback");
		
		forbiddenPackages = Collections.unmodifiableSet(newSet);
	}
	
	@Override
	public void checkPackageAccess(String pkg)
	{
		if(forbiddenPackages.contains(pkg))
		{
			SecurityException e = new SecurityException("Package is forbidden at this point:" + pkg);
			
			
			StackTraceElement[] t = e.getStackTrace();
			
			
			String bestGuessClass = "Look at stack trace";
			
			
			for(StackTraceElement el : t )
			{
				if(el.getClassName().equals(TargetAlgorithmEvaluatorLoader.class.getCanonicalName()))
				{
					continue;
				}
				
				
				if(el.getClassName().contains("TargetAlgorithmEvaluator") || el.getClassName().contains("TAE"))
				{
					bestGuessClass = el.getClassName();
				}
			}
			
			
			
			throw new LoggingLoadingSecurityException("Class <" + bestGuessClass +  "> seems to be trying to use logging before it is configured");
		}
		
		
		//System.out.println("Checking " + pkg);
		//super.checkPackageAccess(pkg);
	}
	
	@Override
	public void checkPermission(Permission perm)
	{
		//System.out.println("Checking Permission: " + perm);
		/*
		if(perm instanceof FilePermission)
		{
			
			for(String s : forbiddenPackages)
			{
				if(perm.getName().contains(s))
				{
					System.err.println("Boo!"  + perm.getName());
					
					throw new SecurityException("Package is forbidden at this point:" + perm.getName());
				}
			}
			
			
			
		}
		//super.checkPermission(perm);
		 *
		 */
	}
	
}

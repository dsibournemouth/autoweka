package ca.ubc.cs.beta.aeatk.ant.execscript;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*****
 * This is a launcher used to provide some friendly error messages with weird Java versions.
 * 
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5 
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * WARNING: DO NOT IMPORT OR DEPEND ON ANYTHING ELSE IN AEATK, ALSO THIS CLASS MUST COMPILE WITH JDK VERSION 1.5
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public class Launcher {
	public static void main(String[] args)
	{
		try {
			
			if(args.length < 1)
			{
				throw new IllegalArgumentException("First argument to launcher was support to be a class");
			}
			Class<?> cls = Class.forName(args[0]);
			
			Method m = cls.getMethod("main", String[].class);
			String[] newArgs = new String[args.length-1];
			if(args.length > 1)
			{
				System.arraycopy(args, 1, newArgs, 0, args.length - 1);
				m.invoke(null, (Object) newArgs);
			} else
			{
				m.invoke(null, (Object) new String[0]);
			}
			
			return;
		} catch (ClassNotFoundException e) {
			System.err.println("================================================================================");
			System.err.println("Unable to run program, please contact the author with this full message:\n");
			System.err.println("DEV NOTE: Most likely the shell script created points to the wrong class, \n which in this case we couldn't find.");
			e.printStackTrace();
			System.err.println("================================================================================");
			
			System.exit(128);
			
		} catch (NoSuchMethodException e) {
			System.err.println("================================================================================");
			System.err.println("Unable to run program, please contact the author with this full message:\n");
			System.err.println("DEV NOTE: Most likely the shell script created points to the wrong class, \n which in this case doesn't have a main(String[]) static method");
			e.printStackTrace();
			
			System.err.println("================================================================================");
			System.exit(128);
		} catch(UnsupportedClassVersionError e)
		{
			System.err.println("================================================================================");
			System.err.println("Unable to run program, most likely you need to upgrade to Java >=7.\n"
					+ " Current Java Virtual Machine Name is: " +  System.getProperty("java.vm.name") + "\n Version: (" + System.getProperty("java.version") + ")\n"
					+ " Location: " + System.getProperty("java.home") + "\n"  
					+ "Please check your PATH and JAVA_HOME environment variables to ensure that it is the correct version.\n"
					+ "For more information see: https://www.java.com/en/download/help/download_options.xml");
			System.err.println("================================================================================");
			System.exit(128);
		} catch(LinkageError e)
			{
				System.err.println("================================================================================");
				System.err.println("Unable to run program, most likely you need to upgrade to Java >=7.\n"
						+ " Current Java Virtual Machine Name is: " +  System.getProperty("java.vm.name") + "\n Version: (" + System.getProperty("java.version") + ")\n"
						+ " Location: " + System.getProperty("java.home") + "\n"  
						+ "Please check your PATH and JAVA_HOME environment variables to ensure that it is the correct version\n");
				System.err.println("Otherwise you may have corrupted or missing files, it is suggested you redownload this application");
				System.err.println("or contact the author with this full error message");
				e.printStackTrace();
				System.err.println("================================================================================");
				
				System.exit(128);
			}
		catch (SecurityException e) {
			System.err.println("================================================================================");
			System.err.println("Unable to run program, this seems to be because of your environment (are you running in a Servlet or a secured environment");	
			System.err.println("DEV NOTE: This would seem to be because a SecurityManager is precluding us from using reflection, or some weird class loader magic");
			e.printStackTrace();
			System.err.println("================================================================================");
			System.exit(128);
		} catch(IllegalArgumentException e)
		{
			System.err.println("================================================================================");
			System.err.println("Unable to run program, this seems to be a problem with this build of this software");
			System.err.println("Please contact the author with this message");
			
			System.err.println("DEV NOTE: No argument specified to the launcher, the first argument must be a class");
			e.printStackTrace();
			System.err.println("================================================================================");
			System.exit(128);
		} catch (IllegalAccessException e) {
			System.err.println("================================================================================");
			System.err.println("Unable to run program, this seems to be a problem with this build of this software");
			System.err.println("Please contact the author with this message");
			
			System.err.println("DEV NOTE: Can't seem to invoke main method, is it private?");
			e.printStackTrace();
			System.err.println("================================================================================");
			System.exit(128);
		} catch (InvocationTargetException e) {
			System.err.println("================================================================================");
			System.err.println("Unable to run program, this seems to be a problem with this build of this software");
			System.err.println("Please contact the author with this message");
			
			System.err.println("DEV NOTE: Can't seem to invoke main method, is it private?");
			e.printStackTrace();
			System.err.println("================================================================================");
			System.exit(128);
		}
	}
}

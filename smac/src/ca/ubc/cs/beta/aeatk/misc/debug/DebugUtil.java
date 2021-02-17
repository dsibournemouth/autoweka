package ca.ubc.cs.beta.aeatk.misc.debug;

public final class DebugUtil {
	public static String getCurrentMethodName()
	{
		
		Exception e = new Exception();
		try {
			String methodName =  e.getStackTrace()[1].getMethodName();
			System.err.println("Method Name Returned:" + methodName);
			System.err.flush();
			System.out.flush();
			return methodName;
		} catch(RuntimeException e2)
		{
			
			return "Unknown Method";
		}
	}
	
	private DebugUtil()
	{
		
	}
}

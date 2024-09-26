package ca.ubc.cs.beta.aeatk.parameterconfigurationspace.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TopologicalSorter {

	public static class Constraint<A>
	{
		private A less;
		private A greater;
		
		public Constraint(A a, A b)
		{
			this.less = a;
			this.greater = b;
			
			if(a.equals(b))
			{
				throw new IllegalArgumentException("Contraint specified the same value " + a + " == " + b );
			}
		}
		
		public A getLesser()
		{
			return less;
		}
		
		public A getGreater()
		{
			return greater;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((greater == null) ? 0 : greater.hashCode());
			result = prime * result + ((less == null) ? 0 : less.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Constraint other = (Constraint) obj;
			if (greater == null) {
				if (other.greater != null)
					return false;
			} else if (!greater.equals(other.greater))
				return false;
			if (less == null) {
				if (other.less != null)
					return false;
			} else if (!less.equals(other.less))
				return false;
			return true;
		}
		
		
	}
	
	public static class NoTopologicalOrderAvailableException extends Exception
	{

		public NoTopologicalOrderAvailableException(String string) {
			super(string);
		}
		
	}
	
	
	public static Set<Constraint<String>> getNumericConstraints(Collection<String> values)
	{
		Map<Double, String> constraintKeys = new TreeMap<>();
		
		
		for(String s : values)
		{
			try 
			{
				Double d = Double.valueOf(s);
				constraintKeys.put(d, s);
			} catch(RuntimeException e)
			{
				//Ignore this case
			}
		}
	
		List<Entry<Double, String>> sortedEntries = new ArrayList<>();
		
		sortedEntries.addAll(constraintKeys.entrySet());
		
		Set<Constraint<String>> results = new HashSet<>();
		for(int i=1; i < sortedEntries.size(); i++)
		{
			results.add(new Constraint<String>(sortedEntries.get(i-1).getValue(),sortedEntries.get(i).getValue()));
		}
		
		return results;
	}
	
	
	public static List<String> getTopologicalOrder(Collection<String> values, Collection<Constraint<String>> constraints ) throws NoTopologicalOrderAvailableException
	{
		
		List<String> outputValues = new ArrayList<String>(values.size());
		
		List<String> inputValues = new ArrayList<String>(new TreeSet<String>(values));
		
		List<Constraint<String>> constraintValues = new ArrayList<>(constraints);
		
		constraintValues.addAll(getNumericConstraints(values));
		
	
		
	
nextVariableLoop:			
		for(int i=0; i < inputValues.size(); i++)
		{
			String value = inputValues.get(i);
			for(int j=0; j < constraintValues.size(); j++)
			{
				Constraint<String> constraint = constraintValues.get(j);
				
				if(constraint.getGreater().equals(value))
				{
					//System.out.println(value + " has dependency to: " + constraint.getLesser());
					
					continue nextVariableLoop;
				}
			}
			
			//System.out.println(value + " has no dependencies, consider set");
			
			//System.out.println("input values: " + inputValues);
			inputValues.remove(i);
			i = -1;
			
			//System.out.println("post input values: " + inputValues);
			outputValues.add(value);
			
			for(int j=0; j < constraintValues.size(); j++)
			{
				Constraint<String> constraint = constraintValues.get(j);
				
				if(constraint.getLesser().equals(value))
				{
					constraintValues.remove(j);
					j--;
				}
			}
		
		}
		
		if(inputValues.size() > 0)
		{
			throw new NoTopologicalOrderAvailableException( "Couldn't sort topological order the following values may be involved in a cycle " + inputValues);
		}

		return outputValues;
	}
	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws NoTopologicalOrderAvailableException
	{
		
		{
		String[] values = { "a", "1", "2", "b" , "c" , "3", "3.5", "5" , "4", "INFINITY", "NEGATIVE_INFINITY", "z", "b2"};
		
		Constraint[] constraints = { new Constraint("b","3"),
									new Constraint("5", "INFINITY"),
									new Constraint("1", "b"),
									new Constraint("2", "c"),
									new Constraint("b","2"),
									new Constraint("z","a"),
									new Constraint("a", "NEGATIVE_INFINITY"),
									new Constraint("NEGATIVE_INFINITY", "1")};  
		
		
		System.out.println(getTopologicalOrder(Arrays.asList(values), (List<Constraint<String>>) (Object) Arrays.asList(constraints)));
		}
		
		{
			String[] values = { "Z","a", "1", "2", "b" , "c" , "3", "3.5", "5" , "4", "INFINITY", "NEGATIVE_INFINITY", "z", "b2"};
			
			Constraint[] constraints = { new Constraint("z", "INFINITY")};  
			
			List<String> valuesList = Arrays.asList(values);
			Collections.shuffle(valuesList);
			
			System.out.println(getTopologicalOrder(valuesList, (List<Constraint<String>>) (Object) Arrays.asList(constraints)));
			}
		
	}
}

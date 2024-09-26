package ca.ubc.cs.beta.aeatk.parameterconfigurationspace;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enumeration for conditional statements
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 */
public enum ConditionalOperator 
{

	
	EQ("==",0), NEQ("!=",1), LE("<",2), GR(">",3), IN("in",4);
	
	
	private final String operatorString;
	private final int opCode;
	
	
	ConditionalOperator(String operatorString, int opCode)
	{
		this.operatorString = operatorString;
		this.opCode = opCode;
	}
	
	private static Pattern conditionalMatch = Pattern.compile("^\\s*(\\S+)\\s+(\\S+)\\s+(\\S+|\\{.+\\})\\s*$");
	private static Pattern inSetMatch = Pattern.compile("^\\s*(\\S+)\\s+in\\s+\\{(.+)\\}\\s*$");
	
	static ConditionalOperator getOperatorFromConditionalClause(String clause)
	{
		
		Matcher m = conditionalMatch.matcher(clause);
		
		if(m.find())
		{
			String operator = m.group(2);
			
			for(ConditionalOperator op : ConditionalOperator.values())
			{
				if(op.operatorString.equals(operator))
				{
					return op;
				}
			}
			throw new IllegalArgumentException("Detected conditional operator of \"" + operator + "\" in clause: " +  clause + ", this operator is unsupported only the following operators are legal: " + getAllOperators());
			
			
		}
		
		throw new IllegalArgumentException("Could not find conditional operator in clause: " + clause);
	}
	
	public static String getParent(String clause)
	{
		Matcher m = conditionalMatch.matcher(clause);
		
		if(m.find())
		{
			return m.group(1);
		}
			
		throw new IllegalArgumentException("The following is not a valid conditional clause: " + clause);
	}
	
	private static String getAllOperators()
	{
		StringBuilder sb = new StringBuilder("{");
		for(ConditionalOperator op : ConditionalOperator.values())
		{
			sb.append(op.operatorString).append(",");
		}
		
		sb.setCharAt(sb.length() - 1, '}');
		return sb.toString();
	}

	public static String[] getValues(String clause)
	{
		
		Matcher m = conditionalMatch.matcher(clause);
		ConditionalOperator op = getOperatorFromConditionalClause(clause);
		
		
		/*
		if(!m.find())
		{
			throw new IllegalStateException("Expected to find matched clause already how did this happen: " + clause);
		}
		*/
		
		String[] values = null;
		switch(op)
		{
			case EQ:
			case NEQ:
			case LE:
			case GR:
				values = new String[1];
				
				if(m.find())
				{
					values[0] = m.group(3);
				} else
				{
					throw new IllegalStateException("No match found, this shouldn't happen with clause:" + clause);
				}
				return values;
				/*
				} else if (con.indexOf(" in ") >= 0){
					String[] split = con.split(" in ");
					String values = split[1].trim();
					List<String> values_list = getValues(values);
					
					
					
					value =  values_list.toArray(new String[values_list.size()]);
					
				} else {
					throw new IllegalArgumentException("Unknown conditional operator: "+op+" in pcs file.");
				}
				*/
				
				
			case IN:
				Matcher setValue = inSetMatch.matcher(clause);
				
				
				if(setValue.find())
				{
					String[] args = setValue.group(2).split(",");
					values = new String[args.length];
					Set<String> previousValues = new HashSet<String>();
					
					for(int i=0; i < args.length; i++)
					{
						values[i] = args[i].trim();
						
						if(!previousValues.add(values[i]))
						{
							throw new IllegalArgumentException("Duplicate value in conditional clause detected: " + values[i] + " clause: " + clause);
						}
					}
					
					return values;
				} else
				{
					throw new IllegalArgumentException("Illegal value for \"in\" operator, must contain list of values in set notation e.g., {1,2,3}, failed parsing: " + clause);
				}
				
			default:
				throw new IllegalStateException("Unsupported operator: " + op);
		}
	}
	
	public boolean conditionalClauseMatch(double currentValue , double[] possibleValues)
	{
		
		
		switch (this)
		{
		case EQ:
			return currentValue == possibleValues[0];
		case NEQ:
			return currentValue != possibleValues[0];
		case LE:
			return currentValue < possibleValues[0];
		case GR:
			return currentValue > possibleValues[0];
		case IN:
			
			for(double d : possibleValues)
			{
				if(currentValue == d)
				{
					return true;
				}
			}
			
			return false;
		default:
			throw new IllegalStateException("Unsupported Operation: " + this);
		}

	}
	
	public int getOperatorCode() {
		return opCode;
	}
}
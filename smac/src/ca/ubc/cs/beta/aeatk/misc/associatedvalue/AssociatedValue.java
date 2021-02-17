package ca.ubc.cs.beta.aeatk.misc.associatedvalue;

/**
 * AssociatedValue allows you to 
 * 
 * Note: At some point we refactor this class to not use Comparables, and have a subtype use this
 * 
 * @author Steve Ramage <seramage@cs.ubc.ca>
 *
 * @param <T>
 * @param <V>
 */
public class AssociatedValue<T extends Comparable<T>,V> implements Comparable<AssociatedValue<T,?>>{
	
	private final T Tobj;
	private final V Vobj;
	/**
	 * Standard Constructor
	 * @param t Comparabale to Associate with Value (Key)
	 * @param v Value 
	 */
	public AssociatedValue(T t, V v)
	{
		Tobj = t;
		Vobj = v;
	}
	
	/**
	 * Retrieves the value of this object 
	 * @return value object
	 */
	public V getValue()
	{
		return Vobj;
	}
	/**
	 * Retrieves the associated value (Comparable) of this object (Key)
	 * @return associated value
	 */
	public T getAssociatedValue()
	{
		return Tobj;
	}

	@Override
	/**
	 * When comparaing these we consider the objects equal if there associated values are equal
	 * 
	 */
	public int compareTo(AssociatedValue<T, ?> o) {
		return Tobj.compareTo(o.Tobj);
	}
	
	@Override
	public String toString()
	{
		return Tobj.toString() + " => " + Vobj.toString();
	}
	
	@Override
	/**
	 * Two associated values are the same if the associated value they point two are the same.
	 */
	public boolean equals(Object o)
	{
		if(o instanceof AssociatedValue<?,?>)
		{
			AssociatedValue<?,?> oValue = (AssociatedValue<?, ?>) o;
			return Tobj.equals(oValue.Tobj);
		} else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return Vobj.hashCode() ^ Tobj.hashCode();
	}


}

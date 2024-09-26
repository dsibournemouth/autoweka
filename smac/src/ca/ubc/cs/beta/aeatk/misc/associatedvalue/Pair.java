package ca.ubc.cs.beta.aeatk.misc.associatedvalue;

/**
 * The Pair class is an immutable pairing of two objects
 * 
 * @author geschd
 *
 * @param <T>
 * @param <V>
 */
public class Pair<T,V> {
	
	private final T Tobj;
	private final V Vobj;
	/**
	 * Standard Constructor
	 * @param t Value
	 * @param v Value 
	 */
	public Pair(T t, V v)
	{
		Tobj = t;
		Vobj = v;
	}

	/**
	 * Retrieves the first object this object
	 * @return associated value
	 */
	public T getFirst()
	{
		return Tobj;
	}
	/**
	 * Retrieves the second object in the pair
	 * @return second object
	 */
	public V getSecond()
	{
		return Vobj;
	}
	
	@Override
	public String toString()
	{
		return "<"+Tobj.toString() + ", " + Vobj.toString()+">";
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((Tobj == null) ? 0 : Tobj.hashCode());
        result = prime * result + ((Vobj == null) ? 0 : Vobj.hashCode());
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
        Pair other = (Pair) obj;
        if (Tobj == null) {
            if (other.Tobj != null)
                return false;
        } else if (!Tobj.equals(other.Tobj))
            return false;
        if (Vobj == null) {
            if (other.Vobj != null)
                return false;
        } else if (!Vobj.equals(other.Vobj))
            return false;
        return true;
    }
	





}

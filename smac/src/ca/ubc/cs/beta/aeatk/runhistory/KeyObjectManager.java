package ca.ubc.cs.beta.aeatk.runhistory;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

/**
 * Helper class that roughly allows you to associate with entries an index. This is a proto-List/Set that lets you get the index 
 * of an object in O(1) time. Additionally each element can appear only once in the list.
 * 
 * This class is now thread safe
 * 
 * @author seramage
 *
 * @param <V> object to associate the id with
 */
@ThreadSafe
public class KeyObjectManager<V> {

	/**
	 * Bidirectional / Bijective Map (enforces unique values, and allows O(1) lookup of key from Value)
	 */
	private final BidiMap bidiMap;

	private int nextID;
	
	private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();
	
	/**
	 * Default Constructor 
	 */
	public KeyObjectManager()
	{
		this(1);
	}
	
	/**
	 * Constructor
	 * @param firstID firstID to assign to objects
	 */
	public KeyObjectManager(int firstID)
	{
		nextID = firstID;
		bidiMap = new DualHashBidiMap();
	}
	
	/**
	 * Add the object to Collection with the specified id
	 * @param id 	id to associate with object
	 * @param obj	object to store
	 */
	public void add(Integer id, V obj)
	{
		try {
			myLock.writeLock().lock();
			
			if(bidiMap.containsKey(id) || bidiMap.containsValue(obj))
			{
				System.out.println("Trying to add < " + id + "," + obj + ">");
				if(bidiMap.containsKey(id))
				{
					System.out.println("bidiMap contains key " + id +  " already");
					System.out.println("Value : " + bidiMap.get(id));
				}
				
				if(bidiMap.containsValue(obj))
				{
					System.out.println("bidiMap contains value " + obj +  " already");
					System.out.println("Key: " + bidiMap.getKey(obj));
				}
				
				try {
				if(bidiMap.get(id).equals(obj)) 
				{
					return;
				} else
				{
					if(bidiMap.containsKey(id)) throw new IllegalArgumentException("Cannot replace index");
					if(bidiMap.containsValue(obj)) throw new IllegalArgumentException("Cannot replace value");
				}
				} catch(NullPointerException e)
				{
					System.err.println("Help me data structure corruption caused by RACE CONDITION!");
					throw e;
				}
			} 
			
			bidiMap.put(id, obj);
		} finally
		{
			myLock.writeLock().unlock();
		}
		
	}
	
	/**
	 * Appends an object to the map
	 * <b>NOTE:</b> You cannot use this method and the add() method together, and get consistent results
	 * @param obj	object to add
	 * @return	the id associated with the object
	 */
	public int append(V obj)
	{
		try {
			myLock.writeLock().lock();
		
			add(nextID++, obj);
			return nextID-1;
		} finally
		{
			myLock.writeLock().unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Gets the value from the collection
	 * @param key
	 * @return object associated with the key
	 */
	public V getValue(int key)
	{
		try {
			myLock.readLock().lock();
			return (V) bidiMap.get(key);
		} finally
		{
			myLock.readLock().unlock();
		}
	}
	
	/**
	 * Gets the key for a given object
	 * @param obj object to find key of
	 * @return	key
	 */
	public int getKey(V obj)
	{
		try {
			myLock.readLock().lock();
			
			Integer i = (Integer) bidiMap.getKey(obj);
			if(i == null)
			{
				return -1;
			} else
			{
				return i;
			}
		} finally
		{
			myLock.readLock().unlock();
		}
	}
	
	/**
	 * Get or create the key if it doesn't already exist
	 * @param obj	object to store
	 * @return	key
	 */
	public int getOrCreateKey(V obj)
	{
		if(obj == null)
		{
			throw new IllegalStateException("Can't write a null key");
		}
		
	
		try {
			myLock.writeLock().lock();	
			myLock.readLock().lock();
			if(bidiMap.containsValue(obj))
			{ 
				//Downgrade lock
				
				myLock.writeLock().unlock();
			
				return (Integer) bidiMap.getKey(obj);
			} else
			{
				return append(obj);
			}
		} finally
		{
			if(myLock.writeLock().isHeldByCurrentThread())
			{
				myLock.writeLock().unlock();
			}
			myLock.readLock().unlock();
		}
		
	}

	/**
	 * Size of the collection
	 * @return number of entities we have mapped
	 */
	public int size() {
		try {
			myLock.readLock().lock();
		return bidiMap.size();
		} finally
		{
			myLock.readLock().unlock();
		}
	}
}

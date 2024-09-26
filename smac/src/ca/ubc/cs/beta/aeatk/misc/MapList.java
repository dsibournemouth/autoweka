package ca.ubc.cs.beta.aeatk.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class MapList<K,V> implements Map<K, List<V>> {

	private final Map<K, List<V>> map;

	public static final <K,V> MapList<K,V> getHashMapList()
	{
		return new MapList<K,V>(new HashMap<K,List<V>>());
	}
	public static final <K,V> MapList<K,V> getLinkedHashMapList()
	{
		return new MapList<K,V>(new LinkedHashMap<K,List<V>>());
	}
	public static final <K,V> MapList<K,V> getTreeMapList()
	{
		return new MapList<K,V>(new TreeMap<K,List<V>>());
	}
	/**
	 *  DO NOT ADD A CONCURRENT HASH MAP AS THIS CLASS IS NOT THREAD SAFE
	 */

	public MapList(Map<K, List<V>> emptyMap)
	{
		if(emptyMap == null || emptyMap.size() > 0)
		{
			throw new IllegalArgumentException("Supplied map must be empty");
		}
		this.map=emptyMap;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		// 
		throw new UnsupportedOperationException("This method isn't supported");
	}

	@Override
	public List<V> get(Object key) {
		return map.get(key);
	}

	@Override
	public List<V> put(K key, List<V> value) {
		throw new UnsupportedOperationException("This method isn't supported");
	}

	@Override
	public List<V> remove(Object key) {
		return map.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends List<V>> m) {
		for(java.util.Map.Entry<? extends K, ? extends List<V>> ent : m.entrySet())
		{
			this.addAllToList(ent.getKey(), ent.getValue());
		}
		
	}

	@Override
	public void clear() {
		map.clear();
		
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<List<V>> values() {
		return map.values();
	}

	@Override
	public Set<java.util.Map.Entry<K, List<V>>> entrySet()
	{
		return map.entrySet();
	}
	
	
	public List<V> getList(K key)
	{
		
		if(map.get(key) == null)
		{
			map.put(key, new ArrayList<V>());
		}
		
		return map.get(key);
		
	}
	public void addToList(K key, V value)
	{
		if(map.get(key) == null)
		{
			map.put(key, new ArrayList<V>());
		}
		
		map.get(key).add(value);
	}
	
	public void addAllToList(K key, Collection<V> value)
	{
		if(map.get(key) == null)
		{
			map.put(key, new ArrayList<V>());
		}
		
		List<V> vList =  map.get(key);
		vList.addAll(value);
	}
	
	public int hashCode()
	{
		return map.hashCode();
	}
	
	public boolean equals(Object o)
	{
		if(o instanceof MapList)
		{
			return ((MapList<?, ?>) o).map.equals(map);
		} else
		{
			return false;
		}
	}
	
	public String toString()
	{
		return map.toString();
	}
}

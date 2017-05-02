import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Extension of the standard java map collection which allows keys and values access each other. This assumes that
 * the data being stored is in a 1:1 ratio. I will be using this to map point objects to nodes and vice-versa.
 * @author Jonathan Yin
 *
 * @param <K> The first Key value to be stored.
 * @param <V> The second Key value to be stored.
 */
public class BiMap<K,V> {
	private Map<K,V> FMap;
	private Map<V,K> BMap;
	
	public BiMap()
	{
		FMap = new HashMap<K,V>();
		BMap = new HashMap<V,K>();
	}
	
	public void put(K key, V value)
	{
		FMap.put(key, value);
		BMap.put(value, key);
	}
	
	public Object getForward(Object key)
	{
		return FMap.get(key);
	}
	
	public Object getBackward(Object key)
	{
		return BMap.get(key);
	}
	
	public Set<K> keySet()
	{
		return FMap.keySet();
	}
	
	public Collection<V> valueSet()
	{
		return FMap.values();
	}
	
	public void clear()
	{
		FMap.clear();
		BMap.clear();
	}

	public String toString()
	{
		return BMap.toString();
	}
}

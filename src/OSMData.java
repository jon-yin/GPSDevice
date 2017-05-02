import java.util.HashMap;
import java.util.Map;

/**
 * Represents a piece of data that is parsed throughout an OSM file. These data points share common traits
 * such as an ID,and a list of tags that correspond to it.
 * @author Jonathan Yin
 *
 */
public class OSMData {

	private String id;
	private Map<String, String> tags;
	
	public OSMData(String id)
	{
		this.id = id;
		tags = new HashMap<String, String>();
	}
	
	
	/**
	 * Gets the id of this OSMData as parsed in the OSM file.
	 * @return
	 */
	public String getID()
	{
		return id;
	}
	
	/**
	 * Obtains the tag value associated with a tag key such as name, highway,barrier.
	 * @param key The tag key.
	 * @return The tag value associated with this tag key.
	 */
	public String getTag(String key)
	{
		return tags.get(key);
	}
	
	/**
	 * Adds a tag to the map by specifiying the tag's key and its corresponding value
	 * @param key The key of the tag to be added.
	 * @param value The value of the tag to be added.
	 */
	public void addTag(String key,String value)
	{
		tags.put(key,value);
	}
	
	/**
	 * Equals method which is necessary for both Hashmaps and tests of equality.
	 * I assume that what separates one piece of OSMData from another is its id, i.e
	 * each node, way, and relation is a unique id.
	 */
	@Override
	public boolean equals (Object other)
	{
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (getClass() != other.getClass())
			return false;
		OSMData otherData = (OSMData) other;
		return id.equals(otherData.id);
	}
	
	/**
	 * hashCode function overwritten to allow support with HashMaps
	 */
	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
}

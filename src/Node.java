/**
 * Represents a Node that is parsed from OSMData. This type of data is significant as it actually contains
 * a latitude and longitude allowing it to be displayed in a specific location.
 * @author Jonathan Yin
 *
 */
public class Node extends OSMData{

	private double latitude;
	private double longitude;
	
	public Node(double latitude, double longitude)
	{
		super("Dummy Node");
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Node(String id, double latitude, double longitude)
	{
		super(id);
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/**
	 * Returns the latitude of this Node.
	 * @return the latitude of the node.
	 */
	public double getLatitude()
	{
		return latitude;
	}
	/**
	 * Returns the longitude of this Node.
	 * @return The longitude of this Node.
	 */
	
	public double getLongitude()
	{
		return longitude;
	}
	
	/**
	 * Returns the distance from another node to this node.
	 * @param other The other node to measure distance towards. This algorithm uses Haversine's algorithm.
	 * @return The distance between the two nodes.
	 */
	public double distanceTo(Node other)
	{
		long radius = 6371000;
		double latRad = Math.toRadians(latitude);
		double latRad2 = Math.toRadians(other.getLatitude());
		double deltaLatRad = Math.toRadians(other.getLatitude() - latitude);
		double deltaLonRad = Math.toRadians(other.getLongitude() - longitude);
		double term1 = Math.pow(Math.sin(deltaLatRad/2), 2) + Math.cos(latRad) *Math.cos(latRad2) *Math.pow(Math.sin(deltaLonRad/2), 2);
		double term2 = 2 * Math.atan2(Math.sqrt(term1), Math.sqrt(1-term1));
		return radius * term2;
	}
	
}

import java.util.ArrayList;
import java.util.List;

/**
 * A way in an OSMFile which represents a collection of nodes which together represent a road/highway. 
 * @author Jonathan Yin
 *
 */
public class Way extends OSMData{

	private List<Node> nodes;
	
	public Way()
	{
		super("Generated Directions Way");
		nodes = new ArrayList<Node>();
	}
	
	public Way(String id)
	{
		super(id);
		nodes = new ArrayList<Node>();
	}
	
	/**
	 * Returns the list of nodes that are contained in this way.
	 * @return List of nodes that comprise this way.
	 */
	public List<Node> getNodes()
	{
		return nodes;
	}
	/**
	 * Adds a node to the list of nodes that comprise this way
	 * @param node Node to be added
	 */
	public void addNode(Node node)
	{
		nodes.add(node);
	}
	/**
	 * Determines whether if this node is part of this way.
	 * @param node The node to be tested.
	 * @return true if the nodes is contained in this way, false otherwise.
	 */
	public boolean containsNode(Node node)
	{
		return nodes.contains(node);
	}
	
	/**
	 * Adds a node to the beginning of this way. Utilized in order to place the current Location in a directions Path.
	 * @param node the node to add to the beginning of this way.
	 */
	
	public void addHead(Node node)
	{
		nodes.add(0, node);
	}
	
}

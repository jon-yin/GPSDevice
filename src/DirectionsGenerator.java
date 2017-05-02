import java.awt.Dialog;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.starkeffect.highway.GPSEvent;
import com.starkeffect.highway.GPSListener;

/**
 * A class that will calculate directions based on two locations that have been
 * entered into the directions generator. This will be used in order to support
 * both driveThere mode as well as handling general directions.
 * 
 * @author Jonathan Yin
 *
 */
public class DirectionsGenerator implements DataModelObserver, GPSListener {
	// The dataModel that this DirectionsGenerator is based off of.
	private MapDataModel model;
	private Way generatedPath;
	private boolean driveThere;
	private MapDisplay display;
	private Node driveDestination = null;
	// Arbitrary amount to determine if the current User location is out of
	// range of the generated Path.
	private final double OUT_OF_RANGE = 30;
	private final double DESTINATION_RANGE = 10;

	public DirectionsGenerator(MapDataModel model, MapDisplay display) {
		this.model = model;
		generatedPath = null;
		driveThere = false;
		this.display = display;
	}

	/**
	 * Parses two strings which represent node names, id's, or requests to
	 * utilize the currentLocation. This will then generate a path between the
	 * two nodes represented by these Strings.
	 * 
	 * @param start
	 *            String representing the starting node.
	 * @param dest
	 *            String representing the destination node.
	 * @return A way between the start and dest nodes or Null if no such path
	 *         exists or if the start and end nodes are the same.
	 */
	public Way parseString(String start, String dest) {
		Node startNode = (Node) model.getDataPoint(start);

		Node endNode = (Node) model.getDataPoint(dest);

		if (startNode.equals(endNode)) {
			return null;
		}

		return generateDirections(startNode, endNode);

	}

	/**
	 * Creates a DirectionsPath which will contain lines from the start point to
	 * the destination
	 * 
	 * @param start
	 *            The starting Location
	 * @param dest
	 *            The ending Location
	 * @param driveThere
	 *            Whether of not to enable driveThere mode.
	 * @return A way of nodes which will highlight the way from the startNode to
	 *         the endNode or null if no such path exists.
	 */
	public Way generateDirections(Node start, Node dest) {
		// Implementing Djkstra's algorithm.
		List<Node> nodes = model.getNodes();
		List<Way> ways = model.getWays();
		Node LastNode = start;
		List<Node> visited = new ArrayList<Node>();
		// Keeps track of the last node of the shortest path to each of these
		// nodes
		Map<Node, Node> predecessors = new HashMap<Node, Node>();
		HashMap<Node, Double> distances = new HashMap<Node, Double>();
		for (Node node : nodes) {
			distances.put(node, Double.POSITIVE_INFINITY);
			distances.put(start, 0.0);
			// Assumed at start that predecessor node is the start node at
			// first.
			predecessors.put(node, start);
		}
		predecessors.put(start, null);
		while (!(LastNode.equals(dest)) && (visited.size() != nodes.size())) {
			// Look through each way, If it contains the node we are checking,
			// find the distance from the node immediately behind and in front
			// of it.
			for (Way way : ways) {
				if (way.containsNode(LastNode)) {
					List<Node> wayNodes = way.getNodes();
					int index = wayNodes.indexOf(LastNode);
					Node prevNode = null, postNode = null;
					if (index == 0) {
						postNode = wayNodes.get(index + 1);
					} else if (index == wayNodes.size() - 1) {
						prevNode = wayNodes.get(index - 1);
					} else {
						prevNode = wayNodes.get(index - 1);
						postNode = wayNodes.get(index + 1);
					}
					double distanceToPrev = Double.POSITIVE_INFINITY, distanceToPost = Double.POSITIVE_INFINITY;
					if (!(prevNode == null)) {
						distanceToPrev = LastNode.distanceTo(prevNode);
					}
					if (!(postNode == null)) {
						distanceToPost = LastNode.distanceTo(postNode);
					}
					// Update predecessors if needed.
					double distanceSumPrev = distanceToPrev + distances.get(LastNode);
					double distanceSumPost = distanceToPost + distances.get(LastNode);
					if (!(prevNode == null)) {
						if (distanceSumPrev < distances.get(prevNode)) {
							distances.put(prevNode, distanceSumPrev);
							predecessors.put(prevNode, LastNode);
						}
					}
					if (!(postNode == null)) {
						if (distanceSumPost < distances.get(postNode)) {
							distances.put(postNode, distanceSumPost);
							predecessors.put(postNode, LastNode);
						}
					}

				}

			}
			// Look for next shortest distance Node.
			nodes.remove(LastNode);
			visited.add(LastNode);
			Node shortestNode = null;
			double shortestDistance = Double.POSITIVE_INFINITY;
			for (Node node : nodes) {
				double nodeDistance = distances.get(node);
				if (nodeDistance < shortestDistance) {
					shortestDistance = nodeDistance;
					shortestNode = node;
				}
			}
			LastNode = shortestNode;
		}
		if (distances.get(dest) == Double.POSITIVE_INFINITY) {
			JOptionPane optionPane = new JOptionPane("There doesn't exist a path between these two points");
			JDialog dialog = optionPane.createDialog("No path found");
			dialog.setModalityType(Dialog.ModalityType.MODELESS);
			dialog.setVisible(true);
			return null;
		}
		Way destWay = new Way();
		Stack<Node> stack = new Stack<Node>();
		Node destination = dest;
		while (dest != null) {
			stack.push(dest);
			dest = predecessors.get(dest);
		}
		while (!stack.isEmpty()) {
			destWay.addNode(stack.pop());
		}
		destWay.addNode(destination);
		generatedPath = destWay;
		display.setDirectionsPath(generatedPath);
		return destWay;
	}

	/**
	 * Gets the closest location from a specified latitude and long value.
	 * 
	 * @param lat
	 *            The latitude to get the closest point from.
	 * @param lon
	 *            The longitude to get the closest point from.
	 * @return The closest location from that point.
	 */

	public Node getClosestLocation(double lat, double lon) {
		Node givenNode = new Node(lat, lon);
		List<Node> nodes = model.getNodes();
		double minDistance = Double.POSITIVE_INFINITY;
		Node shortestNode = null;
		for (Node node : nodes) {
			if (node.distanceTo(givenNode) < minDistance) {
				minDistance = node.distanceTo(givenNode);
				shortestNode = node;
			}
		}
		return shortestNode;
	}

	/**
	 * This method will simulate driving to a destination by periodically
	 * checking the User's new location and redrawing it. The path will remain
	 * unchanged until the user goes offcourse of the Directions Path.
	 */
	public void driveThere() {
		driveThere = true;
	}

	/**
	 * Determines whether if the user is still on course on the Directionspath
	 * generated, the DirectionsGenerator will display an warning as well as
	 * recalculate directions if it does not. Used for driveThere mode.
	 * 
	 * @param node
	 *            The node which represents the currentLocation of the user.
	 * @return true if user is still on course. false if not.
	 */

	public boolean onCourse(Node node) {
		//Transform into map display coordinates.
		Node center = model.getCenterNode();
		double latitudeOffset = center.getLatitude();
		double longitudeOffset = center.getLongitude();
		double adjustedLat = node.getLatitude() - latitudeOffset;
		double adjustedLon = node.getLongitude() - longitudeOffset;
		adjustedLat *= -1;
		double convertedLat = node.getLatitude() * (Math.PI / 180.0);
		adjustedLon *= MapDisplay.PIXELS_PER_DEGREE * Math.cos(convertedLat);
		adjustedLat *= MapDisplay.PIXELS_PER_DEGREE;
		Point2D.Double currentLoc = new Point2D.Double( adjustedLon,  adjustedLat);
		if (generatedPath != null) {
			List<Node> wayPoints = generatedPath.getNodes();
			for (int i = 0; i < wayPoints.size() - 1; i++) {
				double latitude1 = wayPoints.get(i).getLatitude();
				double longitude1 = wayPoints.get(i).getLongitude();
				double latitude2 = wayPoints.get(i+1).getLatitude();
				double longitude2 = wayPoints.get(i+1).getLongitude();
				double adjustedLat1 = latitude1 - latitudeOffset;
				double adjustedLat2 = latitude2 - latitudeOffset;
				double adjustedLon1 = longitude1 - longitudeOffset;
				double adjustedLon2 = longitude2 - longitudeOffset;
				adjustedLat1 *= -1;
				adjustedLat2 *= -1;
				double lat1Rad = Math.toRadians(latitude1);
				double lat2Rad = Math.toRadians(latitude2);
				adjustedLon1 *= MapDisplay.PIXELS_PER_DEGREE * Math.cos(lat1Rad);
				adjustedLon2 *= MapDisplay.PIXELS_PER_DEGREE * Math.cos(lat2Rad);
				adjustedLat1 *= MapDisplay.PIXELS_PER_DEGREE;
				adjustedLat2 *= MapDisplay.PIXELS_PER_DEGREE;
				Point2D pointOne = new Point2D.Double(adjustedLon1, adjustedLat1);
				Point2D pointTwo = new Point2D.Double(adjustedLon2, adjustedLat2);
				Line2D.Double line = new Line2D.Double(pointOne, pointTwo);
				if(line.ptSegDist(currentLoc) < OUT_OF_RANGE)
					{
					return true;
					};
			}

		}
		return false;
	}

	/**
	 * Remove all traces of the old map data model from this directions
	 * generator, this includes clearing any drive there options selected, and
	 * loading in a new MapDataModel
	 */
	@Override
	public void reset() {
		generatedPath = null;
		driveThere = false;

	}

	/**
	 * Only utilized when using drive there mode in order to track the user's
	 * current location. This method will keep track to see if the user is
	 * currently ontrack of the path generated by the directionsGenerator,
	 * Create Warning Messages as well as regenerate a directions Path if
	 * needed.
	 */
	@Override
	public void processEvent(GPSEvent ev) {
		if (driveThere) {
			Node currentLocation = new Node(ev.getLatitude(), ev.getLongitude());
			if (driveDestination != null) {
				display.setIsDriving(true);
				if (!onCourse(currentLocation) && !(generatedPath == null)) {
					JOptionPane optionPane = new JOptionPane("Off Course, rerouting. . .");
					JDialog dialog = optionPane.createDialog("Off Course");
					dialog.setModalityType(Dialog.ModalityType.MODELESS);
					dialog.setVisible(true);
				}
				if ((generatedPath == null) || !onCourse(currentLocation)) {
					Node closestLocation = getClosestLocation(ev.getLatitude(), ev.getLongitude());
					generateDirections(closestLocation, driveDestination);
					generatedPath.addHead(currentLocation);
					display.setDirectionsPath(generatedPath);
				}
				//Check if we are close to our destination to a certain point, if we are, then stop the drive there mode.
				if (currentLocation.distanceTo(driveDestination) <= DESTINATION_RANGE)
				{
					JOptionPane.showMessageDialog(null, "You have arrived at your destination.");
					driveThere = false;
					display.setIsDriving(false);
					
				}
			}
			else
			{
				JOptionPane.showMessageDialog(null, "No ending destination selected, please select an ending destination and press the drive there button");
				driveThere = false;
				display.setIsDriving(false);
			}
		}
	}

	/**
	 * Removes the generated path from display and the directions generator.
	 */

	public void cancel() {
		driveThere = false;
		generatedPath = null;
		display.setIsDriving(false);
		display.setDirectionsPath(null);
	}

	/**
	 * Used for drive there mode in order to determine when the generator should
	 * finish and stop when driving to a destination
	 * 
	 * @param destination
	 *            The destination for the drive there mode.
	 */
	public void driveDestination(Node destination) {
		driveDestination = destination;
	}

}

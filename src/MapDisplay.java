import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.starkeffect.highway.GPSEvent;
import com.starkeffect.highway.GPSListener;

/**
 * This class will be responsible for displaying the map that is represented by
 * the OSMFile. This class is also responsible for panning and zooming and later
 * for location tracking.
 * 
 * @author Jonathan Yin
 *
 */
public class MapDisplay extends JPanel implements DataModelObserver, GPSListener {

	// Data model to get nodes, ways, relations from.
	private MapDataModel model;
	// What data should be displayed based on current Zoom level. This primarily
	// focuses on what points are displayable.
	private List<Point> visiblePoints;
	// The value that a point of latitude and longitude is equal to in pixel
	// units
	public static final double PIXELS_PER_DEGREE = 20000;
	// The zoom amount 1.0 is default
	private double zoom = 1.0;
	// Minimum Zoom Amount
	public final double MINIMUM_ZOOM = 0.2;
	// Maximum Zoom Amount
	public final double MAXIMUM_ZOOM = 5.0;
	// Significance value is related to the zoom level of the Display, we
	// distinguish between 3 levels
	private int level = 2;
	// The current location of the user.
	private Point currentLocation = null;
	// Used later for Project part three to allow selection of locations based
	// on Mouse Click. No longer needed.
	// private Location selectedLocation;
	// Corresponds to the amount that this zoom has panned, used for panning.
	private int panX = 0, panY = 0;
	// Map points to nodes to allow obtaining nodes from points;
	private BiMap<Node, Point> map;
	// Directions way that is displayed when directions are requested.
	private Way directionsWay = null;
	// Initial default panel size for the Map display
	private final int INITIAL_PANEL_SIZE = 600;
	// Location of the mouse on the JPanel, initialized to a sentinel point.
	private Point currentMouse = new Point();
	// Strokes and colors to differentiate between different types of ways.
	private final BasicStroke thinStroke = new BasicStroke(1);
	private final BasicStroke medStroke = new BasicStroke(3);
	private final BasicStroke thickStroke = new BasicStroke(5);
	private final BasicStroke thickestStroke = new BasicStroke(7);
	private final Color HIGH_LEVEL_BOUNDARY = Color.RED;
	private final Color MEDIUM_LEVEL_BOUNDARY = Color.GREEN;
	private final Color LOW_LEVEL_BOUNDARY = Color.YELLOW;
	private final Color WATERWAY = Color.cyan;
	private final Color DIRECTIONS = Color.MAGENTA;
	private final int BOUNDS_WIDTH = 5;
	// To determine if we are still using the GPS system in order to drive.
	private boolean isDriving;

	public MapDisplay(MapDataModel model) {
		this.model = model;
		setPreferredSize(new Dimension(INITIAL_PANEL_SIZE, INITIAL_PANEL_SIZE));
		map = new BiMap<Node, Point>();
		visiblePoints = new ArrayList<Point>();
		isDriving = false;
	}

	/**
	 * Initialize will be used in order to set up the initial nodes locations as
	 * well as the initial display.
	 */
	public void initialize() {
		assignPointCoordinates();
		findVisiblePoints();
		paintComponent(getGraphics());
	}

	/**
	 * This method will properly assign each Node to a point which is scaled
	 * properly with respect to our scale value Called when dataModel is changed
	 * and the current Nodes, Ways, and Relations no longer apply.
	 */
	public void assignPointCoordinates() {
		Node center = model.getCenterNode();
		double latitudeOffset = center.getLatitude();
		double longitudeOffset = center.getLongitude();
		List<Node> nodes = model.getNodes();
		for (Node node : nodes) {
			double adjustedLat = node.getLatitude() - latitudeOffset;
			double adjustedLon = node.getLongitude() - longitudeOffset;
			adjustedLat *= -1;
			double convertedLat = node.getLatitude() * (Math.PI / 180.0); // Converting
																			// latitude
																			// measurement
																			// into
																			// radiants
			adjustedLon *= PIXELS_PER_DEGREE * Math.cos(convertedLat);
			adjustedLat *= PIXELS_PER_DEGREE;
			adjustedLat += (INITIAL_PANEL_SIZE / 2);
			adjustedLon += (INITIAL_PANEL_SIZE / 2);
			Point point = new Point((int) adjustedLon, (int) adjustedLat);
			map.put(node, point);
		}
	}

	/**
	 * PaintComponent will draw the MapDisplay with its points and
	 */

	/**
	 * This method will pan the display based off the amount that the mouse is
	 * dragged on the MapDisplay.
	 * 
	 * @param panAmountX
	 *            The amount to pan the display in the X direction
	 * @param panAmountY
	 *            The amount to pan the display in the Y direction
	 */
	public void pan(int panAmountX, int panAmountY) {
		panX += panAmountX;
		panY += panAmountY;
	}

	/**
	 * This method will zoom in the display by the amount that the mouse wheel
	 * is moved. This will change the value of scale and thus also change to
	 * coordinates of each Node point
	 * 
	 * @param zoomAmount
	 *            The amount to zoom the display by.
	 */
	public void zoom(int rotations) {
		double newZoom = zoom - (rotations * 0.1);
		if (newZoom > MAXIMUM_ZOOM) {
			newZoom = MAXIMUM_ZOOM;
		} else if (newZoom < MINIMUM_ZOOM) {
			newZoom = MINIMUM_ZOOM;
		}
		double ratio = newZoom / zoom;
		panX *= ratio; // Update pan amount.
		panY *= ratio;
		zoom = newZoom;
		// Farthest out zoom
		if (zoom < 0.5) {
			level = 1;
		}
		// Moderate Zoom
		else if (zoom < 2.0) {
			level = 2;
		}
		// Closest zoom.
		else {
			level = 3;
		}

	}

	/**
	 * Determines what node was clicked on by the mouse.
	 * 
	 * @param clickedPoint
	 *            The point that the mouse clicked on.
	 */
	public Node findClickedLocation(Point clickedPoint) {
		// Look for the point with the shortest distance to our clicked point.
		int min = Integer.MAX_VALUE;
		Map<Integer, Point> mapPoints = new HashMap<Integer, Point>();
		for (Point point : visiblePoints) {
			Point adjustedPoint = new Point((int) (point.x * zoom + (panX + getWidth() / 2)),
					(int) (point.y * zoom + (panY + getHeight() / 2)));
			int distance = (int) (adjustedPoint.distance(clickedPoint));
			mapPoints.put(distance, point);
			if (distance < min) {
				min = distance;
			}

		}
		Point closestPoint = mapPoints.get(min);
		Node obtainedNode = (Node) (map.getBackward(closestPoint));
		return obtainedNode;
	}

	/**
	 * Resets the data model for this MapDisplay and clears all relavent data
	 * models used.
	 */
	@Override
	public void reset() {
		visiblePoints.clear();
		zoom = 1.0;
		level = 2;
		panX = 0;
		panY = 0;
		map.clear();
		currentMouse = new Point();
		directionsWay = null;
		isDriving = false;
		initialize();
	}

	/**
	 * Fills the Visible points list with points that will actually be displayed
	 * in the JPanel
	 */
	public void findVisiblePoints() {
		visiblePoints.clear();
		Collection<Point> points = map.valueSet();
		List<Point> allPoints = new ArrayList<Point>();
		allPoints.addAll(points);
		if (currentLocation != null)
			allPoints.add(currentLocation);
		for (Point point : allPoints) {
			Point adjustedPoint = new Point((int) (point.x * zoom + (panX + getWidth() / 2)),
					(int) (point.y * zoom + (panY + getHeight() / 2)));
			if (contains(adjustedPoint)) {
				visiblePoints.add(point);

			}
		}
		if (!visiblePoints.contains(currentLocation) && currentLocation != null && isDriving) {
			pan(currentLocation);
		}
		visiblePoints.remove(currentLocation);

	}

	/**
	 * Pans the display such that a point becomes the center focus of the
	 * display.
	 * 
	 * @param center
	 *            The new centerPoint of the display.
	 */
	private void pan(Point center) {
		panX = -(int) (center.x * zoom);
		panY = -(int) (center.y * zoom);

	}

	/**
	 * Paints the MapDisplay
	 */
	@Override
	public void paintComponent(Graphics g) {
		findVisiblePoints();
		List<Way> ways = model.getWays();
		Graphics2D graphics = (Graphics2D) g;
		graphics.translate(panX, panY);
		graphics.translate(getWidth() / 2, getHeight() / 2);
		graphics.scale(zoom, zoom);
		for (Point point : visiblePoints) {
			if (level == 3) {
				if (((Node) (map.getBackward(point))).getTag("name") != null) {
					graphics.setColor(Color.RED);
				} else
					graphics.setColor(Color.BLACK);
				Ellipse2D.Double ellipse = new Ellipse2D.Double(point.getX() - 5, point.getY() - 5, 10, 10);
				int coorX = (int) (point.x * zoom + (panX + getWidth() / 2));
				int coorY = (int) (point.y * zoom + (panY + getHeight() / 2));
				int cornerOffset = (int) (BOUNDS_WIDTH * zoom) / 2;
				Rectangle rectBounds = new Rectangle(coorX - cornerOffset, coorY - cornerOffset,
						(int) (BOUNDS_WIDTH * zoom), (int) (BOUNDS_WIDTH * zoom));
				if (rectBounds.contains(currentMouse)) {
					Node node = (Node) map.getBackward(point);
					setToolTipText(node.getTag("name"));
				} else {
					setToolTipText(null);
				}
				graphics.draw(ellipse);
			}
		}
		// Display standard ways
		for (Way way : ways) {
			String roadType = way.getTag("highway");
			String boundaryLevel = way.getTag("admin_level");
			String water = way.getTag("waterway");
			if (boundaryLevel != null) {
				Integer adminlevel = Integer.parseInt(boundaryLevel);
				graphics.setStroke(thickestStroke);
				if (adminlevel <= 3) {
					graphics.setColor(HIGH_LEVEL_BOUNDARY);
					displayWay(way, graphics);
				} else if (adminlevel <= 6) {
					graphics.setColor(MEDIUM_LEVEL_BOUNDARY);
					displayWay(way, graphics);
				} else {
					graphics.setColor(LOW_LEVEL_BOUNDARY);
					displayWay(way, graphics);
				}
			} else if (water != null) {
				graphics.setStroke(medStroke);
				graphics.setColor(WATERWAY);
				displayWay(way, graphics);
			} else {
				graphics.setColor(Color.BLACK);
				// Assume non tagged ways are residential.
				if (level == 1) {
					if (roadType == null) {

					} else if (roadType.equals("motorway") || roadType.equals("trunk") || roadType.equals("primary")) {
						graphics.setStroke(medStroke);
						displayWay(way, graphics);
					} else if (roadType.equals("secondary") || roadType.equals("tertiary")) {
						graphics.setStroke(thinStroke);
						displayWay(way, graphics);
					}
				} else if (level == 2) {
					if (roadType == null) {
						graphics.setStroke(thinStroke);
						displayWay(way, graphics);
					} else if (roadType.equals("motorway") || roadType.equals("trunk") || roadType.equals("primary")) {
						graphics.setStroke(thickStroke);
						displayWay(way, graphics);
					} else if (roadType.equals("secondary") || roadType.equals("tertiary")) {
						graphics.setStroke(medStroke);
						displayWay(way, graphics);
					} else {
						graphics.setStroke(thinStroke);
						displayWay(way, graphics);
					}
				} else if (level == 3) {
					if (roadType == null) {
						graphics.setStroke(medStroke);
						displayWay(way, graphics);
					} else if (roadType.equals("motorway") || roadType.equals("trunk") || roadType.equals("primary")) {
						graphics.setStroke(thickestStroke);
						displayWay(way, graphics);
					} else if (roadType.equals("secondary") || roadType.equals("tertiary")) {
						graphics.setStroke(thickStroke);
						displayWay(way, graphics);
					} else {
						graphics.setStroke(medStroke);
						displayWay(way, graphics);
					}
				}
			}
			if (directionsWay != null) {
				graphics.setStroke(medStroke);
				graphics.setColor(DIRECTIONS);
				displayWay(directionsWay, graphics);
			}
			// Display current location if there is one
			if (currentLocation != null) {
				graphics.setColor(Color.PINK);
				Ellipse2D.Double ellipse = new Ellipse2D.Double(currentLocation.getX() - 5, currentLocation.getY() - 5,
						10, 10);
				graphics.draw(ellipse);
			}
		}

	}

	/**
	 * Helper method used to display ways(lines)
	 * 
	 * @param g
	 *            The graphics context of the MapDisplay
	 * @param way
	 *            The way to be displayed
	 */
	public void displayWay(Way way, Graphics2D graphics) {
		List<Node> nodes = way.getNodes();
		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < nodes.size() - 1; i++) {
			Node node = nodes.get(i);
			Point point = (Point) map.getForward(node);
			points.add(point);
		}
		for (int i = 0; i < points.size() - 1; i++) {
			Line2D.Double line = new Line2D.Double(points.get(i), points.get(i + 1));
			graphics.draw(line);
		}
	}

	/**
	 * This method will set a DirectionsPath for this mapDisplay
	 * 
	 * @param MapPath
	 *            The map path to be displayed by this mapDisplay.
	 */

	public void setDirectionsPath(Way MapPath) {
		directionsWay = MapPath;
		if (MapPath == null)
		{
			return;
		}
		List<Node> nodes = MapPath.getNodes();
		// Must add a new point to the bi map which represents the user's
		// current location. if it's not in the model.
		Node key = nodes.get(0);
		if (key.getID().equals("Dummy Node")) {
			Node center = model.getCenterNode();
			double latitudeOffset = center.getLatitude();
			double longitudeOffset = center.getLongitude();
			double adjustedLat = key.getLatitude() - latitudeOffset;
			double adjustedLon = key.getLongitude() - longitudeOffset;
			adjustedLat *= -1;
			double convertedLat = key.getLatitude() * (Math.PI / 180.0); // Converting
																			// latitude
																			// measurement
																			// into
																			// radians
			adjustedLon *= PIXELS_PER_DEGREE * Math.cos(convertedLat);
			adjustedLat *= PIXELS_PER_DEGREE;
			adjustedLat += (INITIAL_PANEL_SIZE / 2);
			adjustedLon += (INITIAL_PANEL_SIZE / 2);
			Point point = new Point((int) adjustedLon, (int) adjustedLat);
			map.put(key, point);

		}
	}

	/**
	 * Helper method which simply used in order to determine which tooltip to
	 * use (i.e. which node the mouse is on).
	 */
	public void currentMouseLocation(Point Location) {
		currentMouse = Location;

	}

	@Override
	public void processEvent(GPSEvent ev) {
		// Keep track to see if user
		// location is on screen. Node center = model.getCenterNode();
		if (isDriving) {
			Node center = model.getCenterNode();
			double latitudeOffset = center.getLatitude();
			double longitudeOffset = center.getLongitude();
			double adjustedLat = ev.getLatitude() - latitudeOffset;
			double adjustedLon = ev.getLongitude() - longitudeOffset;
			adjustedLat *= -1;
			double convertedLat = ev.getLatitude() * (Math.PI / 180.0); // Converting
																		// latitude
																		// measurement
																		// into
																		// radians
			adjustedLon *= PIXELS_PER_DEGREE * Math.cos(convertedLat);
			adjustedLat *= PIXELS_PER_DEGREE;
			adjustedLat += (INITIAL_PANEL_SIZE / 2);
			adjustedLon += (INITIAL_PANEL_SIZE / 2);
			Point point = new Point((int) adjustedLon, (int) adjustedLat);
			currentLocation = point;

		}
		getParent().repaint();
	}

	/**
	 * Tells the map display whether to keep updating the display with the
	 * currentLocation as well as MapPath for the driveThere mode.
	 * 
	 * @param isDriving
	 *            Whether the system is currently in drive there mode.
	 * @return
	 */
	public void setIsDriving(boolean isDriving) {
		this.isDriving = isDriving;
	}
}

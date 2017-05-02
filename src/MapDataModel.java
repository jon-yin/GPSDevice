import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A class that will parse through an OSM file and obtain data important for
 * obtaining directions and displaying a map. In this case, it will parse
 * through and gather all of the nodes and ways as well as tags associated with
 * each of these data types.
 * 
 * @author Jonathan Yin
 *
 */
public class MapDataModel {
	private Map<String, OSMData> dataPoints;
	private List<DataModelObserver> dataObservers;
	private Node centerNode = null;

	public MapDataModel() {
		dataPoints = new HashMap<String, OSMData>();
		dataObservers = new ArrayList<DataModelObserver>();
	}

	/**
	 * Parses through an OSMFile in order to extract the datapoints throughout
	 * the file.
	 * 
	 * @param f
	 *            The file to be parsed.
	 */
	public void parseFile(File f) throws IOException, ParserConfigurationException, SAXException {
		dataPoints.clear();
		centerNode = null;
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setValidating(false);
		SAXParser saxParser = spf.newSAXParser();
		XMLReader xmlReader = saxParser.getXMLReader();
		OSMHandler handler = new OSMHandler();
		xmlReader.setContentHandler(handler);
		InputStream stream = null;
		try {
			stream = new FileInputStream(f);
			InputSource source = new InputSource(stream);
			xmlReader.parse(source);
		} catch (IOException x) {
			throw x;
		} finally {
			if (stream != null)
				stream.close();
		}
	}

	class OSMHandler extends DefaultHandler {
		OSMData currentData;

		/** Attributes of the current element. */
		private Attributes attributes;

		/**
		 * Get the attributes of the most recently encountered XML element.
		 */
		public Attributes getAttributes() {
			return attributes;
		}

		/**
		 * Processes the creation of new nodes, ways, and relations. Also
		 * processes tags and members in ways and relations.
		 */
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
			attributes = atts;

			if (atts.getLength() > 0)
				if (qName.equals("node")) {
					String id = "";
					double lat = 0, lon = 0;
					for (int i = 0; i < attributes.getLength(); i++) {
						if (attributes.getQName(i).equals("id")) {
							id = attributes.getValue(i);
						} else if (attributes.getQName(i).equals("lat")) {
							lat = Double.parseDouble(attributes.getValue(i));
						} else if (attributes.getQName(i).equals("lon")) {
							lon = Double.parseDouble(attributes.getValue(i));
						}
					}
					Node newNode = new Node(id, lat, lon);
					currentData = newNode;
					if (centerNode == null)
						centerNode = newNode;
				} else if (qName.equals("way")) {
					String id = "";
					for (int i = 0; i < attributes.getLength(); i++) {
						if (attributes.getQName(i).equals("id")) {
							id = attributes.getValue(i);
						}
					}
					currentData = new Way(id);
				} else if (qName.equals("nd")) {
					String ref = null;
					for (int i = 0; i < attributes.getLength(); i++) {
						if (attributes.getQName(i).equals("ref")) {
							ref = attributes.getValue(i);
						}
					}
					Node member = (Node) dataPoints.get(ref);
					if (member != null && currentData != null) {
						Way way = (Way) currentData;
						way.addNode(member);
						currentData = way;
					}
				} else if (qName.equals("tag")) {
					if (currentData != null) {
						String key = null;
						String value = null;
						for (int i = 0; i < attributes.getLength(); i++) {
							if (attributes.getQName(i).equals("k")) {
								key = attributes.getValue(i);
							} else if (attributes.getQName(i).equals("v")) {
								value = attributes.getValue(i);
							}
						}
						currentData.addTag(key, value);
					}
				}
		}

		public void endElement(String namespaceURI, String localName, String qName) throws SAXParseException {
			if (qName.equals("node") || qName.equals("way")) {
				dataPoints.put(currentData.getID(), currentData);
				String name = currentData.getTag("name");
				if (name != null)
					dataPoints.put(name, currentData);
				currentData = null;
			}
		}
	}

	/**
	 * Gets a data point from the dataPoints currently stored in the model given
	 * a String key.
	 * 
	 * @param key
	 *            The key of the data point to be obtained.
	 * @return The OSMDataPoint which is
	 */
	public OSMData getDataPoint(String key) {
		return dataPoints.get(key);

	}

	/**
	 * Adds a data observer which will be notified when this dataModel changes
	 * in content. (Such as loading a new OSM file into the the dataModel.
	 * 
	 * @param observer
	 */
	public void addDataObserver(DataModelObserver observer) {
		dataObservers.add(observer);
	}

	/**
	 * Notify observers that the dataModel has changed and that they must
	 * perform an appropriate action (resetting both Observers when a new file
	 * is parsed).
	 */
	public void notifyObservers() {
		for (DataModelObserver observer : dataObservers)
			observer.reset();
	}

	/**
	 * Returns a list of all nodes that are parsed by the parser, mainly serves
	 * as convenience in order to avoid parsing the entire dataPoints collection
	 * 
	 * @return A list of all nodes in the osm file
	 */
	public List<Node> getNodes() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		Collection<OSMData> values = dataPoints.values();
		for (OSMData data : values) {
			if (data instanceof Node) {
				Node node = (Node) data;
				nodes.add(node);
			}
		}
		return nodes;
	}

	/**
	 * Returns a list of all ways that are parsed by the parser, mainly serves
	 * as convenience in order to avoid parsing the entire dataPoints collection
	 * 
	 * @return A list of all nodes in the osm file
	 */
	public List<Way> getWays() {
		ArrayList<Way> ways = new ArrayList<Way>();
		Collection<OSMData> values = dataPoints.values();
		for (OSMData data : values) {
			if (data instanceof Way) {
				Way way = (Way) data;
				ways.add(way);
			}
		}
		return ways;
	}

	/**
	 * Gets the node that the MapDisplay will center the map around. By default
	 * this is simply the first Node processed. Unneeded in part 4.
	 */
	public Node getCenterNode() {
		List<Node> nodes = getNodes();
		// Essentially returns a random node.
		return nodes.get(0);
	}

}

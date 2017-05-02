import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.starkeffect.highway.GPSDevice;

/*
 * Organizes the major components of the GPS namely the MapDisplay and the DirectionsGenerator(Not Shown in this part of the assignment)
 */
public class Mainframe {
	private MapDataModel datamodel;
	private boolean findNode = false;
	private DirectionsGenerator generator;
	private JTextField focused;
	private GPSDevice gps = null;

	public Mainframe() {
		datamodel = new MapDataModel();
	}

	/**
	 * Sets up the main components such as the display and the directionsGetter
	 * and adds action listeners to both these components to make them fully
	 * functional
	 */
	public void initialize() {
		JFrame frame = new JFrame("GPS system");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem LoadData = new JMenuItem("Load Map");
		menu.add(LoadData);
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);
		MapDisplay display = new MapDisplay(datamodel);
		generator = new DirectionsGenerator(datamodel, display);
		LoadData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter("OSM file", "osm");
				fileChooser.setFileFilter(filter);
				// Only allow one file at a time
				fileChooser.setMultiSelectionEnabled(false);
				int option = fileChooser.showOpenDialog(frame);
				if (option == JFileChooser.APPROVE_OPTION) {
					File ChosenFile = fileChooser.getSelectedFile();
					try {
						datamodel.parseFile(ChosenFile);
						datamodel.notifyObservers();
						if (gps != null)
						{
							gps.removeGPSListener(display);
							gps.removeGPSListener(generator);
						}
						gps = new GPSDevice(ChosenFile.getPath());
						gps.addGPSListener(display);
						gps.addGPSListener(generator);
						frame.repaint();
					} catch (Exception e) {
						JOptionPane.showMessageDialog(frame, "Something went wrong with the fileParsing, check for "
								+ "if the file you selected is valid.");
						e.printStackTrace();
						return;
					}
				}

			}

		});
		MouseAdapter mouseAdapter = new MouseAdapter() {
			int originalX;
			int originalY;

			@Override
			public void mouseClicked(MouseEvent e) {
				if (findNode) {
					Point mouseClick = e.getPoint();
					Node node = display.findClickedLocation(mouseClick);
					findNode = false;
					if (focused != null && node != null) {
						String name = node.getTag("name");
						String id = node.getID();
						if (name != null) {
							focused.setText(name);
						} else {
							focused.setText(id);
						}
					}
					frame.repaint();
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				display.currentMouseLocation(e.getPoint());
				frame.repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (originalX == 0 && originalY == 0) {
					originalX = e.getX();
					originalY = e.getY();
				}
				int newX = e.getX();
				int newY = e.getY();
				display.pan(newX - originalX, newY - originalY);
				originalX = newX;
				originalY = newY;
				frame.repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				originalX = 0;
				originalY = 0;
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int rotations = e.getWheelRotation();
				display.zoom(rotations);
				frame.repaint();
			}

		};
		display.addMouseMotionListener(mouseAdapter);
		display.addMouseWheelListener(mouseAdapter);
		display.addMouseListener(mouseAdapter);
		datamodel.addDataObserver(display);
		JPanel directionsInputter = new JPanel();
		directionsInputter.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		// FIRST ROW TITLE
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.1;
		c.weighty = 0.1;
		c.gridx = 1;
		c.gridy = 0;
		c.insets = new Insets(50, 150, 0, 0);
		c.anchor = GridBagConstraints.PAGE_START;
		JLabel title = new JLabel("Directions Generator");
		directionsInputter.add(title, c);
		///////////////////////////////////////////////////
		// SECOND ROW, START LOCATION
		c.insets = new Insets(0, 20, 0, 0);
		c.anchor = GridBagConstraints.LINE_START;
		c.weightx = 0.1;
		c.weighty = 0.1;
		c.gridx = 0;
		c.gridy = 1; // Start on second row
		JLabel startLoc = new JLabel("Starting Location:");
		directionsInputter.add(startLoc, c);
		c.insets = new Insets(0, 10, 0, 0);
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = .1;
		c.weighty = 0.1;
		JTextField startField = new JTextField(20);
		directionsInputter.add(startField, c);
		//////////////////////////////////////// THIRD ROW ENDING
		//////////////////////////////////////// LOCATION///////////////////////////
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.1;
		c.weighty = 0.1;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(0, 23, 0, 0);
		JLabel endLoc = new JLabel("Ending Location:");
		directionsInputter.add(endLoc, c);
		c.gridx = 1;
		c.insets = new Insets(0, 10, 0, 0);
		JTextField endField = new JTextField(20);
		directionsInputter.add(endField, c);
		c.gridx = 2;
		c.insets = new Insets(0, 0, 0, 0);
		//////////////////////// FORTH ROW ADDING LOCATION
		//////////////////////// BUTTONS//////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 3;
		c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(0, 20, 0, 0);
		c.gridx = 1;
		JButton selectLocation = new JButton("Select location");
		directionsInputter.add(selectLocation, c);
		/////////////////////// FIFTH ROW, OK AND CANCEL
		/////////////////////// BUTTONS/////////////////////////////////
		c.weighty = 0.1;
		c.gridy = 4;
		c.gridx = 0;
		// c.anchor = GridBagConstraints.LINE_END;
		c.insets = new Insets(0, 20, 350, 0);
		JButton okButton = new JButton("OK");
		directionsInputter.add(okButton, c);
		c.gridx = 1;
		JButton drive = new JButton("Drive There");
		directionsInputter.add(drive, c);
		c.gridx = 2;
		JButton cancel = new JButton("Cancel");
		directionsInputter.add(cancel, c);

		startField.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent ev) {
				focused = startField;
			}

			@Override
			public void focusLost(FocusEvent ev) {

			}

		});
		endField.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				focused = endField;

			}

			@Override
			public void focusLost(FocusEvent arg0) {
			}

		});
		selectLocation.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				findNode = true;
			}

		});
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!startField.getText().equals("") && !endField.getText().equals("")) {
					try {
						generator.parseString(startField.getText(), endField.getText());
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null,
								"Something went wrong with generating directions, check to see if the name or I.D is indeed correct.");
					}

				}
			}

		});
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				generator.cancel();
				frame.repaint();
			}

		});
		drive.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Node dest = (Node) datamodel.getDataPoint(endField.getText());
					generator.driveDestination(dest);
					generator.driveThere();
					JOptionPane.showMessageDialog(frame, "Destination successfully set!");

				} catch (Exception ex) {
					JOptionPane.showMessageDialog(frame, "Something went wrong with obtaining an "
							+ "destination for the input in the desination field");
				}

			}

		});
		frame.add(directionsInputter, BorderLayout.WEST);
		frame.add(display, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);

	}

	public static void main(String[] args) {
		Mainframe mainframe = new Mainframe();
		mainframe.initialize();
	}
}

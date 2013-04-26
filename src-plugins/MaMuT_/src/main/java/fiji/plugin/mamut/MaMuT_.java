package fiji.plugin.mamut;

import ij.IJ;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import loci.formats.FormatException;
import net.imglib2.RealPoint;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.io.ImgIOException;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jfree.chart.renderer.InterpolatePaintScale;

import viewer.BrightnessDialog;
import viewer.HelpFrame;
import viewer.render.Source;
import viewer.render.SourceAndConverter;
import fiji.plugin.mamut.gui.MamutConfigPanel;
import fiji.plugin.mamut.viewer.ImgPlusSource;
import fiji.plugin.mamut.viewer.MamutViewer;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class MaMuT_ <T extends RealType<T> & NativeType<T>> implements BrightnessDialog.MinMaxListener, ModelChangeListener {

	public static final String PLUGIN_NAME = "MaMuT";
	public static final String PLUGIN_VERSION = "v0.5.0";
	private static final double DEFAULT_RADIUS = 1;
	/** By how portion of the current radius we change this radius for every
	 * change request.	 */
	private static final double RADIUS_CHANGE_FACTOR = 0.1;
	
	private static final int CHANGE_A_LOT_KEY = KeyEvent.SHIFT_DOWN_MASK;
	private static final int CHANGE_A_BIT_KEY = KeyEvent.CTRL_DOWN_MASK;
	
	private KeyStroke brightnessKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_C, 0 );
	private KeyStroke helpKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 );
	private KeyStroke addSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_A, 0 );
	private KeyStroke deleteSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_D, 0 );
	private KeyStroke moveSpotKeystroke = KeyStroke.getKeyStroke( KeyEvent.VK_SPACE, 0 );
	
	private int increaseRadiusKey = KeyEvent.VK_E;
	private int decreaseRadiusKey = KeyEvent.VK_Q;
	private KeyStroke increaseRadiusKeystroke = KeyStroke.getKeyStroke( increaseRadiusKey, 0 );
	private KeyStroke decreaseRadiusKeystroke = KeyStroke.getKeyStroke( decreaseRadiusKey, 0 );
	private KeyStroke increaseRadiusALotKeystroke = KeyStroke.getKeyStroke( increaseRadiusKey, CHANGE_A_LOT_KEY );
	private KeyStroke decreaseRadiusALotKeystroke = KeyStroke.getKeyStroke( decreaseRadiusKey, CHANGE_A_LOT_KEY );
	private KeyStroke increaseRadiusABitKeystroke = KeyStroke.getKeyStroke( increaseRadiusKey, CHANGE_A_BIT_KEY );
	private KeyStroke decreaseRadiusABitKeystroke = KeyStroke.getKeyStroke( decreaseRadiusKey, CHANGE_A_BIT_KEY );

	private final ArrayList< AbstractLinearRange > displayRanges;
	private BrightnessDialog brightnessDialog;
	/** The {@link MamutViewer}s managed by this plugin. */
	private Collection<MamutViewer> viewers = new ArrayList<MamutViewer>();
	/** The model shown and edited by this plugin. */
	private TrackMateModel model;
	/** The next created spot will be set with this radius. */
	private double radius = DEFAULT_RADIUS;
	private final double minRadius;
	/** The spot currently moved under the mouse. */
	private Spot movedSpot = null;
	/** The image data sources to be displayed in the views. */
	private final List<SourceAndConverter<?>> sources;
	/** The number of timepoints in the image sources. */
	private final int nTimepoints;
	/** The GUI that control the views. */
	private final MamutConfigPanel controlPanel;

	private final Map<Spot, Color> colorProvider;

	public MaMuT_() throws ImgIOException, FormatException, IOException {

		/*
		 * Load image
		 */
		
		final String id = "/Users/tinevez/Desktop/Data/Celegans-XY.tif";
		ImagePlus imp = IJ.openImage(id);
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(imp);
		nTimepoints = (int) img.dimension(3);

		/*
		 * Find adequate rough scales
		 */
		
		minRadius = 2 * Math.min(img.calibration(0), img.calibration(1));
		
		/*
		 * Instantiate model
		 */
		
		model = new TrackMateModel();
		model.addTrackMateModelChangeListener(this);
		
		
		/*
		 * Create image source
		 */

		Source<T> source = new ImgPlusSource<T>(img);
		final RealARGBConverter< T > converter = new RealARGBConverter< T >( 0, img.firstElement().getMaxValue() );
		sources = new ArrayList< SourceAndConverter< ? > >(1);
		sources.add( new SourceAndConverter< T >(source, converter ));

		/*
		 * Create display range
		 */
		
		displayRanges = new ArrayList< AbstractLinearRange >();
		displayRanges.add( converter );
		
		/*
		 * Color provider
		 */
		
		colorProvider = new HashMap<Spot, Color>();
		
		/*
		 * Create control panel
		 */
		
		controlPanel = new MamutConfigPanel(model);
		controlPanel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {
				if (event == controlPanel.COLOR_FEATURE_CHANGED) {
					computeSpotColors(controlPanel.getSpotColorFeature());
					refresh();
				} else {
					System.out.println("Got event " + event);
				}
			}

		});
				
		/*
		 * Create views
		 */
		
		newViewer();
		newViewer();
		
		controlPanel.setVisible(true);
	}		
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	public MamutViewer newViewer() {
		final MamutViewer viewer = new MamutViewer(800, 600, sources, nTimepoints, model, colorProvider);
		installKeyBindings(viewer);
		installMouseListeners(viewer);
		viewer.addHandler(viewer);
		viewer.render();
		viewers.add(viewer);
		controlPanel.register(viewer);
		
		viewer.getFrame().addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent arg0) { }
			@Override
			public void windowIconified(WindowEvent arg0) { }
			@Override
			public void windowDeiconified(WindowEvent arg0) { }
			
			@Override
			public void windowDeactivated(WindowEvent arg0) { }
			
			@Override
			public void windowClosing(WindowEvent arg0) { }
			
			@Override
			public void windowClosed(WindowEvent arg0) {
				viewers.remove(viewer);
			}
			
			@Override
			public void windowActivated(WindowEvent arg0) { }
		});
		
		return viewer;
	}
	

	@Override
	public void modelChanged(ModelChangeEvent event) {
		refresh();
	}
	

	@Override
	public void setMinMax( final int min, final int max ) {
		for ( final AbstractLinearRange r : displayRanges ) {
			r.setMin( min );
			r.setMax( max );
		}
		refresh();
	}
	

	public void toggleBrightnessDialog() {
		brightnessDialog.setVisible( ! brightnessDialog.isVisible() );
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Configures the specified {@link MamutViewer} with key bindings.
	 * @param the {@link MamutViewer} to configure.
	 */
	private void installKeyBindings(final MamutViewer viewer) {
		
		/*
		 *  Help window
		 */
		viewer.addKeyAction( helpKeystroke, new AbstractAction( "help" ) {
			@Override
			public void actionPerformed( final ActionEvent arg0 ) {
				showHelp();
			}

			private static final long serialVersionUID = 1L;
		} );

		/*
		 *  Brightness dialog
		 */
		viewer.addKeyAction( brightnessKeystroke, new AbstractAction( "brightness settings" ) {
			@Override
			public void actionPerformed( final ActionEvent arg0 ) {
				toggleBrightnessDialog();
			}
			
			private static final long serialVersionUID = 1L;
		} );
		brightnessDialog = new BrightnessDialog( viewer.getFrame() );
		viewer.installKeyActions( brightnessDialog );
		brightnessDialog.setListener( this );

		/*
		 *  Add spot
		 */
		viewer.addKeyAction(addSpotKeystroke, new AbstractAction( "add spot" ) {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addSpot(viewer);
			}
			private static final long serialVersionUID = 1L;
		});
		
		/*
		 * Delete spot
		 */
		viewer.addKeyAction(deleteSpotKeystroke, new AbstractAction( "delete spot" ) {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				deleteSpot(viewer);
			}
			private static final long serialVersionUID = 1L;
		});
		
		/*
		 * Change radius
		 */
		
		viewer.addKeyAction(increaseRadiusKeystroke, new AbstractAction( "increase spot radius" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, 1d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(increaseRadiusALotKeystroke, new AbstractAction( "increase spot radius a lot" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, 10d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(increaseRadiusABitKeystroke, new AbstractAction( "increase spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, 0.1d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(decreaseRadiusKeystroke, new AbstractAction( "decrease spot radius" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -1d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(decreaseRadiusALotKeystroke, new AbstractAction( "decrease spot radius a lot" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -5d); }
			private static final long serialVersionUID = 1L;
		});
		
		viewer.addKeyAction(decreaseRadiusABitKeystroke, new AbstractAction( "decrease spot radius a bit" ) {
			@Override
			public void actionPerformed(ActionEvent arg0) { increaseSpotRadius(viewer, -0.1d); }
			private static final long serialVersionUID = 1L;
		});
		
		
		/*
		 * Custom key presses
		 */
		
		viewer.addHandler(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent event) { }
			
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.getKeyCode() == moveSpotKeystroke.getKeyCode()) {
					if (null != movedSpot) {
						model.beginUpdate();
						try {
							model.updateFeatures(movedSpot);
						} finally {
							String str = String.format(
									"Moved spot " + movedSpot + " to location X = %.1f, Y = %.1f, Z = %.1f.", 
									movedSpot.getFeature(Spot.POSITION_X), movedSpot.getFeature(Spot.POSITION_Y), 
									movedSpot.getFeature(Spot.POSITION_Z));
							viewer.getLogger().log(str);
							movedSpot = null;
						}
					}
				}
			}
			
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.getKeyCode() == moveSpotKeystroke.getKeyCode()) {
					movedSpot = getSpotWithinRadius(viewer);
				}
				
			}
		});
		
		
	}
	
	

	/**
	 * Configures the specfied {@link MamutViewer} with mouse listeners. 
	 * @param viewer  the {@link MamutViewer} to configure.
	 */
	private void installMouseListeners(final MamutViewer viewer) {
		viewer.addHandler(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent arg0) {
				if (null != movedSpot) {
					final RealPoint gPos = new RealPoint( 3 );
					viewer.getGlobalMouseCoordinates(gPos);
					double[] coordinates = new double[3];
					gPos.localize(coordinates);
					movedSpot.putFeature(Spot.POSITION_X, coordinates[0]);
					movedSpot.putFeature(Spot.POSITION_Y, coordinates[1]);
					movedSpot.putFeature(Spot.POSITION_Z, coordinates[2]);
				}
				
			}
			
			@Override
			public void mouseDragged(MouseEvent arg0) { }
		});
		
		
		viewer.addHandler(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) { }
			
			@Override
			public void mousePressed(MouseEvent arg0) { }
			
			@Override
			public void mouseExited(MouseEvent arg0) {}
			
			@Override
			public void mouseEntered(MouseEvent arg0) { }
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				Spot spot = getSpotWithinRadius(viewer);
				if (null != spot) {
					for (MamutViewer otherView : viewers) {
						otherView.centerViewOn(spot);
					}
				}
			}
		});
		
	}

	
	private void refresh() {
		// Just ask to repaint the TrackMate overlay
		for (MamutViewer viewer : viewers) {
			viewer.refresh();
		}
	}

	
	
	/**
	 * Adds a new spot at the mouse current location.
	 * @param viewer  the viewer in which the add spot request was made.
	 */
	private void addSpot(final MamutViewer viewer) {

		// Check if the mouse is not off-screen
		Point mouseScreenLocation = MouseInfo.getPointerInfo().getLocation();
		Point viewerPosition = viewer.getFrame().getLocationOnScreen();
		Dimension viewerSize = viewer.getFrame().getSize();
		if (mouseScreenLocation.x < viewerPosition.x ||
				mouseScreenLocation.y < viewerPosition.y ||
				mouseScreenLocation.x > viewerPosition.x + viewerSize.width ||
				mouseScreenLocation.y > viewerPosition.y + viewerSize.height ) {
			return;
		}
		
		// Ok, then create this spot, wherever it is.
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot spot = new Spot(coordinates);
		spot.putFeature(Spot.RADIUS, radius );
		model.beginUpdate();
		try {
			model.addSpotTo(spot, viewer.getCurrentTimepoint());
		} finally {
			model.endUpdate();
			String str = String.format(
					"Added spot " + spot + " at location X = %.1f, Y = %.1f, Z = %.1f, T = %.0f.", 
					spot.getFeature(Spot.POSITION_X), spot.getFeature(Spot.POSITION_Y), 
					spot.getFeature(Spot.POSITION_Z), spot.getFeature(Spot.FRAME));
			viewer.getLogger().log(str);
		}
	}
	
	/**
	 * Adds a new spot at the mouse current location.
	 * @param viewer  the viewer in which the delete spot request was made.
	 */
	private void deleteSpot(final MamutViewer viewer) {
		Spot spot = getSpotWithinRadius(viewer); 
		if (null != spot) {
			// We can delete it
			model.beginUpdate();
			try {
				int frame = viewer.getCurrentTimepoint();
				model.removeSpotFrom(spot, frame);
			} finally {
				model.endUpdate();
				String str = "Removed spot " + spot + "."; 
				viewer.getLogger().log(str);
			}
		}
		
	}
	
	/**
	 * Increases (or decreases) the neighbor spot radius. 
	 * @param viewer  the viewer in which the change radius was made. 
	 * @param factor  the factor by which to change the radius. Negative value are used
	 * to decrease the radius.
	 */
	private void increaseSpotRadius(final MamutViewer viewer, double factor) {
		Spot spot = getSpotWithinRadius(viewer);
		if (null != spot) {
			// Change the spot radius
			double rad = spot.getFeature(Spot.RADIUS);
			rad += factor * RADIUS_CHANGE_FACTOR * rad;
			
			if (rad < minRadius) {
				return;
			}
			
			radius = rad;
			spot.putFeature(Spot.RADIUS, rad);
			// Mark the spot for model update;
			model.beginUpdate();
			try {
				model.updateFeatures(spot);
			} finally {
				model.endUpdate();
				String str = String.format(
						"Changed spot " + spot + " radius to R = %.1f.", 
						spot.getFeature(Spot.RADIUS));
				viewer.getLogger().log(str);
			}
		}
	}
	
	
	private void showHelp() {
		new HelpFrame();
	}

	
	/**
	 * Returns the closest {@link Spot} with respect to the current mouse location, and
	 * for which the current location is within its radius, or <code>null</code> if there is no such spot.
	 * In other words: returns the spot in which the mouse pointer is.
	 * @param viewer  the viewer to inspect. 
	 * @return  the closest spot within radius.
	 */
	private Spot getSpotWithinRadius(final MamutViewer viewer) {
		/*
		 * Get the closest spot
		 */
		int frame = viewer.getCurrentTimepoint();
		final RealPoint gPos = new RealPoint( 3 );
		viewer.getGlobalMouseCoordinates(gPos);
		double[] coordinates = new double[3];
		gPos.localize(coordinates);
		Spot location = new Spot(coordinates);
		Spot closestSpot = model.getFilteredSpots().getClosestSpot(location, frame);
		if (null == closestSpot) {
			return null;
		}
		/*
		 * Determine if we are inside the spot
		 */
		double d2 = closestSpot.squareDistanceTo(location);
		double r = closestSpot.getFeature(Spot.RADIUS);
		if (d2 < r*r) {
			return closestSpot;
		} else {
			return null;
		}
		
	}
	
	
	private void computeSpotColors(final String feature) {
		colorProvider.clear();
		// Check null
		if (null == feature) {
			for(Spot spot : model.getSpots()) {
				colorProvider.put(spot, TrackMateModelView.DEFAULT_COLOR);
			}
			return;
		}
		
		// Get min & max
		double min = Float.POSITIVE_INFINITY;
		double max = Float.NEGATIVE_INFINITY;
		Double val;
		for (int ikey : model.getSpots().keySet()) {
			for (Spot spot : model.getSpots().get(ikey)) {
				val = spot.getFeature(feature);
				if (null == val)
					continue;
				if (val > max) max = val;
				if (val < min) min = val;
			}
		}
		
		for(Spot spot : model.getSpots()) {
			val = spot.getFeature(feature);
			InterpolatePaintScale  colorMap = InterpolatePaintScale.Jet;
			if (null == feature || null == val)
				colorProvider.put(spot, TrackMateModelView.DEFAULT_COLOR);
			else
				colorProvider.put(spot, colorMap .getPaint((val-min)/(max-min)) );
		}
	}
	
	
	
	/*
	 * MAIN METHOD
	 */
	
	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) throws ImgIOException, FormatException, IOException {
		new MaMuT_<T>();
	}
	



}
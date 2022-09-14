package EarthquakeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.providers.Microsoft;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;
import processing.core.PGraphics;

/** EarthquakeCityMap
 * @author Ovidiu Boamba
 * Date: September 14, 2022
 * */
public class EarthquakeCityMap extends PApplet 
{
	
	
	//  It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;
	
	public static final float THRESHOLD_INTERMEDIATE = 70;
	/** Greater than or equal to this threshold is a deep depth */
	public static final float THRESHOLD_DEEP = 300;
	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";
	
	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";
	
	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	public char keyToDraw;
	
	// The map
	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;

	// A List of country markers
	private List<Marker> countryMarkers;
	
	private List<Marker> affectedCities = new ArrayList<Marker>();
	private List<Marker> affectedQuakes = new ArrayList<Marker>(); 
	
	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;
	
	private Marker bestQuake, bestCity;
	private int citiesToPrint, quakesToPrint;
	
	public void setup() {		
		// (1) Initializing canvas and map tiles
		size(1400, 700, OPENGL);
		if (offline) {
		    map = new UnfoldingMap(this, 200, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
		    earthquakesURL = "2.5_week.atom";  // The same feed, but saved August 7, 2015
		}
		else {
			map = new UnfoldingMap(this, 200, 50, 650, 600, new	Microsoft.RoadProvider());
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
		    //earthquakesURL = "2.5_week.atom";
		}
		MapUtils.createDefaultEventDispatcher(this, map);
		
		
		// (2) Reading in earthquake data and geometric properties
	    //     STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		
		//     STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    
		//     STEP 3: read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();
	    
	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }

	    citiesToPrint = forQuake();
	    quakesToPrint = forCity();
	    
	    // could be used for debugging
	   // printQuakes();
	 		
	    // (3) Add markers to map
	    //     NOTE: Country markers are not added to the map.  They are used
	    //           for their geometric properties
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	    
	    sortAndPrint(20);
	    
	    
	    
	}  // End setup
	
	
	public void draw() {
		background(0,0,0);
		map.draw();
		addKey();
		
		if(keyToDraw == 'f')
		{
			fill(255,255,255);
			rect(900, 50, 400, 170 + citiesToPrint*20);
			fill(0,0,0);
			text("The earthquake that affected", 920, 75);
			text("the most cities was: ", 950, 90);
			EarthquakeMarker eq = (EarthquakeMarker)bestQuake;
			text(eq.getTitle(), 930, 105);
			float depth = eq.getDepth();
			
			if (depth < THRESHOLD_INTERMEDIATE) 
			{
				fill(255, 255, 0);
			}
			else if (depth < THRESHOLD_DEEP) 
			{
				fill(0, 0, 255);
			}
			else 
			{
				fill(255, 0, 0);
			}
			ellipse(915, 105, 15, 15);
			
			String age = eq.getStringProperty("age");
			fill(0,0,0);
			
			text("Earthquake took place: " + age, 920, 130);
			text("Cities affected by it were:", 920, 150);
			
			float x, y;
			x = 915;
			y = 170;
			for(Marker city: affectedCities)
			{
				String name = (String)city.getProperty("name");
				fill(255, 0, 0);
				triangle(x, y-5, x-5, y+5, x+5, y+5);
				fill(0,0,0);
				text((String)name, x+20, y);
				y = y + 20;
			}
			
		}
		else if(keyToDraw == 'g')
		{
			fill(255,255,255);
			rect(900, 50, 450, 150 + quakesToPrint*20);
			fill(0,0,0);
			text("The city affected by the most", 1020, 75);
			text("earthquakes was: ", 1050, 90);
			
			text(bestCity.getProperty("name") + ", " + bestCity.getProperty("country"), 1050, 105);
			fill(255, 0, 0);
			triangle(1040, 100, 1035, 110, 1045, 110);
			
			fill(0,0,0);
			text("The earthquakes, their magnitude and when they took place were:", 920, 140);
			
			float x = 915, y = 160;
			for(Marker quake: affectedQuakes)
			{
				
				EarthquakeMarker eq = (EarthquakeMarker)quake;
				String name = (String)eq.getTitle();
				String age = eq.getStringProperty("age");
				float depth = eq.getDepth();
				
				if (depth < THRESHOLD_INTERMEDIATE) {
					fill(255, 255, 0);
				}
				else if (depth < THRESHOLD_DEEP) {
					fill(0, 0, 255);
				}
				else {
					fill(255, 0, 0);
				}
				
				if(eq.isOnLand() == true) ellipse(x, y, 10, 10);
				else rect(x-5, y-5, 10, 10);

				fill(0,0,0);
				text(name + ", " + age, x+10, y);
				y = y + 20;
			}
		}
		else if(keyToDraw == 'c')
		{
			fill(255, 255, 255);
			rect(940, 250, 220, 100);
			fill(0,0,0);
			text("Incorrect key!", 1000, 275);
			text("Please, try a valid key that", 975, 290);
			text("is visible in the legend of the map", 955, 305);
		}
	}
	
	private void sortAndPrint(int numToPrint)
	{
		Object[] objects = quakeMarkers.toArray();
		Arrays.sort(objects);
		
		int i, n;
		n = objects.length;
		for(i = 0; i < numToPrint && i < n; i++) System.out.println(objects[i]);
	}
	
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
	}
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> markers)
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker m : markers) 
		{
			CommonMarker marker = (CommonMarker)m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}
	
	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes 
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		}
		else if (lastClicked == null) 
		{
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
		}
	}
	
	private void hideMarkers(List<Marker> markers)
	{
		for(Marker it: markers) it.setHidden(true);
	}
	
	private void showMarkers(List<Marker> markers)
	{
		for(Marker it: markers) it.setHidden(false);
	}
	
	private int forQuake()
	{
		int maxNumber = 0;
		for(Marker quake: quakeMarkers)
		{
			double threatRadius = ((EarthquakeMarker) quake).threatCircle();
			int nrOfCities = 0;
			for(Marker city: cityMarkers)
			{
				if(distance(quake, city) < threatRadius) nrOfCities++; 
			}
			if(maxNumber < nrOfCities)
			{
				maxNumber = nrOfCities;
				bestQuake = quake;
			}
		}
		return maxNumber;
	}
	
	private int forCity()
	{
		int maxNumber = 0;
		for(Marker city: cityMarkers)
		{
			int nrOfQuakes = 0;
			for(Marker quake: quakeMarkers)
			{
				double threatRadius = ((EarthquakeMarker) quake).threatCircle();
				if(distance(quake, city) < threatRadius) nrOfQuakes++; 
			}
			if(maxNumber < nrOfQuakes)
			{
				maxNumber = nrOfQuakes;
				bestCity = city;
			}
		}
		return maxNumber;
	}
	
	public void keyPressed()
	{
		double threatRadius;
		keyToDraw = 'b';
		hideMarkers(quakeMarkers);
		hideMarkers(cityMarkers);
		if(key == 'm') unhideMarkers();
		else if(key == 'a') // show cities only
		{
			showMarkers(cityMarkers);
		}
		else if(key == 's') // show land earthquakes
		{
			for(Marker it: quakeMarkers)
			{
				EarthquakeMarker m = (EarthquakeMarker)it;
				if(m.isOnLand() == true) it.setHidden(false);
			}
		}
		else if(key == 'd') // show ocean earthquakes
		{			
			for(Marker it: quakeMarkers)
			{
				EarthquakeMarker m = (EarthquakeMarker)it;
				if(m.isOnLand() == false) it.setHidden(false);
			}
		}
		else if(key == 'f') // show the earthquake that affected the most cities
		{
			keyToDraw = 'f';			
			affectedCities.clear();
			bestQuake.setHidden(false);
			threatRadius = ((EarthquakeMarker) bestQuake).threatCircle();
			for(Marker it: cityMarkers)
			{
				if(distance(bestQuake, it) < threatRadius)
				{
					it.setHidden(false);
					affectedCities.add(it);
				}
			}
		}
		else if(key == 'g') // the city affected by the most earthquakes
		{
			keyToDraw = 'g';
			affectedQuakes.clear();
			bestCity.setHidden(false);
			for(Marker it: quakeMarkers)
			{
				threatRadius = ((EarthquakeMarker) it).threatCircle();
				if(distance(bestCity, it) < threatRadius)
				{
					it.setHidden(false);
					affectedQuakes.add(it);
				}
			}
		}
		else 
		{
			keyToDraw = 'c'; //wrong key pressed -> pop up
			unhideMarkers();
		}
	} 
	
	private double distance(Marker a, Marker b)
	{
		Location aa = a.getLocation();
		Location bb = b.getLocation();
		return aa.getDistance(bb);
	}
	
	// Helper method that will check if a city marker was clicked on
	// and respond appropriately
	private void checkCitiesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) 
							> quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}		
	}
	
	// Helper method that will check if an earthquake marker was clicked on
	// and respond appropriately
	private void checkEarthquakesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker)m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) 
							> marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}
			
		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}
	
	// helper method to draw key in GUI
	private void addKey() 
	{	
		fill(255, 250, 240);
		
		int xbase = 25;
		int ybase = 50;
		
		rect(xbase, ybase, 150, 450);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);
		
		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);
		
		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);
		
		fill(255, 255, 255);
		ellipse(xbase+35, 
				ybase+70, 
				10, 
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);
		
		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);
		
		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);
		
		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);
		
		fill(0,0,0);
		text("Key Commands:", xbase+25, ybase+240);
		text("a - cities", xbase+10, ybase+260);
		text("s - land earthquakes", xbase+10, ybase+280);
		text("d - ocean earthquakes", xbase+10, ybase+300);
		text("f - earthquake that", xbase+10, ybase+320);
		text("affected the most cities", xbase+10, ybase+335);
		text("d - city affected by", xbase+10, ybase+355);
		text("most earthquakes", xbase+10, ybase+370);
		text("m - restore map", xbase+10, ybase+390);
		
		
	}

	
	
	// Checks whether this quake occurred on land.  If it did, it sets the 
	// "country" property of its PointFeature to the country where it occurred
	// and returns true. 
	private boolean isLand(PointFeature earthquake) 
	{
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}
		
		// not inside any country
		return false;
	}
	
	// prints countries with number of earthquakes
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers)
			{
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}
	
	
	
	// helper method to test whether a given earthquake is in a given country
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {
				
			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {
					
				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));
						
					// return if is inside one
					return true;
				}
			}
		}
			
		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));
			
			return true;
		}
		return false;
	}

}

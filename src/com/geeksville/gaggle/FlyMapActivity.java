/*******************************************************************************
 * Gaggle is Copyright 2010 by Geeksville Industries LLC, a California limited liability corporation. 
 * 
 * Gaggle is distributed under a dual license.  We've chosen this approach because within Gaggle we've used a number
 * of components that Geeksville Industries LLC might reuse for commercial products.  Gaggle can be distributed under
 * either of the two licenses listed below.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details. 
 * 
 * Commercial Distribution License
 * If you would like to distribute Gaggle (or portions thereof) under a license other than 
 * the "GNU General Public License, version 2", contact Geeksville Industries.  Geeksville Industries reserves
 * the right to release Gaggle source code under a commercial license of its choice.
 * 
 * GNU Public License, version 2
 * All other distribution of Gaggle must conform to the terms of the GNU Public License, version 2.  The full
 * text of this license is included in the Gaggle source, see assets/manual/gpl-2.0.txt.
 ******************************************************************************/
package com.geeksville.gaggle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.overlay.MyLocationOverlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.geeksville.airspace.AirspaceScrollListener;
import com.geeksville.android.AndroidUtil;
import com.geeksville.info.Units;
import com.geeksville.location.IGCReader;
import com.geeksville.location.LocationList;
import com.geeksville.maps.CenteredMyLocationOverlay;
import com.geeksville.maps.GeeksvilleMapActivity;
import com.geeksville.maps.PolygonOverlay;
import com.geeksville.maps.TracklogOverlay;
import com.geeksville.maps.WaypointOverlay;
import com.geeksville.weather.StationProviderable;
import com.geeksville.weather.ffvl.FFVLStationProvider;
import com.geeksville.weather.overlay.WeatherStationsOverlay;

public class FlyMapActivity extends GeeksvilleMapActivity implements Observer, OnSharedPreferenceChangeListener {

	/**
	 * Extra data we look for in our Intent. If specified it will be a Bundle
	 * generated by LocationList
	 */
	private static final String EXTRA_TRACKLOG = "tracklog";

	/**
	 * Extra intend data, a boolean, true == show the user's position
	 */
	private static final String EXTRA_ISLIVE = "live";

	/**
	 * Are we showing the current user position?
	 */
	private boolean isLive = false;

	private WaypointOverlay wptOver;
	private PolygonOverlay polyOver;

	private WeatherStationsOverlay weather_overlay;

	private AirspaceScrollListener airspace_scroll_lst;
	private AltitudeView altitudeView;

	/**
	 * Generate an intent that can be used to play back a tracklog
	 * 
	 * @param parent
	 * @param locs
	 * @return
	 */
	public static Intent createIntentLogView(Context parent, LocationList locs) {
		Bundle locbundle = new Bundle();
		locs.writeTo(locbundle);

		Intent i = new Intent(parent, FlyMapActivity.class);

		i.putExtra(FlyMapActivity.EXTRA_TRACKLOG, locbundle);

		return i;
	}

	// FIXME - skanky, find a better way to pass in ptrs when we ain't
	// crossing process boundaries
	public static LocationList liveList;

	/**
	 * Generate an intent suitable for live flight tracking
	 * 
	 * @param parent
	 * @return
	 */
	public static Intent createIntentLive(Context parent) {

		Intent i = new Intent(parent, FlyMapActivity.class);

		i.putExtra(FlyMapActivity.EXTRA_ISLIVE, true);

		return i;
	}

	/**
	 * Collect app metrics on Flurry
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		GagglePrefs prefs = new GagglePrefs(this);
	    if (prefs.isFlurryEnabled())
		  FlurryAgent.onStartSession(this, "XBPNNCR4T72PEBX17GKF");
	}

	/**
	 * Collect app metrics on Flurry
	 * 
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();

		GagglePrefs prefs = new GagglePrefs(this);
		if (prefs.isFlurryEnabled())
			FlurryAgent.onEndSession(this);

		// Save current map position
		prefs.setMapCenterZoom(mapView.getMapCenter().getLatitudeE6(),
				mapView.getMapCenter().getLongitudeE6(),
				mapView.getZoomLevel());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();

		Bundle extras = intent.getExtras();

		if (extras != null) {
			// Do we show the current user position?
			isLive = extras.getBoolean(FlyMapActivity.EXTRA_ISLIVE, isLive);
		}

		int layoutId = isLive ? R.layout.map_view_live : R.layout.map_view_delayed;
		int mapviewId = isLive ? R.id.mapview_live : R.id.mapview_delayed;

		super.onCreate(savedInstanceState, layoutId, mapviewId);

		altitudeView = (AltitudeView) findViewById(R.id.altitude_view);

		addWaypoints();
		addPolyoverlay();
		perhapsAddFromUri();
		perhapsAddExtraTracklog();

		// get the last center/zoom
		// This may trigger a small flickering if 
		// current user location is available (GeeksvilleMapA will
		// zoom on this location immediately)
		GagglePrefs prefs = new GagglePrefs(this);
		final int latE6 = prefs.getMapCenterZoom_Lat();
		final int lonE6 = prefs.getMapCenterZoom_Lon();
		final int zoom = prefs.getMapCenterZoom_Zoom();
		if (latE6 != -1 && lonE6 != -1 && zoom != -1) {
			final GeoPoint center = new GeoPoint(latE6,  lonE6);
			mapView.getController().setCenter(center);
			mapView.getController().setZoom(zoom);
		}
		if (isLive){
			showCurrentPosition(true);
		}
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("airspace_enable", false)){
			enableAirspaceManagement();
		}
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("weather_stations_enable", false)){
			Log.d("FMA", "weather enabled on start");
			enableWeatherStations();
		}
	}

	/**
	 * See if the user wants us to open an IGC file
	 */
	private void perhapsAddFromUri() {
		Uri uri = getIntent().getData();
		String action = getIntent().getAction();

		if (uri != null && action != null && action.equals(Intent.ACTION_VIEW)) {
			// See if we can read the file
			try {
				GagglePrefs prefs = new GagglePrefs(this);
			    if (prefs.isFlurryEnabled())
				  FlurryAgent.onEvent("IGC view start");

				InputStream s = AndroidUtil.getFromURI(this, uri);

				IGCReader iread = new IGCReader("gps", s);
				LocationList loclist = iread.toLocationList();
				iread.close();
				
			    if (prefs.isFlurryEnabled())
				  FlurryAgent.onEvent("IGC view success");

				// Show the points
				altitudeView.setLocs(loclist);
				mapView.getOverlays().add(createTracklogOverlay(loclist));
			} catch (IOException ex) {
				GagglePrefs prefs = new GagglePrefs(this);
				if (prefs.isFlurryEnabled())
				  FlurryAgent.onEvent("IGC view fail");

				// FIXME - move this alert goo into a standard localized utility
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.unable_to_open_igc_file);
				builder.setMessage(ex.toString());
				builder.setPositiveButton(R.string.okay, null);

				AlertDialog alert = builder.create();
				alert.show();
			}
		}
	}

	private void addWaypoints() {
		wptOver = new WaypointOverlay(this, mapView);
		mapView.getOverlays().add(wptOver);
	}

	private void addPolyoverlay() {
		polyOver = new PolygonOverlay(this);
		mapView.getOverlays().add(polyOver);
	}

	/**
	 * If a tracklog was added to our intent, then show it
	 */
	private void perhapsAddExtraTracklog() {
		GagglePrefs prefs = new GagglePrefs(this);
		if (prefs.isFlurryEnabled())
		  FlurryAgent.onEvent("View delayed");

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();

		// Parse our params
		if (extras != null) {
			// Any stored tracklogs? if yes, then show them
			Bundle locBundle = extras.getBundle(FlyMapActivity.EXTRA_TRACKLOG);

			// Draw our track log on top of the (big) overlay icons?
			if (locBundle != null) {
				LocationList locs = new LocationList(locBundle);

				altitudeView.setLocs(locs);
				mapView.getOverlays().add(createTracklogOverlay(locs));
			}
		}
	}
//	
//	private void handleAirspaceTrigger(){
//		if (mapView.getOnScrollChangeListener() == null){
//			mapView.setOnScrollChangeListener(new AirspaceScrollListener(polyOver));
//		} else {
//			mapView.setOnScrollChangeListener(null);
//		}
//	}

	private void enableAirspaceManagement(){
//		if (mapView.getOnScrollChangeListener() == null){
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			airspace_scroll_lst = new AirspaceScrollListener(mapView, polyOver, prefs);
			mapView.setMapListener(airspace_scroll_lst);
			airspace_scroll_lst.update();
//			mapView.setOnScrollChangeListener(airspace_scroll_lst);
//		}
		// else { error } => should not get enable if already enabled
	}
	private void disableAirspaceManagement(){
//		if (mapView.getOnScrollChangeListener() != null){
			//mapView.setOnScrollChangeListener(null);
		mapView.setMapListener(null);
			airspace_scroll_lst = null;
//		}
		// else { error } => should not get disable if already disabled
	}

	private void enableWeatherStations(){
		StationProviderable provider = new FFVLStationProvider(this);
		Log.d("FMA", "Enabling weather");
		weather_overlay = new WeatherStationsOverlay(this, mapView, provider);
		mapView.getOverlays().add(weather_overlay);
	}

	private void disableWeatherStations(){
		Log.d("FMA", "Disabling weather");
		mapView.getOverlays().remove(weather_overlay);
	}

	/**
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//
//		case R.id.airspacemode_menu:
////			handleAirspaceTrigger();
//			return true;
//		}
//		return super.onOptionsItemSelected(item);
//	}
	
	@Override
	protected void onPause() {
		super.onPause();

		wptOver.onPause();

		// Remove any live tracklog (in case the next instance doesn't want it -
		// YUCK)
		if (liveTracklogOverlay != null) {
			liveList.deleteObserver(this);

			mapView.getOverlays().remove(liveTracklogOverlay);
			liveTracklogOverlay = null;
		}
	}

	/**
	 * FIXME, move all the live map stuff into a subclass?
	 */
	private TracklogOverlay liveTracklogOverlay;

	@Override
	protected void onResume() {
		super.onResume();

		Units.instance.setFromPrefs(this);
		wptOver.onResume();

		// Show our latest live tracklog
		if (liveList != null && isLive) {
			LocationList locs = liveList; // Super skanky, find a better way
			// to pass in ptrs FIXME.

			liveList.addObserver(this);

			altitudeView.setLocs(locs);
			liveTracklogOverlay = new TracklogOverlay(this, locs);
			mapView.getOverlays().add(liveTracklogOverlay);
		}

//		new AirspaceLoader().execute();
	}

	/**
	 * Create an overlay for a canned tracklog
	 * 
	 * @param locBundle
	 */
	private TracklogOverlay createTracklogOverlay(final LocationList locs) {

		// Show a tracklog
		TracklogOverlay tlog = new TracklogOverlay(this, locs);

		// Center on the tracklog (we do this in a callback, because OSM only
		// works after the view has been layed out
		if (locs.numPoints() > 0) {
			mapView.setPostLayout(new Runnable() {

				@Override
				public void run() {
					MapController control = mapView.getController();

					control.setCenter(locs.getGeoPoint(0));

					// it is about 80 feet to one second of a degree. So if we
					// want to
					// limit the max default zoom to about 500 feet
					double maxZoomFeet = 2500;
					double feetPerSecond = 80;
					double minDegrees = (maxZoomFeet / feetPerSecond) / 60 / 60;
					int minDegreesE6 = (int) (minDegrees * 1e6);

					// FIXME - the following is busted on OSM, it must be called
					// later -
					// after the view has been sized and fully created
					control.zoomToSpan(Math.max(locs.latitudeSpanE6(), minDegreesE6), Math.max(locs
							.longitudeSpanE6(), minDegreesE6));
					// control.setZoom(11); // hack till fixed for OSM
				}
			});
		}

		return tlog;
	}

	/**
	 * Use our spiffy heading display
	 * 
	 * @see com.geeksville.maps.GeeksvilleMapActivity#createLocationOverlay()
	 */
	@Override
	protected MyLocationOverlay createLocationOverlay() {
		return new CenteredMyLocationOverlay(this, mapView);
	}

	/**
	 * Called when our tracklog gets updated
	 * 
	 * @param observable
	 * @param data
	 */
	@Override
	public void update(Observable observable, Object data) {
		mapView.postInvalidateDelayed(1000);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals("airspace_enable")){
			if (sharedPreferences.getBoolean(key, false)){
				enableAirspaceManagement();
			} else {
				disableAirspaceManagement();
			}
		} else if (key.equals("weather_stations_enable")) {
			if (sharedPreferences.getBoolean(key, false)){
				enableWeatherStations();
			} else {
				disableWeatherStations();
			}
		} else {
			if (airspace_scroll_lst != null)
				airspace_scroll_lst.refresh(sharedPreferences, key);
		}
	}
}

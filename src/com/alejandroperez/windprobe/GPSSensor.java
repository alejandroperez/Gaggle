package com.alejandroperez.windprobe;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

public class GPSSensor extends PollingSensor<GeoCoordinates> {
	private final LocationSource locationSource;
	public GPSSensor() {
		super(200);
		locationSource = new LocationSource();
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected Sample<GeoCoordinates> getLatestSample() {
		Location loc = locationSource.getLatestLocation();
		GeoCoordinates gc = new GeoCoordinates(loc.getLatitude(), loc.getLongitude(), loc.getAltitude());
		GeoCoordinates error = new GeoCoordinates(loc.getAccuracy(), loc.getAccuracy(), loc.getAccuracy());
		return new Sample<GeoCoordinates>(gc, error, loc.getTime());
		loc.
	}
	
	private static class LocationSource extends Service {
		private final LocationManager manager;
		private final String providerStr;
		
		public LocationSource() {
			manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = new Criteria();
		    providerStr = manager.getBestProvider(criteria, false);
		}
		
		Location getLatestLocation() {
			return manager.getLastKnownLocation(providerStr);
		}
		
		@Override
		public IBinder onBind(Intent arg0) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}

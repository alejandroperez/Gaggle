package com.alejandroperez.windprobe;

public class GeoCoordinates{
	private double latitude, longitude, altitude;
	
	GeoCoordinates(double latitude, double longitude, double altitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}
	
	double getLatitude() {
		return latitude;
	}
	double getLongitude() {
		return longitude;
	}
	double getAltitude() {
		return altitude;
	}

}

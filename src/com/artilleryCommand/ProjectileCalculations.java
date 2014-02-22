package com.artilleryCommand;

import android.hardware.SensorManager;
import android.location.LocationManager;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;


/** TODO this class will have to be redone in order to use proper math for global scale projectile translation
 * currently it only uses basic Spherical trig which becomes inaccurate at long ranges (which the game will eventually use);
 * however, it is useful for prototype/proof of concept stage.
 * 
 * @author David
 *
 */
public class ProjectileCalculations {

		/**
		 * @param startPoint - origin of the projectile
		 * @param d - in kilometers
		 * @param bearing - in degrees
		 * @return
		 */
		public static LatLng computeShellLandPoint(LatLng startPoint, double dist, double bearing){

			dist = dist / 6371;  
			bearing = bearing * Math.PI / 180;  

			double lat1 = startPoint.latitude * Math.PI / 180;
			double lon1 = startPoint.longitude * Math.PI / 180;

			double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) + 
					Math.cos(lat1) * Math.sin(dist) * Math.cos(bearing));

			double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(dist) *
					Math.cos(lat1), 
					Math.cos(dist) - Math.sin(lat1) *
					Math.sin(lat2));

			return new LatLng(lat2 * 180 / Math.PI, lon2 * 180 / Math.PI);
		}

		public static double computeShellDistanceTraveled(double shellAngleDegrees, double velocity){

			double landPointX = 0;
			double shellVelocity = velocity; //Random number that should come from trigger press/time button held down.
			double worldGravity = 9.80665; //standard gravity is 9.80665 m/s^2 
			double shellAngleRadians=shellAngleDegrees*0.0174532925;

			//((2v^2) / g) * cos(a) * sin(a) 
			landPointX = ( (2* Math.pow(shellVelocity,2.0)) / worldGravity) * Math.cos(shellAngleRadians) * Math.sin(shellAngleRadians);

			return landPointX;
		}
}

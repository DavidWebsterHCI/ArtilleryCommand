package com.artilleryCommand;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.telephony.CellLocation;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

/**
 * Comment placeholder.
 */
public class MapActivity extends FragmentActivity implements SensorEventListener, LocationListener {

	private static final double POWER_UPDATE_RATE = 10;
	private static final double PROJECTILE_TRAVEL_UPDATE_RATE = 10;
	private double currentLat;
	private double currentLon;

	private GoogleMap map;

	//Timer variables (used for power calculations)
	private double startTime;
	private double endTime;
	private double timeFireButtonWasHeldDown;
	private LocationManager mLocManager;

	//Sensor variables used for orientation/projectile calculations
	private SensorManager sensorManager;
	private float[] lastMagFields;
	private float[] lastAccels;
	private float[] rotationMatrix = new float[16];
	private float[] orientation = new float[4];

	//Variables to fix random noise by averaging tilt values
	private Filter [] filters = { new Filter(), new Filter(), new Filter() };
	private float lastPitch;
	private float lastYaw;
	private float lastRoll;
    private Handler mHandler = new Handler();

    private Runnable powerBarUpdater = new Runnable() {
        public void run() {
            updateStatus();
            mHandler.postDelayed(powerBarUpdater, (long)POWER_UPDATE_RATE);
        }
    };

    private Runnable projectileTravelUpdater = new Runnable() {
        public void run() {
            updateProjectileTravel();
            mHandler.postDelayed(projectileTravelUpdater, (long)PROJECTILE_TRAVEL_UPDATE_RATE );
        }
    };

    /**
     * default constructor.
     */
    public MapActivity() {

    }

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_layout);

		//Lock to landscape
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		//init and register listeners needed for gameplay functionality
		initAndRegisterListeners();

		map = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		locationUpdate();

		final Button fire = (Button)findViewById(R.id.fireButton);
		fire.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN) {
					startTime = System.currentTimeMillis();
					startRepeatingTask("powerbar");
				} else if (action == MotionEvent.ACTION_UP) {
					endTime = System.currentTimeMillis();
					timeFireButtonWasHeldDown = ((endTime - startTime) / POWER_UPDATE_RATE);
					stopRepeatingTask("powerbar");
				}

				return false;
			}
		});

		fire.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				//force device orientation computations in case no movement has happened (extremely unlikely, but still a fail safe)
				computeOrientation();

				LatLng currentLoc = new LatLng(currentLat, currentLon);
				
				LatLng projectileLandLoc = ProjectileCalculations.computeShellLandPoint(currentLoc,
                        ProjectileCalculations.computeShellDistanceTraveled(lastRoll, timeFireButtonWasHeldDown), lastYaw - 90); //-90 rotates it so 0 degree is north instead of east.
				
				map.addMarker(new MarkerOptions() 
				.position(projectileLandLoc) 
				.title("Hit here!"));
				
				// Add a thin red line from London to New York.
				   Polyline line = map.addPolyline(new PolylineOptions()
				       .add(currentLoc, projectileLandLoc)
				       .width(5)
				       .color(Color.RED));
			}
		});
	}

	private void updateProjectileTravel() {
		
	}
	
	private void updateStatus() {
		TextView tv = (TextView)findViewById(R.id.powerMeterTextView);
		timeFireButtonWasHeldDown = ((System.currentTimeMillis() - startTime) / POWER_UPDATE_RATE);
		tv.setText("Power: " + Double.toString(timeFireButtonWasHeldDown)); 
	}

	void startRepeatingTask(String str) {
		if (str.equalsIgnoreCase("powerbar")) {
            powerBarUpdater.run();
        } else if (str.equalsIgnoreCase("projectiletravel")) {
            projectileTravelUpdater.run();
        }
	}

	void stopRepeatingTask(String str) {
		if (str.equalsIgnoreCase("powerbar")) {
            mHandler.removeCallbacks(powerBarUpdater);
        } else if (str.equalsIgnoreCase("projectiletravel")) {
            mHandler.removeCallbacks(projectileTravelUpdater);
        }
	}

    /**
     * Comment placeholder.
     */
	public void locationUpdate() {
        CellLocation.requestLocationUpdate();
	}

    /**
     * Comment placeholder.
     * @param str .
     */
	public void onProviderDisabled(String str) {
		// TODO Auto-generated method stub
		Toast.makeText(MapActivity.this, "Gps Disabled", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

    /**
     * Comment placeholder.
     * @param str .
     */
	public void onProviderEnabled(String str) {
		// TODO Auto-generated method stub

	}

    /**
     * Comment placeholder.
     * @param str .
     * @param i .
     * @param bundle .
     */
	public void onStatusChanged(String str, int i, Bundle bundle) {
		if (i == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			Toast.makeText(MapActivity.this, 
					"LocationProvider.TEMPORARILY_UNAVAILABLE", 
					Toast.LENGTH_SHORT).show(); 
		} else if (i == LocationProvider.OUT_OF_SERVICE) {
			Toast.makeText(MapActivity.this, 
					"LocationProvider.OUT_OF_SERVICE", Toast.LENGTH_SHORT).show(); 
		}
	}

    /**
     * Comment placeholder.
     * @param location .
     */
	public void onLocationChanged(Location location) {
		currentLat = location.getLatitude();
		currentLon = location.getLongitude();

		Log.v("Test", "IGA" + "Lat" + currentLat + "   Lng" + currentLon);

		if (location != null) {
			mLocManager.removeUpdates(this);
		}
		Toast.makeText(this, "Lat" + currentLat + "   Lng" + currentLon,
				Toast.LENGTH_SHORT).show();

		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(currentLat, currentLon), 10);
		map.moveCamera(cameraUpdate);

		map.addMarker(new MarkerOptions()
		.position(new LatLng(currentLat, currentLon))
		.title("Current"));

	}

	private void initAndRegisterListeners() {
		mLocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}

	private void unregisterListeners() {
		sensorManager.unregisterListener(this);

		mLocManager.removeUpdates(this);
	}

	@Override
	public void onDestroy() {
		unregisterListeners();
		super.onDestroy();
	}

	@Override
	public void onPause() {
		unregisterListeners();
		super.onPause();
	}

	@Override
	public void onResume() {
		initAndRegisterListeners();
		super.onResume();
	}

    /**
     * Comment placeholder.
     * @param sensor .
     * @param accuracy .
     */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//TODO auto gen stub.
	}

    /**
     * Comment placeholder.
     * @param event .
     */
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accel(event);
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mag(event);
		}
	}

	private void accel(SensorEvent event) {
		if (lastAccels == null) {
			lastAccels = new float[3];
		}

		System.arraycopy(event.values, 0, lastAccels, 0, 3);

		if (lastMagFields != null) {
			computeOrientation();
		}
	}

	private void mag(SensorEvent event) {
		if (lastMagFields == null) {
			lastMagFields = new float[3];
		}

		System.arraycopy(event.values, 0, lastMagFields, 0, 3);

		if (lastAccels != null) {
			computeOrientation();
		}
	}

	private void computeOrientation() {
		if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccels, lastMagFields)) {
			SensorManager.getOrientation(rotationMatrix, orientation);

			/* 1 radian = 57.2957795 degrees */
			/* [0] : yaw, rotation around z axis
			 * [1] : pitch, rotation around x axis
			 * [2] : roll, rotation around y axis */
			float yaw = orientation[0] * 57.2957795f;
			float pitch = orientation[1] * 57.2957795f;
			float roll = orientation[2] * 57.2957795f;

			lastYaw = filters[0].append(yaw);
			lastPitch = filters[1].append(pitch);
			lastRoll = filters[2].append(roll);
		}
	}

	private double computeShellDistanceTraveled(double shellAngleDegrees, double velocity) {

		double landPointX = 0;
		double shellVelocity = velocity; //Random number that should come from trigger press/time button held down.
		double worldGravity = 9.80665; //standard gravity is 9.80665 m/s^2 
		double shellAngleRadians = shellAngleDegrees * 0.0174532925;

		//((2v^2) / g) * cos(a) * sin(a) 
		landPointX = ((2 * Math.pow(shellVelocity, 2.0)) / worldGravity) * Math.cos(shellAngleRadians) * Math.sin(shellAngleRadians);
		Context context = getApplicationContext();
		CharSequence text = Double.toString(landPointX);

		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();

		return landPointX;
	}

	private class Filter {
		static final int AVERAGE_BUFFER = 1;
		float[] arr = new float[AVERAGE_BUFFER];
		int idx;

        /**
         * Default constructor.
         */
        public Filter() {

        }

		public float append(float val) {
			arr[idx] = val;
			idx++;
			if (idx == AVERAGE_BUFFER) {
                idx = 0;
            }
			return avg();
		}
		public float avg() {
			float sum = 0;
			for (float x: arr) {
                sum += x;
            }
			return sum / AVERAGE_BUFFER;
		}
	}
}



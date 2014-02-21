package com.artilleryCommand;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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

public class MapActivity extends FragmentActivity implements SensorEventListener, LocationListener {

	private final double POWER_UPDATE_RATE = 10;
	private double currentLat, currentLon;

	private GoogleMap map;

	//Timer variables (used for power calculations)
	private double startTime = 0;
	private double endTime = 0;
	private double timeFireButtonWasHeldDown = 0;
	private LocationManager mLocManager;

	//Sensor variables used for orientation/projectile calculations
	SensorManager m_sensorManager;
	private float[] m_lastMagFields;
	private float[] m_lastAccels;
	private float[] m_rotationMatrix = new float[16];
	private float[] m_orientation = new float[4];

	//Variables to fix random noise by averaging tilt values
	private Filter [] m_filters = { new Filter(), new Filter(), new Filter() };
	private float m_lastPitch = 0.f;
	private float m_lastYaw = 0.f;
	private float m_lastRoll = 0.f;


	private Handler mHandler = new Handler();


	/*^***********^*/
	/*START METHODS*/
	/*^***********^*/
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_layout);

		//Lock to landscape
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		//init and register listeners needed for gameplay functionality
		initAndRegisterListeners();

		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		locationUpdate();



		final Button fire = (Button) findViewById(R.id.fireButton);
		fire.setOnTouchListener(new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {

				int action = event.getAction();
				if (action == MotionEvent.ACTION_DOWN) {
					startTime = System.currentTimeMillis();
					startRepeatingTask();
				} 
				else if (action == MotionEvent.ACTION_UP) {
					endTime = System.currentTimeMillis();
					timeFireButtonWasHeldDown = ((endTime-startTime) / POWER_UPDATE_RATE);
					stopRepeatingTask();
				}

				return false;
			}


		});

		fire.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				//force device orientation computations in case no movement has happened (extremely unlikely, but still a fail safe)
				computeOrientation();

				LatLng currentLoc = new LatLng(currentLat, currentLon);

				map.addMarker(new MarkerOptions() 
				.position(computeShellLandPoint(currentLoc, computeShellDistanceTraveled(m_lastRoll, timeFireButtonWasHeldDown), m_lastYaw-90)) // -90 rotates it so 0 degree is north instead of east.
				.title("Hit here!"));
			}
		});
	}


	Runnable mStatusChecker = new Runnable() {
		public void run() {
			updateStatus(); //this function can change value of mInterval. 
			mHandler.postDelayed(mStatusChecker, (long) POWER_UPDATE_RATE);
		}
	};

	private void updateStatus()
	{
		TextView tv = (TextView) findViewById(R.id.powerMeterTextView);
		timeFireButtonWasHeldDown = ((System.currentTimeMillis()-startTime) / POWER_UPDATE_RATE);
		tv.setText("Power: " + Double.toString(timeFireButtonWasHeldDown)); 
	}

	void startRepeatingTask() {
		mStatusChecker.run(); 
	}

	void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
	}

	/**
	 * @param startPoint - origin of the projectile
	 * @param d - in kilometers
	 * @param bearing - in degrees
	 * @return
	 */
	private LatLng computeShellLandPoint(LatLng startPoint, double dist, double bearing){

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

	public void locationUpdate()
	{
		CellLocation.requestLocationUpdate();
	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(MapActivity.this, "Gps Disabled", Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		if(arg1 == 
				LocationProvider.TEMPORARILY_UNAVAILABLE) { 
			Toast.makeText(MapActivity.this, 
					"LocationProvider.TEMPORARILY_UNAVAILABLE", 
					Toast.LENGTH_SHORT).show(); 
		} 
		else if(arg1== LocationProvider.OUT_OF_SERVICE) { 
			Toast.makeText(MapActivity.this, 
					"LocationProvider.OUT_OF_SERVICE", Toast.LENGTH_SHORT).show(); 
		} 

	}


	public void onLocationChanged(Location location) {
		currentLat = location.getLatitude();
		currentLon = location.getLongitude();

		Log.v("Test", "IGA" + "Lat" + currentLat + "   Lng" + currentLon);

		if(location != null)
		{
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
		mLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
		m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

		mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	}

	private void unregisterListeners() {
		m_sensorManager.unregisterListener(this);

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


	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//TODO auto gen stub thing.
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accel(event);
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mag(event);
		}
	}

	private void accel(SensorEvent event) {
		if (m_lastAccels == null) {
			m_lastAccels = new float[3];
		}

		System.arraycopy(event.values, 0, m_lastAccels, 0, 3);

		if (m_lastMagFields != null) {
			computeOrientation();
		}
	}

	private void mag(SensorEvent event) {
		if (m_lastMagFields == null) {
			m_lastMagFields = new float[3];
		}

		System.arraycopy(event.values, 0, m_lastMagFields, 0, 3);

		if (m_lastAccels != null) {
			computeOrientation();
		}
	}

	private void computeOrientation() {
		if (SensorManager.getRotationMatrix(m_rotationMatrix, null, m_lastAccels, m_lastMagFields)) {
			SensorManager.getOrientation(m_rotationMatrix, m_orientation);

			/* 1 radian = 57.2957795 degrees */
			/* [0] : yaw, rotation around z axis
			 * [1] : pitch, rotation around x axis
			 * [2] : roll, rotation around y axis */
			float yaw = m_orientation[0] * 57.2957795f;
			float pitch = m_orientation[1] * 57.2957795f;
			float roll = m_orientation[2] * 57.2957795f;

			m_lastYaw = m_filters[0].append(yaw);
			m_lastPitch = m_filters[1].append(pitch);
			m_lastRoll = m_filters[2].append(roll);
		}
	}

	private double computeShellDistanceTraveled(double shellAngleDegrees, double velocity){

		double landPointX = 0;
		double shellVelocity = velocity; //Random number that should come from trigger press/time button held down.
		double worldGravity = 9.80665; //standard gravity is 9.80665 m/s^2 
		double shellAngleRadians=shellAngleDegrees*0.0174532925;

		//((2v^2) / g) * cos(a) * sin(a) 
		landPointX = ( (2* Math.pow(shellVelocity,2.0)) / worldGravity) * Math.cos(shellAngleRadians) * Math.sin(shellAngleRadians);
		Context context = getApplicationContext();
		CharSequence text = Double.toString(landPointX);

		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();

		return landPointX;
	}

	private class Filter {
		static final int AVERAGE_BUFFER = 1;
		float []m_arr = new float[AVERAGE_BUFFER];
		int m_idx = 0;

		public float append(float val) {
			m_arr[m_idx] = val;
			m_idx++;
			if (m_idx == AVERAGE_BUFFER)
				m_idx = 0;
			return avg();
		}
		public float avg() {
			float sum = 0;
			for (float x: m_arr)
				sum += x;
			return sum / AVERAGE_BUFFER;
		}

	}

}



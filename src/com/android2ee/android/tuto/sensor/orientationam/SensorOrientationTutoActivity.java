/**<ul>
 * <li>SensorOrientation</li>
 * <li>com.android2ee.android.tuto.sensor.orientationam</li>
 * <li>27 nov. 2011</li>
 * 
 * <li>======================================================</li>
 *
 * <li>Projet : Mathias Seguy Project</li>
 * <li>Produit par MSE.</li>
 *
 /**
 * <ul>
 * Android Tutorial, An <strong>Android2EE</strong>'s project.</br> 
 * Produced by <strong>Dr. Mathias SEGUY</strong>.</br>
 * Delivered by <strong>http://android2ee.com/</strong></br>
 *  Belongs to <strong>Mathias Seguy</strong></br>
 ****************************************************************************************************************</br>
 * This code is free for any usage but can't be distribute.</br>
 * The distribution is reserved to the site <strong>http://android2ee.com</strong>.</br>
 * The intelectual property belongs to <strong>Mathias Seguy</strong>.</br>
 * <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * 
 * *****************************************************************************************************************</br>
 *  Ce code est libre de toute utilisation mais n'est pas distribuable.</br>
 *  Sa distribution est reservée au site <strong>http://android2ee.com</strong>.</br> 
 *  Sa propriété intellectuelle appartient à <strong>Mathias Seguy</strong>.</br>
 *  <em>http://mathias-seguy.developpez.com/</em></br> </br>
 * *****************************************************************************************************************</br>
 */
package com.android2ee.android.tuto.sensor.orientationam;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Surface;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

/**
 * @author Mathias Seguy (Android2EE)
 * @goals
 *        This class aims to:
 *        <ul>
 *        <li></li>
 *        </ul>
 */
public class SensorOrientationTutoActivity extends Activity implements SensorEventListener {
	// see :http://developer.android.com/reference/android/hardware/SensorEvent.html
	// see also:http://developer.android.com/reference/android/hardware/SensorManager.html
	/**
	 * The Tag for the Log
	 */
	private static final String LOG_TAG = "SensorsOrientation";

	/******************************************************************************************/
	/** Current sensor value **************************************************************************/
	/******************************************************************************************/
	/**
	 * Current value of the accelerometer
	 */
	float x, y, z;

	float[] acceleromterVector=new float[3];
	float[] magneticVector=new float[3];
	float[] resultMatrix=new float[9];
	float[] values=new float[3];
	/******************************************************************************************/
	/** View **************************************************************************/
	/******************************************************************************************/
	/**
	 * The Layout Parameter used to add Name in LilContent
	 */
	LinearLayout.LayoutParams lParamsName;
	/**
	 * The layout within the graphic is draw
	 */
	LinearLayout xyAccelerationLayout;
	/**
	 * The view that draw the graphic
	 */
	OrientationView xyAccelerationView;
	/**
	 * The progress bar that displays the X, Y, Z value of the vector
	 */
	ProgressBar pgbX, pgbY, pgbZ;
	/******************************************************************************************/
	/** Sensors and co **************************************************************************/
	/******************************************************************************************/
	/** * The sensor manager */
	SensorManager sensorManager;
	/**
	 * The magnetic field
	 */
	Sensor magnetic;
	/**
	 * The accelerometer
	 */
	Sensor accelerometer;

	/******************************************************************************************/
	/** Manage life cycle **************************************************************************/
	/******************************************************************************************/
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// build the GUI
		setContentView(R.layout.main);
		// instantiate the progress bars
		pgbX = (ProgressBar) findViewById(R.id.progressBarX);
		pgbY = (ProgressBar) findViewById(R.id.progressBarY);
		pgbZ = (ProgressBar) findViewById(R.id.progressBarZ);
		// the azimut max value
		pgbX.setMax((int) 360);
		// the pitch max value
		pgbY.setMax((int) 90);
		// the roll max value
		pgbZ.setMax((int) 180);
		// Then manage the sensors and listen for changes
		// Instantiate the SensorManager
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		// Instantiate the magnetic sensor and its max range
		magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		// Instantiate the accelerometer
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		// Then build the GUI:
		// Build the acceleration view
		// first retrieve the layout:
		xyAccelerationLayout = (LinearLayout) findViewById(R.id.layoutOfXYAcceleration);
		// then build the view
		xyAccelerationView = new OrientationView(this);
		// define the layout parameters and add the view to the layout
		LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		// add the view in the layout
		xyAccelerationLayout.addView(xyAccelerationView, layoutParam);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// unregister every body
		sensorManager.unregisterListener(this, accelerometer);
		sensorManager.unregisterListener(this, magnetic);
		// and don't forget to pause the thread that redraw the xyAccelerationView
		xyAccelerationView.isPausing.set(true);
		super.onPause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		/*
		 * It is not necessary to get accelerometer events at a very high
		 * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
		 * automatic low-pass filter, which "extracts" the gravity component
		 * of the acceleration. As an added benefit, we use less power and
		 * CPU resources.
		 */
		sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_UI);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		// and don't forget to re-launch the thread that redraws the xyAccelerationView
		xyAccelerationView.isPausing.set(false);
		super.onResume();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// Log.d(LOG_TAG, "onDestroy()");
		// kill the thread
		xyAccelerationView.isRunning.set(false);
		super.onDestroy();
	}

	/******************************************************************************************/
	/** ProgressBar update **************************************************************************/
	/******************************************************************************************/

	/**
	 * Update the value of the progressbar according to the value of the sensor
	 * we use the secondary progress to display negative value
	 */
	private void updateProgressBar() {
		if (x > 0) {
			pgbX.setProgress((int) x);
		} else {
			pgbX.setSecondaryProgress(-1 * (int) x);
		}
		if (y > 0) {
			pgbY.setProgress((int) y);
		} else {
			pgbY.setSecondaryProgress(-1 * (int) y);
		}
		if (z > 0) {
			pgbZ.setProgress((int) z);
		} else {
			pgbZ.setSecondaryProgress(-1 * (int) z);
		}
	}

	/******************************************************************************************/
	/** SensorEventListener **************************************************************************/
	/******************************************************************************************/

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Nothing to do
	}

/*
 * (non-Javadoc)
 * 
 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
 */
@Override
public void onSensorChanged(SensorEvent event) {
	
	if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
		acceleromterVector=event.values;
	} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
		magneticVector=event.values;
	}
	if (acceleromterVector != null && magneticVector != null) {

            /* Create rotation Matrix */
		float[] rotationMatrix = new float[9];
		if (SensorManager.getRotationMatrix(rotationMatrix, null,
				acceleromterVector, magneticVector)) {

                /* Compensate device orientation */
			// http://android-developers.blogspot.de/2010/09/one-screen-turn-deserves-another.html
			float[] remappedRotationMatrix = new float[9];
			switch (getWindowManager().getDefaultDisplay()
					.getRotation()) {
				case Surface.ROTATION_0:
					SensorManager.remapCoordinateSystem(rotationMatrix,
							SensorManager.AXIS_X, SensorManager.AXIS_Y,
							remappedRotationMatrix);
					break;
				case Surface.ROTATION_90:
					SensorManager.remapCoordinateSystem(rotationMatrix,
							SensorManager.AXIS_Y,
							SensorManager.AXIS_MINUS_X,
							remappedRotationMatrix);
					break;
				case Surface.ROTATION_180:
					SensorManager.remapCoordinateSystem(rotationMatrix,
							SensorManager.AXIS_MINUS_X,
							SensorManager.AXIS_MINUS_Y,
							remappedRotationMatrix);
					break;
				case Surface.ROTATION_270:
					SensorManager.remapCoordinateSystem(rotationMatrix,
							SensorManager.AXIS_MINUS_Y,
							SensorManager.AXIS_X, remappedRotationMatrix);
					break;
			}

                /* Calculate Orientation */
			float results[] = new float[3];
			SensorManager.getOrientation(remappedRotationMatrix,
					results);

                /* Get measured value */
			float current_measured_bearing = (float) (results[0] * 180 / Math.PI);
			x =(float) Math.toDegrees(results[0]);
			y =(float) Math.toDegrees(results[1]);
			z =(float) Math.toDegrees(results[2]);

		}
	}
//	SensorManager.getRotationMatrix(resultMatrix, null, acceleromterVector, magneticVector);
////	//Remap the coordonate system using those values:
////	float[] outR=new float[9];
////	SensorManager.remapCoordinateSystem(resultMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
//
//	SensorManager.getOrientation(resultMatrix, values);
//	// the azimuts
//	x =(float) Math.toDegrees(values[0]);
//	// the pitch
//	y = (float) Math.toDegrees(values[1]);
//	// the roll
//	z = (float) Math.toDegrees(values[2]);
	// update the progressBar
	updateProgressBar();
}
}

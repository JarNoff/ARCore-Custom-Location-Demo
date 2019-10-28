/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.hellosceneform;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.Camera;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
//import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.OnLocationUpdatedListener;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity implements SensorEventListener {
    private FusedLocationProviderClient fusedLocationClient;
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION };
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.0;
  private int azimuth = 0;
  private SensorManager sensorManager;
  private float[] rMat = new  float[9];
  private float[] orientation = new float[3];
  private float[] lastAccelerometer = new float[3];
  private float[] lastMagnetometer = new float[3];
  private boolean lastAccelSet = false;
  private boolean lastMagnetometerSet = false;
  private ArFragment arFragment;
  private ModelRenderable andyRenderable;
  private boolean isArPlaced = false;

  private Sensor accelerometer;
  private Sensor magnetometer;
  private Sensor rotationV;

  private Scene scene;
  private Camera camera;

  private CompletableFuture<ViewRenderable> exampleLayout;

  @Override
  public void onSensorChanged(SensorEvent event)
  {
      if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
      {
          SensorManager.getRotationMatrixFromVector(rMat, event.values);
          azimuth = (int)(Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
      }
      if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
      {
          System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
      } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
      {
          System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
          lastMagnetometerSet = true;
      }

      if (lastAccelSet && lastMagnetometerSet) {
          SensorManager.getRotationMatrix(rMat, null, lastAccelerometer, lastMagnetometer);
          SensorManager.getOrientation(rMat, orientation);
          azimuth = (int)(Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
      }

      Activity act = this;

      if (!isArPlaced) {
          isArPlaced = true;
          final Handler handler = new Handler();
          final Runnable r = new Runnable() {
              @Override
              public void run() {
                  fusedLocationClient.getLastLocation()
                          .addOnSuccessListener(HelloSceneformActivity.this, new OnSuccessListener<Location>() {
                              @Override
                              public void onSuccess(Location location) {
                                  // Got last known location. In some rare situations this can be null.
                                  if (location != null) {
                                      // Logic to handle location object

                                      //Add in something here to not place AR objects if they are not in a good location.
                                      //Or get list of possible locations from firebase and check which are in range here.

                                      placeARByLocation(location, new LatLng(34.002989, -81.0160088));
                                      placeARByLocation(location, new LatLng(34.003200, -81.020000));
                                      placeARByLocation(location, new LatLng(34.001500, -81.014000));
                                  }
                                  else
                                  {
                                      Log.v("Warning:", "Failed to get location");
                                  }
                              }
                          });
              }
          };
          handler.postDelayed(r, 2000);
      }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy)
  {

  }

  @Override
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }
      setContentView(R.layout.activity_ux);

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
    //sensorManager.
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    startSensor();
    arFragment.getPlaneDiscoveryController().hide();
    arFragment.getPlaneDiscoveryController().setInstructionView(null);
    scene = arFragment.getArSceneView().getScene();
    camera = scene.getCamera();
  }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }

  private void addPointByXYZ(float x, float y, float z)
  {
      Activity act = this;
      //ViewRenderable view = ViewRenderable.builder().setView(this, R.layout.samplenode).build();//.thenAccept(new CompletableFuture<AnchorNode>() {
      ViewRenderable.builder().setView(this, R.layout.samplenode).build().thenAccept((renderable) -> {
          View inflatedView = getLayoutInflater().inflate(R.layout.samplenode, null);
          ImageView img = (ImageView)inflatedView.findViewById(R.id.imageView);
          //I need food.
          AnchorNode node = new AnchorNode();
          node.setRenderable(renderable);
          scene.addChild(node);
          node.setWorldPosition(new Vector3(x, y, z));

          Vector3 cameraPosition = scene.getCamera().getWorldPosition();
          Vector3 direction = Vector3.subtract(cameraPosition, node.getWorldPosition());
          Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
          node.setWorldRotation(lookRotation);
      });
  }

  private double bearing(Location A, Location B)
  {
      if (A != null && B != null) {
          double LatA = A.getLatitude() * Math.PI / 180.0;
          double LonA = A.getLongitude() * Math.PI / 180.0;
          double LatB = B.getLatitude() * Math.PI / 180.0;
          double LonB = B.getLongitude() * Math.PI / 180.0;

          double deltaOmega = Math.log(Math.tan((LatB / 2) + (Math.PI / 4)) / Math.tan((LatA / 2) + (Math.PI / 4)));
          double deltaLong = Math.abs(LonA - LonB);

          return Math.atan2(deltaLong, deltaOmega);
      }
      //runtime error in the future?
      return 15.0;
  }

  private void placeARByLocation(Location myLocation, LatLng target)
  {
    Location targetLoc = new Location("");
    targetLoc.setLatitude(target.latitude);
    targetLoc.setLongitude(target.longitude);

    double degree = (360 - (bearing(myLocation, targetLoc) * 180 / Math.PI));
    double distant = 3.0;

    double y = 0.0;
    double x = distant * Math.cos(Math.PI * degree / 180.0);
    double z = -1 * distant * Math.sin(Math.PI * degree / 180.0);

      addPointByXYZ((float)x, (float)y, (float)z);


      //Toast toast = new Toast(this, "Compass: $azimuth, Degree: $degree", Toast.LENGTH_LONG);
      //toast.show();
      if (myLocation != null && targetLoc != null) {
          Log.i("ARCore_MyLat", String.valueOf(myLocation.getLatitude()));
          Log.i("ARCore_MyLon", String.valueOf(myLocation.getLongitude()));
          Log.i("ARCore_TargetLat", String.valueOf(targetLoc.getLatitude()));
          Log.i("ARCore_TargetLon", String.valueOf(targetLoc.getLongitude()));
          Log.i("ARCore_COMPASS", String.valueOf(azimuth));
          Log.i("ARCore_Degree", String.valueOf(degree));
          Log.i("ARCore_X", String.valueOf(x));
          Log.i("ARCore_Y", String.valueOf(y));
          Log.i("ARCore_Z", String.valueOf(z));
      }
      else
      {
          Log.e("Null Location:", "Location is null");
      }
  }

  private void startSensor()
  {
      if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
          accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
          magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
          sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
          sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
      } else {
          rotationV = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
          sensorManager.registerListener(this, rotationV, SensorManager.SENSOR_DELAY_UI);
      }
  }

  private void stopSensor()
  {
      sensorManager.unregisterListener(this, accelerometer);
      sensorManager.unregisterListener(this, magnetometer);
  }

  @Override
  public void onPause()
  {
      super.onPause();
      stopSensor();
  }

  @Override
  public void onResume()
  {
      super.onResume();
      startSensor();
  }



}

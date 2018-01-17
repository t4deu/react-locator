package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;

import com.reactlibrary.LocationMapper;

public class ReactLocatorModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private FusedLocationProviderClient locationClient;
  private static final String TAG = "REACT_LOCATOR";

  public ReactLocatorModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "ReactLocator";
  }

  @ReactMethod
  public void getCurrentLocation(final Promise promise) {
    try {
      this.getLocationClient()
        .getLastLocation()
        .addOnCompleteListener(new OnCompleteListener<Location>() {
          @Override
          public void onComplete(@NonNull Task<Location> task) {
            if (task.isSuccessful() && task.getResult() != null) {
              Location location = task.getResult();

              promise.resolve(LocationMapper.toWriteableMap(location));
            } else {
              promise.reject(TAG, task.getException());
            }
          }
        });
    } catch (Exception ex) {
      promise.reject(TAG, ex.toString());
    }
  }

  private FusedLocationProviderClient getLocationClient() {
    if (locationClient == null) {
      locationClient = LocationServices.getFusedLocationProviderClient(getReactApplicationContext());
    }

    return locationClient;
  }
}
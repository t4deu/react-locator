package com.t4deu.RNLocator;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import android.location.Location;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;
import android.support.v4.app.ActivityCompat;

public class LocationUtils {
  public static WritableMap locationToMap(Location location) {
    WritableMap output = Arguments.createMap();
    output.putDouble("latitude", location.getLatitude());
    output.putDouble("longitude", location.getLongitude());
    output.putDouble("altitude", location.getAltitude());
    output.putDouble("accuracy", location.getAccuracy());
    output.putDouble("heading", location.getBearing());
    output.putDouble("speed", location.getSpeed());
    output.putDouble("timestamp", location.getTime());

    return output;
  }

  public static boolean checkLocationPermissions(ReactApplicationContext context) {
    // Stop here since there is no run-time permissions 
    // and checkSelfPermission does not exists before this version
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return true;
    }

    return (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
  }

  public static boolean checkPlayServices(ReactApplicationContext context) {
    int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);

    return result == ConnectionResult.SUCCESS || result == ConnectionResult.SERVICE_UPDATING;
  }
}
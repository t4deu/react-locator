package com.reactlibrary;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.os.Looper;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.reactlibrary.LocationMapper;

public class ReactLocatorModule extends ReactContextBaseJavaModule {

  private static final String TAG = "REACT_LOCATOR";
  private static final String LOCATION_EVENT = "location";
  private static final long SMALLEST_DISPLACEMENT = 0;
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

  private final ReactApplicationContext reactContext;
  private FusedLocationProviderClient locationClient;
  private SettingsClient settingsClient;
  private LocationRequest locationRequest;
  private LocationSettingsRequest locationSettingsRequest;
  private LocationCallback locationCallback;
  private Boolean tracking;

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
      if (!checkLocationSettings(promise)) {
        return;
      }

      this.getLocationClient().getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
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

  private void setup() {
    tracking = true;
    createLocationRequest();
    createLocationCallback();
  }

  @ReactMethod
  public void startTracking(final Promise promise) {
    // stop if already tracking or have incorrect settings
    if (tracking || !checkLocationSettings(promise)) {
      return;
    }

    setup();
    getLocationClient().requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    promise.resolve();
  }

  @Override
  public void onHostDestroy() {
  }

  private void sendEvent(String eventName, @Nullable WritableMap params) {
    getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,
        params);
  }

  private FusedLocationProviderClient getLocationClient() {
    if (locationClient == null) {
      locationClient = LocationServices.getFusedLocationProviderClient(getReactApplicationContext());
    }

    return locationClient;
  }

  private void createLocationRequest() {
    locationRequest = new LocationRequest();

    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT)
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  private void createLocationCallback() {
    locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);

        Location location = locationResult.getLastLocation();

        sendEvent(LOCATION_EVENT, LocationMapper.toWriteableMap(location));
      }
    };
  }

  private boolean checkLocationSettings(final Promise promise) {
    if (checkPermissions() && isGooglePlayServicesAvailable()) {
      return true;
    }

    return false;
  }

  private boolean checkPermissions() {
    int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    return permissionState == PackageManager.PERMISSION_GRANTED;
  }

  private boolean isGooglePlayServicesAvailable() {
    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
    int status = googleApiAvailability.isGooglePlayServicesAvailable(getReactApplicationContext());
    if (status != ConnectionResult.SUCCESS) {
      if (googleApiAvailability.isUserResolvableError(status)) {
        googleApiAvailability.getErrorDialog(activity, status, 2404).show();
      }
      return false;
    }
    return true;
  }
}
package com.t4deu.RNLocator;

import java.util.HashMap;
import java.util.Map;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.BaseActivityEventListener;
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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.app.Activity;
import android.location.Location;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;

public class RNGeoLocatorModule extends ReactContextBaseJavaModule {

  private static final String TAG = "RNGEOLOCATOR";
  private static final int REQUEST_CHECK_SETTINGS = 0x1;
  private static final String LOCATION_EVENT = "location";

  private static final String PERMISSION_DENIED_ERROR = "PERMISSION_DENIED_ERROR";
  private static final String PLAY_SERVICE_NOT_AVAILABLE_ERROR = "PLAY_SERVICE_NOT_AVAILABLE_ERROR";
  private static final String LOCATION_UNAVAILABLE_ERROR = "LOCATION_UNAVAILABLE_ERROR";

  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mSettingsRequest;
  private Promise mPromise;

  public RNGeoLocatorModule(ReactApplicationContext reactContext) {
    super(reactContext);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(reactContext);
    mSettingsClient = LocationServices.getSettingsClient(reactContext);
    reactContext.addActivityEventListener(new RNLocationModuleActivityListener());
  }

  @Override
  public String getName() {
    return "RNGeoLocator";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("LOCATION_EVENT", LOCATION_EVENT);
    return constants;
  }

  @ReactMethod
  public void getCurrentLocation(ReadableMap options, final Promise promise) {
    ReactApplicationContext context = getReactApplicationContext();
    LocationOptions locationOptions = LocationOptions.fromReactMap(options);
    mPromise = promise;

    createLocationRequest(locationOptions);
    createLocationSettingsRequest();

    if (!LocationUtils.checkLocationPermissions(context)) {
      promise.reject(PERMISSION_DENIED_ERROR, "Location permissions not granted.");
      return;
    }

    if (!LocationUtils.checkPlayServices(context)) {
      promise.reject(PLAY_SERVICE_NOT_AVAILABLE_ERROR, "Google play services not available.");
      return;
    }

    mSettingsClient.checkLocationSettings(mSettingsRequest)
        .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
      @Override
      public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        // All location settings are satisfied
        requestLocation();
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        int statusCode = ((ApiException) e).getStatusCode();
        switch (statusCode) {
          case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
            try {
              // Location settings are not satisfied. Attempting to resolve
              ResolvableApiException resolvable = (ResolvableApiException) e;
              resolvable.startResolutionForResult(getCurrentActivity(), REQUEST_CHECK_SETTINGS);
            } catch (SendIntentException sie) {
              promise.reject(PERMISSION_DENIED_ERROR, "Could not ask for required permissions.");
            }
            break;
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
          default:
            promise.reject(PERMISSION_DENIED_ERROR, "Location permissions not granted.");
            break;
          }
      }
    });
  }

  private void requestLocation() {
    mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
      @Override
      public void onComplete(@NonNull Task<Location> task) {
        Location location = task.getResult();
        if (location != null) {
          mPromise.resolve(LocationUtils.locationToMap(location));
        } else {
          requestSingleUpdate();
        }
      }
    });
  }

  private void requestSingleUpdate() {
    // All location settings are satisfied
    mFusedLocationClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();
        mFusedLocationClient.removeLocationUpdates(this);
        mPromise.resolve(LocationUtils.locationToMap(location));
      }
    }, Looper.getMainLooper());
  }

  private void createLocationRequest(LocationOptions options) {
    mLocationRequest = new LocationRequest();

    mLocationRequest.setInterval(options.getInterval());
    mLocationRequest.setFastestInterval(options.getFastestInterval());
    mLocationRequest.setSmallestDisplacement(options.getDistanceFilter());
    mLocationRequest.setPriority(options.getAccuracy());
  }
  
  private void createLocationSettingsRequest() {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mSettingsRequest = builder.build();
  }

  private class RNLocationModuleActivityListener extends BaseActivityEventListener {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      switch (requestCode) {
      // Check for the integer request code originally supplied to startResolutionForResult().
      case REQUEST_CHECK_SETTINGS:
        switch (resultCode) {
        case Activity.RESULT_OK:
          // User agreed to make required location settings changes
          if (mFusedLocationClient != null && mPromise != null) {
            requestLocation();
          }
          break;
        case Activity.RESULT_CANCELED:
          // User chose not to make required location settings changes
          mPromise.reject(PERMISSION_DENIED_ERROR, "Location permissions not granted.");
          break;
        }
        break;
      }
    }
  }

}

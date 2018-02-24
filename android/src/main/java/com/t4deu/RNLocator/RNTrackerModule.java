package com.t4deu.RNLocator;
/*
import java.util.Map;
import java.util.HashMap;

import android.location.Location;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.os.Looper;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.google.android.gms.tasks.Task;

import com.reactlibrary.LocationMapper;

public class RNLocatorModule extends ReactContextBaseJavaModule {

  private static final String TAG = "RNLOCATOR";
  private static final int REQUEST_CHECK_SETTINGS = 0x1;
  private static final String LOCATION_EVENT = "location";

  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationCallback mLocationChangeCallback;
  private Boolean mTracking = false;

  public RNLocatorModule(ReactApplicationContext reactContext) {
    super(reactContext);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(reactContext);
    reactContext.addActivityEventListener(new RNLocationModuleActivityListener());
  }

  @Override
  public String getName() {
    return "ReactLocator";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("LOCATION_EVENT", LOCATION_EVENT);
    return constants;
  }

  @ReactMethod
  public void getCurrentLocation(ReadableMap options, final Promise promise) {
    Context context = getReactApplicationContext().getBaseContext();
    LocationOptions locationOptions = LocationOptions.fromReactMap(options);

    if (!checkLocationPermissions(context)) {
      promise.reject(TAG, LocationError.buildError(PositionError.PERMISSION_DENIED, "No location provider available."));
      return;
    }

    if (!checkPlayServices(context)) {
      promise.reject(TAG,
          LocationError.buildError(PositionError.POSITION_UNAVAILABLE, "No location provider available."));
      return;
    }


    LocationRequest request = createLocationRequest(locationOptions);
    LocationSettingsRequest settingsRequest = createLocationSettingsRequest(request);

    mSettingsClient.checkLocationSettings(settingsRequest)
        .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
      @Override
      public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        // All location settings are satisfied
        mFusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
          @Override
          public void onLocationResult(LocationResult locationResult) {
            Location location = locationResult.getLastLocation();
            mFusedLocationClient.removeLocationUpdates(googleApiClient, this);
            promise.resolve(locationToMap(location));
          }
        }, Looper.getMainLooper());
      }
    }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        int statusCode = ((ApiException) e).getStatusCode();
        switch (statusCode) {
        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
          try {
            // Location settings are not satisfied. Attempting to upgrade
            ResolvableApiException resolvable = (ResolvableApiException) e;
            resolvable.startResolutionForResult(getCurrentActivity(), REQUEST_CHECK_SETTINGS);
          } catch (IntentSender.SendIntentException sie) {
            // TODO error alert
            // PendingIntent unable to execute request
          }
          break;
        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
          // TODO error alert
          // Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings.";
        }

        updateUI();
      }
    });

    try {
      mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
        @Override
        public void onComplete(@NonNull Task<Location> task) {
          Location location = task.getResult();
          if (location != null) {
            promise.resolve(locationToMap(location));
          } else {
            requestSingleUpdate(locationOptions, promise);
          }
        }
      });
    } catch (SecurityException e) {
      promise.reject(TAG, task.getException());
      //throwLocationPermissionMissing(e);
    }
  }

  private void requestSingleUpdate(LocationOptions locationOptions, final Promise promise) {
    LocationRequest request = createLocationRequest(locationOptions);
    LocationSettingsRequest settingsRequest = createLocationSettingsRequest(request);

    mSettingsClient.checkLocationSettings(settingsRequest)
        .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
          @Override
          public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            // All location settings are satisfied
            mFusedLocationClient.requestLocationUpdates(request, new LocationCallback() {
              @Override
              public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                mFusedLocationClient.removeLocationUpdates(googleApiClient, this);
                promise.resolve(locationToMap(location));
              }
            }, Looper.getMainLooper());
          }
        }).addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            int statusCode = ((ApiException) e).getStatusCode();
            switch (statusCode) {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
              try {
                // Location settings are not satisfied. Attempting to upgrade
                ResolvableApiException resolvable = (ResolvableApiException) e;
                rresolvable.startResolutionForResult(getCurrentActivity(), REQUEST_CHECK_SETTINGS);
              } catch (IntentSender.SendIntentException sie) {
                // TODO error alert
                // PendingIntent unable to execute request
              }
              break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
              // TODO error alert
              // Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings.";
            }

            updateUI();
          }
        });
  }

  private void setup(ReadableMap options) {
    tracking = true;
    createLocationRequest();
    createLocationCallback();
  }

  @ReactMethod
  public void startTracking(final ReadableMap options, final Promise promise) {
    Context context = getReactApplicationContext().getBaseContext();
    // stop if already tracking or have incorrect settings
    if (tracking || !checkLocationSettings(promise)) {
      return;
    }

    setup(options);
    mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationChangeCallback, Looper.myLooper());
    promise.resolve("");
  }

  private void sendEvent(String eventName, @Nullable WritableMap params) {
    getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,
        params);
  }

  /**
  * Sets up the location request. Android has two location request settings:
  * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
  * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
  * the AndroidManifest.xml.
  * <p/>
  * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
  * interval (5 seconds), the Fused Location Provider API returns location updates that are
  * accurate to within a few feet.
  * <p/>
  * These settings are appropriate for mapping applications that show real-time location
  * updates.
  *
  private void createLocationRequest(LocationOptions options) {
    mLocationRequest = new LocationRequest();

    mLocationRequest.setInterval(UPDATE_INTERVAL);
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
    mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  private void createLocationCallback() {
    mLocationChangeCallback = new mLocationChangeCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);

        Location location = locationResult.getLastLocation();

        sendEvent(LOCATION_EVENT, LocationMapper.toWriteableMap(location));
      }
    };
  }

  /**
  * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
  * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
  * if a device has the needed location settings.
  *
  private void createLocationSettingsRequest(LocationRequest request) {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(request);
    return builder.build();
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
          //TODO getUserLocation();
          break;
        case Activity.RESULT_CANCELED:
          // User chose not to make required location settings changes
          //TODO "Location settings are not satisfied."
          break;
        }
        break;
      }
    }
  }

  private class static LocationOptions

  {
  private static final long DEFAULT_DISTANCE_FILTER = 10;
  private static final long DEFAULT_UPDATE_INTERVAL = 10000;
  private static final int DEFAULT_LOCATION_ACCURACY = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

  private final int accuracy;
  private final long distanceFilter;
  private final long interval;

  private LocationOptions(
      int accuracy,
      long distanceFilter,
      long interval) {
      this.accuracy = accuracy;
      this.distanceFilter = distanceFilter;
      this.interval = interval;
    }

  private static LocationOptions fromReactMap(ReadableMap map) {
    boolean accuracy = map.hasKey("highAccuracy") && map.getBoolean("highAccuracy")
        ? LocationRequest.PRIORITY_HIGH_ACCURACY
        : DEFAULT_LOCATION_ACCURACY;
    long interval = map.hasKey("interval") ? (long) map.getDouble("interval") : DEFAULT_DISTANCE_FILTER;
    float distanceFilter = map.hasKey("distanceFilter") ? (float) map.getDouble("distanceFilter")
        : DEFAULT_DISTANCE_FILTER;

    return new LocationOptions(accuracy, distanceFilter, interval);
  }
}}
*/
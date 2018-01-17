package com.t4deu.RNLocator;

import com.google.android.gms.location.LocationRequest;

import android.content.pm.PackageManager;
import com.facebook.react.bridge.ReadableMap;

public class LocationOptions
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

  public static LocationOptions fromReactMap(ReadableMap map) {
    int accuracy = map.hasKey("highAccuracy") && map.getBoolean("highAccuracy")
        ? LocationRequest.PRIORITY_HIGH_ACCURACY
        : DEFAULT_LOCATION_ACCURACY;
    long distanceFilter = map.hasKey("distanceFilter") ? (long) map.getDouble("distanceFilter")
        : DEFAULT_DISTANCE_FILTER;
    long interval = map.hasKey("interval") ? (long) map.getDouble("interval") : DEFAULT_DISTANCE_FILTER;

    return new LocationOptions(accuracy, distanceFilter, interval);
  }

  public int getAccuracy() {
    return accuracy;
  }

  public long getDistanceFilter() {
    return distanceFilter;
  }

  public long getInterval() {
    return interval;
  }

  public long getFastestInterval() {
    return interval / 2;
  }
}
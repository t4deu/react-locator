package com.reactlibrary;

import android.location.Location;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class LocationMapper {
    public static WritableMap toWriteableMap(Location location) {
        WritableMap output = Arguments.createMap();
        
        output.putDouble("time", new Long(location.getTime()).doubleValue());
        output.putDouble("latitude", location.getLatitude());
        output.putDouble("longitude", location.getLongitude());
        output.putDouble("accuracy", location.getAccuracy());
        output.putDouble("speed", location.getSpeed());
        output.putDouble("altitude", location.getAltitude());
        output.putDouble("bearing", location.getBearing());

        return output;
    }
}
package com.dreamerindia.clbd;

/**
 * Created by user on 06-02-2015.
 */

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationLocality {
    private static final String TAG = "LocationLocality";

    public static void getLocalityFromLocation(final double latitude, final double longitude,
                                               final Context context, final Handler handler) {

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        String result = null;
        try {
            List<Address> addressList = geocoder.getFromLocation(
                    latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 1; i++) {
                    sb.append(address.getAddressLine(i));
                }
                result = sb.toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable connect to Geocoder", e);
        } finally {
            Message message = Message.obtain();
            message.setTarget(handler);
            if (result != null) {
                message.what = 1;
                Bundle bundle = new Bundle();
                bundle.putString("address", result);
                message.setData(bundle);
            } else {
                message.what = 1;
                Bundle bundle = new Bundle();
                result = "";
                bundle.putString("address", result);
                message.setData(bundle);
            }
            message.sendToTarget();
        }
    }
}

